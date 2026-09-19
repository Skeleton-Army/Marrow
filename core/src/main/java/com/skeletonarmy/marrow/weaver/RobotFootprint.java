package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles robot footprint representation for collision checks.
 */
final class RobotFootprint {

    private RobotFootprint() {
    }

    static PolygonZone asZone(Point center, double headingRad, double size) {
        return new PolygonZone(center, size, size, headingRad);
    }

    static List<Point> samplePoints(Point center, double headingRad, double size) {
        List<Point> pts = new ArrayList<>();
        if (size <= 0) {
            pts.add(center);
            return pts;
        }

        double half = size / 2.0;
        double cos = Math.cos(headingRad);
        double sin = Math.sin(headingRad);

        List<double[]> offsets = new ArrayList<>();
        offsets.add(new double[]{half, half});
        offsets.add(new double[]{half, -half});
        offsets.add(new double[]{-half, -half});
        offsets.add(new double[]{-half, half});

        double[] fractions = {-0.5, -0.25, 0, 0.25, 0.5};
        for (double f : fractions) {
            offsets.add(new double[]{half, f * size});
            offsets.add(new double[]{-half, f * size});
            offsets.add(new double[]{f * size, half});
            offsets.add(new double[]{f * size, -half});
        }

        pts.add(center);
        for (double[] o : offsets) {
            pts.add(new Point(center.getX() + o[0] * cos - o[1] * sin, center.getY() + o[0] * sin + o[1] * cos));
        }
        return pts;
    }
}
