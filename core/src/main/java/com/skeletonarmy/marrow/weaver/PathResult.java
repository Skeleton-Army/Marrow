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

    /**
     * Creates a result. Package-private because results are produced by
     * {@link WeaverGenerator}.
     *
     * @param path                  the generated route
     * @param segmentEndHeadingsRad heading to hold at the end of each segment
     * @param skippedTargets        number of targets dropped as unreachable
     */
    PathResult(PathRoute path, List<Double> segmentEndHeadingsRad, int skippedTargets) {
        this.path = path;
        this.segmentEndHeadingsRad = segmentEndHeadingsRad;
        this.skippedTargets = skippedTargets;
    }

    /** @return the generated route */
    public PathRoute getPath() {
        return path;
    }

    /**
     * @return the number of targets excluded because they were unreachable (blocked by an obstacle).
     */
    public int getSkippedTargets() {
        return skippedTargets;
    }

    /** @return the route's curve segments */
    public List<PathCurve> getSegments() {
        return path.getSegments();
    }

    /**
     * Returns the control points of one segment.
     *
     * @param segmentIndex index of the segment
     * @return the control points of that segment
     */
    public List<Point> getControlPoints(int segmentIndex) {
        return path.getSegments().get(segmentIndex).getControlPoints();
    }

    /** @return a read-only list of the heading to hold at each segment end, in radians */
    public List<Double> getSegmentEndHeadingsRad() {
        return Collections.unmodifiableList(segmentEndHeadingsRad);
    }
}
