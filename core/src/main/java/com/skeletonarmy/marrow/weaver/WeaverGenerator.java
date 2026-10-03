package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
     * Generates a path from the given inputs.
     *
     * @param startPose        where the robot starts (pose and heading)
     * @param destinationPose  goal pose in avoidance mode; may be {@code null} when targets exist
     * @param targets          intake targets to visit; may be empty
     * @param obstacles        zones the robot must not cut through; may be {@code null}
     * @param reorder          {@code true} to let {@link TargetOrderer} pick the visit order
     * @param config           geometry and tuning parameters
     * @return the generated route plus metadata
     * @throws IllegalStateException if neither targets nor a destination are provided
     */
    public static PathResult generate(PathPose startPose, PathPose destinationPose,
                                      List<PathPose> targets, List<Zone> obstacles,
                                      boolean reorder, PathConfig config) {
        if (!targets.isEmpty()) {
            return generateIntakeResult(startPose, targets, obstacles, reorder, config);
        }

        if (destinationPose != null) {
            return generateAvoidanceResult(
                    new Point(startPose.getX(), startPose.getY()),
                    new Point(destinationPose.getX(), destinationPose.getY()),
                    obstacles,
                    config
            );
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
     * clearance from every obstacle.
     *
     * @param curve       curve to sample
     * @param obstacles   zones to avoid
     * @param clearance   extra distance to keep from obstacles
     * @param robotWidth  footprint width along the forward axis
     * @param robotHeight footprint height across the forward axis
     * @param samples     number of points to test along the curve
     * @return {@code true} if the whole curve is clear
     */
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, double clearance,
                                      double robotWidth, double robotHeight, int samples) {
        for (int i = 0; i < samples; i++) {
            double progress = samples == 1 ? 0 : (double) i / (samples - 1);
            Point samplePoint = curve.get(progress);

            if (robotWidth <= 0 || robotHeight <= 0) {
                for (Zone zone : obstacles) {
                    if (zone.contains(samplePoint) || zone.distanceToBoundary(samplePoint) < clearance) {
                        return false;
                    }
                }
            } else {
                Zone footprint = new PolygonZone(samplePoint, robotWidth, robotHeight, curve.getHeading(progress));

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
        return isPathClear(path, obstacles, config.getClearance(), config.getRobotWidth(),
                config.getRobotHeight(), samplesPerSegment);
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
        return isPathClear(curve, obstacles, config.getClearance(), config.getRobotWidth(),
                config.getRobotHeight(), samples);
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
    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, double clearance, double robotWidth, double robotHeight, int samplesPerSegment) {
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
     * @param start     robot start pose
     * @param targets   targets to visit
     * @param obstacles zones to avoid; may be {@code null}
     * @param reorder   {@code true} to let {@link TargetOrderer} pick the visit order
     * @param config    geometry and tuning parameters
     * @return the generated route plus metadata
     */
    private static PathResult generateIntakeResult(PathPose start, List<PathPose> targets,
                                                   List<Zone> obstacles, boolean reorder,
                                                   PathConfig config) {
        Point startPoint = new Point(start.getX(), start.getY());

        List<PathPose> orderedTargets = reorder
                ? TargetOrderer.order(startPoint, start.getHeadingRad(), targets, config)
                : new ArrayList<>(targets);

        if (config.isExcludeBlockedTargets()) {
            orderedTargets = excludeBlockedTargets(startPoint, orderedTargets, obstacles, config);
        }

        int skippedTargets = targets.size() - orderedTargets.size();

        // No reachable target left: produce a stationary path at the start pose.
        if (orderedTargets.isEmpty()) {
            PathRoute idlePath = finish(new PathRoute(Collections.singletonList(
                    new PathCurve(Arrays.asList(startPoint, startPoint)))), obstacles, config);

            return new PathResult(idlePath, Collections.singletonList(start.getHeadingRad()), skippedTargets);
        }

        List<Point> keyPoints = new ArrayList<>();
        List<Double> headings = new ArrayList<>();

        keyPoints.add(startPoint);
        headings.add(start.getHeadingRad());

        // The robot can only sweep multiple targets at once if the intake has width.
        double intakeHalfWidth = config.getIntakeWidth() / 2.0;
        List<int[]> targetGroups = groupTargets(startPoint, orderedTargets, config.getIntakeWidth());

        Point previousRawTarget = startPoint;

        for (int groupIndex = 0; groupIndex < targetGroups.size(); groupIndex++) {
            int[] groupIndices = targetGroups.get(groupIndex);

            // A merged group (several targets in one intake pass) is handled separately.
            if (groupIndices.length > 1 && intakeHalfWidth > 1e-9) {
                previousRawTarget = addMergedGroup(orderedTargets, groupIndices, previousRawTarget, keyPoints, headings);
                continue;
            }

            PathPose firstPose = orderedTargets.get(groupIndices[0]);
            Point target = new Point(firstPose.getX(), firstPose.getY());

            double heading = !Double.isNaN(firstPose.getHeadingRad())
                    ? firstPose.getHeadingRad()
                    : Math.atan2(target.getY() - previousRawTarget.getY(),
                                 target.getX() - previousRawTarget.getX());

            Point robotCenter;

            // The last target is reached dead-on; earlier ones may be offset so the
            // intake mouth lines up with the target while the body stays on the path.
            if (intakeHalfWidth <= 1e-9 || groupIndex == targetGroups.size() - 1) {
                robotCenter = target;
            } else {
                Point nextGroupRepresentative = groupRepresentative(orderedTargets, targetGroups.get(groupIndex + 1));
                robotCenter = solveIntakeCapturePoint(target, heading, intakeHalfWidth,
                        previousRawTarget, nextGroupRepresentative);
            }

            keyPoints.add(robotCenter);
            headings.add(heading);
            previousRawTarget = target;
        }

        PathCurve curve = cubicHermiteChain(keyPoints, headings);
        PathRoute path = finish(new PathRoute(Collections.singletonList(curve)), obstacles, config);

        return new PathResult(path, Collections.singletonList(headings.get(headings.size() - 1)), skippedTargets);
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
    private static PathResult generateAvoidanceResult(Point start, Point end,
                                                      List<Zone> obstacles, PathConfig config) {
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
        PathRoute finished = finish(seedPath, obstacles, config);

        return new PathResult(finished, Collections.singletonList(finished.getHeading(1.0)), 0);
    }

    /**
     * Applies the optional smoothing and obstacle-avoidance passes to a route.
     *
     * @param path      route to post-process
     * @param obstacles zones to avoid; may be {@code null}
     * @param config    geometry and tuning parameters
     * @return the processed route (possibly the same instance)
     */
    private static PathRoute finish(PathRoute path, List<Zone> obstacles, PathConfig config) {
        if (config.isSmoothing()) {
            path = ObstacleAvoider.smooth(path);
        }

        if (obstacles != null && !obstacles.isEmpty()) {
            path = ObstacleAvoider.avoid(path, obstacles, config);
        }

        return path;
    }

    /**
     * Splits an ordered target list into groups that can each be collected in a
     * single straight pass of the intake.
     * <p>
     * Starting at the current key point, consecutive targets are added while they
     * stay within {@code width} of the line running through the first target. A target
     * with an explicit heading always ends the group.
     *
     * @param startPoint point the first group starts from
     * @param targets    ordered targets
     * @param width      usable intake width; non-positive disables grouping
     * @return each group as an array of indices into {@code targets}
     */
    private static List<int[]> groupTargets(Point startPoint, List<PathPose> targets, double width) {
        List<int[]> groups = new ArrayList<>();
        int targetCount = targets.size();

        // Without width there is no benefit in merging; every target is its own group.
        if (width <= 1e-9) {
            for (int i = 0; i < targetCount; i++) {
                groups.add(new int[]{i});
            }

            return groups;
        }

        Point previousKeyPoint = startPoint;
        int groupStart = 0;

        while (groupStart < targetCount) {
            PathPose firstPose = targets.get(groupStart);

            // Direction from the previous key point to the first target of this group.
            double deltaX = firstPose.getX() - previousKeyPoint.getX();
            double deltaY = firstPose.getY() - previousKeyPoint.getY();
            double distance = Math.hypot(deltaX, deltaY);

            double directionX = distance > 1e-9 ? deltaX / distance : 1.0;
            double directionY = distance > 1e-9 ? deltaY / distance : 0.0;

            double minLateralOffset = 0;
            double maxLateralOffset = 0;

            int groupEnd = groupStart + 1;

            while (groupEnd < targetCount) {
                PathPose previousPose = targets.get(groupEnd - 1);
                PathPose candidatePose = targets.get(groupEnd);

                // An explicit heading on either target means they cannot be merged.
                if (!Double.isNaN(previousPose.getHeadingRad()) || !Double.isNaN(candidatePose.getHeadingRad())) {
                    break;
                }

                double offsetX = candidatePose.getX() - firstPose.getX();
                double offsetY = candidatePose.getY() - firstPose.getY();

                // Perpendicular distance from the group's base line.
                double lateralOffset = offsetX * -directionY + offsetY * directionX;

                double candidateMin = Math.min(minLateralOffset, lateralOffset);
                double candidateMax = Math.max(maxLateralOffset, lateralOffset);

                // Stop before the row would no longer fit the intake.
                if (candidateMax - candidateMin > width) {
                    break;
                }

                minLateralOffset = candidateMin;
                maxLateralOffset = candidateMax;
                groupEnd++;
            }

            int[] groupIndices = new int[groupEnd - groupStart];

            for (int k = 0; k < groupIndices.length; k++) {
                groupIndices[k] = groupStart + k;
            }

            groups.add(groupIndices);
            previousKeyPoint = groupRepresentative(targets, groupIndices);
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
     * @param previousPoint key point the group starts from
     * @param keyPoints     key point list to append to
     * @param headings      heading list to append to
     * @return the last key point added
     */
    private static Point addMergedGroup(List<PathPose> targets, int[] groupIndices, Point previousPoint, List<Point> keyPoints, List<Double> headings) {
        PathPose firstPose = targets.get(groupIndices[0]);

        double deltaX = firstPose.getX() - previousPoint.getX();
        double deltaY = firstPose.getY() - previousPoint.getY();
        double distance = Math.hypot(deltaX, deltaY);

        double directionX = distance > 1e-9 ? deltaX / distance : 1.0;
        double directionY = distance > 1e-9 ? deltaY / distance : 0.0;

        // Perpendicular to the travel direction.
        double perpendicularX = -directionY;
        double perpendicularY = directionX;

        double heading = distance > 1e-9
                ? Math.atan2(deltaY, deltaX)
                : headings.get(headings.size() - 1);

        double[] longitudinalOffsets = new double[groupIndices.length];
        Integer[] visitOrder = new Integer[groupIndices.length];

        double minLateralOffset = Double.MAX_VALUE;
        double maxLateralOffset = -Double.MAX_VALUE;

        for (int k = 0; k < groupIndices.length; k++) {
            PathPose pose = targets.get(groupIndices[k]);

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
        Point lastPoint = previousPoint;

        for (int k : visitOrder) {
            lastPoint = new Point(
                    previousPoint.getX() + longitudinalOffsets[k] * directionX + centerLateralOffset * perpendicularX,
                    previousPoint.getY() + longitudinalOffsets[k] * directionY + centerLateralOffset * perpendicularY);

            keyPoints.add(lastPoint);
            headings.add(heading);
        }

        return lastPoint;
    }

    /**
     * Returns the average of the target positions at the given indices.
     *
     * @param targets ordered targets
     * @param indices indices of the targets to average
     * @return the average position
     */
    private static Point groupRepresentative(List<PathPose> targets, int[] indices) {
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
    private static Point solveIntakeCapturePoint(Point target, double heading, double halfWidth, Point previousRaw, Point nextRaw) {
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
            lateralOffset = ((previousRaw.getX() - target.getX()) * travelY
                    - (previousRaw.getY() - target.getY()) * travelX) / denominator;
        }

        // Never step further sideways than the intake can reach.
        lateralOffset = Math.max(-halfWidth, Math.min(halfWidth, lateralOffset));

        return new Point(target.getX() + lateralOffset * lateralAxisX,
                target.getY() + lateralOffset * lateralAxisY);
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
    private static List<PathPose> excludeBlockedTargets(Point startPoint, List<PathPose> targets,
                                                        List<Zone> obstacles, PathConfig config) {
        if (obstacles == null || obstacles.isEmpty()) {
            return targets;
        }

        List<PathPose> reachableTargets = new ArrayList<>();
        Point previousPoint = startPoint;

        for (PathPose target : targets) {
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
    private static boolean isTargetBlocked(PathPose target, Point from, List<Zone> obstacles, PathConfig config) {
        Point targetPoint = new Point(target.getX(), target.getY());

        double heading = !Double.isNaN(target.getHeadingRad())
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
                new Point(start.getX() + handleLength * Math.cos(startHeading),
                          start.getY() + handleLength * Math.sin(startHeading)),
                new Point(end.getX() - handleLength * Math.cos(endHeading),
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
