package com.skeletonarmy.marrow.weaver;

import androidx.annotation.NonNull;
import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** The generated route and the metadata produced alongside it. */
public final class PathResult {
    private final PathRoute path;
    private final List<Double> segmentEndHeadingsRad;
    private final List<Point> visitedTargets;
    private final List<Point> unvisitedTargets;

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

    public PathRoute getPath() {
        return path;
    }

    /** Returns the targets the route visits, in visit order. */
    public List<Point> getVisitedTargets() {
        return Collections.unmodifiableList(visitedTargets);
    }

    /** Returns targets that were blocked or exceeded the target limit. */
    public List<Point> getUnvisitedTargets() {
        return Collections.unmodifiableList(unvisitedTargets);
    }

    public List<PathCurve> getSegments() {
        return path.getSegments();
    }

    public List<Point> getControlPoints(int segmentIndex) {
        return path.getSegments().get(segmentIndex).getControlPoints();
    }

    /** Returns the heading to hold at the end of each segment, in radians. */
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
