package com.skeletonarmy.marrow.weaver;

public class PathPose {

    private final double x;
    private final double y;
    private final double headingRad;

    public PathPose(double x, double y, double headingRad) {
        this.x = x;
        this.y = y;
        this.headingRad = headingRad;
    }

    public PathPose(double x, double y) {
        this(x, y, Double.NaN);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getHeadingRad() {
        return headingRad;
    }
}
