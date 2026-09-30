package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

import java.util.Collections;
import java.util.List;

/**
 * The output of a path generation, containing the geometry and execution metadata.
 */
public final class PathResult {

    private final PathRoute path;
    private final List<Double> segmentEndHeadingsRad;
    private final int skippedTargets;

    PathResult(PathRoute path, List<Double> segmentEndHeadingsRad, int skippedTargets) {
        this.path = path;
        this.segmentEndHeadingsRad = segmentEndHeadingsRad;
        this.skippedTargets = skippedTargets;
    }

    public PathRoute getPath() {
        return path;
    }

    /**
     * @return the number of targets excluded because they were unreachable (blocked by an obstacle).
     */
    public int getSkippedTargets() {
        return skippedTargets;
    }

    public List<PathCurve> getSegments() {
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
