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
 * Builds the geometric path (a chain of cubic Bézier curves) that a robot should
 * follow to visit targets or reach a destination.
 *
 * <p>The generator works in two distinct modes:
 * <ul>
 *     <li><b>Intake mode</b> - when one or more targets are supplied. Targets are
 *     ordered (optionally), clustered into rows that fit the intake, and connected
 *     by a Hermite curve whose tangents follow the intake heading so the robot can
 *     sweep them up.</li>
 *     <li><b>Avoidance mode</b> - when only a destination is supplied. A straight-ish
 *     seed path is generated and then bent around obstacles by {@link ObstacleAvoider}.</li>
 * </ul>
 */
public final class WeaverGenerator {
    private WeaverGenerator() {}

    /**
     * A leg that leaves more than this off its key point's heading is a turnaround rather
     * than a bend: fitted straight through, the control handles collapse onto one line and
     * the curve hooks back on itself.
     */
    private static final double TURNAROUND_ANGLE = Math.toRadians(60.0);

    /**
     * Tightest arc a turnaround is fitted at, in inches. Below this there is no point
     * swinging out; the robot turns on the spot instead.
     */
    private static final double MIN_TURN_RADIUS = 1.0;

    /**
     * Shortest gap kept between two key points of one sweep, in inches. Targets packed
     * closer than this share a key point: the intake passes over the whole cluster either
     * way, and a span this short only hands the fitter a segment the robot cannot turn in.
     */
    private static final double MIN_KEY_POINT_GAP = 0.5;

    /** Targets collected in a single intake pass, and the direction the pass runs in. */
    private static final class TargetGroup {
        private final int[] indices;
        private final double heading;

        private TargetGroup(int[] indices, double heading) {
            this.indices = indices;
            this.heading = heading;
        }
    }

    /**
     * Generates a path from the given inputs.
     *
     * @param startPose        where the robot starts (pose and heading)
     * @param destinationPose  goal pose in avoidance mode; may be {@code null} when targets exist
     * @param targets          intake targets to visit; may be empty
     * @param obstacles        zones the robot must not cut through; may be {@code null}
     * @param reorder          {@code true} to let {@link TargetOrderer} pick the visit order
     * @param maxTargets       maximum number of targets to visit; {@code 0} or less visits all
     * @param config           geometry and tuning parameters
     * @return the generated route plus metadata
     * @throws IllegalStateException if neither targets nor a destination are provided
     */
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

    /**
     * Generates a path from target specifications that carry their own heading policy.
     *
     * @param startPose            where the robot starts
     * @param destinationPose      goal pose in avoidance mode; may be {@code null} when targets exist
     * @param targets              intake targets to visit; may be empty
     * @param destinationPolicy    heading policy for the destination
     * @param obstacles            zones the robot must not cut through; may be {@code null}
     * @param reorder              {@code true} to let {@link TargetOrderer} pick the visit order
     * @param maxTargets           maximum number of targets to visit; {@code 0} or less visits all
     * @param config               geometry and tuning parameters
     * @return the generated route plus metadata
     */
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

    /**
     * Tests whether {@code target} lies inside the robot's intake mouth.
     * <p>
     * The target is projected into the robot's local frame (forward axis and
     * lateral axis). It is captured when it is (almost) exactly on the forward axis
     * and within half the intake width sideways.
     *
     * @param robotCenter  current robot position
     * @param headingRad   robot heading in radians
     * @param intakeWidth  total intake width; half is allowed to each side
     * @param target       game element position
     * @return {@code true} if the target is within the intake
     */
    public static boolean isCapturedByIntake(Point robotCenter, double headingRad, double intakeWidth, Point target) {
        double cosHeading = Math.cos(headingRad);
        double sinHeading = Math.sin(headingRad);

        double relativeX = target.getX() - robotCenter.getX();
        double relativeY = target.getY() - robotCenter.getY();

        // Forward axis component of the target, zero means dead ahead.
        double alongRobot = relativeX * cosHeading + relativeY * sinHeading;

        // Lateral axis component, bounded by half the intake width.
        double lateral = relativeX * -sinHeading + relativeY * cosHeading;

        return Math.abs(alongRobot) < 1e-6 && Math.abs(lateral) <= intakeWidth / 2.0 + 1e-6;
    }

    /**
     * Convenience overload of {@link #isCapturedByIntake(Point, double, double, Point)}
     * that reads the intake width from the config.
     *
     * @param robotCenter current robot position
     * @param headingRad  robot heading in radians
     * @param config      configuration supplying the intake width
     * @param target      game element position
     * @return {@code true} if the target is within the intake
     */
    public static boolean isCapturedByIntake(Point robotCenter, double headingRad, PathConfig config, Point target) {
        return isCapturedByIntake(robotCenter, headingRad, config.getIntakeWidth(), target);
    }

    /**
     * Samples a single curve and checks that the robot footprint keeps the required
     * clearance from every obstacle. A sample with a non-finite position or heading is
     * never clear.
     *
     * @param curve       curve to sample
     * @param obstacles   zones to avoid
     * @param clearance   extra distance to keep from obstacles
     * @param robotWidth  footprint width along the forward axis
     * @param robotHeight footprint height across the forward axis
     * @param samples     number of points to test along the curve
     * @return {@code true} if the whole curve is clear
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

            // A sample with no finite position cannot be cleared, and the distance
            // comparisons below would let it through.
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

                // A sample with no finite heading yields a footprint of NaN points, and
                // the comparisons below would let it through as clear.
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

    /**
     * Config-based overload of
     * {@link #isPathClear(PathCurve, List, double, double, double, int)} for a whole route.
     *
     * @param path              route to test
     * @param obstacles         zones to avoid
     * @param config            configuration supplying clearance and footprint
     * @param samplesPerSegment number of points to test along each segment
     * @return {@code true} if every segment of the route is clear
     */
    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, PathConfig config, int samplesPerSegment) {
        return isPathClear(
                path,
                obstacles,
                config.getClearance(),
                config.getRobotWidth(),
                config.getRobotHeight(),
                samplesPerSegment);
    }

    /**
     * Config-based overload of
     * {@link #isPathClear(PathCurve, List, double, double, double, int)}.
     *
     * @param curve     curve to test
     * @param obstacles zones to avoid
     * @param config    configuration supplying clearance and footprint
     * @param samples   number of points to test along the curve
     * @return {@code true} if the whole curve is clear
     */
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, PathConfig config, int samples) {
        return isPathClear(
                curve, obstacles, config.getClearance(), config.getRobotWidth(), config.getRobotHeight(), samples);
    }

    /**
     * Checks a whole route using the config and 100 samples per segment.
     *
     * @param path      route to test
     * @param obstacles zones to avoid
     * @param config    configuration supplying clearance and footprint
     * @return {@code true} if every segment of the route is clear
     */
    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, PathConfig config) {
        return isPathClear(path, obstacles, config, 100);
    }

    /**
     * Checks a single curve using the config and 100 samples.
     *
     * @param curve     curve to test
     * @param obstacles zones to avoid
     * @param config    configuration supplying clearance and footprint
     * @return {@code true} if the whole curve is clear
     */
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, PathConfig config) {
        return isPathClear(curve, obstacles, config, 100);
    }

    /**
     * Checks that every segment of a route is clear.
     *
     * @param path              route to test
     * @param obstacles         zones to avoid
     * @param clearance         extra distance to keep from obstacles
     * @param robotWidth        footprint width along the forward axis
     * @param robotHeight       footprint height across the forward axis
     * @param samplesPerSegment number of points to test along each segment
     * @return {@code true} if every segment of the route is clear
     * @see #isPathClear(PathCurve, List, double, double, double, int)
     */
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

    /**
     * Checks a route against point clearance (no robot footprint).
     *
     * @param path               route to test
     * @param obstacles          zones to avoid
     * @param clearance          extra distance to keep from obstacles
     * @param samplesPerSegment  number of points to test along each segment
     * @return {@code true} if every segment of the route is clear
     */
    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, double clearance, int samplesPerSegment) {
        return isPathClear(path, obstacles, clearance, 0.0, 0.0, samplesPerSegment);
    }

    /**
     * Checks a single curve against point clearance (no robot footprint).
     *
     * @param curve     curve to test
     * @param obstacles zones to avoid
     * @param clearance extra distance to keep from obstacles
     * @param samples   number of points to test along the curve
     * @return {@code true} if the whole curve is clear
     */
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, double clearance, int samples) {
        return isPathClear(curve, obstacles, clearance, 0.0, 0.0, samples);
    }

    /**
     * Orders and clusters targets, then builds one smooth curve that sweeps through
     * them.
     *
     * @param start      robot start pose
     * @param targets    targets to visit
     * @param obstacles  zones to avoid; may be {@code null}
     * @param reorder    {@code true} to let {@link TargetOrderer} pick the visit order
     * @param maxTargets maximum number of targets to visit; {@code 0} or less visits all
     * @param config     geometry and tuning parameters
     * @return the generated route plus metadata
     */
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
            // No limit: keep the original ordering-then-filter pipeline unchanged.
            orderedTargets = reorder
                    ? TargetOrderer.orderSpecs(startPoint, start.getHeadingRad(), targets, config)
                    : new ArrayList<>(targets);

            if (config.isExcludeBlockedTargets()) {
                orderedTargets = excludeBlockedTargets(startPoint, orderedTargets, obstacles, config);
            }
        }

        // With an intake to sweep with, order so targets that can be swept together stay
        // together. A point-to-point order would otherwise thread a distant target
        // through a clump and break it into several passes.
        if (reorder
                && config.getIntakeWidth() > 1e-9
                && !hasLockedHeading(orderedTargets, 0, orderedTargets.size() - 1)) {
            orderedTargets = orderForIntake(startPoint, orderedTargets, config);
        }

        // Remove one entry per visit, so a target passed twice and visited once
        // still reports its second entry as unvisited.
        List<Point> unvisitedTargets = new ArrayList<>();

        for (TargetSpec target : targets) {
            unvisitedTargets.add(target.getPoint());
        }

        for (TargetSpec visited : orderedTargets) {
            unvisitedTargets.remove(visited.getPoint());
        }

        // No reachable target left: produce a stationary path at the start pose.
        if (orderedTargets.isEmpty()) {
            PathRoute idlePath = finish(
                    new PathRoute(Collections.singletonList(new PathCurve(Arrays.asList(startPoint, startPoint)))),
                    obstacles,
                    config,
                    true);

            // A start with no heading has no bearing to hold; report 0 rather than NaN.
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

        // The robot can only sweep multiple targets at once if the intake has width.
        double intakeHalfWidth = config.getIntakeWidth() / 2.0;
        List<TargetGroup> targetGroups =
                groupTargets(startPoint, headings.get(0), orderedTargets, obstacles, config, reorder);

        // The targets in the order the route actually passes them, which for a merged
        // sweep is the direction it runs in, not the requested order.
        List<Point> visitedTargets = new ArrayList<>();

        Point previousRawTarget = startPoint;

        for (int groupIndex = 0; groupIndex < targetGroups.size(); groupIndex++) {
            TargetGroup group = targetGroups.get(groupIndex);
            int[] groupIndices = group.indices;

            for (int index : groupIndices) {
                visitedTargets.add(orderedTargets.get(index).getPoint());
            }

            // A merged group (several targets in one intake pass) is handled separately.
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
            // intake mouth lines up with the target while the body stays on the path.
            if (intakeHalfWidth <= 1e-9 || groupIndex == targetGroups.size() - 1) {
                robotCenter = target;
            } else {
                Point nextGroupRepresentative =
                        groupRepresentative(orderedTargets, targetGroups.get(groupIndex + 1).indices);
                robotCenter = solveIntakeCapturePoint(
                        target, heading, intakeHalfWidth, previousRawTarget, nextGroupRepresentative);

                // The sideways shift must not push the robot into an obstacle the
                // target itself is clear of; reach the target dead-on instead.
                if (obstacles != null
                        && !obstacles.isEmpty()
                        && isTargetBlocked(robotCenter, heading, obstacles, config)) {
                    robotCenter = target;
                }
            }

            appendKeyPoint(keyPoints, headings, lockedKeyPoints, robotCenter, heading, firstPose.hasLockedHeading());
            previousRawTarget = target;
        }

        // A start with no heading leaves toward the first key point.
        if (!start.hasHeading()) {
            headings.set(0, firstBearing(startPoint, keyPoints));
        }

        // Every target was on top of the start: stay there.
        if (keyPoints.size() == 1) {
            keyPoints.add(startPoint);
            headings.add(headings.get(0));
            lockedKeyPoints.add(false);
        }

        // A last target with its own locked heading keeps it, even when it shares a key
        // point with the target before it.
        TargetSpec lastTarget = orderedTargets.get(orderedTargets.size() - 1);
        double endHeading =
                lastTarget.hasLockedHeading() ? lastTarget.getHeadingRad() : headings.get(headings.size() - 1);

        applyTurnaround(keyPoints, headings, lockedKeyPoints, obstacles, config);

        PathRoute path = buildRoute(keyPoints, headings, lockedKeyPoints);
        path = finish(path, obstacles, config, true);

        return new PathResult(path, Collections.singletonList(endHeading), visitedTargets, unvisitedTargets);
    }

    /**
     * Chooses at most {@code maxTargets} targets to visit.
     * <p>
     * When {@code reorder} is {@code true} the largest reachable set wins first, then
     * the cheapest order. When {@code false} the supplied order is kept and the first
     * reachable targets are taken.
     *
     * @param start      robot start pose
     * @param startPoint start position without heading
     * @param targets    candidate targets
     * @param obstacles  zones to avoid; may be {@code null}
     * @param reorder    {@code true} to let the orderer choose the best subset
     * @param maxTargets maximum number of targets to select
     * @param config     geometry and tuning parameters
     * @return the selected targets in visit order
     */
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

    /**
     * Seeds a straight line of control points between start and end, then lets
     * {@link ObstacleAvoider} bend and smooth it. A locked destination seeds a curve
     * that arrives along the requested heading instead.
     *
     * @param start         start pose
     * @param end           destination pose
     * @param endPolicy     heading policy for the destination
     * @param obstacles     zones to avoid; may be {@code null}
     * @param config        geometry and tuning parameters
     * @return the generated route plus metadata
     */
    private static PathResult generateAvoidanceResult(
            Point start, Point end, HeadingPolicy endPolicy, List<Zone> obstacles, PathConfig config) {
        Point startPoint = new Point(start.getX(), start.getY());
        Point endPoint = new Point(end.getX(), end.getY());
        PathRoute finished;

        if (endPolicy == HeadingPolicy.LOCKED) {
            double startHeading = start.hasHeading() ? start.getHeadingRad() : bearing(startPoint, endPoint);
            PathCurve curve = twoPointCubic(startPoint, endPoint, startHeading, end.getHeadingRad());
            PathRoute seedPath = new PathRoute(Collections.singletonList(curve));

            finished = finish(seedPath, obstacles, config, true);
        } else {
            // Number of control points used to seed the curve before avoidance.
            int seedControlPointCount = 6;

            List<Point> seedPoints = new ArrayList<>();

            for (int i = 0; i < seedControlPointCount; i++) {
                double progress = (double) i / (seedControlPointCount - 1);
                seedPoints.add(new Point(
                        startPoint.getX() + (endPoint.getX() - startPoint.getX()) * progress,
                        startPoint.getY() + (endPoint.getY() - startPoint.getY()) * progress));
            }

            PathCurve curve = new PathCurve(seedPoints);
            PathRoute seedPath = new PathRoute(Collections.singletonList(curve));
            // The seed's bearing means nothing once the route bends round an obstacle.
            finished = finish(seedPath, obstacles, config, false);
        }

        double endHeading = endPolicy == HeadingPolicy.LOCKED ? end.getHeadingRad() : finished.getHeading(1.0);

        return new PathResult(
                finished,
                Collections.singletonList(endHeading),
                Collections.<Point>emptyList(),
                Collections.<Point>emptyList());
    }

    /**
     * Applies the optional smoothing and obstacle-avoidance passes to a route.
     *
     * @param path            route to post-process
     * @param obstacles       zones to avoid; may be {@code null}
     * @param config          geometry and tuning parameters
     * @param keepEndTangents {@code true} to keep each segment's start and end tangent
     *                        while avoiding obstacles; see
     *                        {@link ObstacleAvoider#avoid(PathRoute, List, PathConfig, boolean)}
     * @return the processed route (possibly the same instance)
     */
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
     * Reworks every leg that turns around into waypoints the robot can actually follow.
     * <p>
     * A leg whose key point arrives along one direction and leaves along another cannot be
     * fitted straight through: the control handles collapse onto one line and the curve
     * hooks back on itself, so the tangent heading flips inside a fraction of an inch.
     * Every such leg swings out along an arc of a real radius, so the route still runs
     * through every key point and the start pose's own heading shapes the first leg.
     *
     * @param keyPoints       key point list to extend
     * @param headings        heading list to extend
     * @param lockedKeyPoints lock flags to extend
     * @param obstacles       zones to avoid; may be {@code null}
     * @param config          geometry and tuning parameters
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

            if (Math.abs(normalizeAngle(travel - headings.get(index))) < TURNAROUND_ANGLE) {
                index++;
                continue;
            }

            // Swing as wide as a quarter of the footprint allows, and never wider than the
            // leg itself has room for: circles that close together have no crossing tangent.
            double swing = Math.max(Math.max(config.getRobotWidth(), config.getRobotHeight()), 1.0) / 4.0;
            double radius = Math.min(swing, from.distanceTo(to) / 2.5);
            List<Point> arc = radius < MIN_TURN_RADIUS
                    ? null
                    : TurnaroundPath.build(from, headings.get(index), to, travel, radius);

            if (arc == null || arc.isEmpty()) {
                // Too short to swing out in: leave along the line of travel rather than
                // hooking.
                headings.set(index, travel);
                index++;
                continue;
            }

            // An arc the robot does not fit is no better than the hook it replaces, so keep
            // the leg as fitted and leave the swing to the obstacle pass.
            if (!fits(keyPoints.get(index), arc, obstacles, config)) {
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

    /**
     * Checks whether the robot fits along a run of waypoints.
     *
     * @param start     point the run leaves from
     * @param waypoints waypoints of the run
     * @param obstacles zones to avoid; when {@code null} there is nothing to fit between
     * @param config    geometry and tuning parameters
     * @return {@code true} when every waypoint is clear
     */
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

    /**
     * Wraps an angle onto {@code (-pi, pi]}.
     *
     * @param angle angle in radians
     * @return the equivalent angle in {@code (-pi, pi]}
     */
    private static double normalizeAngle(double angle) {
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

    /**
     * Splits an ordered target list into groups that can each be collected in a
     * single straight pass of the intake.
     * <p>
     * Starting at the current key point, consecutive targets are added while they
     * stay within {@code width} of the line running through the group toward its
     * centre and lie no further back along that line than the target before them, and
     * while the sweep keeps the robot clear of every obstacle. A target with an
     * explicit heading always ends the group.
     *
     * @param startPoint      point the first group starts from
     * @param startHeadingRad robot heading at {@code startPoint}, in radians
     * @param targets         ordered targets
     * @param obstacles       zones to avoid; may be {@code null}
     * @param config          geometry parameters; a non-positive intake width disables
     *                        grouping
     * @param reorder         {@code true} when the caller chose the order, so a whole
     *                        row may be swept together and passed in sweep order
     * @return each group, with the direction it is swept along
     */
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

        // Without width there is no benefit in merging; every target is its own group.
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

            // If the whole rest of the row can be swept in one pass, take it first: a
            // heading that fits a few targets can hide the line the whole row runs along.
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

                    // A locked heading on either target means they cannot be merged.
                    if (previousPose.hasLockedHeading() || candidatePose.hasLockedHeading()) {
                        break;
                    }

                    // The line the whole candidate group would run along, recomputed as
                    // the group grows so a cluster spread across the intake keeps a
                    // steady line.
                    double candidateHeading = fitHeading(
                            targets, groupStart, groupEnd, previousKeyPoint, previousHeading, width, width / 2.0);

                    if (Double.isNaN(candidateHeading)) {
                        break;
                    }

                    // Stop before the sweep's sideways shift would put the robot into an
                    // obstacle.
                    if (isSweepBlocked(
                            targets, groupStart, groupEnd, previousKeyPoint, candidateHeading, obstacles, config)) {
                        break;
                    }

                    groupHeading = candidateHeading;
                    groupEnd++;
                }

                // A single target keeps its locked heading, or the original direction:
                // toward the next target when it sits on the previous key point.
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

            // Report a merged group in the order the sweep passes it, which is the
            // order along the sweep direction (the route sorts it that way too).
            if (groupIndices.length > 1) {
                groupIndices = sortedAlong(targets, groupIndices, previousKeyPoint, groupHeading);
            }

            groups.add(new TargetGroup(groupIndices, groupHeading));

            // The next group is measured from where the robot will be: the target
            // itself, or the end of a merged sweep as addMergedGroup places it.
            previousKeyPoint = groupIndices.length > 1
                    ? lastOf(sweepKeyPoints(targets, groupIndices, previousKeyPoint, groupHeading))
                    : firstPose.position();
            previousHeading = groupHeading;
            groupStart = groupEnd;
        }

        return groups;
    }

    /**
     * Appends the sweep points for a group that is collected in one pass.
     * <p>
     * The group's targets are sorted along the travel direction and placed on a
     * line centered laterally within the group, so the intake passes over all of them.
     *
     * @param targets       ordered targets being visited
     * @param groupIndices  indices of the targets in this group
     * @param heading       direction the group is swept along, in radians
     * @param previousPoint key point the group starts from
     * @param keyPoints       key point list to append to
     * @param headings        heading list to append to
     * @param lockedKeyPoints lock flags to append to
     * @return the last key point added
     */
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

            // Everything after the group's first point has to earn its own key point:
            // targets packed this tight are swept up in one pass regardless.
            if (i > 0 && lastOf(keyPoints).distanceTo(point) < MIN_KEY_POINT_GAP) {
                continue;
            }

            appendKeyPoint(keyPoints, headings, lockedKeyPoints, point, heading, false);
        }

        return lastOf(sweep);
    }

    /**
     * Checks whether any key point of a merged sweep would put the robot inside or too
     * close to an obstacle.
     *
     * @param targets       ordered targets being visited
     * @param firstIndex    index of the sweep's first target
     * @param lastIndex     index of the sweep's last target
     * @param previousPoint key point the sweep starts from
     * @param heading       heading the robot sweeps along, in radians
     * @param obstacles     zones to avoid; may be {@code null}
     * @param config        geometry parameters
     * @return {@code true} if the sweep is blocked
     */
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

    /**
     * Places the key points of a merged sweep: the group's targets projected onto a
     * line through the previous key point, centered laterally within the group, in
     * the order they appear along the travel direction.
     *
     * @param targets       ordered targets being visited
     * @param groupIndices  indices of the targets in this group
     * @param previousPoint key point the group starts from
     * @param sweepHeading  direction the sweep runs in, in radians (see {@link #fitHeading})
     * @return the sweep key points, in visit order
     */
    private static List<Point> sweepKeyPoints(
            List<TargetSpec> targets, int[] groupIndices, Point previousPoint, double sweepHeading) {
        double directionX = Math.cos(sweepHeading);
        double directionY = Math.sin(sweepHeading);

        // Perpendicular to the travel direction.
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

        // Visit the group's targets in the order they appear along the travel direction.
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
     * Chooses the direction a candidate group runs in, or {@code NaN} when it cannot
     * be collected in a single pass.
     * <p>
     * The line must not point back away from the group, and must keep every target
     * within the intake width in a monotonic visit order. Aiming at the group's first
     * target (keeping a row ahead dead straight) or at its centre (keeping a spread
     * cluster on one line) handles most cases. For a row the robot reaches from the
     * side, the direction along the pair is tried, and the bounds perpendicular to a
     * pair keep a cluster spread across the intake running along one straight line
     * instead of being split into several passes. If none keeps the strict visit order,
     * a target may sit up to half an intake behind the one before it, which lets a
     * tight cluster that fits the intake sweep as one pass.
     *
     * @param targets         ordered targets being visited
     * @param firstIndex      index of the group's first target
     * @param lastIndex       index of the group's last target
     * @param previousPoint   key point the group starts from
     * @param fallbackHeading heading to use when every target coincides with
     *                        {@code previousPoint}, in radians
     * @param width           intake width
     * @param relaxedTolerance how far a target may sit behind the one before it when no
     *                        direction keeps the strict visit order
     * @return the sweep direction in radians, or {@code NaN} if the group does not fit
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

        // A pair whose visit order would reverse along the aim direction defines a
        // bound perpendicular to that pair; a direction on either side keeps the order.
        // The pair's own direction is tried too: a row of targets runs straight along
        // it, so an approach the row does not point at still sweeps it in one pass.
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

        // No direction keeps the strict visit order. A tight cluster whose whole spread
        // fits the intake is still one pass: let a target sit up to the allowed distance
        // behind the one before it, so near-coincident targets do not force a hook. When
        // the caller chose the order, the whole row may be swept together and passed in
        // whatever order the sweep runs in.
        for (double candidate : candidates) {
            if (pointsForward(towardCenterX, towardCenterY, candidate)
                    && fitsSweep(targets, firstIndex, lastIndex, previousPoint, candidate, width, relaxedTolerance)) {
                return candidate;
            }
        }

        return Double.NaN;
    }

    /**
     * Whether {@code heading} points from the previous key point into the group, that
     * is, not back away from the group's centre. A direction square to the centre line
     * counts, since a sweep may run along a row the robot reaches from the side. A
     * group whose centre sits on the previous key point gives no direction to test, so
     * it is treated as forward.
     *
     * @param towardCenterX x component from the previous key point to the group centre
     * @param towardCenterY y component from the previous key point to the group centre
     * @param heading       direction to test, in radians
     * @return {@code true} if the direction does not lead back away from the group
     */
    private static boolean pointsForward(double towardCenterX, double towardCenterY, double heading) {
        if (Math.hypot(towardCenterX, towardCenterY) < 1e-9) {
            return true;
        }

        return towardCenterX * Math.cos(heading) + towardCenterY * Math.sin(heading) > -1e-9;
    }

    /**
     * Returns the direction from the previous key point to the first target of a range
     * that is not on top of it.
     *
     * @param previousPoint   key point the group starts from
     * @param targets         ordered targets being visited
     * @param firstIndex      index of the group's first target
     * @param lastIndex       index of the group's last target
     * @param fallbackHeading heading to use when every target coincides with
     *                        {@code previousPoint}, in radians
     * @return the direction in radians
     */
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

    /**
     * Returns the direction a sweep of a single target runs in: from the previous key
     * point toward the target, or, when the target is on top of the previous key point,
     * toward the next target that is not.
     *
     * @param previousPoint   key point the sweep starts from
     * @param targets         ordered targets being visited
     * @param firstIndex      index of the sweep's first target
     * @param fallbackHeading heading to use when every remaining target is on top of
     *                        {@code previousPoint}, in radians
     * @return the sweep direction in radians
     */
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
     * Checks whether a candidate group can be collected in one pass: every target
     * within the intake width of the line through {@code previousPoint} along
     * {@code heading}, and none lying further back along that line than the target
     * before it by more than {@code alongTolerance}, so the merge keeps the visit
     * order.
     *
     * @param targets         ordered targets being visited
     * @param firstIndex      index of the group's first target
     * @param lastIndex       index of the group's last target
     * @param previousPoint   key point the group starts from
     * @param heading         direction the group runs in, in radians
     * @param width           intake width
     * @param alongTolerance  how far a target may sit behind the one before it and
     *                        still be swept along, in the same units as the targets
     * @return {@code true} if the whole group fits the intake in visit order
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

            // A sweep only moves forward: a target behind the one before it in the
            // visit order needs a pass of its own.
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
     * Appends a key point and its heading, unless it coincides with the previous key
     * point. Two targets side by side in one sweep, or a target on top of the start
     * or of the target before it, would otherwise leave a zero-length cubic whose
     * tangent, and so whose heading, is undefined.
     * <p>
     * The earlier key point keeps its heading: the robot may already need that
     * heading to capture the earlier target.
     *
     * @param keyPoints       key point list to append to
     * @param headings        heading list to append to
     * @param lockedKeyPoints lock flags to append to
     * @param point           key point to append
     * @param heading         heading at that key point, in radians
     * @param locked          whether the key point's heading is enforced
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

    /**
     * Returns the last element of a non-empty list.
     *
     * @param points the list
     * @return its last element
     */
    private static Point lastOf(List<Point> points) {
        return points.get(points.size() - 1);
    }

    /**
     * Whether any target in a range has a locked heading. A locked target cannot be
     * merged with its neighbours into one sweep.
     *
     * @param targets    ordered targets
     * @param firstIndex index of the first target to check
     * @param lastIndex  index of the last target to check
     * @return {@code true} if any target in the range has a locked heading
     */
    private static boolean hasLockedHeading(List<TargetSpec> targets, int firstIndex, int lastIndex) {
        for (int i = firstIndex; i <= lastIndex; i++) {
            if (targets.get(i).hasLockedHeading()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Orders targets so the intake can sweep them: targets within the intake width of
     * each other are kept together as one cluster, clusters are visited nearest-first,
     * and within a cluster the targets are listed along the line the pass runs in.
     *
     * @param start   point the robot starts from
     * @param targets targets to order
     * @param config  geometry parameters supplying the intake width
     * @return the targets in sweep-friendly visit order
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

    /**
     * Appends one cluster to the order, along its widest direction and starting from the
     * end nearer {@code cursor}.
     *
     * @param targets targets being ordered
     * @param cluster indices of the cluster's targets
     * @param cursor  point the robot reaches the cluster from
     * @param ordered order list to append to
     */
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

    /**
     * Returns the direction in which a cluster is widest, so a pass along it leaves the
     * smallest spread across the intake.
     *
     * @param targets targets being ordered
     * @param cluster indices of the cluster's targets
     * @return the direction in radians
     */
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

    /**
     * Returns the average position of the targets at the given indices.
     *
     * @param targets targets being ordered
     * @param indices indices of the targets to average
     * @return the average position
     */
    private static Point clusterCentroid(List<TargetSpec> targets, List<Integer> indices) {
        double sumX = 0;
        double sumY = 0;

        for (int index : indices) {
            sumX += targets.get(index).getX();
            sumY += targets.get(index).getY();
        }

        return new Point(sumX / indices.size(), sumY / indices.size());
    }

    /**
     * Finds the representative of a target's cluster.
     *
     * @param roots cluster parents
     * @param index target index
     * @return the cluster's representative index
     */
    private static int findRoot(int[] roots, int index) {
        while (roots[index] != index) {
            roots[index] = roots[roots[index]];
            index = roots[index];
        }

        return index;
    }

    /**
     * Joins the clusters of two targets.
     *
     * @param roots cluster parents
     * @param a     one target index
     * @param b     another target index
     */
    private static void unionRoots(int[] roots, int a, int b) {
        int rootA = findRoot(roots, a);
        int rootB = findRoot(roots, b);

        if (rootA != rootB) {
            roots[rootA] = rootB;
        }
    }

    /**
     * Returns the given target indices ordered by how far along {@code heading} each
     * target lies from {@code previousPoint}, the order a sweep passes them in.
     *
     * @param targets       ordered targets
     * @param indices       indices of the targets to order
     * @param previousPoint key point the sweep starts from
     * @param heading       direction the sweep runs in, in radians
     * @return the indices in sweep order
     */
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

    /**
     * Returns the average of the target positions at the given indices.
     *
     * @param targets ordered targets
     * @param indices indices of the targets to average
     * @return the average position
     */
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
     * Finds the robot-center point that lines the intake mouth up with a target.
     * <p>
     * The robot center is offset sideways from the target along the axis
     * perpendicular to its heading. The offset is chosen so the line through the
     * previous and next raw targets also crosses that offset line, and is clamped to
     * the intake half width.
     *
     * @param target      target the intake should cover
     * @param heading     robot heading at the target
     * @param halfWidth   maximum sideways offset the intake allows
     * @param previousRaw previous raw target point
     * @param nextRaw     next raw target point
     * @return the computed robot center
     */
    private static Point solveIntakeCapturePoint(
            Point target, double heading, double halfWidth, Point previousRaw, Point nextRaw) {
        double cosHeading = Math.cos(heading);
        double sinHeading = Math.sin(heading);

        // Lateral axis of the intake, perpendicular to the heading.
        double lateralAxisX = sinHeading;
        double lateralAxisY = -cosHeading;

        // Direction of travel through the surrounding targets.
        double travelX = nextRaw.getX() - previousRaw.getX();
        double travelY = nextRaw.getY() - previousRaw.getY();

        // Intersection of the lateral axis through the target with the travel line.
        double denominator = lateralAxisX * travelY - lateralAxisY * travelX;
        double lateralOffset = 0;

        if (Math.abs(denominator) > 1e-9) {
            lateralOffset =
                    ((previousRaw.getX() - target.getX()) * travelY - (previousRaw.getY() - target.getY()) * travelX)
                            / denominator;
        }

        // Never step further sideways than the intake can reach.
        lateralOffset = Math.max(-halfWidth, Math.min(halfWidth, lateralOffset));

        return new Point(target.getX() + lateralOffset * lateralAxisX, target.getY() + lateralOffset * lateralAxisY);
    }

    /**
     * Drops targets that an obstacle prevents the robot from reaching.
     *
     * @param startPoint point the robot moves from
     * @param targets    ordered targets to filter
     * @param obstacles  zones to avoid; may be {@code null}
     * @param config     geometry and tuning parameters
     * @return a new list containing only reachable targets
     */
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

    /**
     * Checks whether stopping at {@code target} would put the robot inside or too
     * close to an obstacle.
     *
     * @param target    target specification to test
     * @param from      point the robot arrives from
     * @param obstacles zones to avoid
     * @param config    geometry and tuning parameters
     * @return {@code true} if the target is blocked
     */
    private static boolean isTargetBlocked(TargetSpec target, Point from, List<Zone> obstacles, PathConfig config) {
        double heading = target.hasLockedHeading() ? target.getHeadingRad() : bearing(from, target.position());

        return isTargetBlocked(target.position(), heading, obstacles, config);
    }

    /**
     * Checks whether a robot centered at {@code target} with {@code heading} would be
     * inside or too close to an obstacle.
     *
     * @param target    robot-center point to test
     * @param heading   robot heading at the point
     * @param obstacles zones to avoid
     * @param config    geometry and tuning parameters
     * @return {@code true} if the point is blocked
     */
    private static boolean isTargetBlocked(Point target, double heading, List<Zone> obstacles, PathConfig config) {
        // Point-clearance test when the robot has no meaningful footprint.
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

    /**
     * Returns the direction from {@code from} to the first of {@code points} that is
     * not on top of it.
     *
     * @param from   point travelled from
     * @param points candidate points, in order
     * @return the bearing in radians, or {@code 0} when every point coincides with
     *         {@code from}
     */
    private static double firstBearing(Point from, List<Point> points) {
        for (Point point : points) {
            if (from.distanceTo(point) > 1e-6) {
                return bearing(from, point);
            }
        }

        return 0.0;
    }

    /**
     * Returns the direction from {@code from} to the first of {@code targets} that is
     * not on top of it.
     *
     * @param from    point travelled from
     * @param targets candidate targets, in order
     * @return the bearing in radians, or {@code 0} when every target coincides with
     *         {@code from}
     */
    private static double firstTargetBearing(Point from, List<TargetSpec> targets) {
        for (TargetSpec target : targets) {
            if (from.distanceTo(target.position()) > 1e-6) {
                return bearing(from, target.position());
            }
        }

        return 0.0;
    }

    /**
     * Builds the route, splitting it at every locked target so smoothing and obstacle
     * avoidance preserve that target's heading.
     *
     * @param keyPoints       key points to connect, in order
     * @param headings        heading in radians for each key point
     * @param lockedKeyPoints whether each key point's heading is enforced
     * @return the route
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

    /**
     * Returns the direction from one point to another.
     *
     * @param from point travelled from
     * @param to   point travelled to
     * @return the bearing in radians, or {@code 0} when the points coincide
     */
    private static double bearing(Point from, Point to) {
        return Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
    }

    /**
     * Builds a single cubic Bezier from start to end, with control handles pointing
     * along the supplied headings and one third of the distance long.
     *
     * @param start        start point
     * @param end          end point
     * @param startHeading start heading in radians
     * @param endHeading   end heading in radians
     * @return the single cubic curve
     */
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
     * Smooths a sequence of key points into a chain of cubic Bezier segments.
     * <p>
     * Each segment's control handles are aligned with the heading stored for its
     * key point, giving continuous tangents across the whole chain.
     *
     * @param points   key points to connect, in order
     * @param headings heading in radians for each key point
     * @return the fitted cubic chain
     */
    private static PathCurve cubicHermiteChain(List<Point> points, List<Double> headings) {
        int pointCount = points.size();

        // A two-point chain is the simple single-cubic case.
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
