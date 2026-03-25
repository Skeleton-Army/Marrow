package com.skeletonarmy.marrow.zones;

/**
 * A polygon-based zone on the field.
 */
public class PolygonZone implements Zone {
    private final Point[] corners;
    private double rotation;

    private Point cachedCentroid;
    private boolean centroidDirty;

    public PolygonZone(Point... points) {
        if (points.length < 3) throw new IllegalArgumentException("Not enough points to create a polygon. Minimum is 3.");
        this.corners = points;
        this.rotation = 0.0;
        this.centroidDirty = true;
    }

    public PolygonZone(double width, double height) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;

        this.corners = new Point[] {
                new Point(-halfWidth, -halfHeight),
                new Point(halfWidth, -halfHeight),
                new Point(halfWidth, halfHeight),
                new Point(-halfWidth, halfHeight)
        };
        this.rotation = 0.0;
        this.centroidDirty = true;
    }

    public PolygonZone(Point center, double width, double height) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double centerX = center.getX();
        double centerY = center.getY();

        this.corners = new Point[] {
                new Point(-halfWidth + centerX, -halfHeight + centerY),
                new Point(halfWidth + centerX, -halfHeight + centerY),
                new Point(halfWidth + centerX, halfHeight + centerY),
                new Point(-halfWidth + centerX, halfHeight + centerY)
        };
        this.rotation = 0.0;
        this.cachedCentroid = center;
        this.centroidDirty = false;
    }

    public PolygonZone(Point center, double width, double height, double angle) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;
        double centerX = center.getX();
        double centerY = center.getY();

        Point[] baseCorners = new Point[] {
                new Point(-halfWidth, -halfHeight),
                new Point(halfWidth, -halfHeight),
                new Point(halfWidth, halfHeight),
                new Point(-halfWidth, halfHeight)
        };

        Point[] rotatedCorners = rotatePolygon(baseCorners, angle);
        Point[] finalCorners = new Point[rotatedCorners.length];
        for (int i = 0; i < rotatedCorners.length; i++) {
            finalCorners[i] = new Point(
                    rotatedCorners[i].getX() + centerX,
                    rotatedCorners[i].getY() + centerY
            );
        }

        this.corners = finalCorners;
        this.rotation = angle;
        this.cachedCentroid = center;
        this.centroidDirty = false;
    }

    public PolygonZone(Point point1, Point point2, double thickness) {
        double dx = point2.getX() - point1.getX();
        double dy = point2.getY() - point1.getY();
        double length = Math.hypot(dx, dy);

        if (length == 0) throw new IllegalArgumentException("Points should not overlap in a polygon.");

        double px = -dy * (thickness / 2) / length;
        double py = dx * (thickness / 2) / length;

        this.corners = new Point[] {
                new Point(point1.getX() + px, point1.getY() + py),
                new Point(point1.getX() - px, point1.getY() - py),
                new Point(point2.getX() - px, point2.getY() - py),
                new Point(point2.getX() + px, point2.getY() + py)
        };
        this.rotation = 0.0;
        this.centroidDirty = true;
    }

    private void recalculateCentroid() {
        if (!centroidDirty) return;

        double sumX = 0.0;
        double sumY = 0.0;
        for (Point corner : corners) {
            sumX += corner.getX();
            sumY += corner.getY();
        }
        cachedCentroid = new Point(sumX / corners.length, sumY / corners.length);
        centroidDirty = false;
    }

    @Override
    public Point getPosition() {
        recalculateCentroid();
        return cachedCentroid;
    }

    public Point[] getCorners() {
        return corners.clone();
    }

    public double getRotation() {
        return rotation;
    }

    public double getRotationDegrees() {
        return Math.toDegrees(rotation);
    }

    /**
     * Determines if a point is contained within this polygon zone.
     *
     * @param point The point to check for containment
     * @return True if the point is inside or on the boundary of the polygon
     */
    @Override
    public boolean contains(Point point) {
        // Early exit: check if exactly on boundary
        double distSqToBoundary = distanceToBoundarySq(point);
        final double BOUNDARY_EPSILON_SQ = 1e-18; // 1e-9 squared

        if (distSqToBoundary <= BOUNDARY_EPSILON_SQ) {
            return true;
        }

        // Ray casting algorithm
        int crossings = 0;
        int numVertices = corners.length;

        for (int i = 0; i < numVertices; i++) {
            Point currentVertex = corners[i];
            Point nextVertex = corners[(i + 1) % numVertices];

            double currY = currentVertex.getY();
            double nextY = nextVertex.getY();
            double pointY = point.getY();

            if (((currY > pointY) != (nextY > pointY))) {
                double currX = currentVertex.getX();
                double nextX = nextVertex.getX();
                double pointX = point.getX();

                if (pointX < (nextX - currX) * (pointY - currY) / (nextY - currY) + currX) {
                    crossings++;
                }
            }
        }

        return (crossings % 2 == 1);
    }

    @Override
    public boolean isInside(Zone zone) {
        return this.distanceTo(zone) == 0.0;
    }

    @Override
    public boolean isFullyInside(Zone zone) {
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

        for (int i = 0; i < numVertices; i++) {
            Point currentVertex = corners[i];
            Point nextVertex = corners[(i + 1) % numVertices];

            double distSq = distancePointToSegmentSq(point, currentVertex, nextVertex);
            minDistanceSq = Math.min(minDistanceSq, distSq);

            // Early exit if we find a very close point
            if (minDistanceSq < 1e-12) break;
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
            double polygonDistanceToCenter = this.distanceTo(other.getPosition());
            return Math.max(0, polygonDistanceToCenter - other.getRadius());
        }

        if (zone instanceof PolygonZone) {
            PolygonZone other = (PolygonZone) zone;

            // Quick bounding box check first
            if (!boundingBoxesOverlap(this, other)) {
                // Non-overlapping bounding boxes - use simpler distance calc
                return distanceBetweenPolygonsSimple(this, other);
            }

            // Full collision check for potentially overlapping boxes
            int n1 = this.corners.length;
            int n2 = other.corners.length;

            // Check edge intersections (only needed if bounding boxes overlap)
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

            // Check containment
            for (Point vertex : this.corners) {
                if (other.contains(vertex)) {
                    return 0.0;
                }
            }
            for (Point vertex : other.corners) {
                if (this.contains(vertex)) {
                    return 0.0;
                }
            }

            // Calculate minimum distance
            double minDistance = Double.MAX_VALUE;
            for (Point vertex : this.corners) {
                double dist = other.distanceTo(vertex);
                minDistance = Math.min(minDistance, dist);
            }
            for (Point vertex : other.corners) {
                double dist = this.distanceTo(vertex);
                minDistance = Math.min(minDistance, dist);
            }

            return minDistance;
        }

        if (zone instanceof CompositeZone) {
            return zone.distanceTo(this);
        }

        return Double.NaN;
    }

    /**
     * Calculates the shortest distance from the given point to the zone's boundary.
     * 
     * @param point The point to measure to
     * @return The minimum distance to the boundary
     */
    @Override
    public double distanceToBoundary(Point point) {
        return Math.sqrt(distanceToBoundarySq(point));
    }

    private double distanceToBoundarySq(Point point) {
        double minDistanceSq = Double.MAX_VALUE;
        int numVertices = corners.length;

        for (int i = 0; i < numVertices; i++) {
            Point currentVertex = corners[i];
            Point nextVertex = corners[(i + 1) % numVertices];

            double distSq = distancePointToSegmentSq(point, currentVertex, nextVertex);
            minDistanceSq = Math.min(minDistanceSq, distSq);
        }

        return minDistanceSq;
    }

    /**
     * Moves the polygon by the specified offset.
     *
     * @param deltaX The amount to move in the X direction
     * @param deltaY The amount to move in the Y direction
     */
    @Override
    public void moveBy(double deltaX, double deltaY) {
        // Modify in-place instead of creating new Point objects
        for (int i = 0; i < corners.length; i++) {
            Point old = corners[i];
            corners[i] = new Point(old.getX() + deltaX, old.getY() + deltaY);
        }
        centroidDirty = true;
    }

    /**
     * Moves the polygon to a new position by translating all corners.
     *
     * @param posX The new X position for the polygon's center
     * @param posY The new Y position for the polygon's center
     */
    @Override
    public void setPosition(double posX, double posY) {
        recalculateCentroid();
        Point center = cachedCentroid;

        double deltaX = posX - center.getX();
        double deltaY = posY - center.getY();

        for (int i = 0; i < corners.length; i++) {
            Point old = corners[i];
            corners[i] = new Point(old.getX() + deltaX, old.getY() + deltaY);
        }

        // Update cached centroid directly
        cachedCentroid = new Point(posX, posY);
        centroidDirty = false;
    }

    /**
     * Rotates the polygon around its center by the specified angle.
     *
     * @param angleRadians The angle to rotate by in radians
     */
    public void rotateBy(double angleRadians) {
        if (Math.abs(angleRadians) < 1e-12) return;

        recalculateCentroid();
        Point center = cachedCentroid;
        double centerX = center.getX();
        double centerY = center.getY();

        // Precompute trig values once
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);

        for (int i = 0; i < corners.length; i++) {
            double x = corners[i].getX() - centerX;
            double y = corners[i].getY() - centerY;

            double newX = x * cos - y * sin;
            double newY = x * sin + y * cos;

            corners[i] = new Point(newX + centerX, newY + centerY);
        }

        rotation += angleRadians;
    }

    /**
     * Rotates the polygon around its center by the specified angle in degrees.
     *
     * @param angleDegrees The angle to rotate by in degrees
     */
    public void rotateByDegrees(double angleDegrees) {
        rotateBy(Math.toRadians(angleDegrees));
    }

    /**
     * Sets the rotation of the polygon to the specified angle.
     *
     * @param angleRadians The new rotation angle in radians
     */
    public void setRotation(double angleRadians) {
        double deltaAngle = angleRadians - rotation;
        rotateBy(deltaAngle);
    }

    /**
     * Sets the rotation of the polygon to the specified angle in degrees.
     *
     * @param angleDegrees The new rotation angle in degrees
     */
    public void setRotationDegrees(double angleDegrees) {
        setRotation(Math.toRadians(angleDegrees));
    }

    /**
     * Quick bounding box overlap check.
     */
    private static boolean boundingBoxesOverlap(PolygonZone p1, PolygonZone p2) {
        double minX1 = Double.MAX_VALUE, maxX1 = -Double.MAX_VALUE;
        double minY1 = Double.MAX_VALUE, maxY1 = -Double.MAX_VALUE;
        double minX2 = Double.MAX_VALUE, maxX2 = -Double.MAX_VALUE;
        double minY2 = Double.MAX_VALUE, maxY2 = -Double.MAX_VALUE;

        for (Point p : p1.corners) {
            minX1 = Math.min(minX1, p.getX());
            maxX1 = Math.max(maxX1, p.getX());
            minY1 = Math.min(minY1, p.getY());
            maxY1 = Math.max(maxY1, p.getY());
        }

        for (Point p : p2.corners) {
            minX2 = Math.min(minX2, p.getX());
            maxX2 = Math.max(maxX2, p.getX());
            minY2 = Math.min(minY2, p.getY());
            maxY2 = Math.max(maxY2, p.getY());
        }

        return !(maxX1 < minX2 || maxX2 < minX1 || maxY1 < minY2 || maxY2 < minY1);
    }

    /**
     * Simple distance calc for non-overlapping polygons.
     */
    private static double distanceBetweenPolygonsSimple(PolygonZone p1, PolygonZone p2) {
        double minDistance = Double.MAX_VALUE;
        for (Point v : p1.corners) {
            minDistance = Math.min(minDistance, p2.distanceTo(v));
        }
        for (Point v : p2.corners) {
            minDistance = Math.min(minDistance, p1.distanceTo(v));
        }
        return minDistance;
    }

    /**
     * Returns squared distance to avoid sqrt in hot loops.
     */
    private static double distancePointToSegmentSq(Point p, Point a, Point b) {
        double segmentLengthSq = a.distanceTo(b);
        segmentLengthSq = segmentLengthSq * segmentLengthSq;

        if (segmentLengthSq == 0.0) {
            double dx = p.getX() - a.getX();
            double dy = p.getY() - a.getY();
            return dx * dx + dy * dy;
        }

        double t = ((p.getX() - a.getX()) * (b.getX() - a.getX()) +
                (p.getY() - a.getY()) * (b.getY() - a.getY())) / segmentLengthSq;

        Point closest;
        if (t < 0.0) {
            closest = a;
        } else if (t > 1.0) {
            closest = b;
        } else {
            closest = new Point(
                    a.getX() + t * (b.getX() - a.getX()),
                    a.getY() + t * (b.getY() - a.getY())
            );
        }

        double dx = p.getX() - closest.getX();
        double dy = p.getY() - closest.getY();
        return dx * dx + dy * dy;
    }

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
    private static double distancePointToSegment(Point p, Point a, Point b) {
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
        if (Math.abs(val) <= 1e-12) return 0;
        return (val > 0) ? 1 : 2;
    }

    private static boolean onSegment(Point a, Point b, Point c) {
        return b.getX() <= Math.max(a.getX(), c.getX()) + 1e-12 &&
                b.getX() + 1e-12 >= Math.min(a.getX(), c.getX()) &&
                b.getY() <= Math.max(a.getY(), c.getY()) + 1e-12 &&
                b.getY() + 1e-12 >= Math.min(a.getY(), c.getY());
    }
}
