package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

/**
 * A target point together with the policy that controls its heading.
 */
final class TargetSpec {
    private final Point point;
    private final HeadingPolicy policy;

    /**
     * Creates a target specification.
     *
     * @param point  target position and optional heading
     * @param policy heading policy
     * @throws IllegalArgumentException if a locked target has no heading
     */
    TargetSpec(Point point, HeadingPolicy policy) {
        if (point == null) {
            throw new IllegalArgumentException("Target point is required");
        }

        if (policy == null) {
            throw new IllegalArgumentException("Heading policy is required");
        }

        if (policy == HeadingPolicy.LOCKED && !point.hasHeading()) {
            throw new IllegalArgumentException("A locked target requires a heading");
        }

        this.point = point;
        this.policy = policy;
    }

    /**
     * @return the target point
     */
    Point getPoint() {
        return point;
    }

    /**
     * @return the target's heading policy
     */
    HeadingPolicy getPolicy() {
        return policy;
    }

    /**
     * @return whether the target's heading is enforced
     */
    boolean hasLockedHeading() {
        return policy == HeadingPolicy.LOCKED;
    }

    /**
     * @return the target's x coordinate
     */
    double getX() {
        return point.getX();
    }

    /**
     * @return the target's y coordinate
     */
    double getY() {
        return point.getY();
    }

    /**
     * @return the target's heading in radians, or {@link Double#NaN} when absent
     */
    double getHeadingRad() {
        return point.getHeadingRad();
    }

    /**
     * @return the target position without a heading
     */
    Point position() {
        return new Point(point.getX(), point.getY());
    }

    /**
     * @param other point to measure to
     * @return the distance from this target to {@code other}
     */
    double distanceTo(Point other) {
        return point.distanceTo(other);
    }
}
