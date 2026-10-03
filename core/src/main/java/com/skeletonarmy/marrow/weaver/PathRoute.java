package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered set of {@link PathCurve} segments forming one continuous route.
 */
public class PathRoute {
    private final List<PathCurve> segments;

    /**
     * Creates a route from its segments.
     *
     * @param segments at least one curve, stored in order
     * @throws IllegalArgumentException if the list is {@code null} or empty
     */
    public PathRoute(List<PathCurve> segments) {
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException();
        }

        this.segments = new ArrayList<>(segments);
    }

    /**
     * Returns the segments, in order.
     *
     * @return an unmodifiable view of the segments
     */
    public List<PathCurve> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Returns the number of segments in the route.
     *
     * @return the segment count
     */
    public int getSegmentCount() {
        return segments.size();
    }

    /**
     * Evaluates the route at global parameter {@code globalT}, clamped to the route.
     *
     * @param globalT parameter in {@code [0, segmentCount]}
     * @return the point on the route
     */
    public Point get(double globalT) {
        int segmentCount = segments.size();
        double clamped = Math.max(0, Math.min(globalT, segmentCount));

        int segmentIndex = Math.min((int) clamped, segmentCount - 1);
        double localT = clamped - segmentIndex;

        // Clamp the final segment so t == segmentCount lands exactly on its end.
        if (segmentIndex == segmentCount - 1 && localT > 1) {
            localT = 1;
        }

        return segments.get(segmentIndex).get(localT);
    }

    /**
     * Evaluates the route using a normalized parameter where 0 is the start and 1 is
     * the end, regardless of segment count.
     *
     * @param t normalized parameter in {@code [0, 1]}
     * @return the point on the route
     */
    public Point getNormalized(double t) {
        return get(t * segments.size());
    }

    /**
     * Returns the tangent heading at global parameter {@code globalT}.
     *
     * @param globalT parameter in {@code [0, segmentCount]}
     * @return the tangent heading in radians
     */
    public double getHeading(double globalT) {
        int segmentCount = segments.size();
        double clamped = Math.max(0, Math.min(globalT, segmentCount));

        int segmentIndex = Math.min((int) clamped, segmentCount - 1);

        return segments.get(segmentIndex).getHeading(clamped - segmentIndex);
    }

    /**
     * Samples every segment into the given number of points, omitting the shared
     * points between segments so the result has no duplicates.
     *
     * @param pointsPerSegment number of points to sample from each segment
     * @return the combined sampled points
     */
    public List<Point> sample(int pointsPerSegment) {
        List<Point> points = new ArrayList<>();

        for (int s = 0; s < segments.size(); s++) {
            List<Point> segmentPoints = segments.get(s).sample(pointsPerSegment);

            // Skip the first point of every segment after the first: it duplicates the
            // previous segment's last point.
            for (int i = (s == 0 ? 0 : 1); i < segmentPoints.size(); i++) {
                points.add(segmentPoints.get(i));
            }
        }

        return points;
    }

    /**
     * Approximates the total route length by summing each segment's sampled length.
     *
     * @param samplesPerSegment number of sample points to use per segment
     * @return the approximate total length
     */
    public double approxLength(int samplesPerSegment) {
        double total = 0;

        for (PathCurve segment : segments) {
            total += segment.approxLength(samplesPerSegment);
        }

        return total;
    }

    /**
     * Returns the control points of every segment, grouped per segment.
     *
     * @return one control-point list per segment
     */
    public List<List<Point>> toControlPointLists() {
        List<List<Point>> controlPointLists = new ArrayList<>();

        for (PathCurve segment : segments) {
            controlPointLists.add(segment.getControlPoints());
        }

        return controlPointLists;
    }
}
