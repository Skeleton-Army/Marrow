package com.skeletonarmy.marrow.bezier;

import java.util.Objects;

public class Waypoint {

    private final Pose pose;
    private final String tag;

    public Waypoint(Pose pose) {
        this(pose, null);
    }

    public Waypoint(double x, double y) {
        this(new Pose(x, y), null);
    }

    public Waypoint(double x, double y, double headingRad) {
        this(new Pose(x, y, headingRad), null);
    }

    public Waypoint(Pose pose, String tag) {
        this.pose = Objects.requireNonNull(pose);
        this.tag = tag;
    }

    public Pose getPose() {
        return pose;
    }

    public double getX() {
        return pose.getX();
    }

    public double getY() {
        return pose.getY();
    }

    public Double getHeading() {
        return Double.isNaN(pose.getHeadingRad()) ? null : pose.getHeadingRad();
    }

    public String getTag() {
        return tag;
    }
}
