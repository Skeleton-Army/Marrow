package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that a curve already clear of obstacles is kept as it is.
 */
public class ObstacleAvoiderClearCurveTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    private static PathConfig obstacleConfig() {
        return new PathConfig().robotWidth(16).robotHeight(16).clearance(2);
    }

    @Test
    public void generate_farObstacleWithoutSmoothing_keepsTheUnobstructedCurve() {
        // Arrange
        Weaver.setConfig(obstacleConfig().smoothing(false));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .targets(new Point(30, 0, Math.PI / 2), new Point(30, 40))
                .ordered();
        List<Point> expected = builder.generate().getPath().getSegments().get(0).getControlPoints();

        // Act
        builder.addObstacle(new CircleZone(new Point(-60, -60), 3));
        List<Point> actual = builder.generate().getPath().getSegments().get(0).getControlPoints();

        // Assert
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getX(), actual.get(i).getX(), 1e-9);
            assertEquals(expected.get(i).getY(), actual.get(i).getY(), 1e-9);
        }
    }
}
