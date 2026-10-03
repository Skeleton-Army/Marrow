package com.skeletonarmy.marrow.weaver;

/**
 * A 2D position with an optional heading.
 */
public class PathPose {
    private final double x;
    private final double y;
    private final double headingRad;

    /**
     * Creates a pose with an explicit heading.
     *
     * @param x          x coordinate
     * @param y          y coordinate
     * @param headingRad heading in radians, or {@link Double#NaN} for no constraint
     */
    public PathPose(double x, double y, double headingRad) {
        this.x = x;
        this.y = y;
        this.headingRad = headingRad;
    }

    /**
     * Creates a heading-free pose.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public PathPose(double x, double y) {
        this(x, y, Double.NaN);
    }

    /** @return the x coordinate */
    public double getX() {
        return x;
    }

    /** @return the y coordinate */
    public double getY() {
        return y;
    }

    /** @return the heading in radians, or {@link Double#NaN} if unconstrained */
    public double getHeadingRad() {
        return headingRad;
    }
}
