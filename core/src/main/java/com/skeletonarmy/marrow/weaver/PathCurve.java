package com.skeletonarmy.marrow.weaver;

import androidx.annotation.NonNull;
import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** An immutable Bezier curve defined by its control points. */
public class PathCurve {
    private static final double TANGENT_EPSILON = 1e-9;
    private static final double TANGENT_STEP = 1e-4;

    private final List<Point> controlPoints;
    private final int cubicSegmentCount;

    public PathCurve(List<Point> controlPoints) {
        if (controlPoints == null || controlPoints.size() < 2) {
            throw new IllegalArgumentException();
        }

        this.controlPoints = new ArrayList<>(controlPoints);
        int pointCount = this.controlPoints.size();

        // A cubic chain has three control points per segment plus the final endpoint.
        this.cubicSegmentCount = (pointCount >= 4 && (pointCount - 1) % 3 == 0) ? (pointCount - 1) / 3 : 0;
    }

    public List<Point> getControlPoints() {
        return Collections.unmodifiableList(controlPoints);
    }

    public boolean isComposite() {
        return cubicSegmentCount > 0;
    }

    /** Splits a composite curve into cubics; a plain curve is returned as one element. */
    public List<PathCurve> toCubicSegments() {
        if (!isComposite()) {
            return Collections.singletonList(this);
        }

        List<PathCurve> segments = new ArrayList<>();

        for (int i = 0; i + 3 < controlPoints.size(); i += 3) {
            segments.add(new PathCurve(Arrays.asList(
                    controlPoints.get(i),
                    controlPoints.get(i + 1),
                    controlPoints.get(i + 2),
                    controlPoints.get(i + 3))));
        }

        return segments;
    }

    /** Recombines cubic segments, dropping the duplicated shared endpoints. */
    public static PathCurve fromCubicSegments(List<PathCurve> segments) {
        if (segments.size() == 1) {
            return segments.get(0);
        }

        List<Point> combined = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            List<Point> points = segments.get(i).getControlPoints();

            if (i == 0) {
                combined.add(points.get(0));
            }

            combined.add(points.get(1));
            combined.add(points.get(2));
            combined.add(points.get(3));
        }

        return new PathCurve(combined);
    }

    public int getDegree() {
        return cubicSegmentCount > 0 ? 3 : controlPoints.size() - 1;
    }

    /** Evaluates the curve at {@code t}. */
    public Point get(double t) {
        if (cubicSegmentCount == 0) {
            return polynomialPoint(t);
        }

        int segmentIndex = segmentIndex(t);
        double localProgress = (Math.max(0, Math.min(t, 1)) * cubicSegmentCount) - segmentIndex;
        int base = segmentIndex * 3;

        Point start = controlPoints.get(base);
        Point first = controlPoints.get(base + 1);
        Point second = controlPoints.get(base + 2);
        Point end = controlPoints.get(base + 3);

        double oneMinusT = 1 - localProgress;
        double startWeight = oneMinusT * oneMinusT * oneMinusT;
        double firstWeight = 3 * oneMinusT * oneMinusT * localProgress;
        double secondWeight = 3 * oneMinusT * localProgress * localProgress;
        double endWeight = localProgress * localProgress * localProgress;

        return new Point(
                startWeight * start.getX()
                        + firstWeight * first.getX()
                        + secondWeight * second.getX()
                        + endWeight * end.getX(),
                startWeight * start.getY()
                        + firstWeight * first.getY()
                        + secondWeight * second.getY()
                        + endWeight * end.getY());
    }

    /** Evaluates the curve's derivative at {@code t}. */
    public Point derivative(double t) {
        if (cubicSegmentCount == 0) {
            return polynomialDerivative(t);
        }

        int segmentIndex = segmentIndex(t);
        double localProgress = (Math.max(0, Math.min(t, 1)) * cubicSegmentCount) - segmentIndex;
        int base = segmentIndex * 3;

        Point start = controlPoints.get(base);
        Point first = controlPoints.get(base + 1);
        Point second = controlPoints.get(base + 2);
        Point end = controlPoints.get(base + 3);

        double oneMinusT = 1 - localProgress;

        double derivativeX = 3
                * ((first.getX() - start.getX()) * oneMinusT * oneMinusT
                        + 2 * (second.getX() - first.getX()) * oneMinusT * localProgress
                        + (end.getX() - second.getX()) * localProgress * localProgress);

        double derivativeY = 3
                * ((first.getY() - start.getY()) * oneMinusT * oneMinusT
                        + 2 * (second.getY() - first.getY()) * oneMinusT * localProgress
                        + (end.getY() - second.getY()) * localProgress * localProgress);

        return new Point(derivativeX, derivativeY);
    }

    /**
     * Returns the tangent heading at {@code t}. When the local derivative vanishes, the
     * direction is measured across a small step; a completely stationary curve reports 0.
     */
    public double getHeading(double t) {
        Point tangent = derivative(t);

        if (Math.abs(tangent.getX()) > TANGENT_EPSILON || Math.abs(tangent.getY()) > TANGENT_EPSILON) {
            return Math.atan2(tangent.getY(), tangent.getX());
        }

        Point from = get(Math.max(0, t - TANGENT_STEP));
        Point to = get(Math.min(1, t + TANGENT_STEP));

        if (Math.abs(to.getX() - from.getX()) > TANGENT_EPSILON
                || Math.abs(to.getY() - from.getY()) > TANGENT_EPSILON) {
            return Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
        }

        return 0.0;
    }

    /** Samples the curve into {@code numPoints} evenly spaced points. */
    public List<Point> sample(int numPoints) {
        List<Point> points = new ArrayList<>();

        for (int i = 0; i < numPoints; i++) {
            double t = numPoints == 1 ? 0 : (double) i / (numPoints - 1);
            points.add(get(t));
        }

        return points;
    }

    /** Approximates the length by summing the distances between sampled points. */
    public double approxLength(int samples) {
        List<Point> points = sample(Math.max(samples, 2));
        double length = 0;

        for (int i = 1; i < points.size(); i++) {
            length += points.get(i - 1).distanceTo(points.get(i));
        }

        return length;
    }

    private int segmentIndex(double t) {
        double scaled = Math.max(0, Math.min(t, 1)) * cubicSegmentCount;
        int index = (int) scaled;

        return index >= cubicSegmentCount ? cubicSegmentCount - 1 : index;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "PathCurve(degree=%d, composite=%s, controlPoints=%s)",
                getDegree(),
                isComposite(),
                controlPoints);
    }

    private Point polynomialPoint(double t) {
        List<Point> points = new ArrayList<>(controlPoints);
        int pointCount = points.size();

        for (int level = 1; level < pointCount; level++) {
            for (int i = 0; i < pointCount - level; i++) {
                Point a = points.get(i);
                Point b = points.get(i + 1);
                points.set(i, new Point((1 - t) * a.getX() + t * b.getX(), (1 - t) * a.getY() + t * b.getY()));
            }
        }

        return points.get(0);
    }

    private Point polynomialDerivative(double t) {
        int degree = controlPoints.size() - 1;
        List<Point> differencePoints = new ArrayList<>();

        for (int i = 0; i < degree; i++) {
            Point p = controlPoints.get(i);
            Point next = controlPoints.get(i + 1);

            differencePoints.add(new Point(degree * (next.getX() - p.getX()), degree * (next.getY() - p.getY())));
        }

        if (differencePoints.size() == 1) {
            return differencePoints.get(0);
        }

        return new PathCurve(differencePoints).get(t);
    }
}
