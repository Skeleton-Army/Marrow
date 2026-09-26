package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class IntakePathingTest {

    private static final double EPS = 1e-6;
    private static final double START_X = 72;
    private static final double START_Y = 72;

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    @Test
    public void optimizationFindsGlobalMinimum() {
        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 4, START_Y + 1),
                new PathPose(START_X - 3, START_Y + 6),
                new PathPose(START_X + 2, START_Y - 5)
        );
        double k = 10.0;
        PathConfig config = new PathConfig().turnCostWeight(k);

        List<PathPose> chosen = TargetOrderer.order(new Point(START_X, START_Y), 0, targets, config);
        double chosenCost = TargetOrderer.pathCost(new Point(START_X, START_Y), 0, chosen, k);

        double trueMinCost = Double.MAX_VALUE;
        List<List<PathPose>> allPerms = new ArrayList<>();
        permute(new ArrayList<>(targets), 0, allPerms);
        for (List<PathPose> perm : allPerms) {
            trueMinCost = Math.min(trueMinCost, TargetOrderer.pathCost(new Point(START_X, START_Y), 0, perm, k));
        }

        assertEquals(trueMinCost, chosenCost, EPS);
    }

    @Test
    public void pathGoesThroughTarget() {
        List<PathPose> targets = Collections.singletonList(new PathPose(START_X + 20, START_Y));
        
        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .targets(targets)
                .generate()
                .getPath();

        Point pathEnd = path.get(1.0);
        assertEquals(START_X + 20, pathEnd.getX(), EPS);
    }

    @Test
    public void wideIntakeStraightensPath() {
        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 20, START_Y),
                new PathPose(START_X + 40, START_Y + 3),
                new PathPose(START_X + 60, START_Y)
        );

        PathConfig pointConfig = new PathConfig().width(0);
        PathConfig wideConfig = new PathConfig().width(18);

        Weaver.setConfig(pointConfig);
        PathRoute pointPath = Weaver.builder().start(new PathPose(START_X, START_Y, 0)).targets(targets).generate().getPath();

        Weaver.setConfig(wideConfig);
        PathRoute widePath = Weaver.builder().start(new PathPose(START_X, START_Y, 0)).targets(targets).generate().getPath();

        assertTrue("Wide intake should be straighter (shorter length)",
                widePath.approxLength(50) < pointPath.approxLength(50) - 0.1);
    }

    @Test
    public void wideIntakeStraightensFirstTarget() {
        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 20, START_Y + 3),
                new PathPose(START_X + 60, START_Y)
        );

        PathConfig pointConfig = new PathConfig().width(0);
        PathConfig wideConfig = new PathConfig().width(18);

        Weaver.setConfig(pointConfig);
        PathRoute pointPath = Weaver.builder().start(new PathPose(START_X, START_Y, 0)).targets(targets).generate().getPath();

        Weaver.setConfig(wideConfig);
        PathRoute widePath = Weaver.builder().start(new PathPose(START_X, START_Y, 0)).targets(targets).generate().getPath();

        assertTrue("Wide intake should straighten the first target (shorter length)",
                widePath.approxLength(50) < pointPath.approxLength(50) - 0.1);
    }

    @Test
    public void orderedBuilderHonorsProvidedSequence() {
        PathPose far = new PathPose(START_X + 50, START_Y);
        PathPose near = new PathPose(START_X + 10, START_Y);
        
        PathRoute reordered = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(far)
                .addTarget(near)
                .generate()
                .getPath();
        
        assertEquals(far.getX(), reordered.get(1.0).getX(), EPS);

        PathRoute forced = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(far)
                .addTarget(near)
                .ordered()
                .generate()
                .getPath();
        
        assertEquals(near.getX(), forced.get(1.0).getX(), EPS);
    }

    @Test
    public void intakeTouchesEveryPose() {
        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 20, START_Y),
                new PathPose(START_X + 40, START_Y + 10),
                new PathPose(START_X + 60, START_Y - 5)
        );

        PathConfig config = new PathConfig().width(8);
        Weaver.setConfig(config);

        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .targets(targets)
                .ordered()
                .generate()
                .getPath();

        int poseCount = targets.size();
        for (int i = 0; i < poseCount; i++) {
            double t = (double) (i + 1) / poseCount;
            Point center = path.get(t);
            double heading = path.getHeading(t);
            Point target = new Point(targets.get(i).getX(), targets.get(i).getY());

            assertTrue("PathPose " + i + " should be captured by the intake",
                    Weaver.isCapturedByIntake(center, heading, config.getWidth(), target));
        }
    }

    @Test
    public void closePerpendicularTargetsContinueStraight() {
        PathConfig config = new PathConfig().width(18);
        Weaver.setConfig(config);

        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 20, START_Y),
                new PathPose(START_X + 40, START_Y - 5),
                new PathPose(START_X + 40, START_Y + 5)
        );

        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .targets(targets)
                .ordered()
                .generate()
                .getPath();

        for (int i = 0; i <= 100; i++) {
            Point p = path.get(i / 100.0);
            assertEquals("Path should continue straight through the close perpendicular targets",
                    START_Y, p.getY(), 0.5);
        }

        double previousHeading = path.getHeading(0.0);
        for (int i = 1; i <= 100; i++) {
            double heading = path.getHeading(i / 100.0);
            double delta = Math.abs(heading - previousHeading);
            while (delta > Math.PI) delta = Math.abs(delta - 2 * Math.PI);
            assertTrue("Path should not contort (heading jump of " + Math.toDegrees(delta) + " deg)",
                    delta < Math.toRadians(5));
            previousHeading = heading;
        }

        double endX = path.get(1.0).getX();
        for (int i = 0; i < targets.size(); i++) {
            PathPose target = targets.get(i);
            assertTrue("Pose " + i + " should be within the intake band",
                    Math.abs(target.getY() - START_Y) <= config.getWidth() / 2.0);
            assertTrue("Pose " + i + " should be swept along the path",
                    target.getX() >= START_X - 0.5 && target.getX() <= endX + 0.5);
        }
    }

    @Test
    public void compactClusterContinuesStraight() {
        PathConfig config = new PathConfig().width(18);
        Weaver.setConfig(config);

        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 40, START_Y - 2),
                new PathPose(START_X + 41, START_Y - 1),
                new PathPose(START_X + 40, START_Y),
                new PathPose(START_X + 39, START_Y - 1)
        );

        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .targets(targets)
                .ordered()
                .generate()
                .getPath();

        double previousHeading = path.getHeading(0.0);
        double previousX = path.get(0.0).getX();
        for (int i = 1; i <= 200; i++) {
            Point p = path.get(i / 200.0);
            double heading = path.getHeading(i / 200.0);
            double delta = Math.abs(heading - previousHeading);
            while (delta > Math.PI) delta = Math.abs(delta - 2 * Math.PI);
            assertTrue("Cluster should not contort (heading jump of " + Math.toDegrees(delta) + " deg)",
                    delta < Math.toRadians(5));
            assertTrue("Cluster path should not backtrack in x", p.getX() >= previousX - 0.01);
            previousHeading = heading;
            previousX = p.getX();
        }
    }

    @Test
    public void obstacleAvoidanceStillCapturesEveryTarget() {
        PathConfig config = new PathConfig().width(18);
        Weaver.setConfig(config);

        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 40, START_Y - 2),
                new PathPose(START_X + 41, START_Y - 1),
                new PathPose(START_X + 40, START_Y),
                new PathPose(START_X + 39, START_Y - 1)
        );

        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .targets(targets)
                .ordered()
                .addObstacle(new CircleZone(new Point(START_X + 20, START_Y), 6))
                .generate()
                .getPath();

        for (int i = 0; i < targets.size(); i++) {
            PathPose target = targets.get(i);
            assertTrue("Pose " + i + " should still be captured despite the obstacle",
                    capturedSomewhere(path, config.getWidth(), new Point(target.getX(), target.getY())));
        }
    }

    private static boolean capturedSomewhere(PathRoute path, double width, Point target) {
        int samples = 4000;
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            Point p = path.get(t);
            double heading = path.getHeading(t);
            double rx = target.getX() - p.getX();
            double ry = target.getY() - p.getY();
            double along = rx * Math.cos(heading) + ry * Math.sin(heading);
            double lateral = Math.abs(rx * -Math.sin(heading) + ry * Math.cos(heading));
            if (lateral <= width / 2.0 + 1e-6 && Math.abs(along) < 0.5) {
                return true;
            }
        }
        return false;
    }

    private static void permute(List<PathPose> arr, int k, List<List<PathPose>> out) {
        if (k == arr.size()) {
            out.add(new ArrayList<>(arr));
            return;
        }
        for (int i = k; i < arr.size(); i++) {
            Collections.swap(arr, k, i);
            permute(arr, k + 1, out);
            Collections.swap(arr, k, i);
        }
    }
}
