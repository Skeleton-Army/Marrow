package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;

/**
 * Handles robot footprint representation for collision checks.
 */
final class RobotFootprint {

    private RobotFootprint() {
    }

    static Zone asZone(Point center, double headingRad, double size) {
        return new CircleZone(center, size / 2.0);
    }
}
