package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Tests the input validation in Weaver, WeaverBuilder and PathConfig. */
public class WeaverInputValidationTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    @Test
    public void setConfig_null_isRejected() {
        // Act and assert
        assertThrows(IllegalArgumentException.class, () -> Weaver.setConfig(null));
    }

    @Test
    public void addObstacle_null_isRejected() {
        // Arrange
        WeaverBuilder builder = Weaver.builder();

        // Act and assert
        assertThrows(IllegalArgumentException.class, () -> builder.addObstacle(null));
    }

    @Test
    public void obstacles_listContainingNull_isRejected() {
        // Arrange
        WeaverBuilder builder = Weaver.builder();
        List<Zone> obstacles = Arrays.asList(new CircleZone(new Point(5, 5), 2), null);

        // Act and assert
        assertThrows(IllegalArgumentException.class, () -> builder.obstacles(obstacles));
    }

    @Test
    public void obstacles_nullArray_clearsObstacles() {
        // Arrange
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .end(new Point(30, 0))
                .addObstacle(new CircleZone(new Point(15, 0), 3));

        // Act
        PathResult actual = builder.obstacles((Zone[]) null).generate();

        // Assert: nothing to avoid, so the route stays a straight line.
        assertTrue(
                WeaverGenerator.isPathClear(actual.getPath(), new ArrayList<Zone>(), new PathConfig().robotWidth(0)));
        assertEquals(0.0, actual.getPath().get(0.5).getY(), 1e-9);
    }

    @Test
    public void generate_nonFiniteTarget_isRejected() {
        // Arrange
        WeaverBuilder builder = Weaver.builder().start(new Point(0, 0, 0));

        // Act and assert
        assertThrows(IllegalArgumentException.class, () -> builder.addTarget(new Point(Double.NaN, 5)));
        assertThrows(IllegalArgumentException.class, () -> builder.addTarget(new Point(5, Double.POSITIVE_INFINITY)));
    }

    @Test
    public void start_nonFinite_isRejected() {
        // Arrange
        WeaverBuilder builder = Weaver.builder();

        // Act and assert
        assertThrows(IllegalArgumentException.class, () -> builder.start(new Point(Double.NaN, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> builder.start(new Point(0, 0, Double.POSITIVE_INFINITY)));
    }

    @Test
    public void end_nonFinite_isRejected() {
        // Arrange
        WeaverBuilder builder = Weaver.builder();

        // Act and assert
        assertThrows(IllegalArgumentException.class, () -> builder.end(new Point(Double.NaN, 0)));
    }

    @Test
    public void start_withoutHeading_isAccepted() {
        // Act
        PathResult actual = Weaver.builder()
                .start(new Point(0, 0))
                .addTarget(new Point(10, 0))
                .generate();

        // Assert
        assertEquals(1, actual.getVisitedTargets().size());
    }

    @Test
    public void generate_targetsAndDestination_visitsTargetsThenEndsAtDestination() {
        // Arrange
        Point destination = new Point(60, 5);

        // Act
        PathResult actual = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(new Point(10, 0))
                .addTarget(new Point(30, 10))
                .end(destination)
                .generate();

        // Assert
        assertEquals(2, actual.getVisitedTargets().size());
        assertTrue(actual.getUnvisitedTargets().isEmpty());
        assertTrue(actual.getPath().getNormalized(1.0).distanceTo(destination) < 1e-6);
    }

    @Test
    public void generate_targetsAndDestination_keepsLockedDestinationHeading() {
        // Act
        PathResult actual = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(new Point(20, 0))
                .end(new Point(40, 10, Math.PI / 2), HeadingPolicy.LOCKED)
                .generate();

        // Assert
        assertEquals(Math.PI / 2, actual.getPath().getHeading(actual.getPath().getSegmentCount()), 1e-6);
    }

    @Test
    public void generate_nullConfig_isRejected() {
        // Act and assert
        assertThrows(
                IllegalArgumentException.class,
                () -> WeaverGenerator.generate(
                        new Point(0, 0, 0), null, new ArrayList<Point>(), new ArrayList<Zone>(), true, 0, null));
    }

    @Test
    public void config_nonFiniteValues_areRejected() {
        // Act and assert
        assertThrows(IllegalArgumentException.class, () -> new PathConfig().robotWidth(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new PathConfig().robotHeight(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new PathConfig().intakeWidth(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new PathConfig().clearance(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new PathConfig().turnCostWeight(Double.NaN));
    }

    @Test
    public void config_defaultBruteForceOrderLimit_isWithinTheCap() {
        // Act and assert
        assertEquals(PathConfig.MAX_BRUTE_FORCE_ORDER_LIMIT, new PathConfig().getBruteForceOrderLimit());
    }

    @Test
    public void config_bruteForceOrderLimit_isClamped() {
        // Act
        PathConfig tooBig = new PathConfig().bruteForceOrderLimit(50);
        PathConfig negative = new PathConfig().bruteForceOrderLimit(-4);
        PathConfig fine = new PathConfig().bruteForceOrderLimit(5);

        // Assert
        assertEquals(PathConfig.MAX_BRUTE_FORCE_ORDER_LIMIT, tooBig.getBruteForceOrderLimit());
        assertEquals(0, negative.getBruteForceOrderLimit());
        assertEquals(5, fine.getBruteForceOrderLimit());
    }

    @Test(timeout = 5000)
    public void generate_hugeBruteForceLimit_doesNotHangOnLongTargetLists() {
        // Arrange: the limit is clamped to 7, so 12 targets use the greedy strategy.
        Weaver.setConfig(new PathConfig().bruteForceOrderLimit(12));
        WeaverBuilder builder = Weaver.builder().start(new Point(0, 0, 0));

        for (int i = 0; i < 12; i++) {
            builder.addTarget(new Point(10 + (i * 37) % 100, 10 + (i * 53) % 100));
        }

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertEquals(12, actual.getVisitedTargets().size());
    }
}
