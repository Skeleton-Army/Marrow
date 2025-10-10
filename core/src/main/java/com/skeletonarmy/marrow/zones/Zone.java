package com.skeletonarmy.marrow.zones;

public interface Zone {
    Point getPosition();
    boolean contains(Point point);
    boolean isInside(Zone zone);
    boolean isFullyInside(Zone zone);
    double distanceTo(Point point);
    double distanceTo(Zone zone);
    double distanceToBoundary(Point point);
    void moveBy(double deltaX, double deltaY);
    void setPosition(double posX, double posY);
}
