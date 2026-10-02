package com.skeletonarmy.marrow.weaver;

import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.AtomicPath;
import com.pedropathing.paths.CompoundPath;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.curves.Curve;
import com.pedropathing.paths.curves.Line;
import com.pedropathing.paths.curves.bezier.BezierCurve;
import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts Marrow bezier paths into Pedro Pathing v3 {@link Path} objects.
 */
public final class PedroPathingConverter {
    private PedroPathingConverter() {}

    /**
     * Converts a generated result into a Pedro Pathing path.
     *
     * @param result result to convert
     * @return the equivalent Pedro path
     */
    public static Path toPath(PathResult result) {
        return toPath(result.getPath());
    }

    /**
     * Converts a route into a Pedro Pathing path. A single cubic becomes one atomic
     * path; multiple cubics are wrapped in a {@link CompoundPath}.
     *
     * @param route route to convert
     * @return the equivalent Pedro path
     */
    public static Path toPath(PathRoute route) {
        List<Path> segments = new ArrayList<>();

        for (PathCurve segment : route.getSegments()) {
            for (PathCurve cubic : segment.toCubicSegments()) {
                segments.add(atomicPath(cubic));
            }
        }

        if (segments.size() == 1) {
            return segments.get(0);
        }

        return new CompoundPath(segments.toArray(new Path[0]));
    }

    /**
     * Converts a single curve into a Pedro Pathing path.
     *
     * @param curve curve to convert
     * @return the equivalent Pedro path
     */
    public static Path toPath(PathCurve curve) {
        return toPath(new PathRoute(Collections.singletonList(curve)));
    }

    /** Wraps one cubic in an atomic, tangent-following path. */
    private static Path atomicPath(PathCurve curve) {
        return new AtomicPath(curve(curve)).tangent();
    }

    /**
     * Converts a curve into the matching Pedro primitive: a {@link Line} for two
     * control points, otherwise a {@link BezierCurve}.
     */
    private static Curve curve(PathCurve sourceCurve) {
        List<Point> points = sourceCurve.getControlPoints();

        if (points.size() == 2) {
            return new Line(vector(points.get(0)), vector(points.get(1)));
        }

        List<Vector2D> controlPoints = new ArrayList<>();

        for (Point point : points) {
            controlPoints.add(vector(point));
        }

        return new BezierCurve(controlPoints);
    }

    /** Converts one Marrow point into a Pedro {@link Vector2D}. */
    private static Vector2D vector(Point point) {
        return Vector2D.cartesian(point.getX(), point.getY());
    }
}
