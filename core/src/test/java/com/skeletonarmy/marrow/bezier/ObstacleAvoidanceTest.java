package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ObstacleAvoidanceTest {

    private static final double START_X = 72;
    private static final double START_Y = 72;

    @Before
    public void setup() {
        BezierPathGenerator.resetToDefaults();
    }

    @Test
    public void pathBowsAroundObstacle() {
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(START_X + 24, START_Y), 6));
        
        BezierPath result = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y))
                .to(new Pose(START_X + 48, START_Y))
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        assertTrue("Path should clear obstacle",
                BezierPathGenerator.isPathClear(result, obstacles, BezierPathGenerator.getConfig().getClearance(), 200));
    }

    @Test
    public void footprintCheckingAccountsForRobotSize() {
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(START_X + 24, START_Y + 12), 4));

        double robotSize = 18.0;
        BezierConfig pointConfig = new BezierConfig().clearance(2.0).robotSize(0.0);
        BezierConfig squareConfig = new BezierConfig().clearance(2.0).robotSize(robotSize);

        BezierPathGenerator.setConfig(pointConfig);
        BezierPath pointPath = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y))
                .to(new Pose(START_X + 48, START_Y))
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        BezierPathGenerator.setConfig(squareConfig);
        BezierPath squarePath = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y))
                .to(new Pose(START_X + 48, START_Y))
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        double pointMaxDev = maxDev(pointPath.getSegments().get(0), START_Y);
        double squareMaxDev = maxDev(squarePath.getSegments().get(0), START_Y);
        assertTrue("Square robot should bow more than point robot", squareMaxDev > pointMaxDev);
    }

    @Test
    public void combinedIntakeAndAvoidanceWorks() {
        List<Waypoint> targets = Arrays.asList(
                new Waypoint(START_X + 20, START_Y),
                new Waypoint(START_X + 60, START_Y)
        );
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(START_X + 40, START_Y), 5));

        BezierPath path = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0)) // Intake needs start heading
                .waypoints(targets)
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        assertTrue(BezierPathGenerator.isPathClear(path, obstacles, 1.0, 300));
        
        Point lastTargetCenter = path.getSegments().get(1).getControlPoints().get(3);
        assertEquals(START_X + 60 - 5.0, lastTargetCenter.getX(), 1e-6);
    }

    @Test
    public void pathClearsSecondOfTwoObstacles() {
        List<Zone> obstacles = Arrays.asList(
                new CircleZone(new Point(86, 86), 5),
                new CircleZone(new Point(114, 86), 5)
        );

        BezierPath path = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
                .addWaypoint(new Waypoint(100, 100))
                .addWaypoint(new Waypoint(128, 72))
                .addObstacle(obstacles.get(0))
                .addObstacle(obstacles.get(1))
                .generate()
                .getPath();

        assertTrue("Path should not cut through either obstacle",
                BezierPathGenerator.isPathClear(path, obstacles, BezierPathGenerator.getConfig().getClearance(), 400));
    }

    private double maxDev(BezierCurve curve, double baselineY) {
        return curve.sample(100).stream()
                .mapToDouble(p -> Math.abs(p.getY() - baselineY))
                .max().orElse(0);
    }
}
