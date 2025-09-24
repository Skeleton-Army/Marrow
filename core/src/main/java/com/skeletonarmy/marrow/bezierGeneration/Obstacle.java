package com.skeletonarmy.marrow.bezierGeneration;

/**
 * Represents an obstacle on the field.
 */
public interface Obstacle {
    boolean isOverlapping(Point point);
}
