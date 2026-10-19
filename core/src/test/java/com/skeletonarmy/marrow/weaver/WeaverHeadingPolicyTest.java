package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.skeletonarmy.marrow.zones.Point;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the explicit free and locked target heading policies.
 */
public class WeaverHeadingPolicyTest {

    private static final double EPS = 1e-6;

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    @Test
    public void generate_unmarkedTargetHeading_isFree() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(0).smoothing(false));
        Point target = new Point(30, 0, Math.PI / 2);
        List<Point> expected = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(new Point(target.getX(), target.getY()))
                .generate()
                .getPath()
                .getSegments()
                .get(0)
                .getControlPoints();

        // Act
        PathResult actual =
                Weaver.builder().start(new Point(0, 0, 0)).addTarget(target).generate();

        // Assert
        List<Point> actualPoints = actual.getPath().getSegments().get(0).getControlPoints();
        assertEquals(expected.size(), actualPoints.size());

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getX(), actualPoints.get(i).getX(), EPS);
            assertEquals(expected.get(i).getY(), actualPoints.get(i).getY(), EPS);
        }

        assertEquals(0.0, actual.getSegmentEndHeadingsRad().get(0), EPS);
    }

    @Test
    public void generate_lockedTarget_pinsItsHeading() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(0).smoothing(false));
        Point target = new Point(30, 0, Math.PI / 2);

        // Act
        PathResult actual = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(target, HeadingPolicy.LOCKED)
                .generate();

        // Assert
        assertEquals(Math.PI / 2, actual.getSegmentEndHeadingsRad().get(0), EPS);
        assertEquals(Math.PI / 2, actual.getPath().getHeading(actual.getPath().getSegmentCount()), EPS);
    }

    @Test
    public void generate_lockedInteriorTarget_survivesSmoothing() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(0));
        Point first = new Point(10, 10, Math.PI / 2);

        // Act
        PathRoute actual = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(first, HeadingPolicy.LOCKED)
                .addTarget(new Point(30, 10), HeadingPolicy.FREE)
                .ordered()
                .generate()
                .getPath();

        // Assert
        assertEquals(2, actual.getSegmentCount());
        assertEquals(Math.PI / 2, actual.getHeading(1.0), EPS);
    }

    @Test
    public void generate_defaultLockedPolicy_appliesToTargets() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(0).smoothing(false));

        // Act
        PathResult actual = Weaver.builder()
                .start(new Point(0, 0, 0))
                .defaultHeadingPolicy(HeadingPolicy.LOCKED)
                .addTarget(new Point(30, 0, Math.PI / 2))
                .generate();

        // Assert
        assertEquals(Math.PI / 2, actual.getSegmentEndHeadingsRad().get(0), EPS);
    }

    @Test
    public void generate_lockedDestination_pinsEndHeading() {
        // Arrange
        Weaver.setConfig(new PathConfig());

        // Act
        PathResult actual = Weaver.builder()
                .start(new Point(0, 0, 0))
                .end(new Point(30, 10, Math.PI / 2), HeadingPolicy.LOCKED)
                .generate();

        // Assert
        assertEquals(Math.PI / 2, actual.getSegmentEndHeadingsRad().get(0), EPS);
        assertEquals(Math.PI / 2, actual.getPath().getHeading(1.0), EPS);
    }

    @Test
    public void generate_lockedTargetWithoutHeading_throws() {
        // Act and assert
        assertThrows(
                IllegalArgumentException.class,
                () -> Weaver.builder().start(new Point(0, 0, 0)).addTarget(new Point(30, 0), HeadingPolicy.LOCKED));
    }
}
