package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests routes whose key points fall on top of each other.
 */
public class WeaverCoincidentKeyPointTest {

    private static final double EPS = 1e-6;

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    private static List<PathCurve> cubics(PathRoute route) {
        List<PathCurve> cubics = new ArrayList<>();

        for (PathCurve segment : route.getSegments()) {
            cubics.addAll(segment.toCubicSegments());
        }

        return cubics;
    }

    /**
     * Whether the robot, facing along the route, passes {@code target} through its
     * intake mouth: the target reaches the robot's lateral axis within half the
     * intake width of its centre.
     */
    private static boolean passesThroughIntake(PathRoute route, Point target, double intakeWidth) {
        List<Point> samples = route.sample(2000);
        double previousAlong = Double.NaN;

        // A target under the robot where it starts is already in the intake.
        if (samples.get(0).distanceTo(target) < 1e-3) {
            return true;
        }

        for (int i = 1; i < samples.size(); i++) {
            Point before = samples.get(i - 1);
            Point here = samples.get(i);

            if (before.distanceTo(here) < EPS) {
                continue;
            }

            double heading = Math.atan2(here.getY() - before.getY(), here.getX() - before.getX());
            double relativeX = target.getX() - here.getX();
            double relativeY = target.getY() - here.getY();
            double along = relativeX * Math.cos(heading) + relativeY * Math.sin(heading);
            double lateral = -relativeX * Math.sin(heading) + relativeY * Math.cos(heading);
            boolean reached = Math.abs(along) < 1e-3 || (previousAlong > 0 && along <= 0);

            if (reached && Math.abs(lateral) <= intakeWidth / 2.0 + 1e-3) {
                return true;
            }

            previousAlong = along;
        }

        return false;
    }

    @Test
    public void generate_targetsSideBySide_hasNoZeroLengthSegment() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(13));
        WeaverBuilder builder =
                Weaver.builder().start(new Point(0, 0, Math.PI / 2)).targets(new Point(0, 30), new Point(4, 30));

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        for (PathCurve cubic : cubics(actual)) {
            assertTrue(cubic.toString(), cubic.approxLength(20) > EPS);
        }
        assertEquals(Math.PI / 2, actual.getHeading(actual.getSegmentCount()), 1e-3);
    }

    @Test
    public void generate_targetOnTheStart_hasNoZeroLengthSegment() {
        // Arrange
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .targets(new Point(0, 0), new Point(30, 0))
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        for (PathCurve cubic : cubics(actual)) {
            assertTrue(cubic.toString(), cubic.approxLength(20) > EPS);
        }
    }

    @Test
    public void generate_secondTargetOnTheFirstCaptureCentre_stillCapturesTheFirst() {
        // Arrange: the intake offset puts the robot centre for the first target exactly
        // on the second target, which asks for a different heading.
        double intakeWidth = 10;
        Point first = new Point(10, 5, 0);
        Point second = new Point(10, 0, Math.PI / 2);
        Weaver.setConfig(new PathConfig().intakeWidth(intakeWidth).smoothing(false));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .targets(first, second)
                .ordered();

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertTrue(passesThroughIntake(actual.getPath(), first, intakeWidth));
        assertEquals(Math.PI / 2, actual.getSegmentEndHeadingsRad().get(0), 1e-9);
    }
}
