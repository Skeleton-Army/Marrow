package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BezierCurve {

    private final List<Point> controlPoints;

    public BezierCurve(List<Point> controlPoints) {
        if (controlPoints == null || controlPoints.size() < 2) {
            throw new IllegalArgumentException();
        }
        this.controlPoints = new ArrayList<>(controlPoints);
    }

    public List<Point> getControlPoints() {
        return Collections.unmodifiableList(controlPoints);
    }

    public Point[] getControlPointArray() {
        return controlPoints.toArray(new Point[0]);
    }

    public int getDegree() {
        return controlPoints.size() - 1;
    }

    public Point get(double t) {
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

    public Point derivative(double t) {
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
}
