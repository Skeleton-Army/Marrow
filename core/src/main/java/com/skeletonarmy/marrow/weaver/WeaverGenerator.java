package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the cubic Bezier route that visits targets or reaches a destination.
 *
 * <p>With targets, they are ordered (optionally), clustered into rows that fit the
 * intake, and connected by a Hermite chain following the intake heading. With only a
 * destination, a straight seed is bent around obstacles by {@link ObstacleAvoider}.
 */
public final class WeaverGenerator {
    /** A leg this far off its key point's heading is a turnaround, not a bend. */
    private static final double TURNAROUND_ANGLE = Math.toRadians(60.0);

    /** Tightest arc a turnaround is fitted at, in inches. */
    private static final double MIN_TURN_RADIUS = 1.0;

    /** Shortest gap kept between two key points of one sweep, in inches. */
    private static final double MIN_KEY_POINT_GAP = 0.5;

    /** Number of control points used to seed a straight avoidance path. */
    private static final int SEED_CONTROL_POINTS = 6;

    private WeaverGenerator() {}

    /** Targets collected in one intake pass, and the direction the pass runs in. */
    private static final class TargetGroup {
        private final int[] indices;
        private final double heading;

        private TargetGroup(int[] indices, double heading) {
            this.indices = indices;
            this.heading = heading;
        }
    }

    /** Generates a path from points whose headings are all ignored. */
    public static PathResult generate(
            Point startPose,
            Point destinationPose,
            List<Point> targets,
            List<Zone> obstacles,
            boolean reorder,
            int maxTargets,
            PathConfig config) {
        List<TargetSpec> targetSpecs = new ArrayList<>();

        for (Point target : targets) {
            targetSpecs.add(new TargetSpec(target, HeadingPolicy.FREE));
        }

        return generate(
                startPose, destinationPose, targetSpecs, HeadingPolicy.FREE, obstacles, reorder, maxTargets, config);
    }

    /** Generates a path from targets that carry their own heading policies. */
    static PathResult generate(
            Point startPose,
            Point destinationPose,
            List<TargetSpec> targets,
            HeadingPolicy destinationPolicy,
            List<Zone> obstacles,
            boolean reorder,
            int maxTargets,
            PathConfig config) {
        if (!targets.isEmpty()) {
            return generateIntakeResult(startPose, targets, obstacles, reorder, maxTargets, config);
        }

        if (destinationPose != null) {
            return generateAvoidanceResult(startPose, destinationPose, destinationPolicy, obstacles, config);
        }

        throw new IllegalStateException("Either targets or a destination must be set");
    }

    /** Tests whether {@code target} lies inside the robot's intake mouth. */
    public static boolean isCapturedByIntake(Point robotCenter, double headingRad, double intakeWidth, Point target) {
        double cosHeading = Math.cos(headingRad);
        double sinHeading = Math.sin(headingRad);

        double relativeX = target.getX() - robotCenter.getX();
        double relativeY = target.getY() - robotCenter.getY();
        double alongRobot = relativeX * cosHeading + relativeY * sinHeading;
        double lateral = relativeX * -sinHeading + relativeY * cosHeading;

        return Math.abs(alongRobot) < 1e-6 && Math.abs(lateral) <= intakeWidth / 2.0 + 1e-6;
    }

    /** Convenience overload that reads the intake width from {@code config}. */
    public static boolean isCapturedByIntake(Point robotCenter, double headingRad, PathConfig config, Point target) {
        return isCapturedByIntake(robotCenter, headingRad, config.getIntakeWidth(), target);
    }

    /**
     * Checks that a sampled curve keeps the required clearance from every obstacle.
     * Non-finite positions or headings are never clear.
     */
    public static boolean isPathClear(
            PathCurve curve,
            List<Zone> obstacles,
            double clearance,
            double robotWidth,
            double robotHeight,
            int samples) {
        for (int i = 0; i < samples; i++) {
            double progress = samples == 1 ? 0 : (double) i / (samples - 1);
            Point samplePoint = curve.get(progress);

            if (!Double.isFinite(samplePoint.getX()) || !Double.isFinite(samplePoint.getY())) {
                return false;
            }

            if (robotWidth <= 0 || robotHeight <= 0) {
                for (Zone zone : obstacles) {
                    if (zone.contains(samplePoint) || zone.distanceToBoundary(samplePoint) < clearance) {
                        return false;
                    }
                }
            } else {
                double heading = curve.getHeading(progress);

                if (!Double.isFinite(heading)) {
                    return false;
                }

                Zone footprint = new PolygonZone(samplePoint, robotWidth, robotHeight, heading);

                for (Zone zone : obstacles) {
                    if (zone.isInside(footprint) || zone.distanceTo(footprint) < clearance) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /** Checks a whole route using {@code config}'s clearance and footprint. */
    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, PathConfig config, int samplesPerSegment) {
        return isPathClear(
                path,
                obstacles,
                config.getClearance(),
                config.getRobotWidth(),
                config.getRobotHeight(),
                samplesPerSegment);
    }

    /** Checks a single curve using {@code config}'s clearance and footprint. */
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, PathConfig config, int samples) {
        return isPathClear(
                curve, obstacles, config.getClearance(), config.getRobotWidth(), config.getRobotHeight(), samples);
    }

    /** Checks a whole route using {@code config} and 100 samples per segment. */
    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, PathConfig config) {
        return isPathClear(path, obstacles, config, 100);
    }

    /** Checks a single curve using {@code config} and 100 samples. */
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, PathConfig config) {
        return isPathClear(curve, obstacles, config, 100);
    }

    /** Checks that every segment of a route is clear. */
    public static boolean isPathClear(
            PathRoute path,
            List<Zone> obstacles,
            double clearance,
            double robotWidth,
            double robotHeight,
            int samplesPerSegment) {
        for (PathCurve segment : path.getSegments()) {
            if (!isPathClear(segment, obstacles, clearance, robotWidth, robotHeight, samplesPerSegment)) {
                return false;
            }
        }

        return true;
    }

    /** Checks a route against point clearance, without a robot footprint. */
    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, double clearance, int samplesPerSegment) {
        return isPathClear(path, obstacles, clearance, 0.0, 0.0, samplesPerSegment);
    }

    /** Checks a single curve against point clearance, without a robot footprint. */
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, double clearance, int samples) {
        return isPathClear(curve, obstacles, clearance, 0.0, 0.0, samples);
    }

    private static PathResult generateIntakeResult(
            Point start,
            List<TargetSpec> targets,
            List<Zone> obstacles,
            boolean reorder,
            int maxTargets,
            PathConfig config) {
        Point startPoint = new Point(start.getX(), start.getY());
        List<TargetSpec> orderedTargets;

        if (maxTargets > 0) {
            orderedTargets = selectTargets(start, startPoint, targets, obstacles, reorder, maxTargets, config);
        } else {
            orderedTargets = reorder
                    ? TargetOrderer.orderSpecs(startPoint, start.getHeadingRad(), targets, config)
                    : new ArrayList<>(targets);

            if (config.isExcludeBlockedTargets()) {
                orderedTargets = excludeBlockedTargets(startPoint, orderedTargets, obstacles, config);
            }
        }

        // Keep targets that can be swept together in one pass. A point-to-point order
        // would thread a distant target through a clump and split it apart.
        if (reorder
                && config.getIntakeWidth() > 1e-9
                && !hasLockedHeading(orderedTargets, 0, orderedTargets.size() - 1)) {
            orderedTargets = orderForIntake(startPoint, orderedTargets, config);
        }

        // Remove one entry per visit, so a repeated target visited once still reports
        // its second entry as unvisited.
        List<Point> unvisitedTargets = new ArrayList<>();

        for (TargetSpec target : targets) {
            unvisitedTargets.add(target.getPoint());
        }

        for (TargetSpec visited : orderedTargets) {
            unvisitedTargets.remove(visited.getPoint());
        }

        if (orderedTargets.isEmpty()) {
            PathRoute idlePath = finish(
                    new PathRoute(Collections.singletonList(new PathCurve(Arrays.asList(startPoint, startPoint)))),
                    obstacles,
                    config,
                    true);

            double idleHeading = start.hasHeading() ? start.getHeadingRad() : 0.0;

            return new PathResult(
                    idlePath, Collections.singletonList(idleHeading), Collections.<Point>emptyList(), unvisitedTargets);
        }

        List<Point> keyPoints = new ArrayList<>();
        List<Double> headings = new ArrayList<>();
        List<Boolean> lockedKeyPoints = new ArrayList<>();

        keyPoints.add(startPoint);
        // A start with no heading faces its first target until the key points are known.
        headings.add(start.hasHeading() ? start.getHeadingRad() : firstTargetBearing(startPoint, orderedTargets));
        lockedKeyPoints.add(false);

        double intakeHalfWidth = config.getIntakeWidth() / 2.0;
        List<TargetGroup> targetGroups =
                groupTargets(startPoint, headings.get(0), orderedTargets, obstacles, config, reorder);
        List<Point> visitedTargets = new ArrayList<>();
        Point previousRawTarget = startPoint;

        for (int groupIndex = 0; groupIndex < targetGroups.size(); groupIndex++) {
            TargetGroup group = targetGroups.get(groupIndex);
            int[] groupIndices = group.indices;

            for (int index : groupIndices) {
                visitedTargets.add(orderedTargets.get(index).getPoint());
            }

            if (groupIndices.length > 1 && intakeHalfWidth > 1e-9) {
                previousRawTarget = addMergedGroup(
                        orderedTargets,
                        groupIndices,
                        group.heading,
                        previousRawTarget,
                        keyPoints,
                        headings,
                        lockedKeyPoints);
                continue;
            }

            TargetSpec firstPose = orderedTargets.get(groupIndices[0]);
            Point target = firstPose.position();

            double heading = firstPose.hasLockedHeading()
                    ? firstPose.getHeadingRad()
                    : Math.atan2(target.getY() - previousRawTarget.getY(), target.getX() - previousRawTarget.getX());

            Point robotCenter;

            // The last target is reached dead-on; earlier ones may be offset so the
            // intake lines up with the target while the body stays on the path.
            if (intakeHalfWidth <= 1e-9 || groupIndex == targetGroups.size() - 1) {
                robotCenter = target;
            } else {
                Point nextGroupRepresentative =
                        groupRepresentative(orderedTargets, targetGroups.get(groupIndex + 1).indices);
                robotCenter = solveIntakeCapturePoint(
                        target, heading, intakeHalfWidth, previousRawTarget, nextGroupRepresentative);

                // Do not let the sideways shift push the robot into an obstacle the
                // target itself is clear of.
                if (obstacles != null
                        && !obstacles.isEmpty()
                        && isTargetBlocked(robotCenter, heading, obstacles, config)) {
                    robotCenter = target;
                }
            }

            appendKeyPoint(keyPoints, headings, lockedKeyPoints, robotCenter, heading, firstPose.hasLockedHeading());
            previousRawTarget = target;
        }

        if (!start.hasHeading()) {
            headings.set(0, firstBearing(startPoint, keyPoints));
        }

        if (keyPoints.size() == 1) {
            keyPoints.add(startPoint);
            headings.add(headings.get(0));
            lockedKeyPoints.add(false);
        }

        // A last target with a locked heading keeps it, even when it shares a key point
        // with the target before it.
        TargetSpec lastTarget = orderedTargets.get(orderedTargets.size() - 1);
        double endHeading =
                lastTarget.hasLockedHeading() ? lastTarget.getHeadingRad() : headings.get(headings.size() - 1);

        applyTurnaround(keyPoints, headings, lockedKeyPoints, obstacles, config);

        PathRoute path = buildRoute(keyPoints, headings, lockedKeyPoints);
        path = finish(path, obstacles, config, true);

        return new PathResult(path, Collections.singletonList(endHeading), visitedTargets, unvisitedTargets);
    }

    private static List<TargetSpec> selectTargets(
            Point start,
            Point startPoint,
            List<TargetSpec> targets,
            List<Zone> obstacles,
            boolean reorder,
            int maxTargets,
            PathConfig config) {
        boolean dropBlocked = config.isExcludeBlockedTargets();

        if (!reorder) {
            List<TargetSpec> ordered = dropBlocked
                    ? excludeBlockedTargets(startPoint, new ArrayList<>(targets), obstacles, config)
                    : new ArrayList<>(targets);

            return ordered.size() > maxTargets ? new ArrayList<>(ordered.subList(0, maxTargets)) : ordered;
        }

        TargetOrderer.TargetReachability reachability = dropBlocked
                ? (from, target) -> !isTargetBlocked(target, from, obstacles, config)
                : (from, target) -> true;

        return TargetOrderer.selectSpecs(startPoint, start.getHeadingRad(), targets, maxTargets, config, reachability);
    }

    private static PathResult generateAvoidanceResult(
            Point start, Point end, HeadingPolicy endPolicy, List<Zone> obstacles, PathConfig config) {
        Point startPoint = new Point(start.getX(), start.getY());
        Point endPoint = new Point(end.getX(), end.getY());
        PathRoute finished;

        if (endPolicy == HeadingPolicy.LOCKED) {
            double startHeading = start.hasHeading() ? start.getHeadingRad() : bearing(startPoint, endPoint);
            PathCurve curve =
                    lockedDestinationCurve(startPoint, startHeading, endPoint, end.getHeadingRad(), obstacles, config);
            PathRoute seedPath = new PathRoute(Collections.singletonList(curve));

            finished = finish(seedPath, obstacles, config, true);
        } else {
            List<Point> seedPoints = new ArrayList<>();

            for (int i = 0; i < SEED_CONTROL_POINTS; i++) {
                double progress = (double) i / (SEED_CONTROL_POINTS - 1);
                seedPoints.add(new Point(
                        startPoint.getX() + (endPoint.getX() - startPoint.getX()) * progress,
                        startPoint.getY() + (endPoint.getY() - startPoint.getY()) * progress));
            }

            PathCurve curve = new PathCurve(seedPoints);
            PathRoute seedPath = new PathRoute(Collections.singletonList(curve));

            // The seed's bearing means nothing once the route bends around an obstacle.
            finished = finish(seedPath, obstacles, config, false);
        }

        double endHeading = endPolicy == HeadingPolicy.LOCKED ? end.getHeadingRad() : finished.getHeading(1.0);

        return new PathResult(
                finished,
                Collections.singletonList(endHeading),
                Collections.<Point>emptyList(),
                Collections.<Point>emptyList());
    }

    private static PathCurve lockedDestinationCurve(
            Point startPoint,
            double startHeading,
            Point endPoint,
            double endHeading,
            List<Zone> obstacles,
            PathConfig config) {
        double travel = bearing(startPoint, endPoint);
        boolean startTurns = Math.abs(normalizeAngle(travel - startHeading)) >= TURNAROUND_ANGLE;
        boolean endTurns = Math.abs(normalizeAngle(travel - endHeading)) >= TURNAROUND_ANGLE;

        if (!startTurns && !endTurns) {
            return twoPointCubic(startPoint, endPoint, startHeading, endHeading);
        }

        List<Point> arc = turnaroundArc(startPoint, startHeading, endPoint, endHeading, config);

        if (arc == null || arc.isEmpty() || !fits(startPoint, arc, obstacles, config)) {
            return twoPointCubic(startPoint, endPoint, startHeading, endHeading);
        }

        List<Point> keyPoints = new ArrayList<>();
        List<Double> headings = new ArrayList<>();
        keyPoints.add(startPoint);
        headings.add(startHeading);

        for (Point waypoint : arc) {
            keyPoints.add(waypoint);
            headings.add(waypoint.getHeadingRad());
        }

        keyPoints.add(endPoint);
        headings.add(endHeading);
        return cubicHermiteChain(keyPoints, headings);
    }

    private static PathRoute finish(PathRoute path, List<Zone> obstacles, PathConfig config, boolean keepEndTangents) {
        if (config.isSmoothing()) {
            path = ObstacleAvoider.smooth(path);
        }

        if (obstacles != null && !obstacles.isEmpty()) {
            path = ObstacleAvoider.avoid(path, obstacles, config, keepEndTangents);
        }

        return path;
    }

    /**
     * Replaces every leg that turns around with waypoints along a real-radius arc.
     * A straight Hermite span would collapse and hook back on itself.
     */
    private static void applyTurnaround(
            List<Point> keyPoints,
            List<Double> headings,
            List<Boolean> lockedKeyPoints,
            List<Zone> obstacles,
            PathConfig config) {
        int index = 0;

        while (index + 1 < keyPoints.size()) {
            Point from = keyPoints.get(index);
            Point to = keyPoints.get(index + 1);
            double travel = bearing(from, to);
            double departure = headings.get(index);
            double arrival = headings.get(index + 1);

            // The arrival heading matters too: a locked target can ask to be reached
            // from the opposite side even when the leg leaves cleanly.
            boolean departureTurns = Math.abs(normalizeAngle(travel - departure)) >= TURNAROUND_ANGLE;
            boolean arrivalTurns = Math.abs(normalizeAngle(travel - arrival)) >= TURNAROUND_ANGLE;

            if (!departureTurns && !arrivalTurns) {
                index++;
                continue;
            }

            List<Point> arc = turnaroundArc(from, departure, to, arrival, config);

            if (arc == null || arc.isEmpty()) {
                // Too short to swing out in. A locked heading is a hard constraint; a
                // free one falls back to the line of travel rather than hooking.
                if (!lockedKeyPoints.get(index)) {
                    headings.set(index, travel);
                }

                index++;
                continue;
            }

            // An arc the robot does not fit is no better than the hook it replaces.
            if (!fits(from, arc, obstacles, config)) {
                index++;
                continue;
            }

            for (int i = 0; i < arc.size(); i++) {
                keyPoints.add(index + 1 + i, arc.get(i));
                headings.add(index + 1 + i, arc.get(i).getHeadingRad());
                lockedKeyPoints.add(index + 1 + i, false);
            }

            index += arc.size() + 1;
        }
    }

    private static List<Point> turnaroundArc(
            Point from, double departure, Point to, double arrival, PathConfig config) {
        // Swing as wide as a quarter footprint allows, and never wider than the leg
        // has room for: circles that close together have no crossing tangent.
        double swing = Math.max(Math.max(config.getRobotWidth(), config.getRobotHeight()), 1.0) / 4.0;
        double radius = Math.min(swing, from.distanceTo(to) / 2.5);

        if (!(radius >= MIN_TURN_RADIUS)) {
            return null;
        }

        return TurnaroundPath.build(from, departure, to, arrival, radius);
    }

    private static boolean fits(Point start, List<Point> waypoints, List<Zone> obstacles, PathConfig config) {
        if (obstacles == null || obstacles.isEmpty()) {
            return true;
        }

        for (Point waypoint : waypoints) {
            if (isTargetBlocked(waypoint, waypoint.getHeadingRad(), obstacles, config)) {
                return false;
            }
        }

        return !isTargetBlocked(start, waypoints.get(0).getHeadingRad(), obstacles, config);
    }

    /** Wraps an angle onto {@code (-pi, pi]}. */
    private static double normalizeAngle(double angle) {
        // Use the remainder so a huge heading normalizes in constant time.
        double normalized = angle % (2 * Math.PI);

        if (normalized > Math.PI) {
            normalized -= 2 * Math.PI;
        } else if (normalized <= -Math.PI) {
            normalized += 2 * Math.PI;
        }

        return normalized;
    }

    private static List<TargetGroup> groupTargets(
            Point startPoint,
            double startHeadingRad,
            List<TargetSpec> targets,
            List<Zone> obstacles,
            PathConfig config,
            boolean reorder) {
        List<TargetGroup> groups = new ArrayList<>();
        int targetCount = targets.size();
        double width = config.getIntakeWidth();

        if (width <= 1e-9) {
            for (int i = 0; i < targetCount; i++) {
                groups.add(new TargetGroup(new int[] {i}, startHeadingRad));
            }

            return groups;
        }

        Point previousKeyPoint = startPoint;
        double previousHeading = startHeadingRad;
        int groupStart = 0;

        while (groupStart < targetCount) {
            TargetSpec firstPose = targets.get(groupStart);
            int groupEnd = groupStart + 1;
            double groupHeading = Double.NaN;

            // Try the whole rest of the row first: a heading that fits a few targets can
            // hide the line the entire row runs along.
            double wholeHeading = Double.NaN;

            if (!hasLockedHeading(targets, groupStart, targetCount - 1)) {
                wholeHeading = fitHeading(
                        targets,
                        groupStart,
                        targetCount - 1,
                        previousKeyPoint,
                        previousHeading,
                        width,
                        reorder ? Double.MAX_VALUE : width / 2.0);
            }

            if (!Double.isNaN(wholeHeading)
                    && !isSweepBlocked(
                            targets, groupStart, targetCount - 1, previousKeyPoint, wholeHeading, obstacles, config)) {
                groupEnd = targetCount;
                groupHeading = wholeHeading;
            } else {
                while (groupEnd < targetCount) {
                    TargetSpec previousPose = targets.get(groupEnd - 1);
                    TargetSpec candidatePose = targets.get(groupEnd);

                    if (previousPose.hasLockedHeading() || candidatePose.hasLockedHeading()) {
                        break;
                    }

                    double candidateHeading = fitHeading(
                            targets, groupStart, groupEnd, previousKeyPoint, previousHeading, width, width / 2.0);

                    if (Double.isNaN(candidateHeading)) {
                        break;
                    }

                    if (isSweepBlocked(
                            targets, groupStart, groupEnd, previousKeyPoint, candidateHeading, obstacles, config)) {
                        break;
                    }

                    groupHeading = candidateHeading;
                    groupEnd++;
                }

                // A single target keeps its locked heading, or heads toward the next
                // target when it sits on the previous key point.
                if (groupEnd == groupStart + 1) {
                    groupHeading = firstPose.hasLockedHeading()
                            ? firstPose.getHeadingRad()
                            : sweepDirection(previousKeyPoint, targets, groupStart, previousHeading);
                }
            }

            int[] groupIndices = new int[groupEnd - groupStart];

            for (int k = 0; k < groupIndices.length; k++) {
                groupIndices[k] = groupStart + k;
            }

            if (groupIndices.length > 1) {
                groupIndices = sortedAlong(targets, groupIndices, previousKeyPoint, groupHeading);
            }

            groups.add(new TargetGroup(groupIndices, groupHeading));

            previousKeyPoint = groupIndices.length > 1
                    ? lastOf(sweepKeyPoints(targets, groupIndices, previousKeyPoint, groupHeading))
                    : firstPose.position();
            previousHeading = groupHeading;
            groupStart = groupEnd;
        }

        return groups;
    }

    private static Point addMergedGroup(
            List<TargetSpec> targets,
            int[] groupIndices,
            double heading,
            Point previousPoint,
            List<Point> keyPoints,
            List<Double> headings,
            List<Boolean> lockedKeyPoints) {
        List<Point> sweep = sweepKeyPoints(targets, groupIndices, previousPoint, heading);

        for (int i = 0; i < sweep.size(); i++) {
            Point point = sweep.get(i);

            // After the first point, a target this close adds nothing: the intake
            // sweeps the cluster in one pass either way.
            if (i > 0 && lastOf(keyPoints).distanceTo(point) < MIN_KEY_POINT_GAP) {
                continue;
            }

            appendKeyPoint(keyPoints, headings, lockedKeyPoints, point, heading, false);
        }

        return lastOf(sweep);
    }

    private static boolean isSweepBlocked(
            List<TargetSpec> targets,
            int firstIndex,
            int lastIndex,
            Point previousPoint,
            double heading,
            List<Zone> obstacles,
            PathConfig config) {
        if (obstacles == null || obstacles.isEmpty()) {
            return false;
        }

        int[] groupIndices = new int[lastIndex - firstIndex + 1];

        for (int k = 0; k < groupIndices.length; k++) {
            groupIndices[k] = firstIndex + k;
        }

        for (Point point : sweepKeyPoints(targets, groupIndices, previousPoint, heading)) {
            if (isTargetBlocked(point, heading, obstacles, config)) {
                return true;
            }
        }

        return false;
    }

    private static List<Point> sweepKeyPoints(
            List<TargetSpec> targets, int[] groupIndices, Point previousPoint, double sweepHeading) {
        double directionX = Math.cos(sweepHeading);
        double directionY = Math.sin(sweepHeading);
        double perpendicularX = -directionY;
        double perpendicularY = directionX;

        double[] longitudinalOffsets = new double[groupIndices.length];
        Integer[] visitOrder = new Integer[groupIndices.length];

        double minLateralOffset = Double.MAX_VALUE;
        double maxLateralOffset = -Double.MAX_VALUE;

        for (int k = 0; k < groupIndices.length; k++) {
            TargetSpec pose = targets.get(groupIndices[k]);

            double offsetX = pose.getX() - previousPoint.getX();
            double offsetY = pose.getY() - previousPoint.getY();

            double lateralOffset = offsetX * perpendicularX + offsetY * perpendicularY;
            minLateralOffset = Math.min(minLateralOffset, lateralOffset);
            maxLateralOffset = Math.max(maxLateralOffset, lateralOffset);

            longitudinalOffsets[k] = offsetX * directionX + offsetY * directionY;
            visitOrder[k] = k;
        }

        Arrays.sort(visitOrder, Comparator.comparingDouble(a -> longitudinalOffsets[a]));

        double centerLateralOffset = (minLateralOffset + maxLateralOffset) / 2.0;
        List<Point> sweep = new ArrayList<>();

        for (int k : visitOrder) {
            sweep.add(new Point(
                    previousPoint.getX() + longitudinalOffsets[k] * directionX + centerLateralOffset * perpendicularX,
                    previousPoint.getY() + longitudinalOffsets[k] * directionY + centerLateralOffset * perpendicularY));
        }

        return sweep;
    }

    /**
     * Chooses the direction a candidate group runs in, or {@code NaN} when it cannot be
     * collected in a single pass. The line must not point back away from the group and
     * must keep every target within the intake width in a monotonic visit order.
     */
    private static double fitHeading(
            List<TargetSpec> targets,
            int firstIndex,
            int lastIndex,
            Point previousPoint,
            double fallbackHeading,
            double width,
            double relaxedTolerance) {
        double sumX = 0;
        double sumY = 0;

        for (int i = firstIndex; i <= lastIndex; i++) {
            sumX += targets.get(i).getX();
            sumY += targets.get(i).getY();
        }

        int count = lastIndex - firstIndex + 1;
        double towardCenterX = sumX / count - previousPoint.getX();
        double towardCenterY = sumY / count - previousPoint.getY();

        List<Double> candidates = new ArrayList<>();
        candidates.add(firstTargetHeading(previousPoint, targets, firstIndex, lastIndex, fallbackHeading));

        if (Math.hypot(towardCenterX, towardCenterY) > 1e-9) {
            candidates.add(Math.atan2(towardCenterY, towardCenterX));
        } else {
            candidates.add(fallbackHeading);
        }

        // For a reversed pair, directions perpendicular to that pair keep the order; the
        // pair's own direction is tried too, since a row runs straight along it.
        for (int i = firstIndex; i < lastIndex; i++) {
            TargetSpec from = targets.get(i);
            TargetSpec to = targets.get(i + 1);
            double pairBearing = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
            candidates.add(pairBearing);
            candidates.add(pairBearing + Math.PI / 2.0);
            candidates.add(pairBearing - Math.PI / 2.0);
        }

        for (double candidate : candidates) {
            if (pointsForward(towardCenterX, towardCenterY, candidate)
                    && fitsSweep(targets, firstIndex, lastIndex, previousPoint, candidate, width, 1e-9)) {
                return candidate;
            }
        }

        // No strict order works. A tight cluster whose whole spread fits the intake is
        // still one pass, allowing a target to sit behind the one before it.
        for (double candidate : candidates) {
            if (pointsForward(towardCenterX, towardCenterY, candidate)
                    && fitsSweep(targets, firstIndex, lastIndex, previousPoint, candidate, width, relaxedTolerance)) {
                return candidate;
            }
        }

        return Double.NaN;
    }

    /** Whether {@code heading} points into the group rather than back away from it. */
    private static boolean pointsForward(double towardCenterX, double towardCenterY, double heading) {
        if (Math.hypot(towardCenterX, towardCenterY) < 1e-9) {
            return true;
        }

        return towardCenterX * Math.cos(heading) + towardCenterY * Math.sin(heading) > -1e-9;
    }

    private static double firstTargetHeading(
            Point previousPoint, List<TargetSpec> targets, int firstIndex, int lastIndex, double fallbackHeading) {
        for (int i = firstIndex; i <= lastIndex; i++) {
            TargetSpec target = targets.get(i);

            if (Math.hypot(target.getX() - previousPoint.getX(), target.getY() - previousPoint.getY()) > 1e-9) {
                return bearing(previousPoint, target.position());
            }
        }

        return fallbackHeading;
    }

    private static double sweepDirection(
            Point previousPoint, List<TargetSpec> targets, int firstIndex, double fallbackHeading) {
        for (int i = firstIndex; i < targets.size(); i++) {
            TargetSpec target = targets.get(i);

            if (Math.hypot(target.getX() - previousPoint.getX(), target.getY() - previousPoint.getY()) > 1e-9) {
                return bearing(previousPoint, target.position());
            }
        }

        return fallbackHeading;
    }

    /**
     * Checks that every target fits within the intake width of the sweep line and that
     * none sits more than {@code alongTolerance} behind the target before it.
     */
    private static boolean fitsSweep(
            List<TargetSpec> targets,
            int firstIndex,
            int lastIndex,
            Point previousPoint,
            double heading,
            double width,
            double alongTolerance) {
        double directionX = Math.cos(heading);
        double directionY = Math.sin(heading);

        double minLateralOffset = Double.MAX_VALUE;
        double maxLateralOffset = -Double.MAX_VALUE;
        double previousAlongOffset = -Double.MAX_VALUE;

        for (int i = firstIndex; i <= lastIndex; i++) {
            TargetSpec target = targets.get(i);

            double offsetX = target.getX() - previousPoint.getX();
            double offsetY = target.getY() - previousPoint.getY();
            double alongOffset = offsetX * directionX + offsetY * directionY;

            if (alongOffset < previousAlongOffset - alongTolerance) {
                return false;
            }

            previousAlongOffset = alongOffset;

            double lateralOffset = offsetX * -directionY + offsetY * directionX;
            minLateralOffset = Math.min(minLateralOffset, lateralOffset);
            maxLateralOffset = Math.max(maxLateralOffset, lateralOffset);
        }

        return maxLateralOffset - minLateralOffset <= width + 1e-9;
    }

    /**
     * Appends a key point unless it coincides with the previous one, which would leave
     * a zero-length cubic with an undefined tangent. The earlier point keeps its heading.
     */
    private static void appendKeyPoint(
            List<Point> keyPoints,
            List<Double> headings,
            List<Boolean> lockedKeyPoints,
            Point point,
            double heading,
            boolean locked) {
        if (lastOf(keyPoints).distanceTo(point) < 1e-6) {
            return;
        }

        keyPoints.add(point);
        headings.add(heading);
        lockedKeyPoints.add(locked);
    }

    private static Point lastOf(List<Point> points) {
        return points.get(points.size() - 1);
    }

    private static boolean hasLockedHeading(List<TargetSpec> targets, int firstIndex, int lastIndex) {
        for (int i = firstIndex; i <= lastIndex; i++) {
            if (targets.get(i).hasLockedHeading()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Orders targets so the intake can sweep them: nearby targets form a cluster,
     * clusters are visited nearest-first, and each cluster is ordered along its widest
     * direction.
     */
    private static List<TargetSpec> orderForIntake(Point start, List<TargetSpec> targets, PathConfig config) {
        double width = config.getIntakeWidth();
        int count = targets.size();
        int[] roots = new int[count];

        for (int i = 0; i < count; i++) {
            roots[i] = i;
        }

        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                if (targets.get(i).distanceTo(targets.get(j).position()) <= width) {
                    unionRoots(roots, i, j);
                }
            }
        }

        Map<Integer, List<Integer>> clusters = new LinkedHashMap<>();

        for (int i = 0; i < count; i++) {
            clusters.computeIfAbsent(findRoot(roots, i), key -> new ArrayList<>())
                    .add(i);
        }

        List<List<Integer>> remaining = new ArrayList<>(clusters.values());
        List<TargetSpec> ordered = new ArrayList<>();
        Point cursor = start;

        while (!remaining.isEmpty()) {
            int nearest = 0;
            double nearestDistance = Double.MAX_VALUE;

            for (int c = 0; c < remaining.size(); c++) {
                double distance = cursor.distanceTo(clusterCentroid(targets, remaining.get(c)));

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = c;
                }
            }

            appendCluster(targets, remaining.remove(nearest), cursor, ordered);
            cursor = ordered.get(ordered.size() - 1).position();
        }

        return ordered;
    }

    private static void appendCluster(
            List<TargetSpec> targets, List<Integer> cluster, Point cursor, List<TargetSpec> ordered) {
        if (cluster.size() == 1) {
            ordered.add(targets.get(cluster.get(0)));
            return;
        }

        double heading = widestDirection(targets, cluster);
        double directionX = Math.cos(heading);
        double directionY = Math.sin(heading);

        List<Integer> sorted = new ArrayList<>(cluster);
        sorted.sort(Comparator.comparingDouble(index -> {
            TargetSpec target = targets.get(index);
            return target.getX() * directionX + target.getY() * directionY;
        }));

        TargetSpec first = targets.get(sorted.get(0));
        TargetSpec last = targets.get(sorted.get(sorted.size() - 1));

        if (cursor.distanceTo(first.position()) > cursor.distanceTo(last.position())) {
            Collections.reverse(sorted);
        }

        for (int index : sorted) {
            ordered.add(targets.get(index));
        }
    }

    private static double widestDirection(List<TargetSpec> targets, List<Integer> cluster) {
        double bestHeading = 0.0;
        double smallestSpread = Double.MAX_VALUE;

        for (int step = 0; step < 180; step++) {
            double heading = Math.toRadians(step);
            double directionX = Math.cos(heading);
            double directionY = Math.sin(heading);
            double minLateral = Double.MAX_VALUE;
            double maxLateral = -Double.MAX_VALUE;

            for (int index : cluster) {
                TargetSpec target = targets.get(index);
                double lateral = target.getX() * -directionY + target.getY() * directionX;
                minLateral = Math.min(minLateral, lateral);
                maxLateral = Math.max(maxLateral, lateral);
            }

            if (maxLateral - minLateral < smallestSpread) {
                smallestSpread = maxLateral - minLateral;
                bestHeading = heading;
            }
        }

        return bestHeading;
    }

    private static Point clusterCentroid(List<TargetSpec> targets, List<Integer> indices) {
        double sumX = 0;
        double sumY = 0;

        for (int index : indices) {
            sumX += targets.get(index).getX();
            sumY += targets.get(index).getY();
        }

        return new Point(sumX / indices.size(), sumY / indices.size());
    }

    private static int findRoot(int[] roots, int index) {
        while (roots[index] != index) {
            roots[index] = roots[roots[index]];
            index = roots[index];
        }

        return index;
    }

    private static void unionRoots(int[] roots, int a, int b) {
        int rootA = findRoot(roots, a);
        int rootB = findRoot(roots, b);

        if (rootA != rootB) {
            roots[rootA] = rootB;
        }
    }

    private static int[] sortedAlong(List<TargetSpec> targets, int[] indices, Point previousPoint, double heading) {
        double directionX = Math.cos(heading);
        double directionY = Math.sin(heading);
        Integer[] order = new Integer[indices.length];
        double[] along = new double[indices.length];

        for (int k = 0; k < indices.length; k++) {
            TargetSpec target = targets.get(indices[k]);
            along[k] = (target.getX() - previousPoint.getX()) * directionX
                    + (target.getY() - previousPoint.getY()) * directionY;
            order[k] = k;
        }

        Arrays.sort(order, Comparator.comparingDouble(a -> along[a]));
        int[] sorted = new int[indices.length];

        for (int k = 0; k < indices.length; k++) {
            sorted[k] = indices[order[k]];
        }

        return sorted;
    }

    private static Point groupRepresentative(List<TargetSpec> targets, int[] indices) {
        double sumX = 0;
        double sumY = 0;

        for (int index : indices) {
            sumX += targets.get(index).getX();
            sumY += targets.get(index).getY();
        }

        return new Point(sumX / indices.length, sumY / indices.length);
    }

    /**
     * Offsets the robot center sideways from {@code target} so the intake mouth covers
     * it, where the lateral axis crosses the line through the surrounding targets.
     */
    private static Point solveIntakeCapturePoint(
            Point target, double heading, double halfWidth, Point previousRaw, Point nextRaw) {
        double cosHeading = Math.cos(heading);
        double sinHeading = Math.sin(heading);
        double lateralAxisX = sinHeading;
        double lateralAxisY = -cosHeading;

        double travelX = nextRaw.getX() - previousRaw.getX();
        double travelY = nextRaw.getY() - previousRaw.getY();

        double denominator = lateralAxisX * travelY - lateralAxisY * travelX;
        double lateralOffset = 0;

        if (Math.abs(denominator) > 1e-9) {
            lateralOffset =
                    ((previousRaw.getX() - target.getX()) * travelY - (previousRaw.getY() - target.getY()) * travelX)
                            / denominator;
        }

        lateralOffset = Math.max(-halfWidth, Math.min(halfWidth, lateralOffset));

        return new Point(target.getX() + lateralOffset * lateralAxisX, target.getY() + lateralOffset * lateralAxisY);
    }

    private static List<TargetSpec> excludeBlockedTargets(
            Point startPoint, List<TargetSpec> targets, List<Zone> obstacles, PathConfig config) {
        if (obstacles == null || obstacles.isEmpty()) {
            return targets;
        }

        List<TargetSpec> reachableTargets = new ArrayList<>();
        Point previousPoint = startPoint;

        for (TargetSpec target : targets) {
            if (!isTargetBlocked(target, previousPoint, obstacles, config)) {
                reachableTargets.add(target);
                previousPoint = target.position();
            }
        }

        return reachableTargets;
    }

    private static boolean isTargetBlocked(TargetSpec target, Point from, List<Zone> obstacles, PathConfig config) {
        double heading = target.hasLockedHeading() ? target.getHeadingRad() : bearing(from, target.position());

        return isTargetBlocked(target.position(), heading, obstacles, config);
    }

    private static boolean isTargetBlocked(Point target, double heading, List<Zone> obstacles, PathConfig config) {
        if (config.getRobotWidth() <= 0 || config.getRobotHeight() <= 0) {
            for (Zone zone : obstacles) {
                if (zone.contains(target) || zone.distanceToBoundary(target) < config.getClearance()) {
                    return true;
                }
            }

            return false;
        }

        Zone footprint = new PolygonZone(target, config.getRobotWidth(), config.getRobotHeight(), heading);

        for (Zone zone : obstacles) {
            if (zone.isInside(footprint) || zone.distanceTo(footprint) < config.getClearance()) {
                return true;
            }
        }

        return false;
    }

    private static double firstBearing(Point from, List<Point> points) {
        for (Point point : points) {
            if (from.distanceTo(point) > 1e-6) {
                return bearing(from, point);
            }
        }

        return 0.0;
    }

    private static double firstTargetBearing(Point from, List<TargetSpec> targets) {
        for (TargetSpec target : targets) {
            if (from.distanceTo(target.position()) > 1e-6) {
                return bearing(from, target.position());
            }
        }

        return 0.0;
    }

    /**
     * Builds the route, splitting at every locked target so smoothing and avoidance
     * preserve that target's heading.
     */
    private static PathRoute buildRoute(List<Point> keyPoints, List<Double> headings, List<Boolean> lockedKeyPoints) {
        List<PathCurve> segments = new ArrayList<>();
        int spanStart = 0;

        for (int i = 1; i < keyPoints.size(); i++) {
            if (lockedKeyPoints.get(i) || i == keyPoints.size() - 1) {
                segments.add(
                        cubicHermiteChain(keyPoints.subList(spanStart, i + 1), headings.subList(spanStart, i + 1)));
                spanStart = i;
            }
        }

        if (segments.isEmpty()) {
            segments.add(cubicHermiteChain(keyPoints, headings));
        }

        return new PathRoute(segments);
    }

    private static double bearing(Point from, Point to) {
        return Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
    }

    /** Builds one cubic from start to end, with handles one third of the span long. */
    private static PathCurve twoPointCubic(Point start, Point end, double startHeading, double endHeading) {
        double handleLength = start.distanceTo(end) / 3.0;

        return new PathCurve(Arrays.asList(
                start,
                new Point(
                        start.getX() + handleLength * Math.cos(startHeading),
                        start.getY() + handleLength * Math.sin(startHeading)),
                new Point(
                        end.getX() - handleLength * Math.cos(endHeading),
                        end.getY() - handleLength * Math.sin(endHeading)),
                end));
    }

    /**
     * Smooths key points into a cubic chain. Each handle follows the heading stored for
     * its key point, giving continuous tangents across the chain.
     */
    private static PathCurve cubicHermiteChain(List<Point> points, List<Double> headings) {
        int pointCount = points.size();

        if (pointCount == 2) {
            return twoPointCubic(points.get(0), points.get(1), headings.get(0), headings.get(1));
        }

        List<Point> controlPoints = new ArrayList<>();

        for (int i = 0; i < pointCount - 1; i++) {
            Point startPoint = points.get(i);
            Point endPoint = points.get(i + 1);

            double handleLength = startPoint.distanceTo(endPoint) / 3.0;
            double startHeading = headings.get(i);
            double endHeading = headings.get(i + 1);

            Point firstControlPoint = new Point(
                    startPoint.getX() + handleLength * Math.cos(startHeading),
                    startPoint.getY() + handleLength * Math.sin(startHeading));

            Point secondControlPoint = new Point(
                    endPoint.getX() - handleLength * Math.cos(endHeading),
                    endPoint.getY() - handleLength * Math.sin(endHeading));

            if (i == 0) {
                controlPoints.add(startPoint);
            }

            controlPoints.add(firstControlPoint);
            controlPoints.add(secondControlPoint);
            controlPoints.add(endPoint);
        }

        return new PathCurve(controlPoints);
    }
}
