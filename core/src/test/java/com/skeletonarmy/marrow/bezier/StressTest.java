package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

public class StressTest {

    private static final double START_X = 72;
    private static final double START_Y = 72;

    @Before
    public void setup() {
        BezierPathGenerator.resetToDefaults();
    }

    @Test
    public void handledDuplicateWaypoints() {
        double px = START_X + 20, py = START_Y + 10;
        Waypoint wp = new Waypoint(px, py);

        BezierPath path = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
                .addWaypoint(wp)
                .addWaypoint(wp)
                .generate()
                .getPath();

        assertEquals(2, path.getSegmentCount());
        
        Point end = path.get(2.0);
        double heading = path.getHeading(2.0);
        assertEquals(px - 5.0 * Math.cos(heading), end.getX(), 1e-6);
        assertEquals(py - 5.0 * Math.sin(heading), end.getY(), 1e-6);
    }

    @Test
    public void handlesLargeWaypointCount() {
        Random rng = new Random(42);
        BezierPathGenerator.Builder builder = BezierPathGenerator.builder().start(new Pose(START_X, START_Y, 0));
        
        for (int i = 0; i < 15; i++) {
            builder.addWaypoint(new Waypoint(START_X + rng.nextDouble() * 50, START_Y + rng.nextDouble() * 50));
        }

        BezierPath path = builder.generate().getPath();

        assertEquals(15, path.getSegmentCount());
        for (Point p : path.sample(10)) {
            assertTrue(Double.isFinite(p.getX()));
        }
    }

    @Test
    public void extremeCoordinateSpread() {
        BezierPath path = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
                .addWaypoint(new Waypoint(START_X + 0.1, START_Y + 0.1))
                .addWaypoint(new Waypoint(START_X - 1000, START_Y + 1000))
                .addWaypoint(new Waypoint(START_X + 2000, START_Y - 2000))
                .generate()
                .getPath();

        assertEquals(3, path.getSegmentCount());
    }
}
