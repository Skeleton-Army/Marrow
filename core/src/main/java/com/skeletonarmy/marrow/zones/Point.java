package com.skeletonarmy.marrow.zones;

import java.util.Locale;

import androidx.annotation.NonNull;

public class Point {
    private final double x;
    private final double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double distanceTo(Point other) {
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "Point(x=%.3f, y=%.3f)", x, y);
    }
}
