package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An immutable Bézier curve defined by a list of control points.
 */
public class PathCurve {
    private final List<Point> controlPoints;
    private final int cubicSegmentCount;

    /**
     * Creates a curve from its control points.
     *
     * @param controlPoints at least two points, stored in order
     * @throws IllegalArgumentException if fewer than two points are supplied
     */
    public PathCurve(List<Point> controlPoints) {
        if (controlPoints == null || controlPoints.size() < 2) {
            throw new IllegalArgumentException();
        }

        this.controlPoints = new ArrayList<>(controlPoints);
        int pointCount = this.controlPoints.size();

        // A cubic chain needs a multiple of 3 points plus the final endpoint.
        this.cubicSegmentCount = (pointCount >= 4 && (pointCount - 1) % 3 == 0) ? (pointCount - 1) / 3 : 0;
    }

    /**
     * Returns the control points, in order.
     *
     * @return an unmodifiable view of the control points
     */
    public List<Point> getControlPoints() {
        return Collections.unmodifiableList(controlPoints);
    }

    /**
     * Returns whether this curve is a chain of cubic segments.
     *
     * @return {@code true} when the control points form a multiple cubic chain
     */
    public boolean isComposite() {
        return cubicSegmentCount > 0;
    }

    /**
     * Splits a composite curve into its individual cubic segments. A non-composite
     * curve is returned as a single-element list.
     *
     * @return the cubic segments, or a single-element list when not composite
     */
    public List<PathCurve> toCubicSegments() {
        if (!isComposite()) {
            return Collections.singletonList(this);
        }

        List<PathCurve> segments = new ArrayList<>();

        // Each cubic consumes three new control points plus the shared endpoint.
        for (int i = 0; i + 3 < controlPoints.size(); i += 3) {
            segments.add(new PathCurve(Arrays.asList(
                    controlPoints.get(i),
                    controlPoints.get(i + 1),
                    controlPoints.get(i + 2),
                    controlPoints.get(i + 3))));
        }

        return segments;
    }

    /**
     * Recombines cubic segments into a single composite curve, dropping the
     * duplicated shared endpoints.
     *
     * @param segments cubic segments to combine, in order
     * @return the combined composite curve
     */
    public static PathCurve fromCubicSegments(List<PathCurve> segments) {
        if (segments.size() == 1) {
            return segments.get(0);
        }

        List<Point> combined = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            List<Point> controlPoints = segments.get(i).getControlPoints();

            // Only the first segment contributes its start point; the rest share the
            // previous segment's end point.
            if (i == 0) {
                combined.add(controlPoints.get(0));
            }

            combined.add(controlPoints.get(1));
            combined.add(controlPoints.get(2));
            combined.add(controlPoints.get(3));
        }

        return new PathCurve(combined);
    }

    /**
     * Returns the polynomial degree of the curve.
     *
     * @return {@code 3} for a cubic chain, otherwise the control point count minus one
     */
    public int getDegree() {
        return cubicSegmentCount > 0 ? 3 : controlPoints.size() - 1;
    }

    /**
     * Evaluates the curve at parameter {@code t}.
     *
     * @param t parameter in {@code [0, 1]}
     * @return the point on the curve
     */
    public Point get(double t) {
        if (cubicSegmentCount == 0) {
            return polynomialPoint(t);
        }

        int segmentIndex = segmentIndex(t);
        double localProgress = (Math.max(0, Math.min(t, 1)) * cubicSegmentCount) - segmentIndex;
        int controlPointBase = segmentIndex * 3;

        Point startPoint = controlPoints.get(controlPointBase);
        Point firstControl = controlPoints.get(controlPointBase + 1);
        Point secondControl = controlPoints.get(controlPointBase + 2);
        Point endPoint = controlPoints.get(controlPointBase + 3);

        double oneMinusT = 1 - localProgress;

        // Standard cubic Bernstein basis weights.
        double startWeight = oneMinusT * oneMinusT * oneMinusT;
        double firstWeight = 3 * oneMinusT * oneMinusT * localProgress;
        double secondWeight = 3 * oneMinusT * localProgress * localProgress;
        double endWeight = localProgress * localProgress * localProgress;

        return new Point(
                startWeight * startPoint.getX() + firstWeight * firstControl.getX()
                        + secondWeight * secondControl.getX() + endWeight * endPoint.getX(),
                startWeight * startPoint.getY() + firstWeight * firstControl.getY()
                        + secondWeight * secondControl.getY() + endWeight * endPoint.getY());
    }

    /**
     * Evaluates the curve's derivative (tangent) at parameter {@code t}.
     *
     * @param t parameter in {@code [0, 1]}
     * @return the tangent vector at {@code t}
     */
    public Point derivative(double t) {
        if (cubicSegmentCount == 0) {
            return polynomialDerivative(t);
        }

        int segmentIndex = segmentIndex(t);
        double localProgress = (Math.max(0, Math.min(t, 1)) * cubicSegmentCount) - segmentIndex;
        int controlPointBase = segmentIndex * 3;

        Point startPoint = controlPoints.get(controlPointBase);
        Point firstControl = controlPoints.get(controlPointBase + 1);
        Point secondControl = controlPoints.get(controlPointBase + 2);
        Point endPoint = controlPoints.get(controlPointBase + 3);

        double oneMinusT = 1 - localProgress;

        double derivativeX = 3 * (
                (firstControl.getX() - startPoint.getX()) * oneMinusT * oneMinusT
                        + 2 * (secondControl.getX() - firstControl.getX()) * oneMinusT * localProgress
                        + (endPoint.getX() - secondControl.getX()) * localProgress * localProgress);

        double derivativeY = 3 * (
                (firstControl.getY() - startPoint.getY()) * oneMinusT * oneMinusT
                        + 2 * (secondControl.getY() - firstControl.getY()) * oneMinusT * localProgress
                        + (endPoint.getY() - secondControl.getY()) * localProgress * localProgress);

        return new Point(derivativeX, derivativeY);
    }

    /**
     * Returns the tangent heading at parameter {@code t}.
     *
     * @param t parameter in {@code [0, 1]}
     * @return the tangent heading in radians
     */
    public double getHeading(double t) {
        Point tangent = derivative(t);

        return Math.atan2(tangent.getY(), tangent.getX());
    }

    /**
     * Samples the curve into evenly spaced points.
     *
     * @param numPoints number of points to sample
     * @return the sampled points, from {@code t = 0} to {@code t = 1}
     */
    public List<Point> sample(int numPoints) {
        List<Point> points = new ArrayList<>();

        for (int i = 0; i < numPoints; i++) {
            double t = numPoints == 1 ? 0 : (double) i / (numPoints - 1);
            points.add(get(t));
        }

        return points;
    }

    /**
     * Approximates the curve length by summing the distances between sampled points.
     *
     * @param samples number of points to sample; at least two are used
     * @return the approximate length
     */
    public double approxLength(int samples) {
        List<Point> points = sample(Math.max(samples, 2));
        double length = 0;

        for (int i = 1; i < points.size(); i++) {
            length += points.get(i - 1).distanceTo(points.get(i));
        }

        return length;
    }

    /**
     * Maps a global parameter to the index of the cubic segment it falls on.
     *
     * @param t global parameter in {@code [0, 1]}
     * @return the zero-based cubic segment index
     */
    private int segmentIndex(double t) {
        double scaled = Math.max(0, Math.min(t, 1)) * cubicSegmentCount;
        int index = (int) scaled;

        return index >= cubicSegmentCount ? cubicSegmentCount - 1 : index;
    }

    /**
     * Evaluates a non-composite curve with the de Casteljau algorithm.
     *
     * @param t parameter in {@code [0, 1]}
     * @return the point on the curve
     */
    private Point polynomialPoint(double t) {
        List<Point> points = new ArrayList<>(controlPoints);
        int pointCount = points.size();

        // Repeatedly interpolate neighboring points until one remains.
        for (int level = 1; level < pointCount; level++) {
            for (int i = 0; i < pointCount - level; i++) {
                Point a = points.get(i);
                Point b = points.get(i + 1);
                points.set(i, new Point(
                        (1 - t) * a.getX() + t * b.getX(),
                        (1 - t) * a.getY() + t * b.getY()));
            }
        }

        return points.get(0);
    }

    /**
     * Evaluates the derivative of a non-composite curve by differentiating its
     * control points once and evaluating the resulting lower-degree curve.
     *
     * @param t parameter in {@code [0, 1]}
     * @return the tangent vector at {@code t}
     */
    private Point polynomialDerivative(double t) {
        int degree = controlPoints.size() - 1;
        List<Point> differencePoints = new ArrayList<>();

        for (int i = 0; i < degree; i++) {
            Point p = controlPoints.get(i);
            Point pNext = controlPoints.get(i + 1);

            differencePoints.add(new Point(
                    degree * (pNext.getX() - p.getX()),
                    degree * (pNext.getY() - p.getY())));
        }

        // The derivative of a line is a constant vector.
        if (differencePoints.size() == 1) {
            return differencePoints.get(0);
        }

        return new PathCurve(differencePoints).get(t);
    }
}
