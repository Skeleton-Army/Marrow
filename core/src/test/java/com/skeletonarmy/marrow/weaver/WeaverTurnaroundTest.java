package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.Point;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests routes whose key points ask for a turn the fitted curve has to turn tighter than a
 * robot can follow, and the packed targets that used to hand the fitter sub-inch spans.
 */
public class WeaverTurnaroundTest {

    private static final double EPS = 1e-6;

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    /**
     * Returns the tightest turn the route makes, in inches: the smallest radius reached
     * anywhere along it, measured from how fast the direction of travel changes per inch.
     */
    private static double worstTurnRadius(PathRoute route) {
        int samples = 6000;
        double span = route.getSegmentCount();
        Point previous = null;
        double previousHeading = Double.NaN;
        double worst = Double.MAX_VALUE;

        for (int i = 0; i <= samples; i++) {
            Point point = route.get((double) i / samples * span);

            if (previous != null) {
                double travelled = point.distanceTo(previous);

                if (travelled > 1e-9) {
                    double heading = Math.atan2(point.getY() - previous.getY(), point.getX() - previous.getX());

                    if (!Double.isNaN(previousHeading)) {
                        double curvature = Math.abs(normalize(heading - previousHeading)) / travelled;

                        if (curvature > 1e-9) {
                            worst = Math.min(worst, 1.0 / curvature);
                        }
                    }

                    previousHeading = heading;
                }
            }

            previous = point;
        }

        return worst;
    }

    private static double normalize(double angle) {
        double normalized = angle % (2 * Math.PI);

        if (normalized > Math.PI) {
            normalized -= 2 * Math.PI;
        } else if (normalized <= -Math.PI) {
            normalized += 2 * Math.PI;
        }

        return normalized;
    }

    /** A compact clump of targets south of a robot facing north. */
    private static List<Point> cluster() {
        return Arrays.asList(new Point(24, 62), new Point(24.4, 61.7), new Point(23.7, 61.9), new Point(24.1, 62.3));
    }

    private static PathResult generateFacingAway() {
        Weaver.setConfig(new PathConfig().intakeWidth(13.0).robotWidth(17.0).robotHeight(16.0));

        return Weaver.builder()
                .start(new Point(24, 70, Math.toRadians(90.0)))
                .targets(cluster())
                .maxTargets(3)
                .build();
    }

    @Test
    public void generate_startFacingAway_turnsAroundOnARealArc() {
        // Arrange: the robot faces north with its targets behind it, which used to fit a
        // hook whose tangent flipped within a fraction of an inch.
        PathResult actual = generateFacingAway();

        // Act
        PathRoute route = actual.getPath();

        // Assert: the pose's own heading still shapes the path, and the turn it forces is
        // wide enough for the robot to follow.
        assertEquals(Math.PI / 2, route.getHeading(0.0), 1e-9);
        assertTrue("worst turn " + worstTurnRadius(route), worstTurnRadius(route) >= 1.0);
    }

    @Test
    public void generate_startFacingTheTargets_leavesTheRouteAlone() {
        // Arrange: nothing to turn around, so no arc should be added to the route.
        Weaver.setConfig(new PathConfig().intakeWidth(13.0).robotWidth(17.0).robotHeight(16.0));
        PathResult actual = Weaver.builder()
                .start(new Point(24, 70, Math.toRadians(-90.0)))
                .targets(cluster())
                .maxTargets(3)
                .build();

        // Act
        PathRoute route = actual.getPath();
        double direct = route.get(0).distanceTo(route.get(route.getSegmentCount()));

        // Assert
        assertEquals(-Math.PI / 2, route.getHeading(0.0), 1e-9);
        assertTrue(
                "length " + route.approxLength(2000) + " direct " + direct, route.approxLength(2000) < direct * 1.15);
        assertTrue("worst turn " + worstTurnRadius(route), worstTurnRadius(route) >= 1.0);
    }

    @Test
    public void generate_targetBehindTheRobotWithoutIntake_stillTurnsAround() {
        // Arrange: no intake to merge with, and a target directly behind the pose.
        Weaver.setConfig(new PathConfig().intakeWidth(0).robotWidth(17).robotHeight(16));
        PathResult actual = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(new Point(-30, 0))
                .build();

        // Act
        PathRoute route = actual.getPath();

        // Assert: the route leaves along the requested heading, but never turns tighter
        // than the arc it was given room for.
        assertEquals(0.0, route.getHeading(0.0), 1e-9);
        assertTrue("worst turn " + worstTurnRadius(route), worstTurnRadius(route) >= 1.0);
    }

    @Test
    public void getHeading_derivativeCollapsing_reportsTheDirectionOfTravel() {
        // Arrange: a curve whose derivative vanishes exactly at t = 0.5, where atan2(0, 0)
        // would hand a follower a heading of zero to swing to.
        PathCurve curve =
                new PathCurve(Arrays.asList(new Point(0, 0), new Point(1, 1), new Point(2, -1), new Point(0, 0)));

        // Act
        double actual = curve.getHeading(0.5);
        Point before = curve.get(0.5 - 1e-3);
        Point after = curve.get(0.5 + 1e-3);
        double direction = Math.atan2(after.getY() - before.getY(), after.getX() - before.getX());

        // Assert
        assertTrue("heading " + actual, !Double.isNaN(actual));
        assertEquals(direction, actual, 0.1);
    }

    @Test
    public void getHeading_stationaryCurve_reportsZero() {
        // Arrange: the idle route, a curve that does not move at all.
        PathCurve curve = new PathCurve(Arrays.asList(new Point(5, 5), new Point(5, 5)));

        // Act
        double actual = curve.getHeading(0.5);

        // Assert
        assertEquals(0.0, actual, EPS);
    }
}
