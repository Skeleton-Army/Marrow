package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PathGeneratorTest {

    private static final double START_X = 10;
    private static final double START_Y = 10;

    @Before
    public void setup() {
        BezierPathGenerator.resetToDefaults();
    }

    @Test
    public void globalConfigIsHonoredByBuilder() {
        BezierConfig global = new BezierConfig().reach(99);
        BezierPathGenerator.setConfig(global);

        BezierResult result = BezierPathGenerator.builder()
                .start(new Pose(0, 0, 0))
                .addWaypoint(new Waypoint(10, 0))
                .generate();

        Point[] cps = result.getControlPointArray(0);
        assertEquals(-89.0, cps[cps.length - 1].getX(), 1e-6);

        BezierPathGenerator.resetToDefaults();
    }

    @Test(expected = IllegalStateException.class)
    public void builderThrowsIfStartMissing() {
        BezierPathGenerator.builder()
                .addWaypoint(new Waypoint(30, 30))
                .generate();
    }

    @Test
    public void resultObjectProvidesEasyAccess() {
        BezierResult result = BezierPathGenerator.builder()
                .start(new Pose(START_X, START_Y, 0))
                .addWaypoint(new Waypoint(START_X + 20, START_Y))
                .generate();

        assertEquals(1, result.getSegments().size());
        assertNotNull(result.getControlPointArray(0));
        
        Point endPoint = result.getPath().get(1.0);
        assertEquals(START_X + 15.0, endPoint.getX(), 1e-6);
    }
}
