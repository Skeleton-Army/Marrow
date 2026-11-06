package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.Point;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests a locked target that sits exactly on a free target.
 */
public class WeaverLockedKeyPointTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    @Test
    public void generate_lockedTargetOnAFreeTarget_stillPinsItsHeading() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(0).smoothing(false));
        Point shared = new Point(30, 10);
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(shared, HeadingPolicy.FREE)
                .addTarget(new Point(30, 10, Math.PI / 2), HeadingPolicy.LOCKED)
                .addTarget(new Point(60, 10), HeadingPolicy.FREE)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert: the route splits at the shared point and arrives with the locked heading.
        boolean found = false;

        for (PathCurve segment : actual.getSegments()) {
            if (segment.get(1.0).distanceTo(shared) < 1e-6) {
                found = true;
                assertEquals(Math.PI / 2, segment.getHeading(1.0), 1e-6);
            }
        }

        assertTrue("no segment ends on the shared target", found);
    }

    @Test
    public void generate_coincidentLockedTargetsWithDifferentHeadings_useTheLastHeading() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(0).smoothing(false));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(new Point(30, 10, 0), HeadingPolicy.LOCKED)
                .addTarget(new Point(30, 10, Math.PI / 2), HeadingPolicy.LOCKED)
                .ordered();

        // Act
        PathResult actual = builder.generate();

        // Assert: the route and the reported end heading agree on the last request.
        assertEquals(2, actual.getVisitedTargets().size());
        assertEquals(Math.PI / 2, actual.getPath().getHeading(actual.getPath().getSegmentCount()), 1e-6);
        assertEquals(Math.PI / 2, actual.getSegmentEndHeadingsRad().get(0), 1e-9);
    }

    @Test
    public void generate_coincidentLockedTargetsWithSameHeading_areAccepted() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(0).smoothing(false));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(new Point(30, 10, Math.PI / 2), HeadingPolicy.LOCKED)
                .addTarget(new Point(30, 10, Math.PI / 2), HeadingPolicy.LOCKED)
                .ordered();

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertEquals(2, actual.getVisitedTargets().size());
        assertEquals(Math.PI / 2, actual.getPath().getHeading(actual.getPath().getSegmentCount()), 1e-6);
    }

    @Test
    public void generate_lockedTargetAtStartWithDifferentHeading_isRejected() {
        // Arrange
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(10, 10, 0))
                .addTarget(new Point(10, 10, Math.PI / 2), HeadingPolicy.LOCKED);

        // Act and assert
        assertThrows(IllegalArgumentException.class, builder::generate);
    }
}
