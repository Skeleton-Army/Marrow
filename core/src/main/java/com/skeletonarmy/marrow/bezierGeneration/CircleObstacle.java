package com.skeletonarmy.marrow.bezierGeneration;

/**
 * A circle-based obstacle on the field.
 */
public class CircleObstacle implements Obstacle {
    public final Point center;
    public final double radius;

    public CircleObstacle(Point center, double radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public boolean isOverlapping(Point point) {
        return point.distanceTo(this.center) <= this.radius;
    }
}
