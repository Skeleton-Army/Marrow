package com.skeletonarmy.marrow.weaver;

import androidx.annotation.NonNull;
import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** An ordered set of {@link PathCurve} segments forming one continuous route. */
public class PathRoute {
    private final List<PathCurve> segments;

    public PathRoute(List<PathCurve> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException();
        }

        this.segments = new ArrayList<>(segments);
    }

    public List<PathCurve> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    public int getSegmentCount() {
        return segments.size();
    }

    /** Evaluates the route at {@code globalT}, clamped to {@code [0, segmentCount]}. */
    public Point get(double globalT) {
        int segmentCount = segments.size();
        double clamped = Math.max(0, Math.min(globalT, segmentCount));
        int segmentIndex = Math.min((int) clamped, segmentCount - 1);

        return segments.get(segmentIndex).get(clamped - segmentIndex);
    }

    /** Evaluates the route with {@code 0} at the start and {@code 1} at the end. */
    public Point getNormalized(double t) {
        return get(t * segments.size());
    }

    /** Returns the tangent heading at {@code globalT}, clamped to the route. */
    public double getHeading(double globalT) {
        int segmentCount = segments.size();
        double clamped = Math.max(0, Math.min(globalT, segmentCount));
        int segmentIndex = Math.min((int) clamped, segmentCount - 1);

        return segments.get(segmentIndex).getHeading(clamped - segmentIndex);
    }

    /** Samples every segment, omitting the point shared between consecutive segments. */
    public List<Point> sample(int pointsPerSegment) {
        List<Point> points = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            List<Point> segmentPoints = segments.get(i).sample(pointsPerSegment);

            for (int j = i == 0 ? 0 : 1; j < segmentPoints.size(); j++) {
                points.add(segmentPoints.get(j));
            }
        }

        return points;
    }

    public double approxLength(int samplesPerSegment) {
        double total = 0;

        for (PathCurve segment : segments) {
            total += segment.approxLength(samplesPerSegment);
        }

        return total;
    }

    public List<List<Point>> toControlPointLists() {
        List<List<Point>> controlPointLists = new ArrayList<>();

        for (PathCurve segment : segments) {
            controlPointLists.add(segment.getControlPoints());
        }

        return controlPointLists;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "PathRoute(segmentCount=%d, segments=%s)", getSegmentCount(), segments);
    }
}
