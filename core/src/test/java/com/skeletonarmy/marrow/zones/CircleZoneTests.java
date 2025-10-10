package com.skeletonarmy.marrow.zones;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class CircleZoneTests {
    private static final double DELTA = 0.0001;

    @Test
    public void contains_pointIsInside_returnsTrue() {
        // Arrange
        Point center = new Point(0, 0);
        CircleZone zone = new CircleZone(center, 5.0);
        Point insidePoint = new Point(1.0, 1.0);

        // Act
        boolean isInside = zone.contains(insidePoint);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void contains_pointIsOutside_returnsFalse() {
        // Arrange
        Point center = new Point(0, 0);
        CircleZone zone = new CircleZone(center, 5.0);
        Point outsidePoint = new Point(5.0, 5.0);

        // Act
        boolean isInside = zone.contains(outsidePoint);

        // Assert
        assertFalse(isInside);
    }

    @Test
    public void contains_pointIsOnBoundary_returnsTrue() {
        // Arrange
        Point center = new Point(0, 0);
        CircleZone zone = new CircleZone(center, 5.0);
        Point boundaryPoint = new Point(5.0, 0.0);

        // Act
        boolean isInside = zone.contains(boundaryPoint);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void distanceToPoint_pointIsOutside_returnsCorrectDistance() {
        // Arrange
        Point center = new Point(0, 0);
        CircleZone zone = new CircleZone(center, 3.0);
        Point outsidePoint = new Point(7.0, 0.0);

        // Act
        double distance = zone.distanceTo(outsidePoint);

        // Assert
        assertEquals(4.0, distance, DELTA); // (7.0 - 3.0)
    }

    @Test
    public void distanceToPoint_pointIsInside_returnsZero() {
        // Arrange
        Point center = new Point(0, 0);
        CircleZone zone = new CircleZone(center, 5.0);
        Point insidePoint = new Point(1.0, 1.0);

        // Act
        double distance = zone.distanceTo(insidePoint);

        // Assert
        assertEquals(0.0, distance, DELTA);
    }

    // --- Tests for distanceTo(Zone) ---

    @Test
    public void distanceToZone_twoSeparatedCircles_returnsCorrectDistance() {
        // Arrange
        CircleZone c1 = new CircleZone(new Point(0, 0), 2.0);
        CircleZone c2 = new CircleZone(new Point(10, 0), 3.0);
        // Distance between centers: 10.0. Sum of radii: 5.0. Distance: 10.0 - 5.0 = 5.0

        // Act
        double distance = c1.distanceTo(c2);

        // Assert
        assertEquals(5.0, distance, DELTA);
    }

    @Test
    public void distanceToZone_twoOverlappingCircles_returnsZero() {
        // Arrange
        CircleZone c1 = new CircleZone(new Point(0, 0), 5.0);
        CircleZone c2 = new CircleZone(new Point(8, 0), 4.0);
        // Distance between centers: 8.0. Sum of radii: 9.0. Overlap.

        // Act
        double distance = c1.distanceTo(c2);

        // Assert
        assertEquals(0.0, distance, DELTA);
    }

    @Test
    public void distanceToZone_circleOutsidePolygon_returnsCorrectDistance() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(10, 0), 2.0);
        // Square centered at (0,0) from -2 to 2
        PolygonZone square = new PolygonZone(
                new Point(-2, -2), new Point(2, -2), new Point(2, 2), new Point(-2, 2)
        );

        // Act
        // Distance from circle center (10,0) to closest polygon point (2,0) is 8.0.
        // Distance to zone is 8.0 - 2.0 (radius) = 6.0
        double distance = circle.distanceTo(square);

        // Assert
        assertEquals(6.0, distance, DELTA);
    }

    @Test
    public void distanceToZone_circleOverlappingPolygon_returnsZero() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(5, 0), 4.0);
        // Square centered at (0,0) from -2 to 2
        PolygonZone square = new PolygonZone(
                new Point(-2, -2), new Point(2, -2), new Point(2, 2), new Point(-2, 2)
        );

        // Act
        // Distance from circle center (5,0) to closest polygon point (2,0) is 3.0.
        // Distance from zone is 3.0 - 4.0 (radius) = -1.0. Clamped to 0.0.
        double distance = circle.distanceTo(square);

        // Assert
        assertEquals(0.0, distance, DELTA);
    }

    @Test
    public void isInside_triangleEdgesCrossCircleNoVerticesInside_returnsTrue() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0); // radius 2
        // Triangle whose edges cross the circle, but all vertices are outside
        PolygonZone triangle = new PolygonZone(
                new Point(5, 0),
                new Point(-5, 2),
                new Point(-5, -2)
        );

        // Act
        boolean isInside = circle.isInside(triangle);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void isInside_triangleVertexExactlyOnCircle_returnsTrue() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        // One vertex exactly on the circle at (2,0), others outside
        PolygonZone triangle = new PolygonZone(
                new Point(2, 0),
                new Point(-4, 3),
                new Point(-4, -3)
        );

        // Act
        boolean isInside = circle.isInside(triangle);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void isInside_triangleEdgeTangentToCircle_returnsTrue() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        // Triangle with one edge tangent to the circle at (0,2)
        PolygonZone triangle = new PolygonZone(
                new Point(-5, 2),
                new Point(5, 2),
                new Point(0, 5)
        );

        // Act
        boolean isInside = circle.isInside(triangle);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void isInside_triangleJustOutsideNoTouch_returnsFalse() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        double eps = 1e-6; // larger than boundary epsilon used in geometry helpers
        // Triangle entirely to the right of the circle; closest edge is a vertical line at x = 2 + eps
        PolygonZone triangle = new PolygonZone(
                new Point(2 + eps, 3),
                new Point(2 + eps, -3),
                new Point(5, 0)
        );

        // Act
        boolean isInside = circle.isInside(triangle);

        // Assert
        assertFalse(isInside);
    }


    // --- Tests for isInside(Zone) / isFullyInside(Zone) ---

    @Test
    public void isInside_circlesTouching_returnsTrue() {
        // Arrange
        CircleZone c1 = new CircleZone(new Point(0, 0), 2.0);
        CircleZone c2 = new CircleZone(new Point(5, 0), 3.0);
        // Distance between them is 0.0

        // Act
        boolean isInside = c1.isInside(c2);

        // Assert
        assertTrue("isInside should be true when zones are touching (distance is 0)", isInside);
    }

    @Test
    public void isInside_circlesSeparated_returnsFalse() {
        // Arrange
        CircleZone c1 = new CircleZone(new Point(0, 0), 1.0);
        CircleZone c2 = new CircleZone(new Point(10, 0), 1.0);

        // Act
        boolean isInside = c1.isInside(c2);

        // Assert
        assertFalse("isInside should be false when zones are separated", isInside);
    }

    @Test
    public void isFullyInside_smallerCircleInLargerCircle_returnsTrue() {
        // Arrange
        CircleZone outer = new CircleZone(new Point(0, 0), 10.0);
        CircleZone inner = new CircleZone(new Point(1, 1), 2.0);

        // Act
        boolean isFullyInside = inner.isFullyInside(outer);

        // Assert
        assertTrue(isFullyInside);
    }

    @Test
    public void isFullyInside_smallerCircleTouchingEdgeOfLargerCircle_returnsTrue() {
        // Arrange
        CircleZone outer = new CircleZone(new Point(0, 0), 10.0);
        CircleZone inner = new CircleZone(new Point(7.0, 0), 3.0);
        // 7.0 + 3.0 = 10.0. Exactly on boundary.

        // Act
        boolean isFullyInside = inner.isFullyInside(outer);

        // Assert
        assertTrue(isFullyInside);
    }

    @Test
    public void isFullyInside_circleOutsidePolygon_returnsFalse() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(10, 0), 1.0);
        // Square centered at (0,0) from -2 to 2
        PolygonZone square = new PolygonZone(
                new Point(-2, -2), new Point(2, -2), new Point(2, 2), new Point(-2, 2)
        );

        // Act
        boolean isFullyInside = circle.isFullyInside(square);

        // Assert
        assertFalse(isFullyInside);
    }

    @Test
    public void isFullyInside_circleTooLargeForPolygon_returnsFalse() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(0, 0), 3.0); // Center at (0,0), Radius 3
        // Square centered at (0,0) from -2 to 2
        PolygonZone square = new PolygonZone(
                new Point(-2, -2), new Point(2, -2), new Point(2, 2), new Point(-2, 2)
        );

        // Act
        // Center is inside, but Radius (3.0) > distance to closest edge (2.0)
        boolean isFullyInside = circle.isFullyInside(square);

        // Assert
        assertFalse(isFullyInside);
    }

    @Test
    public void isFullyInside_circleInPolygon_returnsTrue() {
        // Arrange
        CircleZone circle = new CircleZone(new Point(0, 0), 1.0); // Center at (0,0), Radius 1
        // Square centered at (0,0) from -2 to 2
        PolygonZone square = new PolygonZone(
                new Point(-2, -2), new Point(2, -2), new Point(2, 2), new Point(-2, 2)
        );

        // Act
        // Center is inside, Radius (1.0) <= distance to closest edge (2.0)
        boolean isFullyInside = circle.isFullyInside(square);

        // Assert
        assertTrue(isFullyInside);
    }

    @Test
    public void testMoveBy() {
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        
        // Move the circle by (5, 3)
        circle.moveBy(5, 3);
        
        // Check that the circle is now at the new position
        assertTrue(circle.contains(new Point(5, 3))); // Center
        assertTrue(circle.contains(new Point(7, 3))); // Right edge
        assertTrue(circle.contains(new Point(3, 3))); // Left edge
        assertTrue(circle.contains(new Point(5, 5))); // Top edge
        assertTrue(circle.contains(new Point(5, 1))); // Bottom edge
        
        // Original position should no longer be contained
        assertFalse(circle.contains(new Point(0, 0)));
    }

    @Test
    public void testSetPosition() {
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        
        // Move the circle to center at (10, 7)
        circle.setPosition(10, 7);
        
        // Check that the circle is now centered at (10, 7)
        assertTrue(circle.contains(new Point(10, 7))); // Center
        assertTrue(circle.contains(new Point(12, 7))); // Right edge
        assertTrue(circle.contains(new Point(8, 7))); // Left edge
        assertTrue(circle.contains(new Point(10, 9))); // Top edge
        assertTrue(circle.contains(new Point(10, 5))); // Bottom edge
        
        // Original position should no longer be contained
        assertFalse(circle.contains(new Point(0, 0)));
    }

    // --- Tests for Movement Methods ---

    @Test
    public void testMoveByMultipleTimes() {
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        
        // Move multiple times
        circle.moveBy(1, 1);
        circle.moveBy(2, 3);
        circle.moveBy(-1, 0);
        
        // Total movement: (1+2-1, 1+3+0) = (2, 4)
        assertTrue(circle.contains(new Point(2, 4))); // Center should be at (2, 4)
        assertFalse(circle.contains(new Point(0, 0))); // Original center should not be contained
    }

    @Test
    public void testMoveByZero() {
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        Point originalCenter = circle.getPosition();

        // Move by zero
        circle.moveBy(0, 0);
        
        // Should remain unchanged
        assertEquals(originalCenter.getX(), circle.getPosition().getX(), DELTA);
        assertEquals(originalCenter.getY(), circle.getPosition().getY(), DELTA);
    }

    @Test
    public void testMoveByNegative() {
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        
        // Move by negative values
        circle.moveBy(-5, -3);
        
        // Check new position
        assertTrue(circle.contains(new Point(-5, -3))); // Center should be at (-5, -3)
        assertFalse(circle.contains(new Point(0, 0))); // Original center should not be contained
    }

    @Test
    public void testSetPositionMultipleTimes() {
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        
        // Move to different positions
        circle.setPosition(10, 5);
        assertTrue(circle.contains(new Point(10, 5)));
        
        circle.setPosition(-3, 7);
        assertTrue(circle.contains(new Point(-3, 7)));
        assertFalse(circle.contains(new Point(10, 5))); // Previous position should not be contained
    }

    @Test
    public void testSetPositionSamePosition() {
        CircleZone circle = new CircleZone(new Point(0, 0), 2.0);
        Point originalCenter = circle.getPosition();
        
        // Move to same position
        circle.setPosition(0, 0);
        
        // Should remain unchanged
        assertEquals(originalCenter.getX(), circle.getPosition().getX(), DELTA);
        assertEquals(originalCenter.getY(), circle.getPosition().getY(), DELTA);
    }

    @Test
    public void testMoveByPreservesRadius() {
        CircleZone circle = new CircleZone(new Point(0, 0), 5.0);
        double originalRadius = circle.getRadius();
        
        // Move the circle
        circle.moveBy(5, 3);
        
        // Radius should be preserved
        assertEquals(originalRadius, circle.getRadius(), DELTA);
        
        // Check that it's still a circle with the same radius
        assertTrue(circle.contains(new Point(5, 3))); // Center
        assertTrue(circle.contains(new Point(10, 3))); // Right edge
        assertTrue(circle.contains(new Point(0, 3))); // Left edge
        assertTrue(circle.contains(new Point(5, 8))); // Top edge
        assertTrue(circle.contains(new Point(5, -2))); // Bottom edge
    }
}