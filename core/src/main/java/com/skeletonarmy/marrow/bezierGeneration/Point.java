package com.skeletonarmy.marrow.bezierGeneration;

public class Point {
    public double x, y;
    public Point(double x, double y) { this.x = x; this.y = y; }

    public double distanceTo(Point other) {
        return Math.hypot(this.x - other.x, this.y - other.y);
    }
}
