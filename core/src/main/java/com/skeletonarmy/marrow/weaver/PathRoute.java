package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public Point get(double globalT) {
        int segCount = segments.size();
        double clamped = Math.max(0, Math.min(globalT, segCount));
        int segIndex = Math.min((int) clamped, segCount - 1);
        double localT = clamped - segIndex;

        if (segIndex == segCount - 1 && localT > 1) {
            localT = 1;
        }
        return segments.get(segIndex).get(localT);
    }

    public Point getNormalized(double t) {
        return get(t * segments.size());
    }

    public double getHeading(double globalT) {
        int segCount = segments.size();
        double clamped = Math.max(0, Math.min(globalT, segCount));
        int segIndex = Math.min((int) clamped, segCount - 1);

        return segments.get(segIndex).getHeading(clamped - segIndex);
    }

    public List<Point> sample(int pointsPerSegment) {
        List<Point> pts = new ArrayList<>();
        for (int s = 0; s < segments.size(); s++) {
            List<Point> segPts = segments.get(s).sample(pointsPerSegment);
            for (int i = (s == 0 ? 0 : 1); i < segPts.size(); i++) {
                pts.add(segPts.get(i));
            }
        }
        return pts;
    }

    public double approxLength(int samplesPerSegment) {
        double total = 0;
        for (PathCurve c : segments) {
            total += c.approxLength(samplesPerSegment);
        }
        return total;
    }

    public List<List<Point>> toControlPointArrays() {
        List<List<Point>> out = new ArrayList<>();
        for (PathCurve c : segments) {
            out.add(c.getControlPoints());
        }
        return out;
    }
}
