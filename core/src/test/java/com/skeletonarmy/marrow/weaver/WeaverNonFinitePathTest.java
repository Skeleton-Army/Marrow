package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertFalse;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the clearance check on curves with non-finite points.
 */
public class WeaverNonFinitePathTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    private static PathConfig obstacleConfig() {
        return new PathConfig().robotWidth(16).robotHeight(16).clearance(2);
    }

    @Test
    public void isPathClear_nonFiniteCurve_returnsFalse() {
        // Arrange
        PathCurve curve = new PathCurve(
                Arrays.asList(new Point(0, 0), new Point(Double.NaN, Double.NaN), new Point(20, 0), new Point(30, 0)));
        List<Zone> obstacles = Collections.<Zone>singletonList(new CircleZone(new Point(10, 0), 3));

        // Act
        boolean actual = WeaverGenerator.isPathClear(curve, obstacles, obstacleConfig());

        // Assert
        assertFalse(actual);
    }
}
