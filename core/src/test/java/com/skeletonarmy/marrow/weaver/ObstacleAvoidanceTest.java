package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.CompositeZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
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
        Weaver.resetToDefaults();
    }

    @Test
    public void pathBowsAroundObstacle() {
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(START_X + 24, START_Y), 6));
        
        PathRoute result = Weaver.builder()
                .start(new PathPose(START_X, START_Y))
                .end(new PathPose(START_X + 48, START_Y))
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        assertTrue("Path should clear obstacle",
                Weaver.isPathClear(result, obstacles, Weaver.getConfig().getClearance(), 200));
    }

    @Test
    public void pathBowsAroundPolygonObstacle() {
        List<Zone> obstacles = Collections.singletonList(
                new PolygonZone(new Point(START_X + 24, START_Y), 12, 12));

        PathRoute result = Weaver.builder()
                .start(new PathPose(START_X, START_Y))
                .end(new PathPose(START_X + 48, START_Y))
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        assertTrue("Path should clear polygon obstacle",
                Weaver.isPathClear(result, obstacles, Weaver.getConfig().getClearance(), 200));
    }

    @Test
    public void pathBowsAroundCompositeObstacle() {
        List<Zone> obstacles = Collections.singletonList(
                new CompositeZone(
                        new CircleZone(new Point(START_X + 20, START_Y), 4),
                        new PolygonZone(new Point(START_X + 28, START_Y), 8, 8)));

        PathRoute result = Weaver.builder()
                .start(new PathPose(START_X, START_Y))
                .end(new PathPose(START_X + 48, START_Y))
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        assertTrue("Path should clear composite obstacle",
                Weaver.isPathClear(result, obstacles, Weaver.getConfig().getClearance(), 200));
    }

    @Test
    public void footprintCheckingAccountsForRobotSize() {
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(START_X + 24, START_Y + 12), 4));

        double robotWidth = 18.0;
        double robotHeight = 18.0;
        PathConfig pointConfig = new PathConfig().clearance(2.0).robotWidth(0.0).robotHeight(0.0);
        PathConfig squareConfig = new PathConfig().clearance(2.0).robotWidth(robotWidth).robotHeight(robotHeight);

        Weaver.setConfig(pointConfig);
        PathRoute pointPath = Weaver.builder()
                .start(new PathPose(START_X, START_Y))
                .end(new PathPose(START_X + 48, START_Y))
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        Weaver.setConfig(squareConfig);
        PathRoute squarePath = Weaver.builder()
                .start(new PathPose(START_X, START_Y))
                .end(new PathPose(START_X + 48, START_Y))
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        double pointMaxDev = maxDev(pointPath.getSegments().get(0), START_Y);
        double squareMaxDev = maxDev(squarePath.getSegments().get(0), START_Y);
        assertTrue("Square robot should bow more than point robot", squareMaxDev > pointMaxDev);
    }

    @Test
    public void combinedIntakeAndAvoidanceWorks() {
        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 20, START_Y),
                new PathPose(START_X + 60, START_Y)
        );
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(START_X + 40, START_Y), 5));

        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0)) // Intake needs start heading
                .targets(targets)
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        assertTrue(Weaver.isPathClear(path, obstacles, 1.0, 300));
        
        Point lastTargetCenter = path.get(1.0);
        assertEquals(START_X + 60, lastTargetCenter.getX(), 1e-6);
    }

    @Test
    public void intakePreservesHeadingThroughObstacle() {
        List<PathPose> targets = Arrays.asList(
                new PathPose(72, 96),
                new PathPose(120, 48)
        );
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(72, 60), 6));

        Weaver.setConfig(new PathConfig().width(5));

        PathRoute path = Weaver.builder()
                .start(new PathPose(24, 24, 0))
                .targets(targets)
                .ordered()
                .addObstacle(obstacles.get(0))
                .generate()
                .getPath();

        Point end = path.get(1.0);
        assertEquals("Path should end at the last target even after routing around an obstacle",
                120.0, end.getX(), 1e-6);
        assertEquals(48.0, end.getY(), 1e-6);
        assertEquals("End heading should be preserved through obstacle avoidance",
                -Math.PI / 4.0, path.getHeading(1.0), 1e-6);
    }

    @Test
    public void pathClearsSecondOfTwoObstacles() {
        List<Zone> obstacles = Arrays.asList(
                new CircleZone(new Point(86, 86), 5),
                new CircleZone(new Point(114, 86), 5)
        );

        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(100, 100))
                .addTarget(new PathPose(128, 72))
                .addObstacle(obstacles.get(0))
                .addObstacle(obstacles.get(1))
                .generate()
                .getPath();

        assertTrue("Path should not cut through either obstacle",
                Weaver.isPathClear(path, obstacles, Weaver.getConfig().getClearance(), 400));
    }

    @Test
    public void overlappingObstaclesDoNotOverBow() {
        Zone obstacle = new CircleZone(new Point(START_X + 24, START_Y), 6);

        PathRoute singlePath = Weaver.builder()
                .start(new PathPose(START_X, START_Y))
                .end(new PathPose(START_X + 48, START_Y))
                .addObstacle(obstacle)
                .generate()
                .getPath();

        PathRoute overlapPath = Weaver.builder()
                .start(new PathPose(START_X, START_Y))
                .end(new PathPose(START_X + 48, START_Y))
                .addObstacle(obstacle)
                .addObstacle(new CircleZone(new Point(START_X + 24, START_Y), 6))
                .generate()
                .getPath();

        double singleDev = maxDev(singlePath.getSegments().get(0), START_Y);
        double overlapDev = maxDev(overlapPath.getSegments().get(0), START_Y);
        assertEquals("Overlapping obstacles should not bow farther than a single one",
                singleDev, overlapDev, 1e-6);
    }

    @Test
    public void pathReroutesAroundTouchingObstacleWall() {
        Weaver.setConfig(new PathConfig().width(0).robotWidth(12.0).robotHeight(12.0).clearance(4.0));

        List<Zone> obstacles = Arrays.asList(
                new CircleZone(new Point(66, 66), 6),
                new CircleZone(new Point(66, 78), 6)
        );

        PathRoute path = Weaver.builder()
                .start(new PathPose(24, 48, 0))
                .addTarget(new PathPose(120, 96))
                .addObstacle(obstacles.get(0))
                .addObstacle(obstacles.get(1))
                .generate()
                .getPath();

        assertTrue("Path should reroute around the touching obstacle wall instead of cutting through",
                Weaver.isPathClear(path, obstacles, 4.0, 12.0, 12.0, 400));
    }

    private double maxDev(PathCurve curve, double baselineY) {
        return curve.sample(100).stream()
                .mapToDouble(p -> Math.abs(p.getY() - baselineY))
                .max().orElse(0);
    }
}
