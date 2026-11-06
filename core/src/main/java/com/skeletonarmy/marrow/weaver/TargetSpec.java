package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

/** A target position together with the policy for its heading. */
final class TargetSpec {
    private final Point point;
    private final HeadingPolicy policy;

    TargetSpec(Point point, HeadingPolicy policy) {
        validate(point, policy);

        this.point = point;
        this.policy = policy;
    }

    /** Validates a target before it is stored. */
    static void validate(Point point, HeadingPolicy policy) {
        if (point == null) {
            throw new IllegalArgumentException("Target point is required");
        }

        if (policy == null) {
            throw new IllegalArgumentException("Heading policy is required");
        }

        requireFinitePose(point);

        if (policy == HeadingPolicy.LOCKED && !point.hasHeading()) {
            throw new IllegalArgumentException("A locked target requires a heading");
        }
    }

    /** Rejects non-finite positions. A NaN heading means no heading and is allowed. */
    static void requireFinitePose(Point point) {
        if (!Double.isFinite(point.getX()) || !Double.isFinite(point.getY())) {
            throw new IllegalArgumentException("Point position must be finite: " + point);
        }

        if (Double.isInfinite(point.getHeadingRad())) {
            throw new IllegalArgumentException("Point heading must not be infinite: " + point);
        }
    }

    Point getPoint() {
        return point;
    }

    HeadingPolicy getPolicy() {
        return policy;
    }

    boolean hasLockedHeading() {
        return policy == HeadingPolicy.LOCKED;
    }

    double getX() {
        return point.getX();
    }

    double getY() {
        return point.getY();
    }

    double getHeadingRad() {
        return point.getHeadingRad();
    }

    /** Returns this target's position without the heading. */
    Point position() {
        return new Point(point.getX(), point.getY());
    }

    double distanceTo(Point other) {
        return point.distanceTo(other);
    }
}
