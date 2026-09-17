package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;

import java.util.Collections;
import java.util.List;

/**
 * The output of a path generation, containing the geometry and execution metadata.
 */
public final class BezierResult {

    private final BezierPath path;
    private final List<Double> segmentEndHeadingsRad;

    BezierResult(BezierPath path, List<Double> segmentEndHeadingsRad) {
        this.path = path;
        this.segmentEndHeadingsRad = segmentEndHeadingsRad;
    }

    public BezierPath getPath() {
        return path;
    }

    public List<BezierCurve> getSegments() {
        return path.getSegments();
    }

    public List<Point> getControlPoints(int segmentIndex) {
        return path.getSegments().get(segmentIndex).getControlPoints();
    }

    public Point[] getControlPointArray(int segmentIndex) {
        return path.getSegments().get(segmentIndex).getControlPointArray();
    }

    public List<Double> getSegmentEndHeadingsRad() {
        return Collections.unmodifiableList(segmentEndHeadingsRad);
    }
}
