package com.skeletonarmy.marrow.zones;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

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

    @Test
    public void isInside_vertexBarelyInsideOtherPolygon_returnsTrue() {
        // Arrange
        PolygonZone outerSquare = createUnitSquareZone(); // Extent [-2, 2]
        PolygonZone triangle = new PolygonZone(
                new Point(1.9999, 0),
                new Point(-3, 2),
                new Point(-3, -2)
        );

        // Act
        boolean isInside = outerSquare.isInside(triangle);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void isInside_triangleEdgesCrossSquareNoVerticesInside_returnsTrue() {
        // Arrange
        PolygonZone outerSquare = createUnitSquareZone(); // Extent [-2, 2]
        PolygonZone triangle = new PolygonZone(
                new Point(5, 0),
                new Point(-5, 2),
                new Point(-5, -2)
        );

        // Act
        boolean isInside = outerSquare.isInside(triangle);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void isInside_triangleVertexExactlyOnEdge_returnsTrue() {
        // Arrange
        PolygonZone outerSquare = createUnitSquareZone(); // Extent [-2, 2]
        // Triangle with a vertex exactly on the right edge x=2, others outside
        PolygonZone triangle = new PolygonZone(
                new Point(2, 0), // on boundary
                new Point(4, 3),
                new Point(4, -3)
        );

        // Act
        boolean isInside = outerSquare.isInside(triangle);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void isInside_triangleEdgeColinearWithSquareEdge_returnsTrue() {
        // Arrange
        PolygonZone outerSquare = createUnitSquareZone(); // top edge y=2 from x=-2 to 2
        // Triangle with one edge lying along the square's top edge
        PolygonZone triangle = new PolygonZone(
                new Point(-3, 2), // colinear beyond left
                new Point(3, 2),  // colinear beyond right
                new Point(0, 4)   // above
        );

        // Act
        boolean isInside = outerSquare.isInside(triangle);

        // Assert
        assertTrue(isInside);
    }

    @Test
    public void isInside_triangleJustOutsideNoTouch_returnsFalse() {
        // Arrange
        PolygonZone outerSquare = createUnitSquareZone(); // Extent [-2, 2]
        // Triangle whose closest vertex is just outside the right edge by epsilon
        double eps = 1e-6; // larger than boundary epsilon inside PolygonZone.contains
        PolygonZone triangle = new PolygonZone(
                new Point(2 + eps, 0),
                new Point(4 + eps, 3),
                new Point(4 + eps, -3)
        );

        // Act
        boolean isInside = outerSquare.isInside(triangle);

        // Assert
        assertFalse(isInside);
    }

    // --- Tests for PolygonZone(Point center, double width, double height) constructor ---

    @Test
    public void constructor_centerWidthHeight_createsCorrectRectangle() {
        // Arrange
        Point center = new Point(5, 3);
        double width = 4.0;
        double height = 6.0;

        // Act
        PolygonZone rectangle = new PolygonZone(center, width, height);

        // Assert
        // Expected corners: (3, 0), (7, 0), (7, 6), (3, 6)
        assertEquals(4, rectangle.corners.length);
        assertEquals(3.0, rectangle.corners[0].getX(), DELTA);
        assertEquals(0.0, rectangle.corners[0].getY(), DELTA);
        assertEquals(7.0, rectangle.corners[1].getX(), DELTA);
        assertEquals(0.0, rectangle.corners[1].getY(), DELTA);
        assertEquals(7.0, rectangle.corners[2].getX(), DELTA);
        assertEquals(6.0, rectangle.corners[2].getY(), DELTA);
        assertEquals(3.0, rectangle.corners[3].getX(), DELTA);
        assertEquals(6.0, rectangle.corners[3].getY(), DELTA);
    }

    @Test
    public void constructor_centerWidthHeight_centerPointIsInside() {
        // Arrange
        Point center = new Point(0, 0);
        double width = 4.0;
        double height = 4.0;

        // Act
        PolygonZone square = new PolygonZone(center, width, height);

        // Assert
        assertTrue(square.contains(center));
    }

    @Test
    public void constructor_centerWidthHeight_cornersAreOnBoundary() {
        // Arrange
        Point center = new Point(0, 0);
        double width = 4.0;
        double height = 4.0;

        // Act
        PolygonZone square = new PolygonZone(center, width, height);

        // Assert
        for (Point corner : square.corners) {
            assertTrue(square.contains(corner));
        }
    }

    @Test
    public void constructor_centerWidthHeight_containsInteriorPoints() {
        // Arrange
        Point center = new Point(0, 0);
        double width = 4.0;
        double height = 4.0;
        PolygonZone square = new PolygonZone(center, width, height);

        // Act & Assert
        assertTrue(square.contains(new Point(0, 0))); // center
        assertTrue(square.contains(new Point(1, 1))); // interior
        assertTrue(square.contains(new Point(-1, -1))); // interior
    }

    @Test
    public void constructor_centerWidthHeight_excludesExteriorPoints() {
        // Arrange
        Point center = new Point(0, 0);
        double width = 4.0;
        double height = 4.0;
        PolygonZone square = new PolygonZone(center, width, height);

        // Act & Assert
        assertFalse(square.contains(new Point(3, 0))); // outside right edge
        assertFalse(square.contains(new Point(-3, 0))); // outside left edge
        assertFalse(square.contains(new Point(0, 3))); // outside top edge
        assertFalse(square.contains(new Point(0, -3))); // outside bottom edge
    }

    // --- Tests for PolygonZone(Point center, double width, double height, double angle) constructor ---

    @Test
    public void constructor_centerWidthHeightAngle_zeroAngle_createsSameAsWithoutAngle() {
        // Arrange
        Point center = new Point(0, 0);
        double width = 4.0;
        double height = 4.0;
        double angle = 0.0;

        // Act
        PolygonZone square1 = new PolygonZone(center, width, height);
        PolygonZone square2 = new PolygonZone(center, width, height, angle);

        // Assert
        assertEquals(square1.corners.length, square2.corners.length);
        for (int i = 0; i < square1.corners.length; i++) {
            assertEquals(square1.corners[i].getX(), square2.corners[i].getX(), DELTA);
            assertEquals(square1.corners[i].getY(), square2.corners[i].getY(), DELTA);
        }
    }

    @Test
    public void constructor_centerWidthHeightAngle_90DegreeRotation_rotatesCorrectly() {
        // Arrange
        Point center = new Point(0, 0);
        double width = 4.0;
        double height = 2.0;
        double angle = Math.PI / 2; // 90 degrees

        // Act
        PolygonZone rotatedRectangle = new PolygonZone(center, width, height, angle);

        // Assert
        // Original rectangle: (-2, -1), (2, -1), (2, 1), (-2, 1)
        // After 90° rotation: (-1, -2), (-1, 2), (1, 2), (1, -2)
        assertEquals(4, rotatedRectangle.corners.length);
        
        // Check that all corners are present (order may vary due to floating point precision)
        boolean foundCorner1 = false, foundCorner2 = false, foundCorner3 = false, foundCorner4 = false;
        
        for (Point corner : rotatedRectangle.corners) {
            if (Math.abs(corner.getX() - (-1.0)) < DELTA && Math.abs(corner.getY() - (-2.0)) < DELTA) foundCorner1 = true;
            if (Math.abs(corner.getX() - (-1.0)) < DELTA && Math.abs(corner.getY() - 2.0) < DELTA) foundCorner2 = true;
            if (Math.abs(corner.getX() - 1.0) < DELTA && Math.abs(corner.getY() - 2.0) < DELTA) foundCorner3 = true;
            if (Math.abs(corner.getX() - 1.0) < DELTA && Math.abs(corner.getY() - (-2.0)) < DELTA) foundCorner4 = true;
        }
        
        assertTrue("Missing corner (-1, -2)", foundCorner1);
        assertTrue("Missing corner (-1, 2)", foundCorner2);
        assertTrue("Missing corner (1, 2)", foundCorner3);
        assertTrue("Missing corner (1, -2)", foundCorner4);
    }

    @Test
    public void constructor_centerWidthHeightAngle_centerPointIsInside() {
        // Arrange
        Point center = new Point(5, 3);
        double width = 4.0;
        double height = 6.0;
        double angle = Math.PI / 4; // 45 degrees

        // Act
        PolygonZone rotatedRectangle = new PolygonZone(center, width, height, angle);

        // Assert
        assertTrue(rotatedRectangle.contains(center));
    }

    @Test
    public void constructor_centerWidthHeightAngle_180DegreeRotation_flipsRectangle() {
        // Arrange
        Point center = new Point(0, 0);
        double width = 4.0;
        double height = 2.0;
        double angle = Math.PI; // 180 degrees

        // Act
        PolygonZone rotatedRectangle = new PolygonZone(center, width, height, angle);

        // Assert
        // Original rectangle: (-2, -1), (2, -1), (2, 1), (-2, 1)
        // After 180° rotation: (2, 1), (-2, 1), (-2, -1), (2, -1)
        assertEquals(4, rotatedRectangle.corners.length);
        assertEquals(2.0, rotatedRectangle.corners[0].getX(), DELTA);
        assertEquals(1.0, rotatedRectangle.corners[0].getY(), DELTA);
        assertEquals(-2.0, rotatedRectangle.corners[1].getX(), DELTA);
        assertEquals(1.0, rotatedRectangle.corners[1].getY(), DELTA);
        assertEquals(-2.0, rotatedRectangle.corners[2].getX(), DELTA);
        assertEquals(-1.0, rotatedRectangle.corners[2].getY(), DELTA);
        assertEquals(2.0, rotatedRectangle.corners[3].getX(), DELTA);
        assertEquals(-1.0, rotatedRectangle.corners[3].getY(), DELTA);
    }

    // --- Tests for PolygonZone(Point point1, Point point2, double thickness) constructor ---

    @Test
    public void constructor_pointPointThickness_horizontalLine_createsHorizontalRectangle() {
        // Arrange
        Point point1 = new Point(0, 0);
        Point point2 = new Point(4, 0);
        double thickness = 2.0;

        // Act
        PolygonZone rectangle = new PolygonZone(point1, point2, thickness);

        // Assert
        // Expected corners: (0, 1), (0, -1), (4, -1), (4, 1)
        assertEquals(4, rectangle.corners.length);
        assertEquals(0.0, rectangle.corners[0].getX(), DELTA);
        assertEquals(1.0, rectangle.corners[0].getY(), DELTA);
        assertEquals(0.0, rectangle.corners[1].getX(), DELTA);
        assertEquals(-1.0, rectangle.corners[1].getY(), DELTA);
        assertEquals(4.0, rectangle.corners[2].getX(), DELTA);
        assertEquals(-1.0, rectangle.corners[2].getY(), DELTA);
        assertEquals(4.0, rectangle.corners[3].getX(), DELTA);
        assertEquals(1.0, rectangle.corners[3].getY(), DELTA);
    }

    @Test
    public void constructor_pointPointThickness_verticalLine_createsVerticalRectangle() {
        // Arrange
        Point point1 = new Point(0, 0);
        Point point2 = new Point(0, 4);
        double thickness = 2.0;

        // Act
        PolygonZone rectangle = new PolygonZone(point1, point2, thickness);

        // Assert
        // Expected corners: (-1, 0), (1, 0), (1, 4), (-1, 4)
        assertEquals(4, rectangle.corners.length);
        assertEquals(-1.0, rectangle.corners[0].getX(), DELTA);
        assertEquals(0.0, rectangle.corners[0].getY(), DELTA);
        assertEquals(1.0, rectangle.corners[1].getX(), DELTA);
        assertEquals(0.0, rectangle.corners[1].getY(), DELTA);
        assertEquals(1.0, rectangle.corners[2].getX(), DELTA);
        assertEquals(4.0, rectangle.corners[2].getY(), DELTA);
        assertEquals(-1.0, rectangle.corners[3].getX(), DELTA);
        assertEquals(4.0, rectangle.corners[3].getY(), DELTA);
    }

    @Test
    public void constructor_pointPointThickness_diagonalLine_createsDiagonalRectangle() {
        // Arrange
        Point point1 = new Point(0, 0);
        Point point2 = new Point(3, 4);
        double thickness = 2.0;

        // Act
        PolygonZone rectangle = new PolygonZone(point1, point2, thickness);

        // Assert
        assertEquals(4, rectangle.corners.length);
        // Verify that the rectangle contains both original points
        assertTrue(rectangle.contains(point1));
        assertTrue(rectangle.contains(point2));
        // Verify that the rectangle contains the midpoint
        Point midpoint = new Point(1.5, 2.0);
        assertTrue(rectangle.contains(midpoint));
    }

    @Test
    public void constructor_pointPointThickness_containsOriginalPoints() {
        // Arrange
        Point point1 = new Point(1, 2);
        Point point2 = new Point(5, 6);
        double thickness = 1.0;

        // Act
        PolygonZone rectangle = new PolygonZone(point1, point2, thickness);

        // Assert
        assertTrue(rectangle.contains(point1));
        assertTrue(rectangle.contains(point2));
    }

    @Test
    public void constructor_pointPointThickness_containsMidpoint() {
        // Arrange
        Point point1 = new Point(0, 0);
        Point point2 = new Point(4, 0);
        double thickness = 2.0;

        // Act
        PolygonZone rectangle = new PolygonZone(point1, point2, thickness);

        // Assert
        Point midpoint = new Point(2, 0);
        assertTrue(rectangle.contains(midpoint));
    }

    @Test
    public void constructor_pointPointThickness_zeroThickness_createsDegenerateRectangle() {
        // Arrange
        Point point1 = new Point(0, 0);
        Point point2 = new Point(4, 0);
        double thickness = 0.0;

        // Act
        PolygonZone rectangle = new PolygonZone(point1, point2, thickness);

        // Assert
        // With zero thickness, all corners should collapse to the line segment
        assertEquals(4, rectangle.corners.length);
        // All corners should be on the line from (0,0) to (4,0)
        for (Point corner : rectangle.corners) {
            assertEquals(0.0, corner.getY(), DELTA);
            assertTrue(corner.getX() >= 0.0 && corner.getX() <= 4.0);
        }
    }

    @Test
    public void constructor_pointPointThickness_overlappingPoints_throwsException() {
        // Arrange
        Point point1 = new Point(0, 0);
        Point point2 = new Point(0, 0); // Same point
        double thickness = 2.0;

        // Act & Assert
        try {
            new PolygonZone(point1, point2, thickness);
            // If we get here, the test should fail
            fail("Expected IllegalArgumentException for overlapping points");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertTrue(true);
        }
    }
}
