package com.skeletonarmy.marrow.zones;

public interface Zone {
    boolean contains(Point point);
    boolean isInside(Zone zone);
    boolean isFullyInside(Zone zone);
    double distanceTo(Point point);
    double distanceTo(Zone zone);
}
