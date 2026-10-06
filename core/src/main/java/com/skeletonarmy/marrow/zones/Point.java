package com.skeletonarmy.marrow.zones;

import androidx.annotation.NonNull;
import java.util.Locale;

public class Point {
    private final double x;
    private final double y;
    private final double headingRad;

    public Point(double x, double y) {
        this(x, y, Double.NaN);
    }

    public Point(double x, double y, double headingRad) {
        this.x = x;
        this.y = y;
        this.headingRad = headingRad;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getHeadingRad() {
        return this.headingRad;
    }

    public boolean hasHeading() {
        return !Double.isNaN(this.headingRad);
    }

    public double distanceTo(Point other) {
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    @NonNull
    @Override
    public String toString() {
        if (Double.isNaN(headingRad)) {
            return String.format(Locale.ROOT, "Point(x=%.3f, y=%.3f)", x, y);
        }

        return String.format(Locale.ROOT, "Point(x=%.3f, y=%.3f, headingRad=%.3f)", x, y, headingRad);
    }
}
