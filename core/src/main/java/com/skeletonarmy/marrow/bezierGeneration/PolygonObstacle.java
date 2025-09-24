package com.skeletonarmy.marrow.bezierGeneration;

/**
 * A polygon-based obstacle on the field.
 * <p>
 * This class uses the Ray Casting algorithm to determine if a point
 * is inside the polygon. It also provides utility methods for geometric
 * transformations like rotation.
 */
public class PolygonObstacle implements Obstacle {
    public final Point[] corners;

    public PolygonObstacle(Point... points) {
        if (points.length < 3) throw new IllegalArgumentException("Not enough points to create a polygon. Minimum is 3.");
        this.corners = points;
    }

    public PolygonObstacle(Point center, double width, double height, double angle) {
        double halfWidth = width / 2.0;
        double halfHeight = height / 2.0;

        Point[] corners = new Point[]{
                new Point(-halfWidth, -halfHeight),
                new Point(halfWidth, -halfHeight),
                new Point(halfWidth, halfHeight),
                new Point(-halfWidth, halfHeight)
        };

        rotatePolygon(corners, angle);

        for (Point corner : corners) {
            corner.x += center.x;
            corner.y += center.y;
        }

        this.corners = corners;
    }

    public PolygonObstacle(Point point1, Point point2, double thickness) {
        double dx = point2.x - point1.x;
        double dy = point2.y - point1.y;
        double length = Math.hypot(dx, dy);

        if (length == 0) throw new IllegalArgumentException("Points should not overlap in a polygon.");

        double px = -dy * (thickness / 2) / length;
        double py = dx * (thickness / 2) / length;

        this.corners = new Point[]{
                new Point(point1.x + px, point1.y + py),
                new Point(point1.x - px, point1.y - py),
                new Point(point2.x - px, point2.y - py),
                new Point(point2.x + px, point2.y + py)
        };
    }

    /**
     * Rotates a polygon (an array of points) around the origin.
     *
     * @param points The polygon to rotate.
     * @param angleRad The angle in radians to rotate by.
     */
    private static void rotatePolygon(Point[] points, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);

        for (Point point : points) {
            double x = point.x;
            double y = point.y;
            point.x = x * cos - y * sin;
            point.y = x * sin + y * cos;
        }
    }

    @Override
    public boolean isOverlapping(Point point) {
        int crossings = 0;
        int numVertices = corners.length;
        Point currentVertex, nextVertex;

        for (int i = 0; i < numVertices; i++) {
            currentVertex = corners[i];
            nextVertex = corners[(i + 1) % numVertices];

            if (((currentVertex.y > point.y) != (nextVertex.y > point.y)) &&
                    (point.x < (nextVertex.x - currentVertex.x) * (point.y - currentVertex.y) / (nextVertex.y - currentVertex.y) + currentVertex.x)) {
                crossings++;
            }
        }

        return (crossings % 2 == 1);
    }
}
