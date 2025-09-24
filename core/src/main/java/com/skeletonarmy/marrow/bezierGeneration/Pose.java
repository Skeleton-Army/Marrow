package com.skeletonarmy.marrow.bezierGeneration;

/**
 * Represents a 2D pose with position and heading.
 */
public class Pose {
    public final double x;
    public final double y;
    public final double headingRad;

    public Pose(double x, double y, double headingRad) {
        this.x = x;
        this.y = y;
        this.headingRad = headingRad;
    }

    public Point getPosition() {
        return new Point(x, y);
    }

    public double getHeadingRad() {
        return headingRad;
    }
}
