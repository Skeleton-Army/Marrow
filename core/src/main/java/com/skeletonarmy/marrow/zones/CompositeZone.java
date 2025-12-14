package com.skeletonarmy.marrow.zones;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A composite zone that is the union of all its component zones.
 */
public class CompositeZone implements Zone {
    private final List<Zone> zones;

    /**
     * Constructs a CompositeZone from a collection of Zone objects.
     * @param zones The zones that make up this complex zone.
     */
    public CompositeZone(Zone... zones) {
        if (zones == null || zones.length == 0) {
            throw new IllegalArgumentException("CompositeZone must contain at least one zone.");
        }

        this.zones = Collections.unmodifiableList(Arrays.asList(zones));
    }

    /**
     * Gets the approximate geometric center (centroid) of the complex zone,
     * calculated as the average center of its component zones.
     * @return The center point.
     */
    @Override
    public Point getPosition() {
        double sumX = 0.0;
        double sumY = 0.0;

        for (Zone zone : zones) {
            Point center = zone.getPosition();
            sumX += center.getX();
            sumY += center.getY();
        }

        return new Point(sumX / zones.size(), sumY / zones.size());
    }

    /**
     * Determines if a point is contained within this complex zone.
     * It is contained if it is inside ANY of the component zones.
     *
     * @param point The point to check for containment
     * @return True if the point is inside or on the boundary of any component zone
     */
    @Override
    public boolean contains(Point point) {
        for (Zone zone : zones) {
            if (zone.contains(point)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this complex zone partially overlaps or is fully inside another zone.
     *
     * @param zone The zone to check against
     * @return True if this complex zone overlaps the other zone
     */
    @Override
    public boolean isInside(Zone zone) {
        return this.distanceTo(zone) <= 1e-9;
    }

    /**
     * Checks if this complex zone is fully contained within another zone.
     *
     * @param zone The zone to check against
     * @return True if this zone is fully contained within the other zone
     */
    @Override
    public boolean isFullyInside(Zone zone) {
        for (Zone component : zones) {
            if (!component.isFullyInside(zone)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates the shortest distance from the complex zone to a point.
     * If the point is inside the zone, the distance is 0.
     *
     * @param point The point to measure the distance to
     * @return The minimum distance to the complex zone
     */
    @Override
    public double distanceTo(Point point) {
        if (this.contains(point)) {
            return 0.0;
        }

        double minDistance = Double.MAX_VALUE;
        for (Zone zone : zones) {
            minDistance = Math.min(minDistance, zone.distanceTo(point));
        }

        return minDistance;
    }

    /**
     * Calculates the shortest distance between this complex zone and another zone.
     *
     * @param zone The zone to measure the distance to
     * @return The minimum distance between the two zones
     */
    @Override
    public double distanceTo(Zone zone) {
        double minDistance = Double.MAX_VALUE;
        for (Zone component : zones) {
            minDistance = Math.min(minDistance, component.distanceTo(zone));
        }
        return minDistance;
    }

    /**
     * Calculates the shortest distance from the given point to the complex zone's boundary.
     * Negative if inside, positive if outside.
     *
     * @param point The point to measure to
     * @return The minimum distance to the boundary.
     */
    @Override
    public double distanceToBoundary(Point point) {
        if (!this.contains(point)) {
            return distanceTo(point);
        }

        double maxNegativeDistance = Double.NEGATIVE_INFINITY;
        boolean insideAtLeastOne = false;

        for (Zone zone : zones) {
            double distToBoundary = zone.distanceToBoundary(point);
            if (distToBoundary <= 1e-9) {
                maxNegativeDistance = Math.max(maxNegativeDistance, distToBoundary);
                insideAtLeastOne = true;
            }
        }

        if (insideAtLeastOne) {
            return maxNegativeDistance;
        }

        return distanceTo(point);
    }

    /**
     * Moves the complex zone by applying the offset to all component zones.
     *
     * @param deltaX The amount to move in the X direction
     * @param deltaY The amount to move in the Y direction
     */
    @Override
    public void moveBy(double deltaX, double deltaY) {
        for (Zone zone : zones) {
            zone.moveBy(deltaX, deltaY);
        }
    }

    /**
     * Moves the complex zone to a new position by translating all component zones
     * to maintain their relative positions, centering the overall zone at the new coordinates.
     *
     * @param posX The new X position for the complex zone's center
     * @param posY The new Y position for the complex zone's center
     */
    @Override
    public void setPosition(double posX, double posY) {
        Point currentCenter = getPosition();
        double deltaX = posX - currentCenter.getX();
        double deltaY = posY - currentCenter.getY();

        moveBy(deltaX, deltaY);
    }
}
