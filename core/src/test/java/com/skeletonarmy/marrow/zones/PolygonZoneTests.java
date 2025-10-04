package com.skeletonarmy.marrow.zones;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

// Assuming the existence of:
// 1. Point (with distanceTo)
// 2. PolygonZone (with constructor PolygonZone(Point... corners) and implemented methods)
// 3. CircleZone (with constructor CircleZone(Point center, double radius) and implemented methods)
// 4. Zone interface

public class PolygonZoneTests {

    private static final double DELTA = 0.0001;

    // --- Private Helper Polygon Definitions (for test clarity and modularity) ---

    /**
     * Creates a square centered at (0, 0) with side length 4.
     * Vertices: (-2, 2), (2, 2), (2, -2), (-2, -2).
     */
    private PolygonZone createUnitSquareZone() {
        return new PolygonZone(
                new Point(-2, 2),
                new Point(2, 2),
                new Point(2, -2),
                new Point(-2, -2)
        );
    }

    /**
     * Creates a large square shifted right.
     * Vertices: (8, 2), (12, 2), (12, -2), (8, -2).
     */
    private PolygonZone createShiftedSquareZone() {
        return new PolygonZone(
                new Point(8, 2),
                new Point(12, 2),
                new Point(12, -2),
                new Point(8, -2)
        );
    }

    /**
     * Creates a triangle defined by three vertices.
     * Vertices: (0, 0), (4, 0), (2, 3).
     */
    private PolygonZone createSimpleTriangleZone() {
        return new PolygonZone(
                new Point(0, 0),
                new Point(4, 0),
                new Point(2, 3)
        );
    }

    // --- Tests for contains(Point) ---

    @Test
    public void contains_pointIsInsideSquare_returnsTrue() {
        // Arrange
        PolygonZone square = createUnitSquareZone();
        Point insidePoint = new Point(0, 0);

        // Act
        boolean result = square.contains(insidePoint);

        // Assert
        assertTrue(result);
    }

    @Test
    public void contains_pointIsOutsideSquare_returnsFalse() {
        // Arrange
        PolygonZone square = createUnitSquareZone();
        Point outsidePoint = new Point(3, 0);

        // Act
        boolean result = square.contains(outsidePoint);

        // Assert
        assertFalse(result);
    }

    @Test
    public void contains_pointIsOnVertex_returnsTrue() {
        // Arrange
        PolygonZone square = createUnitSquareZone();
        Point vertexPoint = new Point(2, 2);

        // Act
        boolean result = square.contains(vertexPoint);

        // Assert
        assertTrue(result);
    }

    @Test
    public void contains_pointIsOnEdge_returnsTrue() {
        // Arrange
        PolygonZone square = createUnitSquareZone();
        Point edgePoint = new Point(2, 0); // On the right edge

        // Act
        boolean result = square.contains(edgePoint);

        // Assert
        assertTrue(result);
    }

    @Test
    public void contains_pointIsInsideTriangle_returnsTrue() {
        // Arrange
        PolygonZone triangle = createSimpleTriangleZone();
        Point insidePoint = new Point(2, 1);

        // Act
        boolean result = triangle.contains(insidePoint);

        // Assert
        assertTrue(result);
    }

    // --- Tests for distanceTo(Point) ---

    @Test
    public void distanceToPoint_pointIsOutsideSquare_returnsCorrectDistance() {
        // Arrange
        PolygonZone square = createUnitSquareZone(); // Center at (0,0), extent -2 to 2
        Point outsidePoint = new Point(5, 0);

        // Act
        // Closest point on square is (2, 0). Distance is 5 - 2 = 3.0
        double distance = square.distanceTo(outsidePoint);

        // Assert
        assertEquals(3.0, distance, DELTA);
    }

    @Test
    public void distanceToPoint_pointIsOutsideSquareClosestToVertex_returnsCorrectDistance() {
        // Arrange
        PolygonZone square = createUnitSquareZone(); // Top-right vertex is (2, 2)
        Point outsidePoint = new Point(4, 4);

        // Act
        // Closest point on square is the vertex (2, 2). Distance = sqrt((4-2)^2 + (4-2)^2) = sqrt(4+4) = sqrt(8)
        double expectedDistance = Math.sqrt(8.0);
        double distance = square.distanceTo(outsidePoint);

        // Assert
        assertEquals(expectedDistance, distance, DELTA);
    }

    @Test
    public void distanceToPoint_pointIsInside_returnsZero() {
        // Arrange
        PolygonZone square = createUnitSquareZone();
        Point insidePoint = new Point(1.0, 1.0);

        // Act
        double distance = square.distanceTo(insidePoint);

        // Assert
        assertEquals(0.0, distance, DELTA);
    }

    // --- Tests for distanceTo(Zone) ---

    @Test
    public void distanceToZone_twoSeparatedSquares_returnsCorrectDistance() {
        // Arrange
        PolygonZone p1 = createUnitSquareZone(); // Extent [ -2, 2]
        PolygonZone p2 = createShiftedSquareZone(); // Extent [ 8, 12]

        // Act
        // Closest points are (2, 0) on p1 and (8, 0) on p2. Distance is 8 - 2 = 6.0
        double distance = p1.distanceTo(p2);

        // Assert
        assertEquals(6.0, distance, DELTA);
    }

    @Test
    public void distanceToZone_overlappingSquareAndCircle_returnsZero() {
        // Arrange
        PolygonZone square = createUnitSquareZone(); // Center (0, 0)
        CircleZone circle = new CircleZone(new Point(3, 0), 2.0); // Center (3, 0), Radius 2

        // Act
        // Closest point on square is (2, 0). Circle extends from (1, 0) to (5, 0). They overlap.
        // Distance should be 0.0
        double distance = square.distanceTo(circle);

        // Assert
        assertEquals(0.0, distance, DELTA);
    }

    @Test
    public void distanceToZone_separatedSquareAndCircle_returnsCorrectDistance() {
        // Arrange
        PolygonZone square = createUnitSquareZone(); // Extent [-2, 2]
        CircleZone circle = new CircleZone(new Point(10, 0), 3.0); // Center (10, 0), Radius 3

        // Act
        // Closest point on square is (2, 0). Closest point on circle is (7, 0).
        // Distance is 7 - 2 = 5.0
        double distance = square.distanceTo(circle);

        // Assert
        assertEquals(5.0, distance, DELTA);
    }

    // --- Tests for isInside(Zone) / isFullyInside(Zone) ---

    @Test
    public void isInside_twoTouchingPolygons_returnsTrue() {
        // Arrange
        PolygonZone p1 = createUnitSquareZone(); // Extent [ -2, 2]
        PolygonZone p2 = new PolygonZone(
                new Point(2, 2), new Point(4, 2), new Point(4, -2), new Point(2, -2) // Square from x=2 to x=4
        );

        // Act
        // They share the edge at x=2, distance is 0.0, so they intersect.
        boolean isInside = p1.isInside(p2);

        // Assert
        assertTrue("isInside should be true when zones are touching (distance is 0)", isInside);
    }

    @Test
    public void isInside_fullyOverlappingPolygons_returnsTrue() {
        // Arrange
        PolygonZone p1 = createUnitSquareZone();
        PolygonZone p2 = new PolygonZone(
                new Point(-1, 1), new Point(1, 1), new Point(1, -1), new Point(-1, -1) // Inner square
        );

        // Act
        boolean isInside = p1.isInside(p2); // p1 intersects p2, p2 is inside p1

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void isInside_separatedPolygons_returnsFalse() {
        // Arrange
        PolygonZone p1 = createUnitSquareZone();
        PolygonZone p2 = createShiftedSquareZone();

        // Act
        boolean isInside = p1.isInside(p2);

        // Assert
        assertFalse("isInside should be false when zones are separated", isInside);
    }

    @Test
    public void isFullyInside_smallerSquareInLargerSquare_returnsTrue() {
        // Arrange
        PolygonZone outerSquare = createUnitSquareZone();
        PolygonZone innerSquare = new PolygonZone(
                new Point(-1, 1), new Point(1, 1), new Point(1, -1), new Point(-1, -1) // Inner square
        );

        // Act
        boolean isFullyInside = innerSquare.isFullyInside(outerSquare);

        // Assert
        assertTrue(isFullyInside);
    }

    @Test
    public void isFullyInside_touchingEdges_returnsTrue() {
        // Arrange
        PolygonZone outerSquare = createUnitSquareZone(); // Extent [-2, 2]
        PolygonZone innerTouchingSquare = new PolygonZone(
                new Point(-2, 0), new Point(0, 0), new Point(0, -2), new Point(-2, -2) // Bottom-left quadrant, sharing two edges
        );

        // Act
        boolean isFullyInside = innerTouchingSquare.isFullyInside(outerSquare);

        // Assert
        assertTrue(isFullyInside);
    }

    @Test
    public void isFullyInside_partiallyOutside_returnsFalse() {
        // Arrange
        PolygonZone outerSquare = createUnitSquareZone(); // Extent [-2, 2]
        PolygonZone protrudingSquare = new PolygonZone(
                new Point(1, 1), new Point(3, 1), new Point(3, -1), new Point(1, -1) // Protrudes on the right side
        );

        // Act
        boolean isFullyInside = protrudingSquare.isFullyInside(outerSquare);

        // Assert
        assertFalse(isFullyInside);
    }
}
