package com.skeletonarmy.marrow.zones;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class CompositeZoneTests {
    private static final double DELTA = 0.0001;

    /**
     * Creates a ComplexZone composed of a square and a circle.
     * Square: 4x4, centered at (0, 0). Extent X: [-2, 2].
     * Circle: Radius 2.0, centered at (10, 0). Extent X: [8, 12].
     * The closest distance between them is 6.0 (from x=2 to x=8).
     */
    private CompositeZone createSeparatedComplexZone() {
        // Square: Vertices: (-2, 2), (2, 2), (2, -2), (-2, -2). Center (0, 0)
        PolygonZone square = new PolygonZone(
                new Point(-2, 2),
                new Point(2, 2),
                new Point(2, -2),
                new Point(-2, -2)
        );
        // Circle: Center (10, 0), Radius 2
        CircleZone circle = new CircleZone(new Point(10, 0), 2.0);

        return new CompositeZone(square, circle);
    }

    /**
     * Creates a ComplexZone composed of an overlapping square and a circle.
     * Square: 4x4, centered at (0, 0). Extent X: [-2, 2].
     * Circle: Radius 2.0, centered at (3, 0). Extent X: [1, 5].
     * They overlap between x=1 and x=2.
     */
    private CompositeZone createOverlappingComplexZone() {
        PolygonZone square = new PolygonZone(
                new Point(-2, 2),
                new Point(2, 2),
                new Point(2, -2),
                new Point(-2, -2)
        );
        CircleZone circle = new CircleZone(new Point(3, 0), 2.0);

        return new CompositeZone(square, circle);
    }

    // --- Tests for contains(Point) ---

    @Test
    public void contains_pointIsInsideComponent1_returnsTrue() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        Point insideSquare = new Point(1, 1);

        // Assert
        assertTrue("Point (1, 1) should be inside the ComplexZone (via the square)", complex.contains(insideSquare));
    }

    @Test
    public void contains_pointIsInsideComponent2_returnsTrue() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        Point insideCircle = new Point(11, 0);

        // Assert
        assertTrue("Point (11, 0) should be inside the ComplexZone (via the circle)", complex.contains(insideCircle));
    }

    @Test
    public void contains_pointIsOutsideBoth_returnsFalse() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        Point outsideAll = new Point(6, 0); // Point between the square (x=2) and the circle (x=8)

        // Assert
        assertFalse("Point (6, 0) should be outside the ComplexZone", complex.contains(outsideAll));
    }

    @Test
    public void contains_pointIsOnBoundary_returnsTrue() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        Point onSquareBoundary = new Point(2, 0);

        // Assert
        assertTrue("Point (2, 0) is on the boundary of the square component", complex.contains(onSquareBoundary));
    }

    // --- Tests for distanceTo(Point) ---

    @Test
    public void distanceToPoint_pointIsInside_returnsZero() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        Point insidePoint = new Point(1, 1);

        // Assert
        assertEquals(0.0, complex.distanceTo(insidePoint), DELTA);
    }

    @Test
    public void distanceToPoint_pointIsOutside_returnsMinimumDistance() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        Point testPoint = new Point(5, 0); // Equidistant from square (dist 3) and circle (dist 5)

        // Act
        // Distance to square: 5 - 2 = 3.0
        // Distance to circle: |5 - 10| - 2 = 3.0
        double distance = complex.distanceTo(testPoint);

        // Assert
        assertEquals(3.0, distance, DELTA);
    }

    @Test
    public void distanceToPoint_pointClosestToCircle_returnsCircleDistance() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        Point testPoint = new Point(15, 0);

        // Act
        // Distance to square: 15 - 2 = 13.0
        // Distance to circle: |15 - 10| - 2 = 3.0
        double distance = complex.distanceTo(testPoint);

        // Assert
        assertEquals(3.0, distance, DELTA);
    }

    // --- Tests for distanceTo(Zone) ---

    @Test
    public void distanceToZone_separatedZone_returnsCorrectDistance() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        // A small circle far away
        CircleZone farCircle = new CircleZone(new Point(20, 0), 1.0);

        // Act
        // Dist to square: |20 - 0| - 2 - 1 = 17.0 (using circle's radius for farCircle)
        // Dist to complex circle: |20 - 10| - 2 - 1 = 7.0
        double distance = complex.distanceTo(farCircle);

        // Assert
        assertEquals(7.0, distance, DELTA);
    }

    @Test
    public void distanceToZone_overlappingZone_returnsZero() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone();
        // A circle that overlaps the square component
        CircleZone overlappingCircle = new CircleZone(new Point(3, 0), 2.0); // Extends to x=1, overlaps square at x=2

        // Act
        // Distance to square component is 0.0 (they overlap)
        double distance = complex.distanceTo(overlappingCircle);

        // Assert
        assertEquals(0.0, distance, DELTA);
    }

    @Test
    public void distanceToZone_ComplexZoneToComplexZone_returnsZeroForOverlap() {
        // Arrange
        CompositeZone complex1 = createSeparatedComplexZone();
        CompositeZone complex2 = createOverlappingComplexZone(); // Overlaps C1's square

        // Act
        double distance = complex1.distanceTo(complex2);

        // Assert
        assertEquals(0.0, distance, DELTA);
    }

    // --- Tests for isFullyInside(Zone) ---

    @Test
    public void isFullyInside_allComponentsInside_returnsTrue() {
        // Arrange
        // Complex Zone components: Square (0,0, 4x4) and Circle (10,0, R2)
        CompositeZone complex = createSeparatedComplexZone();

        // Target Zone: A very large polygon covering both components
        PolygonZone hugeZone = new PolygonZone(
                new Point(-10, 10), new Point(30, 10), new Point(30, -10), new Point(-10, -10)
        );

        // Assert
        assertTrue(complex.isFullyInside(hugeZone));
    }

    @Test
    public void isFullyInside_oneComponentOutside_returnsFalse() {
        // Arrange
        // Complex Zone components: Square (0,0, 4x4) and Circle (10,0, R2)
        CompositeZone complex = createSeparatedComplexZone();

        // Target Zone: Only covers the square component
        PolygonZone smallZone = new PolygonZone(
                new Point(-3, 3), new Point(3, 3), new Point(3, -3), new Point(-3, -3)
        );

        // Assert
        assertFalse("The circle component is outside the small zone", complex.isFullyInside(smallZone));
    }

    // --- Tests for Movement and Position ---

    @Test
    public void getPosition_returnsAverageCenter() {
        // Arrange
        // Square center (0, 0), Circle center (10, 0)
        CompositeZone complex = createSeparatedComplexZone();

        // Act
        // Expected center: (0 + 10) / 2 = 5.0, (0 + 0) / 2 = 0.0
        Point center = complex.getPosition();

        // Assert
        assertEquals(5.0, center.getX(), DELTA);
        assertEquals(0.0, center.getY(), DELTA);
    }

    @Test
    public void moveBy_movesAllComponents() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone(); // Center (5, 0)

        // Move by (5, 5)
        complex.moveBy(5, 5);

        // Act & Assert
        // New center of the ComplexZone should be (5 + 5, 0 + 5) = (10, 5)
        Point newCenter = complex.getPosition();
        assertEquals(10.0, newCenter.getX(), DELTA);
        assertEquals(5.0, newCenter.getY(), DELTA);

        // The original component centers must have moved
        // Original square center (0, 0) -> New position (5, 5)
        assertTrue(complex.contains(new Point(5, 5)));

        // Original circle center (10, 0) -> New position (15, 5)
        assertTrue(complex.contains(new Point(15, 5)));
    }

    @Test
    public void setPosition_movesToNewCenter() {
        // Arrange
        CompositeZone complex = createSeparatedComplexZone(); // Current center (5, 0)

        // Act
        // Set new center to (20, 10)
        complex.setPosition(20, 10);

        // Delta movement: (20 - 5) = 15, (10 - 0) = 10

        // Assert
        // New center should be (20, 10)
        Point newCenter = complex.getPosition();
        assertEquals(20.0, newCenter.getX(), DELTA);
        assertEquals(10.0, newCenter.getY(), DELTA);

        // Check if a point relative to the new center is inside
        // Original relative position of square center: -5 from complex center
        // New square center: 20 - 5 = 15, 10 - 0 = 10. Square is centered at (15, 10)
        assertTrue(complex.contains(new Point(15, 10)));
    }
}
