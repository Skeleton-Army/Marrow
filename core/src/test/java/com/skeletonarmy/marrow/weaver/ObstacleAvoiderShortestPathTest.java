package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the route the visibility-graph search chooses around obstacles. These pin
 * the output of the shortest-path search (A* with lazy line-of-sight, distant-margin
 * skip) so a routing regression shows up here.
 */
public class ObstacleAvoiderShortestPathTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    /**
     * Reroutes a straight line that is blocked by an asymmetric barrier: the line at
     * {@code y = 0} crosses the barrier, which reaches from {@code y = -2} to
     * {@code y = 8}. Going below it is much shorter, so the search must pick that side.
     */
    @Test
    public void generate_blockedLine_takesTheNearerDetour() {
        // Arrange
        Zone barrier =
                new PolygonZone(new Point(40, -2), new Point(60, -2), new Point(60, 8), new Point(40, 8));
        Weaver.setConfig(new PathConfig().robotWidth(2).robotHeight(2).clearance(1).smoothing(false));
        WeaverBuilder builder =
                Weaver.builder().start(new Point(0, 0, 0)).end(new Point(100, 0)).obstacles(barrier);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert: clear, reaches the endpoint, and takes the short way under the barrier.
        assertTrue(WeaverGenerator.isPathClear(actual, Collections.singletonList(barrier), Weaver.getConfig()));

        List<Point> samples = actual.sample(40);
        assertTrue(samples.get(samples.size() - 1).distanceTo(new Point(100, 0)) < 0.5);
        assertTrue("detour length " + actual.approxLength(40), actual.approxLength(40) < 110.0);

        for (Point sample : samples) {
            if (sample.getX() > 40 && sample.getX() < 60) {
                assertTrue("passed above the barrier at " + sample, sample.getY() < 8);
            }
        }
    }

    /** A far obstacle is irrelevant, so the straight route is kept unchanged. */
    @Test
    public void generate_distantObstacle_keepsTheStraightRoute() {
        // Arrange
        Weaver.setConfig(new PathConfig().robotWidth(16).robotHeight(16).clearance(2).smoothing(false));
        WeaverBuilder builder = Weaver.builder().start(new Point(0, 0, 0)).end(new Point(60, 0));
        List<Point> expected = builder.generate().getPath().getSegments().get(0).getControlPoints();

        // Act
        builder.addObstacle(new CircleZone(new Point(-100, -100), 3));
        List<Point> actual = builder.generate().getPath().getSegments().get(0).getControlPoints();

        // Assert
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getX(), actual.get(i).getX(), 1e-9);
            assertEquals(expected.get(i).getY(), actual.get(i).getY(), 1e-9);
        }
    }
}
