package com.skeletonarmy.marrow.zones;

/**
 * A circle-based zone on the field.
 */
public class CircleZone implements Zone {
    private Point center;
    private final double radius;

    public CircleZone() {
        this.center = new Point(0, 0);
        this.radius = 1;
    }

    public CircleZone(double radius) {
        this.center = new Point(0, 0);
        this.radius = radius;
    }

    public CircleZone(Point center, double radius) {
        this.center = center;
        this.radius = radius;
    }
    
    /**
     * Gets the center point of the circle.
     * 
     * @return The center point
     */
    @Override
    public Point getPosition() {
        return center;
    }
    
    /**
     * Gets the radius of the circle.
     * 
     * @return The radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Determines if a point is contained within this circle zone.
     *
     * @param point The point to check for containment
     * @return True if the point is inside or on the boundary of the circle
     */
    @Override
    public boolean contains(Point point) {
        return distanceToBoundary(point) <= 0;
    }

    /**
     * Checks if this circle partially overlaps or is fully inside another zone.
     *
     * @param zone The zone to check against
     * @return True if this circle is partially or fully inside the other zone
     */
    @Override
    public boolean isInside(Zone zone) {
        return this.distanceTo(zone) == 0.0;
    }

    /**
     * Checks if this circle is fully contained within another zone.
     *
     * @param zone The zone to check against
     * @return True if this zone is fully contained within the other zone
     */
    @Override
    public boolean isFullyInside(Zone zone) {
        if (zone instanceof CircleZone) {
            CircleZone other = (CircleZone) zone;
            double centerDistance = this.center.distanceTo(other.center);
            return (centerDistance + this.radius) <= other.radius;
        }

        if (zone instanceof PolygonZone) {
            PolygonZone other = (PolygonZone) zone;

            // Must satisfy two conditions for convex polygon containment:
            // 1. Center is inside the polygon.
            if (!other.contains(this.center)) {
                return false;
            }

            // 2. Shortest distance from center to any polygon edge is >= radius.
            return other.distanceToBoundary(this.center) >= this.radius;
        }

        return false;
    }

    /**
     * Calculates the shortest distance from the circle's perimeter to a point.
     * If the point is inside the zone, the distance is 0.
     *
     * @param point The point to measure the distance to
     * @return The minimum distance to the zone
     */
    @Override
    public double distanceTo(Point point) {
        return Math.max(0, distanceToBoundary(point));
    }

    /**
     * Calculates the shortest distance between this circle and another zone.
     * If the zones overlap, the distance is 0.
     *
     * @param zone The zone to measure the distance to
     * @return The minimum distance between the two zones
     */
    @Override
    public double distanceTo(Zone zone) {
        if (zone instanceof CircleZone) {
            CircleZone other = (CircleZone) zone;
            double centerDistance = this.center.distanceTo(other.center);
            double radiiSum = this.radius + other.radius;

            // Distance is center separation minus sum of radii. Clamped at 0 for overlap.
            return Math.max(0, centerDistance - radiiSum);
        }

        if (zone instanceof PolygonZone) {
            // Distance is (shortest distance from circle's center to polygon) - circle's radius.
            PolygonZone other = (PolygonZone) zone;
            double distanceToPolygonCenter = other.distanceTo(this.center);
            return Math.max(0, distanceToPolygonCenter - this.radius);
        }

        return Double.NaN;
    }

    /**
     * Calculates the shortest distance from the given point to the zone's boundary.
     * Negative if the point is inside, positive if outside.
     * 
     * @param point The point to measure to
     * @return The minimum distance to the boundary
     */
    @Override
    public double distanceToBoundary(Point point) {
        return point.distanceTo(this.center) - this.radius;
    }
    
    /**
     * Moves the circle by the specified offset.
     * 
     * @param deltaX The amount to move in the X direction
     * @param deltaY The amount to move in the Y direction
     */
    @Override
    public void moveBy(double deltaX, double deltaY) {
        this.center = new Point(this.center.getX() + deltaX, this.center.getY() + deltaY);
    }
    
    /**
     * Moves the circle to a new position.
     * 
     * @param posX The new X position for the circle's center
     * @param posY The new Y position for the circle's center
     */
    @Override
    public void setPosition(double posX, double posY) {
        this.center = new Point(posX, posY);
    }
}
