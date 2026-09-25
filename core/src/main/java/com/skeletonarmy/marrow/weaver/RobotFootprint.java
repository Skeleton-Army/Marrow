package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;

/**
 * Handles robot footprint representation for collision checks.
 */
final class RobotFootprint {

    private RobotFootprint() {
    }

    static Zone asZone(Point center, double headingRad, double width, double height) {
        return new PolygonZone(center, width, height, headingRad);
    }
}
