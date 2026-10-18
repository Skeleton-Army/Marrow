package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

/**
 * Controls whether a target's heading constrains the generated path.
 * <p>
 * A target with {@link #FREE} may be reached from any direction. A target with
 * {@link #LOCKED} is reached with the heading stored on its {@link Point}.
 */
public enum HeadingPolicy {
    /**
     * The target heading is ignored. The path arrives along its natural direction.
     */
    FREE,

    /**
     * The target must be reached with the heading stored on the target's point.
     */
    LOCKED
}
