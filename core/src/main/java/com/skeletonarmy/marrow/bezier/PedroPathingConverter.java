package com.skeletonarmy.marrow.bezier;

import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.AtomicPath;
import com.pedropathing.paths.CompoundPath;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.curves.Curve;
import com.pedropathing.paths.curves.Line;
import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts Marrow bezier paths into Pedro Pathing v3 {@link Path} objects.
 */
public final class PedroPathingConverter {
    private PedroPathingConverter() {}

    public static Path toPath(BezierResult result) {
        return toPath(result.getPath());
    }

    public static Path toPath(BezierPath path) {
        List<Path> segments = new ArrayList<>();

        for (BezierCurve segment : path.getSegments()) {
            for (BezierCurve cubic : segment.toCubicSegments()) {
                segments.add(atomicPath(cubic));
            }
        }
        if (segments.size() == 1) {
            return segments.get(0);
        }

        return new CompoundPath(segments.toArray(new Path[0]));
    }

    public static Path toPath(BezierCurve curve) {
        return toPath(new BezierPath(Collections.singletonList(curve)));
    }

    private static Path atomicPath(BezierCurve curve) {
        return new AtomicPath(curve(curve)).tangent();
    }

    private static Curve curve(BezierCurve curve) {
        List<Point> points = curve.getControlPoints();
        if (points.size() == 2) {
            return new Line(vector(points.get(0)), vector(points.get(1)));
        }
        List<Vector2D> controlPoints = new ArrayList<>();
        for (Point p : points) {
            controlPoints.add(vector(p));
        }
        return new com.pedropathing.paths.curves.bezier.BezierCurve(controlPoints);
    }

    private static Vector2D vector(Point p) {
        return Vector2D.cartesian(p.getX(), p.getY());
    }
}
