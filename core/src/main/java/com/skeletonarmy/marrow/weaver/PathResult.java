package com.skeletonarmy.marrow.weaver;

import androidx.annotation.NonNull;
import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The output of a path generation, containing the geometry and execution metadata.
 */
public final class PathResult {
    private final PathRoute path;
    private final List<Double> segmentEndHeadingsRad;
    private final List<Point> visitedTargets;
    private final List<Point> unvisitedTargets;

    /**
     * Creates a result. Package-private because results are produced by
     * {@link WeaverGenerator}.
     *
     * @param path                  the generated route
     * @param segmentEndHeadingsRad heading to hold at the end of each segment
     * @param visitedTargets        targets the route visits, in order
     * @param unvisitedTargets      targets that were not visited, either blocked or
     *                              beyond the configured target limit
     */
    PathResult(
            PathRoute path,
            List<Double> segmentEndHeadingsRad,
            List<Point> visitedTargets,
            List<Point> unvisitedTargets) {
        this.path = path;
        this.segmentEndHeadingsRad = segmentEndHeadingsRad;
        this.visitedTargets = new ArrayList<>(visitedTargets);
        this.unvisitedTargets = new ArrayList<>(unvisitedTargets);
    }

    /** @return the generated route */
    public PathRoute getPath() {
        return path;
    }

    /**
     * Returns the targets the route actually visits, in visit order.
     *
     * @return a read-only list of the visited targets
     */
    public List<Point> getVisitedTargets() {
        return Collections.unmodifiableList(visitedTargets);
    }

    /**
     * Returns the targets that were not visited, either because an obstacle blocked
     * them or because they exceeded the configured target limit.
     *
     * @return a read-only list of the unvisited targets
     */
    public List<Point> getUnvisitedTargets() {
        return Collections.unmodifiableList(unvisitedTargets);
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

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "PathResult(skippedTargets=%d, segmentEndHeadingsRad=%s, visitedTargets=%s, path=%s)",
                unvisitedTargets.size(),
                segmentEndHeadingsRad,
                visitedTargets,
                path);
    }
}
