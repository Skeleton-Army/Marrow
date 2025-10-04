package com.skeletonarmy.marrow.zones;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PointTests {

    private static final double DELTA = 0.0001;

    /**
     * Arrange: Define two distinct points.
     * Act: Calculate the distance between them.
     * Assert: Verify the result against a known value (calculated manually or by a reference).
     */
    @Test
    public void calculateDistance_betweenTwoPoints_isCorrect() {
        // Arrange
        Point p1 = new Point(1.0, 1.0);
        Point p2 = new Point(4.0, 5.0);
        // Distance is sqrt((4-1)^2 + (5-1)^2) = sqrt(9 + 16) = sqrt(25) = 5.0

        // Act
        double distance = p1.distanceTo(p2);

        // Assert
        assertEquals(5.0, distance, DELTA);
    }

    /**
     * Arrange: Define two identical points.
     * Act: Calculate the distance between them.
     * Assert: Verify the distance is zero.
     */
    @Test
    public void calculateDistance_betweenSamePoints_isZero() {
        // Arrange
        Point p1 = new Point(10.5, -2.2);
        Point p2 = new Point(10.5, -2.2);

        // Act
        double distance = p1.distanceTo(p2);

        // Assert
        assertEquals(0.0, distance, DELTA);
    }
}
