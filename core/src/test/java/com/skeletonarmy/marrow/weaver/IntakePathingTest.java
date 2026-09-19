package com.skeletonarmy.marrow.weaver;

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
    public void offsetMathIsCorrect() {
        List<PathPose> targets = Collections.singletonList(new PathPose(START_X + 20, START_Y));
        
        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .targets(targets)
                .generate()
                .getPath();

        Point pathEnd = path.get(1.0);
        assertEquals(START_X + 20 - 5.0, pathEnd.getX(), EPS);
    }

    @Test
    public void wideIntakeStraightensPath() {
        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 20, START_Y),
                new PathPose(START_X + 40, START_Y + 3),
                new PathPose(START_X + 60, START_Y)
        );

        PathConfig pointConfig = new PathConfig().width(0).reach(0);
        PathConfig wideConfig = new PathConfig().width(18).reach(0);

        Weaver.setConfig(pointConfig);
        PathRoute pointPath = Weaver.builder().start(new PathPose(START_X, START_Y, 0)).targets(targets).generate().getPath();

        Weaver.setConfig(wideConfig);
        PathRoute widePath = Weaver.builder().start(new PathPose(START_X, START_Y, 0)).targets(targets).generate().getPath();

        assertTrue("Wide intake should be straighter (shorter length)",
                widePath.approxLength(50) < pointPath.approxLength(50) - 0.1);
    }

    @Test
    public void reachMathIsCorrect() {
        List<PathPose> targets = Collections.singletonList(new PathPose(START_X + 20, START_Y));

        Weaver.setConfig(new PathConfig().reach(7.0).width(0));

        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .targets(targets)
                .generate()
                .getPath();

        Point pathEnd = path.get(1.0);
        assertEquals(START_X + 20 - 7.0, pathEnd.getX(), EPS);
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
        
        assertEquals(far.getX() - 5.0, reordered.get(1.0).getX(), EPS);

        PathRoute forced = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(far)
                .addTarget(near)
                .ordered()
                .generate()
                .getPath();
        
        assertEquals(near.getX() + 5.0, forced.get(1.0).getX(), EPS);
    }

    @Test
    public void intakeTouchesEveryPose() {
        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 20, START_Y),
                new PathPose(START_X + 40, START_Y + 10),
                new PathPose(START_X + 60, START_Y - 5)
        );

        PathConfig config = new PathConfig().reach(5).width(8);
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
                    Weaver.isCapturedByIntake(center, heading, config.getReach(), config.getWidth(), target));
        }
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
