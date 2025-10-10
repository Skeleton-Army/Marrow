package com.skeletonarmy.marrow.zones;

/**
 * A polygon-based zone on the field.
 */
public class PolygonZone implements Zone {
    public final Point[] corners;

    public PolygonZone(Point... points) {
        if (points.length < 3) throw new IllegalArgumentException("Not enough points to create a polygon. Minimum is 3.");
        this.corners = points;
    }

    public PolygonZone(Point center, double width, double height) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double centerX = center.getX(); // Use getter
        double centerY = center.getY(); // Use getter

        this.corners = new Point[] {
                // Initialize the corners centered at (0,0) and then translate
                new Point(-halfWidth + centerX, -halfHeight + centerY),
                new Point(halfWidth + centerX, -halfHeight + centerY),
                new Point(halfWidth + centerX, halfHeight + centerY),
                new Point(-halfWidth + centerX, halfHeight + centerY)
        };
    }

    public PolygonZone(Point center, double width, double height, double angle) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double centerX = center.getX();
        double centerY = center.getY();

        // Initialize the corners centered at (0,0)
        Point[] baseCorners = new Point[] {
                new Point(-halfWidth, -halfHeight),
                new Point(halfWidth, -halfHeight),
                new Point(halfWidth, halfHeight),
                new Point(-halfWidth, halfHeight)
        };

        // 1. Rotate the corners (around origin)
        Point[] rotatedCorners = rotatePolygon(baseCorners, angle);

        // 2. Translate the rotated corners
        Point[] finalCorners = new Point[rotatedCorners.length];
        for (int i = 0; i < rotatedCorners.length; i++) {
            finalCorners[i] = new Point(
                    rotatedCorners[i].getX() + centerX,
                    rotatedCorners[i].getY() + centerY
            );
        }

        this.corners = finalCorners;
    }

    public PolygonZone(Point point1, Point point2, double thickness) {
        double dx = point2.getX() - point1.getX();
        double dy = point2.getY() - point1.getY();
        double length = Math.hypot(dx, dy);

        if (length == 0) throw new IllegalArgumentException("Points should not overlap in a polygon.");

        double px = -dy * (thickness / 2) / length;
        double py = dx * (thickness / 2) / length;

        // Use getters and construct new immutable Points
        this.corners = new Point[] {
                new Point(point1.getX() + px, point1.getY() + py),
                new Point(point1.getX() - px, point1.getY() - py),
                new Point(point2.getX() - px, point2.getY() - py),
                new Point(point2.getX() + px, point2.getY() + py)
        };
    }

    /**
     * Determines if a point is contained within this polygon zone.
     *
     * @param point The point to check for containment
     * @return True if the point is inside or on the boundary of the polygon
     */
    @Override
    public boolean contains(Point point) {
        // Treat points on the boundary (edges or vertices) as inside
        if (distanceToBoundary(point) <= 1e-9) {
            return true;
        }

        int crossings = 0;
        int numVertices = corners.length;
        Point currentVertex, nextVertex;

        for (int i = 0; i < numVertices; i++) {
            currentVertex = corners[i];
            nextVertex = corners[(i + 1) % numVertices];

            // Ray casting algorithm: checks if a horizontal ray cast from the point intersects the edge an odd number of times.
            if (((currentVertex.getY() > point.getY()) != (nextVertex.getY() > point.getY())) &&
                    (point.getX() < (nextVertex.getX() - currentVertex.getX()) * (point.getY() - currentVertex.getY()) / (nextVertex.getY() - currentVertex.getY()) + currentVertex.getX())) {
                crossings++;
            }
        }

        return (crossings % 2 == 1);
    }

    /**
     * Checks if this polygon partially overlaps or is fully inside another zone.
     *
     * @param zone The zone to check against
     * @return True if this polygon is partially or fully inside the other zone
     */
    @Override
    public boolean isInside(Zone zone) {
        return this.distanceTo(zone) == 0.0;
    }

    /**
     * Checks if this polygon is fully contained within another zone.
     *
     * @param zone The zone to check against
     * @return True if this zone is fully contained within the other zone
     */
    @Override
    public boolean isFullyInside(Zone zone) {
        // Polygon A is fully inside Zone B if ALL vertices of A are inside B.
        // NOTE: This is sufficient for convex polygons inside another convex polygon/circle,
        // but can fail for complex or concave shapes. This is fine for our case.
        for (Point corner : this.corners) {
            if (!zone.contains(corner)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the shortest distance from the polygon's perimeter to a point.
     * If the point is inside the zone, the distance is 0.
     *
     * @param point The point to measure the distance to
     * @return The minimum distance to the zone
     */
    @Override
    public double distanceTo(Point point) {
        if (this.contains(point)) {
            return 0.0;
        }

        double minDistanceSq = Double.MAX_VALUE;
        int numVertices = corners.length;
        Point currentVertex, nextVertex;

        // Find the shortest distance from the point to any edge segment
        for (int i = 0; i < numVertices; i++) {
            currentVertex = corners[i];
            nextVertex = corners[(i + 1) % numVertices];

            double distance = distancePointToSegment(point, currentVertex, nextVertex);
            minDistanceSq = Math.min(minDistanceSq, distance * distance);
        }

        return Math.sqrt(minDistanceSq);
    }

    /**
     * Calculates the shortest distance between this polygon and another zone.
     * If the zones overlap, the distance is 0.
     *
     * @param zone The zone to measure the distance to
     * @return The minimum distance between the two zones
     */
    @Override
    public double distanceTo(Zone zone) {
        if (zone instanceof CircleZone) {
            CircleZone other = (CircleZone) zone;

            // Find the closest point of the circle's center to the polygon.
            double polygonDistanceToCenter = this.distanceTo(other.center);

            // The distance is the distance to the center minus the circle's radius.
            return Math.max(0, polygonDistanceToCenter - other.radius);
        }

        if (zone instanceof PolygonZone) {
            PolygonZone other = (PolygonZone) zone;
            double minDistance = Double.MAX_VALUE;

            // If any edges intersect, polygons overlap → distance is 0
            int n1 = this.corners.length;
            int n2 = other.corners.length;
            for (int i = 0; i < n1; i++) {
                Point a1 = this.corners[i];
                Point a2 = this.corners[(i + 1) % n1];
                for (int j = 0; j < n2; j++) {
                    Point b1 = other.corners[j];
                    Point b2 = other.corners[(j + 1) % n2];
                    if (segmentsIntersect(a1, a2, b1, b2)) {
                        return 0.0;
                    }
                }
            }

            // Check distance from this polygon's vertices to the other polygon's edges
            for (Point vertex : this.corners) {
                if (other.contains(vertex)) {
                    return 0.0;
                }
                double dist = other.distanceTo(vertex);
                minDistance = Math.min(minDistance, dist);
            }

            // Check distance from other polygon's vertices to this polygon's edges
            for (Point vertex : other.corners) {
                if (this.contains(vertex)) {
                    return 0.0;
                }
                double dist = this.distanceTo(vertex);
                minDistance = Math.min(minDistance, dist);
            }

            return minDistance;
        }

        return Double.NaN;
    }

    /**
     * Calculates the shortest distance from the given point to the zone's boundary.
     * 
     * @param point The point to measure to
     * @return The minimum distance to the boundary
     */
    public double distanceToBoundary(Point point) {
        double minDistanceSq = Double.MAX_VALUE;
        int numVertices = corners.length;
        Point currentVertex, nextVertex;

        for (int i = 0; i < numVertices; i++) {
            currentVertex = corners[i];
            nextVertex = corners[(i + 1) % numVertices];

            double distance = distancePointToSegment(point, currentVertex, nextVertex);
            minDistanceSq = Math.min(minDistanceSq, distance * distance);
        }

        return Math.sqrt(minDistanceSq);
    }

    // ----- HELPERS -----

    /**
     * Rotates a polygon (an array of points) around the origin.
     *
     * @param points The polygon to rotate
     * @param angleRad The angle in radians to rotate by
     * @return The new array of rotated Point objects
     */
    private static Point[] rotatePolygon(Point[] points, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        Point[] newPoints = new Point[points.length];

        for (int i = 0; i < points.length; i++) {
            Point point = points[i];
            double x = point.getX();
            double y = point.getY();

            double newX = x * cos - y * sin;
            double newY = x * sin + y * cos;

            newPoints[i] = new Point(newX, newY);
        }

        return newPoints;
    }

    /**
     * Calculates the shortest distance from a point to a line segment defined by two points.
     *
     * @param p The point
     * @param a The start of the line segment
     * @param b The end of the line segment
     * @return The shortest distance
     */
    private double distancePointToSegment(Point p, Point a, Point b) {
        double segmentLengthSq = a.distanceTo(b) * a.distanceTo(b);

        if (segmentLengthSq == 0.0) return p.distanceTo(a);

        // Vector AB: (bx - ax, by - ay)
        // Vector AP: (px - ax, py - ay)
        double t = ((p.getX() - a.getX()) * (b.getX() - a.getX()) + (p.getY() - a.getY()) * (b.getY() - a.getY())) / segmentLengthSq;

        // Find the closest point on the line *segment*
        Point closest;
        if (t < 0.0) {
            closest = a;
        } else if (t > 1.0) {
            closest = b;
        } else {
            // Point is between a and b. Project P onto line AB.
            closest = new Point(
                    a.getX() + t * (b.getX() - a.getX()),
                    a.getY() + t * (b.getY() - a.getY())
            );
        }

        return p.distanceTo(closest);
    }

    private static boolean segmentsIntersect(Point p1, Point q1, Point p2, Point q2) {
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        if (o1 != o2 && o3 != o4) return true;

        if (o1 == 0 && onSegment(p1, p2, q1)) return true;
        if (o2 == 0 && onSegment(p1, q2, q1)) return true;
        if (o3 == 0 && onSegment(p2, p1, q2)) return true;
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;

        return false;
    }

    private static int orientation(Point a, Point b, Point c) {
        double val = (b.getY() - a.getY()) * (c.getX() - b.getX()) -
                (b.getX() - a.getX()) * (c.getY() - b.getY());
        if (Math.abs(val) <= 1e-12) return 0; // colinear
        return (val > 0) ? 1 : 2; // 1: clockwise, 2: counterclockwise
    }

    private static boolean onSegment(Point a, Point b, Point c) {
        return b.getX() <= Math.max(a.getX(), c.getX()) + 1e-12 &&
                b.getX() + 1e-12 >= Math.min(a.getX(), c.getX()) &&
                b.getY() <= Math.max(a.getY(), c.getY()) + 1e-12 &&
                b.getY() + 1e-12 >= Math.min(a.getY(), c.getY());
    }
}
