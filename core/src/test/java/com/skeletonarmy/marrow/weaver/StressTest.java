package com.skeletonarmy.marrow.weaver;

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
        Weaver.resetToDefaults();
    }

    @Test
    public void handledDuplicatePoses() {
        double px = START_X + 20, py = START_Y + 10;
        PathPose pose = new PathPose(px, py);

        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(pose)
                .addTarget(pose)
                .generate()
                .getPath();

        assertEquals(1, path.getSegmentCount());
        
        Point end = path.get(1.0);
        assertEquals(px, end.getX(), 1e-6);
        assertEquals(py, end.getY(), 1e-6);
    }

    @Test
    public void handlesLargePoseCount() {
        Random rng = new Random(42);
        Weaver.Builder builder = Weaver.builder().start(new PathPose(START_X, START_Y, 0));
        
        for (int i = 0; i < 15; i++) {
            builder.addTarget(new PathPose(START_X + rng.nextDouble() * 50, START_Y + rng.nextDouble() * 50));
        }

        PathRoute path = builder.generate().getPath();

        assertEquals(1, path.getSegmentCount());
        for (Point p : path.sample(10)) {
            assertTrue(Double.isFinite(p.getX()));
        }
    }

    @Test
    public void extremeCoordinateSpread() {
        PathRoute path = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(START_X + 0.1, START_Y + 0.1))
                .addTarget(new PathPose(START_X - 1000, START_Y + 1000))
                .addTarget(new PathPose(START_X + 2000, START_Y - 2000))
                .generate()
                .getPath();

        assertEquals(1, path.getSegmentCount());
    }
}
