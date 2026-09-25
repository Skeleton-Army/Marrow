package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class PathGeneratorTest {

    private static final double START_X = 10;
    private static final double START_Y = 10;

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    @Test
    public void globalConfigIsHonoredByBuilder() {
        PathConfig global = new PathConfig().width(99);
        Weaver.setConfig(global);

        assertEquals(99.0, Weaver.getConfig().getWidth(), 1e-6);

        Weaver.resetToDefaults();
        assertEquals(5.0, Weaver.getConfig().getWidth(), 1e-6);
    }

    @Test(expected = IllegalStateException.class)
    public void builderThrowsIfStartMissing() {
        Weaver.builder()
                .addTarget(new PathPose(30, 30))
                .generate();
    }

    @Test
    public void resultObjectProvidesEasyAccess() {
        PathResult result = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(START_X + 20, START_Y))
                .generate();

        assertEquals(1, result.getSegments().size());
        assertNotNull(result.getControlPointArray(0));
        
        Point endPoint = result.getPath().get(1.0);
        assertEquals(START_X + 20.0, endPoint.getX(), 1e-6);
    }
}
