package com.skeletonarmy.marrow.bezier;

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
        BezierPathGenerator.resetToDefaults();
    }

    @Test
    public void optimizationFindsGlobalMinimum() {
        List<Pose> targets = Arrays.asList(
                new Pose(START_X + 4, START_Y + 1),
                new Pose(START_X - 3, START_Y + 6),
                new Pose(START_X + 2, START_Y - 5)
        );
        double k = 10.0;
        BezierConfig config = new BezierConfig().turnCostWeight(k);

        List<Pose> chosen = OrderOptimizer.order(new Point(START_X, START_Y), 0, targets, config);
        double chosenCost = OrderOptimizer.pathCost(new Point(START_X, START_Y), 0, chosen, k);

        double trueMinCost = Double.MAX_VALUE;
        List<List<Pose>> allPerms = new ArrayList<>();
        permute(new ArrayList<>(targets), 0, allPerms);
        for (List<Pose> perm : allPerms) {
            trueMinCost = Math.min(trueMinCost, OrderOptimizer.pathCost(new Point(START_X, START_Y), 0, perm, k));
        }

        assertEquals(trueMinCost, chosenCost, EPS);
    }

    @Test
    public void offsetMathIsCorrect() {
        List<Pose> targets = Collections.singletonList(new Pose(START_X + 20, START_Y));
        
        BezierPath path = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
                .targets(targets)
                .generate()
                .getPath();

        Point pathEnd = path.get(1.0);
        assertEquals(START_X + 20 - 5.0, pathEnd.getX(), EPS);
    }

    @Test
    public void wideIntakeStraightensPath() {
        List<Pose> targets = Arrays.asList(
                new Pose(START_X + 20, START_Y),
                new Pose(START_X + 40, START_Y + 3),
                new Pose(START_X + 60, START_Y)
        );

        BezierConfig pointConfig = new BezierConfig().width(0).reach(0);
        BezierConfig wideConfig = new BezierConfig().width(18).reach(0);

        BezierPathGenerator.setConfig(pointConfig);
        BezierPath pointPath = BezierPathGenerator.builder().start(new Pose(START_X, START_Y, 0)).targets(targets).generate().getPath();

        BezierPathGenerator.setConfig(wideConfig);
        BezierPath widePath = BezierPathGenerator.builder().start(new Pose(START_X, START_Y, 0)).targets(targets).generate().getPath();

        assertTrue("Wide intake should be straighter (shorter length)",
                widePath.approxLength(50) < pointPath.approxLength(50) - 0.1);
    }

    @Test
    public void reachMathIsCorrect() {
        List<Pose> targets = Collections.singletonList(new Pose(START_X + 20, START_Y));

        BezierPathGenerator.setConfig(new BezierConfig().reach(7.0).width(0));

        BezierPath path = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
                .targets(targets)
                .generate()
                .getPath();

        Point pathEnd = path.get(1.0);
        assertEquals(START_X + 20 - 7.0, pathEnd.getX(), EPS);
    }

    @Test
    public void orderedBuilderHonorsProvidedSequence() {
        Pose far = new Pose(START_X + 50, START_Y);
        Pose near = new Pose(START_X + 10, START_Y);
        
        BezierPath reordered = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
                .addTarget(far)
                .addTarget(near)
                .generate()
                .getPath();
        
        assertEquals(far.getX() - 5.0, reordered.get(1.0).getX(), EPS);

        BezierPath forced = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
                .addTarget(far)
                .addTarget(near)
                .ordered()
                .generate()
                .getPath();
        
        assertEquals(near.getX() + 5.0, forced.get(1.0).getX(), EPS);
    }

    @Test
    public void intakeTouchesEveryPose() {
        List<Pose> targets = Arrays.asList(
                new Pose(START_X + 20, START_Y),
                new Pose(START_X + 40, START_Y + 10),
                new Pose(START_X + 60, START_Y - 5)
        );

        BezierConfig config = new BezierConfig().reach(5).width(8);
        BezierPathGenerator.setConfig(config);

        BezierPath path = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
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

            assertTrue("pose " + i + " should be captured by the intake",
                    BezierPathGenerator.isCapturedByIntake(center, heading, config.getReach(), config.getWidth(), target));
        }
    }

    private static void permute(List<Pose> arr, int k, List<List<Pose>> out) {
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
