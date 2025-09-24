package com.skeletonarmy.marrow.bezierGeneration;

public class PolygonObstacle implements Obstacle {
    public final Point[] corners;

    public PolygonObstacle(Point... points) {
        if (points.length < 3) throw new IllegalArgumentException("Not enough points to create a polygon. Minimum is 3.");
        this.corners = points;
    }

    public PolygonObstacle(Point center, double width, double height, double angle) {
        double dx = width / 2.0;
        double dy = height / 2.0;

        Point[] corners = new Point[] {
                new Point(-dx, -dy),
                new Point(dx, -dy),
                new Point(dx, -dy),
                new Point(-dx, dy)
        };

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        for (Point corner : corners) {
            double x = corner.x;
            double y = corner.y;
            double rx = x * cos - y * sin;
            double ry = x * sin + y * cos;
            corner.x = rx + center.x;
            corner.y = ry + center.y;
        }

        this.corners = corners;
    }

    public PolygonObstacle(Point point1, Point point2, double thickness) {
        double dx = point2.x - point1.x;
        double dy = point2.y - point1.y;
        double length = Math.hypot(dx, dy);

        if (length == 0) throw new IllegalArgumentException("Points should not overlap in a polygon.");

        dx /= length;
        dy /= length;

        double px = -dy * (thickness / 2);
        double py = dx * (thickness / 2);

        this.corners = new Point[] {
                new Point(point1.x + px, point1.y + py),
                new Point(point1.x - px, point1.y - py),
                new Point(point2.x - px, point2.y - py),
                new Point(point2.x + px, point2.y + py)
        };
    }

    @Override
    public boolean isOverlapping(Point point) {
        boolean isInside = false;
        int numVertices = corners.length;
        Point currentVertex, nextVertex;

        for (int i = 0, j = numVertices - 1; i < numVertices; j = i++) {
            currentVertex = corners[i];
            nextVertex = corners[j];

            if (((currentVertex.y > point.y) != (nextVertex.y > point.y)) &&
                    (point.x < (nextVertex.x - currentVertex.x) * (point.y - currentVertex.y) / (nextVertex.y - currentVertex.y) + currentVertex.x)) {
                isInside = !isInside;
            }
        }
        return isInside;
    }
}
