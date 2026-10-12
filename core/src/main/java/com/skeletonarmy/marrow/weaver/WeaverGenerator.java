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
        if (!targets.isEmpty()) {
            return generateIntakeResult(startPose, targets, obstacles, reorder, maxTargets, config);
        }

        if (destinationPose != null) {
            return generateAvoidanceResult(
                    new Point(startPose.getX(), startPose.getY()),
                    new Point(destinationPose.getX(), destinationPose.getY()),
                    obstacles,
                    config);
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
            List<Point> targets,
            List<Zone> obstacles,
            boolean reorder,
            int maxTargets,
            PathConfig config) {
        Point startPoint = new Point(start.getX(), start.getY());

        List<Point> orderedTargets;

        if (maxTargets > 0) {
            orderedTargets = selectTargets(start, startPoint, targets, obstacles, reorder, maxTargets, config);
        } else {
            // No limit: keep the original ordering-then-filter pipeline unchanged.
            orderedTargets = reorder
                    ? TargetOrderer.order(startPoint, start.getHeadingRad(), targets, config)
                    : new ArrayList<>(targets);

            if (config.isExcludeBlockedTargets()) {
                orderedTargets = excludeBlockedTargets(startPoint, orderedTargets, obstacles, config);
            }
        }

        // With an intake to sweep with, order so targets that can be swept together stay
        // together. A point-to-point order would otherwise thread a distant target
        // through a clump and break it into several passes.
        if (reorder && config.getIntakeWidth() > 1e-9 && !hasHeading(orderedTargets, 0, orderedTargets.size() - 1)) {
            orderedTargets = orderForIntake(startPoint, orderedTargets, config);
        }

        // Remove one entry per visit, so a target passed twice and visited once
        // still reports its second entry as unvisited.
        List<Point> unvisitedTargets = new ArrayList<>(targets);

        for (Point visited : orderedTargets) {
            unvisitedTargets.remove(visited);
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

        keyPoints.add(startPoint);
        // A start with no heading faces its first target until the key points are known.
        headings.add(start.hasHeading() ? start.getHeadingRad() : firstBearing(startPoint, orderedTargets));

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
                visitedTargets.add(orderedTargets.get(index));
            }

            // A merged group (several targets in one intake pass) is handled separately.
            if (groupIndices.length > 1 && intakeHalfWidth > 1e-9) {
                previousRawTarget = addMergedGroup(
                        orderedTargets, groupIndices, group.heading, previousRawTarget, keyPoints, headings);
                continue;
            }

            Point firstPose = orderedTargets.get(groupIndices[0]);
            Point target = new Point(firstPose.getX(), firstPose.getY());

            double heading = firstPose.hasHeading()
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
                        && isTargetBlocked(
                                new Point(robotCenter.getX(), robotCenter.getY(), heading),
                                lastOf(keyPoints),
                                obstacles,
                                config)) {
                    robotCenter = target;
                }
            }

            appendKeyPoint(keyPoints, headings, robotCenter, heading);
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
        }

        // A last target with its own heading keeps it, even when it shares a key point
        // with the target before it.
        Point lastTarget = orderedTargets.get(orderedTargets.size() - 1);
        double endHeading = lastTarget.hasHeading() ? lastTarget.getHeadingRad() : headings.get(headings.size() - 1);

        PathCurve curve = cubicHermiteChain(keyPoints, headings);
        PathRoute path = finish(new PathRoute(Collections.singletonList(curve)), obstacles, config, true);

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
    private static List<Point> selectTargets(
            Point start,
            Point startPoint,
            List<Point> targets,
            List<Zone> obstacles,
            boolean reorder,
            int maxTargets,
            PathConfig config) {
        boolean dropBlocked = config.isExcludeBlockedTargets();

        if (!reorder) {
            List<Point> ordered = dropBlocked
                    ? excludeBlockedTargets(startPoint, new ArrayList<>(targets), obstacles, config)
                    : new ArrayList<>(targets);

            return ordered.size() > maxTargets ? new ArrayList<>(ordered.subList(0, maxTargets)) : ordered;
        }

        TargetOrderer.ReachabilityCheck reachability = dropBlocked
                ? (from, target) -> !isTargetBlocked(target, from, obstacles, config)
                : (from, target) -> true;

        return TargetOrderer.select(startPoint, start.getHeadingRad(), targets, maxTargets, config, reachability);
    }

    /**
     * Seeds a straight line of control points between start and end, then lets
     * {@link ObstacleAvoider} bend and smooth it.
     *
     * @param start     start position
     * @param end       destination position
     * @param obstacles zones to avoid; may be {@code null}
     * @param config    geometry and tuning parameters
     * @return the generated route plus metadata
     */
    private static PathResult generateAvoidanceResult(Point start, Point end, List<Zone> obstacles, PathConfig config) {
        // Number of control points used to seed the curve before avoidance.
        int seedControlPointCount = 6;

        List<Point> seedPoints = new ArrayList<>();

        for (int i = 0; i < seedControlPointCount; i++) {
            double progress = (double) i / (seedControlPointCount - 1);
            seedPoints.add(new Point(
                    start.getX() + (end.getX() - start.getX()) * progress,
                    start.getY() + (end.getY() - start.getY()) * progress));
        }

        PathCurve curve = new PathCurve(seedPoints);
        PathRoute seedPath = new PathRoute(Collections.singletonList(curve));
        // The seed's bearing means nothing once the route bends round an obstacle.
        PathRoute finished = finish(seedPath, obstacles, config, false);

        return new PathResult(
                finished,
                Collections.singletonList(finished.getHeading(1.0)),
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
            List<Point> targets,
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
            Point firstPose = targets.get(groupStart);
            int groupEnd = groupStart + 1;
            double groupHeading = Double.NaN;

            // If the whole rest of the row can be swept in one pass, take it first: a
            // heading that fits a few targets can hide the line the whole row runs along.
            double wholeHeading = Double.NaN;

            if (!hasHeading(targets, groupStart, targetCount - 1)) {
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
                    Point previousPose = targets.get(groupEnd - 1);
                    Point candidatePose = targets.get(groupEnd);

                    // An explicit heading on either target means they cannot be merged.
                    if (previousPose.hasHeading() || candidatePose.hasHeading()) {
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

                // A single target keeps the original direction: toward the next target
                // when it sits on the previous key point.
                if (groupEnd == groupStart + 1) {
                    groupHeading = sweepDirection(previousKeyPoint, targets, groupStart, previousHeading);
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
                    : new Point(firstPose.getX(), firstPose.getY());
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
     * @param keyPoints     key point list to append to
     * @param headings      heading list to append to
     * @return the last key point added
     */
    private static Point addMergedGroup(
            List<Point> targets,
            int[] groupIndices,
            double heading,
            Point previousPoint,
            List<Point> keyPoints,
            List<Double> headings) {
        List<Point> sweep = sweepKeyPoints(targets, groupIndices, previousPoint, heading);

        for (Point point : sweep) {
            appendKeyPoint(keyPoints, headings, point, heading);
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
            List<Point> targets,
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
            if (isTargetBlocked(new Point(point.getX(), point.getY(), heading), previousPoint, obstacles, config)) {
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
            List<Point> targets, int[] groupIndices, Point previousPoint, double sweepHeading) {
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
            Point pose = targets.get(groupIndices[k]);

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
            List<Point> targets,
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
            Point from = targets.get(i);
            Point to = targets.get(i + 1);
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
            Point previousPoint, List<Point> targets, int firstIndex, int lastIndex, double fallbackHeading) {
        for (int i = firstIndex; i <= lastIndex; i++) {
            Point target = targets.get(i);

            if (Math.hypot(target.getX() - previousPoint.getX(), target.getY() - previousPoint.getY()) > 1e-9) {
                return bearing(previousPoint, target);
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
            Point previousPoint, List<Point> targets, int firstIndex, double fallbackHeading) {
        for (int i = firstIndex; i < targets.size(); i++) {
            Point target = targets.get(i);

            if (Math.hypot(target.getX() - previousPoint.getX(), target.getY() - previousPoint.getY()) > 1e-9) {
                return bearing(previousPoint, target);
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
            List<Point> targets,
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
            Point target = targets.get(i);

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
     * @param keyPoints key point list to append to
     * @param headings  heading list to append to
     * @param point     key point to append
     * @param heading   heading at that key point, in radians
     */
    private static void appendKeyPoint(List<Point> keyPoints, List<Double> headings, Point point, double heading) {
        if (lastOf(keyPoints).distanceTo(point) < 1e-6) {
            return;
        }

        keyPoints.add(point);
        headings.add(heading);
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
     * Whether any target in a range carries an explicit heading. A headed target cannot
     * be merged with its neighbours into one sweep.
     *
     * @param targets    ordered targets
     * @param firstIndex index of the first target to check
     * @param lastIndex  index of the last target to check
     * @return {@code true} if any target in the range has a heading
     */
    private static boolean hasHeading(List<Point> targets, int firstIndex, int lastIndex) {
        for (int i = firstIndex; i <= lastIndex; i++) {
            if (targets.get(i).hasHeading()) {
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
    private static List<Point> orderForIntake(Point start, List<Point> targets, PathConfig config) {
        double width = config.getIntakeWidth();
        int count = targets.size();
        int[] roots = new int[count];

        for (int i = 0; i < count; i++) {
            roots[i] = i;
        }

        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                if (targets.get(i).distanceTo(targets.get(j)) <= width) {
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
        List<Point> ordered = new ArrayList<>();
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
            cursor = lastOf(ordered);
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
    private static void appendCluster(List<Point> targets, List<Integer> cluster, Point cursor, List<Point> ordered) {
        if (cluster.size() == 1) {
            ordered.add(targets.get(cluster.get(0)));
            return;
        }

        double heading = widestDirection(targets, cluster);
        double directionX = Math.cos(heading);
        double directionY = Math.sin(heading);

        List<Integer> sorted = new ArrayList<>(cluster);
        sorted.sort(Comparator.comparingDouble(index -> {
            Point target = targets.get(index);
            return target.getX() * directionX + target.getY() * directionY;
        }));

        Point first = targets.get(sorted.get(0));
        Point last = targets.get(sorted.get(sorted.size() - 1));

        if (cursor.distanceTo(first) > cursor.distanceTo(last)) {
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
    private static double widestDirection(List<Point> targets, List<Integer> cluster) {
        double bestHeading = 0.0;
        double smallestSpread = Double.MAX_VALUE;

        for (int step = 0; step < 180; step++) {
            double heading = Math.toRadians(step);
            double directionX = Math.cos(heading);
            double directionY = Math.sin(heading);
            double minLateral = Double.MAX_VALUE;
            double maxLateral = -Double.MAX_VALUE;

            for (int index : cluster) {
                Point target = targets.get(index);
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
    private static Point clusterCentroid(List<Point> targets, List<Integer> indices) {
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
    private static int[] sortedAlong(List<Point> targets, int[] indices, Point previousPoint, double heading) {
        double directionX = Math.cos(heading);
        double directionY = Math.sin(heading);
        Integer[] order = new Integer[indices.length];
        double[] along = new double[indices.length];

        for (int k = 0; k < indices.length; k++) {
            Point target = targets.get(indices[k]);
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
    private static Point groupRepresentative(List<Point> targets, int[] indices) {
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
    private static List<Point> excludeBlockedTargets(
            Point startPoint, List<Point> targets, List<Zone> obstacles, PathConfig config) {
        if (obstacles == null || obstacles.isEmpty()) {
            return targets;
        }

        List<Point> reachableTargets = new ArrayList<>();
        Point previousPoint = startPoint;

        for (Point target : targets) {
            if (!isTargetBlocked(target, previousPoint, obstacles, config)) {
                reachableTargets.add(target);
                previousPoint = new Point(target.getX(), target.getY());
            }
        }

        return reachableTargets;
    }

    /**
     * Checks whether stopping at {@code target} would put the robot inside or too
     * close to an obstacle.
     *
     * @param target    target pose to test
     * @param from      point the robot arrives from
     * @param obstacles zones to avoid
     * @param config    geometry and tuning parameters
     * @return {@code true} if the target is blocked
     */
    private static boolean isTargetBlocked(Point target, Point from, List<Zone> obstacles, PathConfig config) {
        Point targetPoint = new Point(target.getX(), target.getY());

        double heading = target.hasHeading()
                ? target.getHeadingRad()
                : Math.atan2(targetPoint.getY() - from.getY(), targetPoint.getX() - from.getX());

        // Point-clearance test when the robot has no meaningful footprint.
        if (config.getRobotWidth() <= 0 || config.getRobotHeight() <= 0) {
            for (Zone zone : obstacles) {
                if (zone.contains(targetPoint) || zone.distanceToBoundary(targetPoint) < config.getClearance()) {
                    return true;
                }
            }

            return false;
        }

        Zone footprint = new PolygonZone(targetPoint, config.getRobotWidth(), config.getRobotHeight(), heading);

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
