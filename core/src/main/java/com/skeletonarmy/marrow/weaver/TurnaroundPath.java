package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the waypoints of a turnaround: the shortest route that leaves a point travelling
 * along one heading and arrives at another travelling along a second heading, without ever
 * turning tighter than a given radius.
 * <p>
 * Fitting a single Hermite span through a reversal collapses, because both control handles
 * end up on the line between the ends and the curve retraces itself. The four plain Dubins
 * couples (left/left, right/right, left/right, right/left) keep every turn at the radius
 * they are given, so the tangent heading turns at a rate a robot can follow. The shortest
 * couple that fits is used.
 * <p>
 * Both ends are left out of the returned list: the caller already holds them as key points.
 */
final class TurnaroundPath {

    /** Largest heading step between two waypoints sampled off an arc, in radians. */
    private static final double SAMPLE_STEP = Math.toRadians(30.0);

    /** A crossing tangent only exists while the two ride circles keep this far apart. */
    private static final double MIN_CENTRE_GAP = 2.0;

    private static final double EPSILON = 1e-9;

    /** The origin of the solved frame: where the route leaves from. */
    private static final double[] ORIGIN = {0, 0};

    private TurnaroundPath() {}

    /**
     * Builds the waypoints of a turnaround between two poses.
     *
     * @param start         point to leave from
     * @param startHeading  heading to leave with, in radians
     * @param end           point to arrive at
     * @param endHeading    heading to arrive with, in radians
     * @param radius        tightest turn the route may make, in the units of the points
     * @return the waypoints strictly between the ends, each carrying its tangent heading, or
     *         {@code null} when no couple fits
     */
    static List<Point> build(Point start, double startHeading, Point end, double endHeading, double radius) {
        if (!(radius > 0) || !Double.isFinite(radius)) {
            return null;
        }

        // Solve in the frame of the start pose, with the radius scaled out to one.
        double cos = Math.cos(startHeading);
        double sin = Math.sin(startHeading);
        double offsetX = (end.getX() - start.getX()) / radius;
        double offsetY = (end.getY() - start.getY()) / radius;
        double x = offsetX * cos + offsetY * sin;
        double y = -offsetX * sin + offsetY * cos;
        double heading = normalize(endHeading - startHeading);

        // The circles each end rides on: one radius to the left, one to the right.
        double[] leftStart = {0, 1};
        double[] rightStart = {0, -1};
        double[] leftEnd = {x - Math.sin(heading), y + Math.cos(heading)};
        double[] rightEnd = {x + Math.sin(heading), y - Math.cos(heading)};
        double[] arrival = {x, y};

        List<Candidate> candidates = new ArrayList<>();
        addSameSide(candidates, leftStart, leftEnd, arrival, false);
        addSameSide(candidates, rightStart, rightEnd, arrival, true);
        addCrossing(candidates, leftStart, rightEnd, arrival, false);
        addCrossing(candidates, rightStart, leftEnd, arrival, true);

        Candidate best = null;

        for (Candidate candidate : candidates) {
            if (candidate != null
                    && Double.isFinite(candidate.length)
                    && (best == null || candidate.length < best.length)) {
                best = candidate;
            }
        }

        if (best == null) {
            return null;
        }

        return sample(best, start, startHeading, radius, end, arrival);
    }

    /**
     * Adds the couple whose two circles turn the same way, joined by their outer tangent.
     *
     * @param candidates list to add to
     * @param centre0    circle the route leaves on
     * @param centre1    circle the route arrives on
     * @param arrival    point to arrive at
     * @param clockwise  {@code true} for a right-hand couple, {@code false} for a left-hand one
     */
    private static void addSameSide(
            List<Candidate> candidates, double[] centre0, double[] centre1, double[] arrival, boolean clockwise) {
        double toX = centre1[0] - centre0[0];
        double toY = centre1[1] - centre0[1];

        if (Math.hypot(toX, toY) < EPSILON) {
            return;
        }

        double direction = Math.atan2(toY, toX);
        double normalX = -Math.sin(direction);
        double normalY = Math.cos(direction);
        double side = clockwise ? 1 : -1;
        double[] tangent0 = {centre0[0] + side * normalX, centre0[1] + side * normalY};
        double[] tangent1 = {centre1[0] + side * normalX, centre1[1] + side * normalY};

        candidates.add(new Candidate(
                centre0,
                centre1,
                tangent0,
                tangent1,
                direction,
                clockwise,
                clockwise,
                sweep(new double[] {0, 0}, tangent0, centre0, clockwise)
                        + sweep(tangent1, arrival, centre1, clockwise)
                        + Math.hypot(tangent1[0] - tangent0[0], tangent1[1] - tangent0[1])));
    }

    /**
     * Adds the couples whose circles turn opposite ways, joined by one of their crossing
     * tangents. Each pair of circles offers two crossings, so up to two couples are added.
     *
     * @param candidates     list to add to
     * @param centre0        circle the route leaves on
     * @param centre1        circle the route arrives on
     * @param arrival        point to arrive at
     * @param startClockwise {@code true} when the first circle turns right
     */
    private static void addCrossing(
            List<Candidate> candidates, double[] centre0, double[] centre1, double[] arrival, boolean startClockwise) {
        double toX = centre1[0] - centre0[0];
        double toY = centre1[1] - centre0[1];
        double gap = Math.hypot(toX, toY);

        if (gap < MIN_CENTRE_GAP) {
            return;
        }

        double centreAngle = Math.atan2(toY, toX);
        double reach = Math.max(-1.0, Math.min(1.0, (startClockwise ? 2.0 : -2.0) / gap));
        double offset = Math.acos(reach);
        boolean endClockwise = !startClockwise;

        for (int side = -1; side <= 1; side += 2) {
            double direction = centreAngle - Math.PI / 2.0 + side * offset;
            double normalX = -Math.sin(direction);
            double normalY = Math.cos(direction);
            double[] tangent0 = {
                centre0[0] + (startClockwise ? normalX : -normalX), centre0[1] + (startClockwise ? normalY : -normalY)
            };
            double[] tangent1 = {
                centre1[0] + (endClockwise ? normalX : -normalX), centre1[1] + (endClockwise ? normalY : -normalY)
            };
            double straight = (tangent1[0] - tangent0[0]) * Math.cos(direction)
                    + (tangent1[1] - tangent0[1]) * Math.sin(direction);

            // The crossing only counts when the route runs forward along it.
            if (straight < -EPSILON) {
                continue;
            }

            candidates.add(new Candidate(
                    centre0,
                    centre1,
                    tangent0,
                    tangent1,
                    direction,
                    startClockwise,
                    endClockwise,
                    sweep(new double[] {0, 0}, tangent0, centre0, startClockwise)
                            + sweep(tangent1, arrival, centre1, endClockwise)
                            + Math.max(straight, 0)));
        }
    }

    /**
     * Returns how far a point travels around a circle on its way to another, measured in the
     * direction the route turns.
     *
     * @param from      point travelled from
     * @param to        point travelled to
     * @param centre    circle centre
     * @param clockwise whether the route turns right around the circle
     * @return the swept angle in radians, in {@code [0, 2 * pi)}
     */
    private static double sweep(double[] from, double[] to, double[] centre, boolean clockwise) {
        double fromAngle = Math.atan2(from[1] - centre[1], from[0] - centre[0]);
        double toAngle = Math.atan2(to[1] - centre[1], to[0] - centre[0]);
        double swept = clockwise ? fromAngle - toAngle : toAngle - fromAngle;

        while (swept < 0) {
            swept += 2 * Math.PI;
        }

        while (swept >= 2 * Math.PI) {
            swept -= 2 * Math.PI;
        }

        return swept;
    }

    /**
     * Samples a fitted couple into waypoints.
     *
     * @param candidate    couple to sample
     * @param start        point the route leaves from
     * @param startHeading heading the route leaves with, in radians
     * @param radius       radius the couple was fitted at
     * @param end          point the route arrives at
     * @param arrival      point the route arrives at, in the solved frame
     * @return the waypoints between the ends, each carrying its tangent heading
     */
    private static List<Point> sample(
            Candidate candidate, Point start, double startHeading, double radius, Point end, double[] arrival) {
        List<Point> waypoints = new ArrayList<>();

        appendArc(waypoints, candidate, true, arrival, start, startHeading, radius, end);
        append(waypoints, candidate.tangent1, candidate.direction, start, startHeading, radius, end);
        appendArc(waypoints, candidate, false, arrival, start, startHeading, radius, end);

        return waypoints;
    }

    /**
     * Samples one of the couple's arcs, skipping both the point it starts on (the caller
     * holds it) and the point it ends on when that is the route's final key point.
     *
     * @param waypoints list to add to
     * @param candidate couple the arc belongs to
     * @param first     {@code true} for the arc the route leaves on, {@code false} for the
     *                  one it arrives on
     * @param arrival   point the route arrives at, in the solved frame
     * @param start     point the route leaves from
     * @param startHeading heading the route leaves with, in radians
     * @param radius    radius the couple was fitted at
     * @param end       point the route arrives at
     */
    private static void appendArc(
            List<Point> waypoints,
            Candidate candidate,
            boolean first,
            double[] arrival,
            Point start,
            double startHeading,
            double radius,
            Point end) {
        double[] centre = first ? candidate.centre0 : candidate.centre1;
        double[] from = first ? ORIGIN : candidate.tangent1;
        double[] to = first ? candidate.tangent0 : arrival;
        boolean clockwise = first ? candidate.clockwise0 : candidate.clockwise1;
        double swept = sweep(from, to, centre, clockwise);
        double fromAngle = Math.atan2(from[1] - centre[1], from[0] - centre[0]);
        int steps = Math.max(1, (int) Math.ceil(swept / SAMPLE_STEP));

        // The sample at the far end lands on a key point the caller already holds, which
        // drops it; running the loop all the way there is what keeps a gap from opening
        // between the last waypoint and that key point.
        for (int i = first ? 1 : 0; i <= steps; i++) {
            double angle = fromAngle + (clockwise ? -1 : 1) * swept * i / steps;
            double heading = angle + (clockwise ? -Math.PI / 2.0 : Math.PI / 2.0);
            append(
                    waypoints,
                    new double[] {centre[0] + Math.cos(angle), centre[1] + Math.sin(angle)},
                    heading,
                    start,
                    startHeading,
                    radius,
                    end);
        }
    }

    /**
     * Converts one point of the solved frame into world coordinates and appends it, skipping
     * anything that lands on a point the caller already holds or on its predecessor.
     *
     * @param waypoints    list to add to
     * @param point        point in the solved frame
     * @param heading      heading at that point in the solved frame, in radians
     * @param start        point the route leaves from
     * @param startHeading heading the route leaves with, in radians
     * @param radius       radius the couple was fitted at
     * @param end          point the route arrives at
     */
    private static void append(
            List<Point> waypoints,
            double[] point,
            double heading,
            Point start,
            double startHeading,
            double radius,
            Point end) {
        double cos = Math.cos(startHeading);
        double sin = Math.sin(startHeading);
        double x = start.getX() + radius * (point[0] * cos - point[1] * sin);
        double y = start.getY() + radius * (point[0] * sin + point[1] * cos);
        Point waypoint = new Point(x, y, normalize(startHeading + heading));

        if (waypoint.distanceTo(start) < EPSILON || waypoint.distanceTo(end) < EPSILON) {
            return;
        }

        if (!waypoints.isEmpty() && waypoint.distanceTo(waypoints.get(waypoints.size() - 1)) < EPSILON) {
            return;
        }

        waypoints.add(waypoint);
    }

    /**
     * Wraps an angle onto {@code (-pi, pi]}.
     *
     * @param angle angle in radians
     * @return the equivalent angle in {@code (-pi, pi]}
     */
    private static double normalize(double angle) {
        // Wrapped by remainder rather than by stepping: a heading supplied in the tens of
        // billions of radians would otherwise take billions of turns to bring under pi.
        double normalized = angle % (2 * Math.PI);

        if (normalized > Math.PI) {
            normalized -= 2 * Math.PI;
        } else if (normalized <= -Math.PI) {
            normalized += 2 * Math.PI;
        }

        return normalized;
    }

    /** One turn of a fitted couple: two circles, the tangent joining them, and its length. */
    private static final class Candidate {
        final double[] centre0;
        final double[] centre1;
        final double[] tangent0;
        final double[] tangent1;
        final double direction;
        final boolean clockwise0;
        final boolean clockwise1;
        final double length;

        Candidate(
                double[] centre0,
                double[] centre1,
                double[] tangent0,
                double[] tangent1,
                double direction,
                boolean clockwise0,
                boolean clockwise1,
                double length) {
            this.centre0 = centre0;
            this.centre1 = centre1;
            this.tangent0 = tangent0;
            this.tangent1 = tangent1;
            this.direction = direction;
            this.clockwise0 = clockwise0;
            this.clockwise1 = clockwise1;
            this.length = length;
        }
    }
}
