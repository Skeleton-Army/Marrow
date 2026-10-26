package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.CompositeZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Bends and smooths generated paths so they clear obstacles.
 *
 * <p>Each segment is bypassed through candidate waypoints sampled around obstacle
 * boundaries, using a visibility-graph shortest path. The result is re-fitted as a
 * smooth cubic chain. If the first attempt is too tight, the clearance is inflated
 * and the attempt repeated; if nothing fits, the original curve is kept.
 */
public class ObstacleAvoider {
    /** Number of points sampled around a circular obstacle. */
    private static final int CIRCLE_SAMPLES = 24;

    /** Number of points sampled around each polygon corner. */
    private static final int CORNER_SAMPLES = 8;

    /** Number of straight-line checks used when testing a segment. */
    private static final int SEGMENT_CHECKS = 12;

    /** Number of samples used when validating a fitted curve. */
    private static final int SMOOTH_SAMPLES = 100;

    /** How many times to inflate the clearance before giving up on a segment. */
    private static final int MAX_INFLATE_ATTEMPTS = 10;

    /** Step used for the finite-difference gradient of a zone's signed distance. */
    private static final double GRADIENT_EPS = 1e-3;

    /** Extra seed padding added to sampled boundary points. */
    private static final double SEED_PAD = 0.75;

    /** Shortest chord a tangent estimate will stand on, in route units. */
    private static final double MIN_TANGENT_BASELINE = 1.0;

    private ObstacleAvoider() {}

    /**
     * Clearance that an endpoint cannot meet itself, forgiven on segments touching it.
     * An obstacle containing the endpoint is ignored.
     */
    private static final class EndpointRelief {
        private final Point endpoint;
        private final double[] marginCaps;

        private EndpointRelief(Point endpoint, double[] marginCaps) {
            this.endpoint = endpoint;
            this.marginCaps = marginCaps;
        }

        static EndpointRelief at(Point endpoint, List<Zone> obstacles) {
            double[] marginCaps = new double[obstacles.size()];

            for (int i = 0; i < obstacles.size(); i++) {
                Zone zone = obstacles.get(i);
                marginCaps[i] = zone.contains(endpoint) ? Double.NEGATIVE_INFINITY : signedDist(zone, endpoint);
            }

            return new EndpointRelief(endpoint, marginCaps);
        }

        double marginCap(int obstacleIndex) {
            return marginCaps[obstacleIndex];
        }

        boolean isAt(Point point) {
            return endpoint.distanceTo(point) < 1e-9;
        }
    }

    private static EndpointRelief[] edgeReliefs(int from, int to, int startIdx, int endIdx, EndpointRelief[] reliefs) {
        List<EndpointRelief> touching = new ArrayList<>();

        if (from == startIdx || to == startIdx) {
            touching.add(reliefs[0]);
        }

        if (from == endIdx || to == endIdx) {
            touching.add(reliefs[1]);
        }

        return touching.toArray(new EndpointRelief[0]);
    }

    /** Routes every segment around the obstacles, keeping each segment's end tangents. */
    public static PathRoute avoid(PathRoute path, List<Zone> obstacles, PathConfig config) {
        return avoid(path, obstacles, config, true);
    }

    /**
     * Routes every segment around the obstacles. With {@code keepEndTangents} false,
     * a rerouted segment leaves along its first leg and arrives along its last.
     */
    public static PathRoute avoid(PathRoute path, List<Zone> obstacles, PathConfig config, boolean keepEndTangents) {
        if (obstacles == null || obstacles.isEmpty()) {
            return path;
        }

        List<Zone> parts = flatten(obstacles);
        List<PathCurve> routedSegments = new ArrayList<>();

        for (PathCurve segment : path.getSegments()) {
            routedSegments.add(routeCurve(segment, parts, config, keepEndTangents));
        }

        return new PathRoute(routedSegments);
    }

    /** Expands composite zones recursively; relief is measured per individual part. */
    private static List<Zone> flatten(List<Zone> obstacles) {
        List<Zone> parts = new ArrayList<>();

        for (Zone zone : obstacles) {
            if (zone instanceof CompositeZone) {
                parts.addAll(flatten(((CompositeZone) zone).getZones()));
            } else {
                parts.add(zone);
            }
        }

        return parts;
    }

    /** Re-fits every segment through its key points using central-difference tangents. */
    public static PathRoute smooth(PathRoute path) {
        List<PathCurve> smoothedSegments = new ArrayList<>();

        for (PathCurve segment : path.getSegments()) {
            smoothedSegments.add(smoothCurve(segment));
        }

        return new PathRoute(smoothedSegments);
    }

    private static PathCurve smoothCurve(PathCurve curve) {
        List<Point> keypoints = keyPoints(curve);

        return fit(keypoints, curve.getHeading(0.0), curve.getHeading(1.0), true);
    }

    /** Returns the control points that begin and end each cubic in a curve chain. */
    private static List<Point> keyPoints(PathCurve curve) {
        List<PathCurve> cubicSegments = curve.toCubicSegments();
        List<Point> keypoints = new ArrayList<>();
        keypoints.add(cubicSegments.get(0).getControlPoints().get(0));

        for (PathCurve cubic : cubicSegments) {
            List<Point> controlPoints = cubic.getControlPoints();
            keypoints.add(controlPoints.get(controlPoints.size() - 1));
        }

        return keypoints;
    }

    /** Returns the footprint radius in the direction {@code relativeAngle}. */
    private static double footprintRadius(double relativeAngle, PathConfig config) {
        double halfWidth = config.getRobotWidth() / 2.0;
        double halfHeight = config.getRobotHeight() / 2.0;

        return halfWidth * Math.abs(Math.cos(relativeAngle)) + halfHeight * Math.abs(Math.sin(relativeAngle));
    }

    /** Estimates the outward direction of {@code zone} at {@code p}. */
    private static double obstacleDirection(Zone zone, Point p) {
        double gradientX = (signedDist(zone, new Point(p.getX() + GRADIENT_EPS, p.getY()))
                        - signedDist(zone, new Point(p.getX() - GRADIENT_EPS, p.getY())))
                / (2 * GRADIENT_EPS);

        double gradientY = (signedDist(zone, new Point(p.getX(), p.getY() + GRADIENT_EPS))
                        - signedDist(zone, new Point(p.getX(), p.getY() - GRADIENT_EPS)))
                / (2 * GRADIENT_EPS);

        if (Math.abs(gradientX) < 1e-9 && Math.abs(gradientY) < 1e-9) {
            return 0.0;
        }

        return Math.atan2(gradientY, gradientX);
    }

    private static double requiredMargin(Point p, double heading, Zone zone, PathConfig config, double extra) {
        double relativeAngle = obstacleDirection(zone, p) - heading;

        return footprintRadius(relativeAngle, config) + config.getClearance() + extra;
    }

    private static PathCurve routeCurve(
            PathCurve sourceCurve, List<Zone> obstacles, PathConfig config, boolean keepEndTangents) {
        // NaN lets the fit take its end headings from the routed legs.
        double startHeading = keepEndTangents ? sourceCurve.getHeading(0.0) : Double.NaN;
        double endHeading = keepEndTangents ? sourceCurve.getHeading(1.0) : Double.NaN;
        List<Point> keypoints = keyPoints(sourceCurve);

        // Key points are real destinations, so their own tight spots are forgiven near
        // them; waypoints added to route around obstacles get no relief.
        List<EndpointRelief> reliefs = new ArrayList<>();

        for (Point keypoint : keypoints) {
            reliefs.add(EndpointRelief.at(keypoint, obstacles));
        }

        if (isCurveClear(sourceCurve, obstacles, config, reliefs)) {
            return sourceCurve;
        }

        List<List<Point>> routes = new ArrayList<>();
        double padding = 0.0;

        for (int attempt = 0; attempt < MAX_INFLATE_ATTEMPTS; attempt++) {
            List<Point> waypoints = collectWaypoints(keypoints, obstacles, config, padding);

            if (waypoints == null) {
                break;
            }

            routes.add(waypoints);
            PathCurve curve = fit(waypoints, startHeading, endHeading, true);

            if (isCurveClear(curve, obstacles, config, reliefs)) {
                return curve;
            }

            padding += 0.5;
        }

        if (routes.isEmpty()) {
            return sourceCurve;
        }

        // A kept tangent can swing the footprint into an obstacle near an end. Where the
        // robot can turn on the spot instead, leave or arrive along the routed leg.
        if (keepEndTangents) {
            for (List<Point> waypoints : routes) {
                PathCurve curve = fitAlongLegs(waypoints, startHeading, endHeading, obstacles, config, reliefs);

                if (curve != null) {
                    return curve;
                }
            }
        }

        return fit(routes.get(0), startHeading, endHeading, false);
    }

    /**
     * Tries replacing an end tangent with the routed leg's direction: start, end, then
     * both. An end is only changed where the robot can turn on the spot.
     */
    private static PathCurve fitAlongLegs(
            List<Point> route,
            double startHeading,
            double endHeading,
            List<Zone> obstacles,
            PathConfig config,
            List<EndpointRelief> reliefs) {
        double firstLeg = headingAt(route, 0);
        double lastLeg = headingAt(route, route.size() - 1);
        boolean canTurnAtStart = isTurnClear(route.get(0), startHeading, firstLeg, obstacles, config);
        boolean canTurnAtEnd = isTurnClear(route.get(route.size() - 1), lastLeg, endHeading, obstacles, config);

        List<double[]> tangentPairs = new ArrayList<>();

        if (canTurnAtStart) {
            tangentPairs.add(new double[] {firstLeg, endHeading});
        }

        if (canTurnAtEnd) {
            tangentPairs.add(new double[] {startHeading, lastLeg});
        }

        if (canTurnAtStart && canTurnAtEnd) {
            tangentPairs.add(new double[] {firstLeg, lastLeg});
        }

        for (double[] tangents : tangentPairs) {
            PathCurve curve = fit(route, tangents[0], tangents[1], true);

            if (isCurveClear(curve, obstacles, config, reliefs)) {
                return curve;
            }
        }

        return null;
    }

    /**
     * Checks that the robot can turn on the spot at {@code p} from one heading to the
     * other, allowing the footprint to come no closer than it already is at either end.
     */
    static boolean isTurnClear(Point p, double fromHeading, double toHeading, List<Zone> obstacles, PathConfig config) {
        double turn = Math.IEEEremainder(toHeading - fromHeading, 2 * Math.PI);
        double halfDiagonal = Math.hypot(config.getRobotWidth() / 2.0, config.getRobotHeight() / 2.0);

        for (Zone zone : obstacles) {
            if (!cornerSwingsPast(obstacleDirection(zone, p) - fromHeading, turn, config)) {
                continue;
            }

            double distance = signedDist(zone, p);
            double slackBefore = distance - requiredMargin(p, fromHeading, zone, config, 0.0);
            double slackAfter = distance - requiredMargin(p, toHeading, zone, config, 0.0);
            double allowedSlack = Math.min(0.0, Math.min(slackBefore, slackAfter));

            if (distance - (halfDiagonal + config.getClearance()) < allowedSlack - 1e-6) {
                return false;
            }
        }

        return true;
    }

    private static boolean cornerSwingsPast(double relativeAngle, double turn, PathConfig config) {
        // Turning by turn moves the direction, relative to the robot, by -turn.
        double low = Math.min(relativeAngle, relativeAngle - turn);
        double high = Math.max(relativeAngle, relativeAngle - turn);
        double cornerAngle = Math.atan2(config.getRobotHeight(), config.getRobotWidth());

        // Corners point along +-cornerAngle, repeating every half turn.
        for (double corner : new double[] {cornerAngle, -cornerAngle}) {
            double firstAtOrAboveLow = corner + Math.ceil((low - corner) / Math.PI) * Math.PI;

            if (firstAtOrAboveLow <= high) {
                return true;
            }
        }

        return false;
    }

    private static List<Point> collectWaypoints(
            List<Point> keypoints, List<Zone> obstacles, PathConfig config, double extra) {
        List<Point> waypoints = new ArrayList<>();
        waypoints.add(keypoints.get(0));

        for (int i = 0; i < keypoints.size() - 1; i++) {
            List<Point> segmentWaypoints =
                    routeSegmentWaypoints(keypoints.get(i), keypoints.get(i + 1), obstacles, config, extra);

            if (segmentWaypoints == null) {
                return null;
            }

            // The shared start point was already added.
            for (int k = 1; k < segmentWaypoints.size(); k++) {
                waypoints.add(segmentWaypoints.get(k));
            }
        }

        return waypoints;
    }

    private static List<Point> routeSegmentWaypoints(
            Point start, Point end, List<Zone> obstacles, PathConfig config, double extra) {
        EndpointRelief[] reliefs = {EndpointRelief.at(start, obstacles), EndpointRelief.at(end, obstacles)};

        if (isSegmentClear(start, end, obstacles, config, extra, reliefs)) {
            return Arrays.asList(start, end);
        }

        double approxHeading = Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());

        // Nodes 0 and 1 are the segment ends; the rest are boundary samples.
        List<Point> nodes = new ArrayList<>();
        nodes.add(start);
        nodes.add(end);

        for (Zone zone : obstacles) {
            nodes.addAll(sampleBoundary(zone, approxHeading, config, extra));
        }

        return shortestPath(0, 1, nodes, obstacles, config, extra, reliefs);
    }

    private static boolean isPointClear(
            Point p,
            double heading,
            List<Zone> obstacles,
            PathConfig config,
            double extra,
            Iterable<EndpointRelief> reliefs) {
        // No heading needs more than the footprint's half diagonal.
        double widestMargin =
                Math.hypot(config.getRobotWidth() / 2.0, config.getRobotHeight() / 2.0) + config.getClearance() + extra;

        for (int i = 0; i < obstacles.size(); i++) {
            Zone zone = obstacles.get(i);
            double distance = signedDist(zone, p);

            if (distance >= widestMargin) {
                continue;
            }

            double requiredClearance = requiredMargin(p, heading, zone, config, extra);

            for (EndpointRelief relief : reliefs) {
                requiredClearance = Math.min(requiredClearance, relief.marginCap(i));
            }

            if (distance < requiredClearance - 1e-6) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSegmentClear(
            Point start, Point end, List<Zone> obstacles, PathConfig config, double extra, EndpointRelief[] reliefs) {
        double heading = Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());

        for (int i = 0; i <= SEGMENT_CHECKS; i++) {
            double progress = (double) i / SEGMENT_CHECKS;
            Point point = new Point(
                    start.getX() + (end.getX() - start.getX()) * progress,
                    start.getY() + (end.getY() - start.getY()) * progress);

            if (!isPointClear(point, heading, obstacles, config, extra, Arrays.asList(reliefs))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isCurveClear(
            PathCurve curve, List<Zone> obstacles, PathConfig config, List<EndpointRelief> reliefs) {
        for (PathCurve segment : curve.toCubicSegments()) {
            List<Point> controlPoints = segment.getControlPoints();
            List<EndpointRelief> touching = new ArrayList<>();

            for (EndpointRelief relief : reliefs) {
                if (relief.isAt(controlPoints.get(0)) || relief.isAt(controlPoints.get(controlPoints.size() - 1))) {
                    touching.add(relief);
                }
            }

            List<Point> samplePoints = segment.sample(SMOOTH_SAMPLES);

            for (int i = 0; i < samplePoints.size(); i++) {
                double progress = samplePoints.size() > 1 ? (double) i / (samplePoints.size() - 1) : 0.0;
                double heading = segment.getHeading(progress);

                if (!isPointClear(samplePoints.get(i), heading, obstacles, config, 0.0, touching)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Returns the signed distance to a zone's boundary; negative inside. */
    private static double signedDist(Zone zone, Point p) {
        double distance = zone.distanceToBoundary(p);

        return zone.contains(p) ? -Math.abs(distance) : distance;
    }

    /** Samples candidate waypoints around an obstacle, offset by footprint and clearance. */
    private static List<Point> sampleBoundary(Zone zone, double heading, PathConfig config, double extra) {
        List<Point> points = new ArrayList<>();
        double clearance = config.getClearance();

        if (zone instanceof CircleZone) {
            CircleZone circle = (CircleZone) zone;
            Point center = circle.getPosition();

            for (int i = 0; i < CIRCLE_SAMPLES; i++) {
                double angle = 2.0 * Math.PI * i / CIRCLE_SAMPLES;
                double offset = footprintRadius(angle - heading, config) + clearance + extra + SEED_PAD;

                // Divide by cos so samples on the circumscribed polygon still enclose
                // the circle.
                double radius = (circle.getRadius() + offset) / Math.cos(Math.PI / CIRCLE_SAMPLES);
                points.add(
                        new Point(center.getX() + radius * Math.cos(angle), center.getY() + radius * Math.sin(angle)));
            }
        } else if (zone instanceof PolygonZone) {
            PolygonZone polygon = (PolygonZone) zone;
            Point[] corners = polygon.getCorners();
            Point center = polygon.getPosition();
            int cornerCount = corners.length;

            // Fan of samples around each corner, covering the exterior turn.
            for (int i = 0; i < cornerCount; i++) {
                Point previousCorner = corners[(i - 1 + cornerCount) % cornerCount];
                Point corner = corners[i];
                Point nextCorner = corners[(i + 1) % cornerCount];

                Point previousNormal = outwardNormal(previousCorner, corner, center);
                Point nextNormal = outwardNormal(corner, nextCorner, center);

                double previousAngle = Math.atan2(previousNormal.getY(), previousNormal.getX());
                double nextAngle = Math.atan2(nextNormal.getY(), nextNormal.getX());

                double sweep = nextAngle - previousAngle;
                while (sweep <= 0) {
                    sweep += 2 * Math.PI;
                }

                for (int k = 0; k <= CORNER_SAMPLES; k++) {
                    double theta = previousAngle + sweep * k / CORNER_SAMPLES;
                    double offset = footprintRadius(theta - heading, config) + clearance + extra + SEED_PAD;

                    points.add(new Point(
                            corner.getX() + offset * Math.cos(theta), corner.getY() + offset * Math.sin(theta)));
                }
            }

            // One sample pushed straight out from each edge midpoint.
            for (int i = 0; i < cornerCount; i++) {
                Point a = corners[i];
                Point b = corners[(i + 1) % cornerCount];

                Point midpoint = new Point((a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0);
                Point normal = outwardNormal(a, b, center);

                double normalAngle = Math.atan2(normal.getY(), normal.getX());
                double offset = footprintRadius(normalAngle - heading, config) + clearance + extra + SEED_PAD;

                points.add(
                        new Point(midpoint.getX() + offset * normal.getX(), midpoint.getY() + offset * normal.getY()));
            }
        } else if (zone instanceof CompositeZone) {
            for (Zone child : ((CompositeZone) zone).getZones()) {
                points.addAll(sampleBoundary(child, heading, config, extra));
            }
        }

        return points;
    }

    /** Returns the unit normal of edge {@code a -> b} pointing away from {@code center}. */
    private static Point outwardNormal(Point a, Point b, Point center) {
        double edgeX = b.getX() - a.getX();
        double edgeY = b.getY() - a.getY();
        double edgeLength = Math.hypot(edgeX, edgeY);

        if (edgeLength < 1e-9) {
            return new Point(0, 0);
        }

        double normalX = -edgeY / edgeLength;
        double normalY = edgeX / edgeLength;
        double altNormalX = edgeY / edgeLength;
        double altNormalY = -edgeX / edgeLength;

        double midpointX = (a.getX() + b.getX()) / 2.0;
        double midpointY = (a.getY() + b.getY()) / 2.0;
        double toCenterX = center.getX() - midpointX;
        double toCenterY = center.getY() - midpointY;

        return normalX * toCenterX + normalY * toCenterY < 0
                ? new Point(normalX, normalY)
                : new Point(altNormalX, altNormalY);
    }

    /**
     * Runs A* over a visibility graph of {@code nodes}. Two nodes are connected when
     * the segment between them is clear; the straight-line distance is the heuristic.
     */
    private static List<Point> shortestPath(
            int startIdx,
            int endIdx,
            List<Point> nodes,
            List<Zone> obstacles,
            PathConfig config,
            double extra,
            EndpointRelief[] reliefs) {
        int nodeCount = nodes.size();
        double[] distances = new double[nodeCount];
        double[] remaining = new double[nodeCount];
        int[] previousNode = new int[nodeCount];
        boolean[] visited = new boolean[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            distances[i] = Double.MAX_VALUE;
            remaining[i] = nodes.get(i).distanceTo(nodes.get(endIdx));
            previousNode[i] = -1;
        }
        distances[startIdx] = 0;

        for (int iteration = 0; iteration < nodeCount; iteration++) {
            int currentNode = -1;
            double bestEstimate = Double.MAX_VALUE;

            for (int i = 0; i < nodeCount; i++) {
                if (!visited[i] && distances[i] < Double.MAX_VALUE && distances[i] + remaining[i] < bestEstimate) {
                    bestEstimate = distances[i] + remaining[i];
                    currentNode = i;
                }
            }

            if (currentNode < 0) {
                break;
            }

            visited[currentNode] = true;

            if (currentNode == endIdx) {
                break;
            }

            // Line of sight is the expensive test, so it only runs for an edge that
            // would shorten the route. Each pair is tested from its lower index, so
            // rounding in the samples cannot change the answer.
            for (int neighbor = 0; neighbor < nodeCount; neighbor++) {
                if (visited[neighbor]) {
                    continue;
                }

                double edgeLength = nodes.get(currentNode).distanceTo(nodes.get(neighbor));

                int from = Math.min(currentNode, neighbor);
                int to = Math.max(currentNode, neighbor);

                if (distances[currentNode] + edgeLength < distances[neighbor]
                        && isSegmentClear(
                                nodes.get(from),
                                nodes.get(to),
                                obstacles,
                                config,
                                extra,
                                edgeReliefs(from, to, startIdx, endIdx, reliefs))) {
                    distances[neighbor] = distances[currentNode] + edgeLength;
                    previousNode[neighbor] = currentNode;
                }
            }
        }

        if (previousNode[endIdx] == -1 && startIdx != endIdx) {
            return null;
        }

        List<Point> route = new ArrayList<>();
        int current = endIdx;

        while (current != -1) {
            route.add(nodes.get(current));
            current = previousNode[current];
        }

        Collections.reverse(route);
        return route;
    }

    /**
     * Fits a cubic chain through {@code route}. With {@code smooth} true, interior
     * tangents come from central differences; otherwise each segment keeps its own
     * direction. Non-NaN end headings override the outer tangents.
     */
    private static PathCurve fit(List<Point> route, double startHeading, double endHeading, boolean smooth) {
        int pointCount = route.size();
        List<Point> controlPoints = new ArrayList<>();

        for (int i = 0; i < pointCount - 1; i++) {
            Point startPoint = route.get(i);
            Point endPoint = route.get(i + 1);
            double handleLength = startPoint.distanceTo(endPoint) / 3.0;
            double segmentHeading =
                    Math.atan2(endPoint.getY() - startPoint.getY(), endPoint.getX() - startPoint.getX());

            double firstHeading = headingFor(route, i, startHeading, i == 0, smooth, segmentHeading);
            double secondHeading = headingFor(route, i + 1, endHeading, i == pointCount - 2, smooth, segmentHeading);

            if (i == 0) {
                controlPoints.add(startPoint);
            }

            controlPoints.add(new Point(
                    startPoint.getX() + handleLength * Math.cos(firstHeading),
                    startPoint.getY() + handleLength * Math.sin(firstHeading)));

            controlPoints.add(new Point(
                    endPoint.getX() - handleLength * Math.cos(secondHeading),
                    endPoint.getY() - handleLength * Math.sin(secondHeading)));

            controlPoints.add(endPoint);
        }

        return new PathCurve(controlPoints);
    }

    private static double headingFor(
            List<Point> route, int index, double override, boolean isEnd, boolean smooth, double segmentHeading) {
        if (isEnd && !Double.isNaN(override)) {
            return override;
        }

        return smooth ? headingAt(route, index) : segmentHeading;
    }

    /**
     * Estimates the heading at route point {@code i}. End points use the nearest point
     * far enough away; interior points use a central difference widened until its chord
     * spans the baseline.
     */
    private static double headingAt(List<Point> route, int i) {
        int last = route.size() - 1;

        if (i <= 0) {
            for (int k = 1; k <= last; k++) {
                if (route.get(0).distanceTo(route.get(k)) >= MIN_TANGENT_BASELINE) {
                    return direction(route.get(0), route.get(k));
                }
            }

            return direction(route.get(0), route.get(last));
        }

        if (i >= last) {
            for (int k = last - 1; k >= 0; k--) {
                if (route.get(k).distanceTo(route.get(last)) >= MIN_TANGENT_BASELINE) {
                    return direction(route.get(k), route.get(last));
                }
            }

            return direction(route.get(0), route.get(last));
        }

        for (int step = 1; step <= i && step + i <= last; step++) {
            Point before = route.get(i - step);
            Point after = route.get(i + step);

            if (before.distanceTo(after) >= MIN_TANGENT_BASELINE) {
                return cornerTangent(before, route.get(i), after);
            }
        }

        int step = Math.min(i, last - i);
        return cornerTangent(route.get(i - step), route.get(i), route.get(i + step));
    }

    /**
     * Returns the tangent at the middle of three points, weighting each leg by the
     * other's length so a corner is split in proportion to the route on either side.
     */
    private static double cornerTangent(Point before, Point point, Point after) {
        double incomingX = point.getX() - before.getX();
        double incomingY = point.getY() - before.getY();
        double outgoingX = after.getX() - point.getX();
        double outgoingY = after.getY() - point.getY();
        double incomingLength = Math.hypot(incomingX, incomingY);
        double outgoingLength = Math.hypot(outgoingX, outgoingY);

        if (incomingLength < 1e-9 || outgoingLength < 1e-9) {
            return direction(before, after);
        }

        double x = incomingX * outgoingLength + outgoingX * incomingLength;
        double y = incomingY * outgoingLength + outgoingY * incomingLength;

        if (Math.hypot(x, y) < 1e-9) {
            return direction(before, after);
        }

        return Math.atan2(y, x);
    }

    private static double direction(Point from, Point to) {
        return Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
    }
}
