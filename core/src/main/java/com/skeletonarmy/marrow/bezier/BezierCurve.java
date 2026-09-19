package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BezierCurve {

    private final List<Point> controlPoints;
    private final int cubicSegments;

    public BezierCurve(List<Point> controlPoints) {
        if (controlPoints == null || controlPoints.size() < 2) {
            throw new IllegalArgumentException();
        }
        this.controlPoints = new ArrayList<>(controlPoints);
        int n = this.controlPoints.size();
        this.cubicSegments = (n >= 4 && (n - 1) % 3 == 0) ? (n - 1) / 3 : 0;
    }

    public List<Point> getControlPoints() {
        return Collections.unmodifiableList(controlPoints);
    }

    public Point[] getControlPointArray() {
        return controlPoints.toArray(new Point[0]);
    }

    public boolean isComposite() {
        return cubicSegments > 0;
    }

    public List<BezierCurve> toCubicSegments() {
        if (!isComposite()) {
            return Collections.singletonList(this);
        }
        List<BezierCurve> segments = new ArrayList<>();
        for (int i = 0; i + 3 < controlPoints.size(); i += 3) {
            segments.add(new BezierCurve(Arrays.asList(
                    controlPoints.get(i), controlPoints.get(i + 1), controlPoints.get(i + 2), controlPoints.get(i + 3))));
        }
        return segments;
    }

    public static BezierCurve fromCubicSegments(List<BezierCurve> segments) {
        if (segments.size() == 1) {
            return segments.get(0);
        }
        List<Point> flat = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            List<Point> cps = segments.get(i).getControlPoints();
            if (i == 0) flat.add(cps.get(0));
            flat.add(cps.get(1));
            flat.add(cps.get(2));
            flat.add(cps.get(3));
        }
        return new BezierCurve(flat);
    }

    public int getDegree() {
        return cubicSegments > 0 ? 3 : controlPoints.size() - 1;
    }

    public Point get(double t) {
        if (cubicSegments == 0) {
            return polynomialPoint(t);
        }
        int index = segmentIndex(t);
        double localT = (Math.max(0, Math.min(t, 1)) * cubicSegments) - index;
        int base = index * 3;
        Point p0 = controlPoints.get(base);
        Point p1 = controlPoints.get(base + 1);
        Point p2 = controlPoints.get(base + 2);
        Point p3 = controlPoints.get(base + 3);
        double mt = 1 - localT;
        double a = mt * mt * mt;
        double b = 3 * mt * mt * localT;
        double c = 3 * mt * localT * localT;
        double d = localT * localT * localT;
        return new Point(a * p0.getX() + b * p1.getX() + c * p2.getX() + d * p3.getX(),
                a * p0.getY() + b * p1.getY() + c * p2.getY() + d * p3.getY());
    }

    public Point derivative(double t) {
        if (cubicSegments == 0) {
            return polynomialDerivative(t);
        }
        int index = segmentIndex(t);
        double localT = (Math.max(0, Math.min(t, 1)) * cubicSegments) - index;
        int base = index * 3;
        Point p0 = controlPoints.get(base);
        Point p1 = controlPoints.get(base + 1);
        Point p2 = controlPoints.get(base + 2);
        Point p3 = controlPoints.get(base + 3);
        double mt = 1 - localT;
        double ax = 3 * ((p1.getX() - p0.getX()) * mt * mt
                + 2 * (p2.getX() - p1.getX()) * mt * localT
                + (p3.getX() - p2.getX()) * localT * localT);
        double ay = 3 * ((p1.getY() - p0.getY()) * mt * mt
                + 2 * (p2.getY() - p1.getY()) * mt * localT
                + (p3.getY() - p2.getY()) * localT * localT);
        return new Point(ax, ay);
    }

    public double getHeading(double t) {
        Point d = derivative(t);
        return Math.atan2(d.getY(), d.getX());
    }

    public List<Point> sample(int numPoints) {
        List<Point> pts = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            pts.add(get(numPoints == 1 ? 0 : (double) i / (numPoints - 1)));
        }
        return pts;
    }

    public double approxLength(int samples) {
        List<Point> pts = sample(Math.max(samples, 2));
        double length = 0;
        for (int i = 1; i < pts.size(); i++) {
            length += pts.get(i - 1).distanceTo(pts.get(i));
        }
        return length;
    }

    private int segmentIndex(double t) {
        double segT = Math.max(0, Math.min(t, 1)) * cubicSegments;
        int index = (int) segT;
        return index >= cubicSegments ? cubicSegments - 1 : index;
    }

    private Point polynomialPoint(double t) {
        List<Point> points = new ArrayList<>(controlPoints);
        int n = points.size();
        for (int k = 1; k < n; k++) {
            for (int i = 0; i < n - k; i++) {
                Point a = points.get(i);
                Point b = points.get(i + 1);
                points.set(i, new Point((1 - t) * a.getX() + t * b.getX(), (1 - t) * a.getY() + t * b.getY()));
            }
        }
        return points.get(0);
    }

    private Point polynomialDerivative(double t) {
        int n = controlPoints.size() - 1;
        List<Point> diffPoints = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Point p = controlPoints.get(i);
            Point pNext = controlPoints.get(i + 1);
            diffPoints.add(new Point(n * (pNext.getX() - p.getX()), n * (pNext.getY() - p.getY())));
        }
        if (diffPoints.size() == 1) {
            return diffPoints.get(0);
        }
        return new BezierCurve(diffPoints).get(t);
    }
}
