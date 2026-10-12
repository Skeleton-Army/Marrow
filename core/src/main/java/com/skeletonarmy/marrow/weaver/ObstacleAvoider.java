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
 * <p>
 * The avoider works on the control points of the incoming curve chain. For each
 * segment it samples the obstacle boundaries into candidate waypoints, runs a
 * visibility-graph shortest path through them, then re-fits a smooth cubic chain
 * through the result. If a first attempt is still too tight, it retries with extra
 * padding, and finally falls back to the original curve when nothing works.
 */
public class ObstacleAvoider {
    private ObstacleAvoider() {}

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

    /**
     * Clearance that a route endpoint cannot meet itself, forgiven on the segments
     * that start or end there.
     * <p>
     * A robot that starts or stops close to an obstacle cannot keep the full margin
     * from it while leaving or reaching that spot. On a segment touching the
     * endpoint, the margin required from each obstacle is capped at the endpoint's
     * own distance to that obstacle. Obstacles the endpoint is not close to keep
     * their full margin, and an obstacle containing the endpoint is ignored, since no
     * segment touching the endpoint can avoid it.
     */
    private static final class EndpointRelief {
        private final Point endpoint;
        private final double[] marginCaps;

        private EndpointRelief(Point endpoint, double[] marginCaps) {
            this.endpoint = endpoint;
            this.marginCaps = marginCaps;
        }

        /**
         * Measures the relief an endpoint needs from each obstacle.
         *
         * @param endpoint  route endpoint
         * @param obstacles zones the route must clear
         * @return the endpoint's relief
         */
        static EndpointRelief at(Point endpoint, List<Zone> obstacles) {
            double[] marginCaps = new double[obstacles.size()];

            for (int i = 0; i < obstacles.size(); i++) {
                Zone zone = obstacles.get(i);
                marginCaps[i] = zone.contains(endpoint) ? Double.NEGATIVE_INFINITY : signedDist(zone, endpoint);
            }

            return new EndpointRelief(endpoint, marginCaps);
        }

        /**
         * @param obstacleIndex index of the obstacle in the list the relief was measured against
         * @return the largest margin that obstacle may require on a segment touching
         *         the endpoint; negative infinity when the endpoint is inside it
         */
        double marginCap(int obstacleIndex) {
            return marginCaps[obstacleIndex];
        }

        /**
         * @param point point to compare
         * @return {@code true} if {@code point} is this relief's endpoint
         */
        boolean isAt(Point point) {
            return endpoint.distanceTo(point) < 1e-9;
        }
    }

    /**
     * Returns the reliefs of the endpoints a visibility edge touches.
     *
     * @param from     index of one edge node
     * @param to       index of the other edge node
     * @param startIdx index of the start node
     * @param endIdx   index of the end node
     * @param reliefs  reliefs of the start node and the end node, in that order
     * @return the reliefs that apply to the edge
     */
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

    /**
     * Routes every segment of {@code path} around the given obstacles.
     *
     * @param path      route to reroute
     * @param obstacles zones to avoid; when {@code null} or empty the route is returned unchanged
     * @param config    geometry and tuning parameters
     * @return the rerouted path
     */
    public static PathRoute avoid(PathRoute path, List<Zone> obstacles, PathConfig config) {
        if (obstacles == null || obstacles.isEmpty()) {
            return path;
        }

        List<Zone> parts = flatten(obstacles);
        List<PathCurve> routedSegments = new ArrayList<>();

        for (PathCurve segment : path.getSegments()) {
            routedSegments.add(routeCurve(segment, parts, config));
        }

        return routedSegments.size() == 1
                ? new PathRoute(Collections.singletonList(routedSegments.get(0)))
                : new PathRoute(routedSegments);
    }

    /**
     * Expands composite zones into their parts, recursively. Endpoint relief is
     * measured per obstacle, so an endpoint inside one part of a composite must not
     * excuse the route from the other parts.
     *
     * @param obstacles zones to expand
     * @return the zones with every composite replaced by its parts
     */
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

    /**
     * Re-fits every segment through its original keypoints using continuous,
     * central-difference tangents. Obstacle routing already smooths as a side
     * effect of fitting; this applies the same treatment to obstacle-free paths.
     *
     * @param path route to smooth
     * @return the smoothed path
     */
    public static PathRoute smooth(PathRoute path) {
        List<PathCurve> smoothedSegments = new ArrayList<>();

        for (PathCurve segment : path.getSegments()) {
            smoothedSegments.add(smoothCurve(segment));
        }

        return smoothedSegments.size() == 1
                ? new PathRoute(Collections.singletonList(smoothedSegments.get(0)))
                : new PathRoute(smoothedSegments);
    }

    /**
     * Smooths a single curve by extracting its keypoints and re-fitting them with
     * central-difference tangents.
     *
     * @param curve curve to smooth
     * @return the smoothed curve
     */
    private static PathCurve smoothCurve(PathCurve curve) {
        List<PathCurve> cubicSegments = curve.toCubicSegments();

        List<Point> keypoints = new ArrayList<>();
        keypoints.add(cubicSegments.get(0).getControlPoints().get(0));

        for (PathCurve cubic : cubicSegments) {
            List<Point> controlPoints = cubic.getControlPoints();
            keypoints.add(controlPoints.get(controlPoints.size() - 1));
        }

        return fitSmooth(keypoints, curve.getHeading(0.0), curve.getHeading(1.0));
    }

    /**
     * Returns the radius of the robot footprint in the direction of
     * {@code relativeAngle}, measured from the robot center.
     *
     * @param relativeAngle direction to measure, relative to the robot heading
     * @param config        geometry parameters
     * @return the footprint radius in that direction
     */
    private static double footprintRadius(double relativeAngle, PathConfig config) {
        double halfWidth = config.getRobotWidth() / 2.0;
        double halfHeight = config.getRobotHeight() / 2.0;

        return halfWidth * Math.abs(Math.cos(relativeAngle)) + halfHeight * Math.abs(Math.sin(relativeAngle));
    }

    /**
     * Estimates the direction pointing away from the obstacle at point {@code p}
     * using a central-difference gradient of the signed distance.
     *
     * @param zone obstacle being measured
     * @param p    point to evaluate at
     * @return the outward angle in radians
     */
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

    /**
     * Computes how far {@code p} must be from {@code zone}, given the robot heading.
     * The footprint radius is measured toward the obstacle, so the required margin
     * grows when the robot presents a corner rather than a flat side.
     *
     * @param p       robot-center point to test
     * @param heading robot heading in radians
     * @param zone    obstacle to clear
     * @param config  geometry parameters
     * @param extra   additional padding added to the margin
     * @return the required clearance
     */
    private static double requiredMargin(Point p, double heading, Zone zone, PathConfig config, double extra) {
        double relativeAngle = obstacleDirection(zone, p) - heading;

        return footprintRadius(relativeAngle, config) + config.getClearance() + extra;
    }

    /**
     * Routes a single curve around obstacles.
     * <p>
     * Candidate waypoints are collected around the obstacle boundaries and a
     * smooth path is fitted through the shortest visible route. If the fitted curve
     * still clips an obstacle, the padding is increased and the attempt repeated.
     *
     * @param sourceCurve curve to reroute
     * @param obstacles   zones to avoid
     * @param config      geometry and tuning parameters
     * @return the rerouted curve, or the original curve when routing fails
     */
    private static PathCurve routeCurve(PathCurve sourceCurve, List<Zone> obstacles, PathConfig config) {
        double startHeading = sourceCurve.getHeading(0.0);
        double endHeading = sourceCurve.getHeading(1.0);
        List<PathCurve> cubicSegments = sourceCurve.toCubicSegments();

        List<Point> keypoints = new ArrayList<>();
        keypoints.add(cubicSegments.get(0).getControlPoints().get(0));

        for (PathCurve cubic : cubicSegments) {
            List<Point> controlPoints = cubic.getControlPoints();
            keypoints.add(controlPoints.get(controlPoints.size() - 1));
        }

        // The key points are where the robot has to go, so their own tight spots are
        // forgiven near them; points added to route around obstacles get no relief.
        List<EndpointRelief> reliefs = new ArrayList<>();

        for (Point keypoint : keypoints) {
            reliefs.add(EndpointRelief.at(keypoint, obstacles));
        }

        List<Point> bestWaypoints = null;
        double padding = 0.0;

        for (int attempt = 0; attempt < MAX_INFLATE_ATTEMPTS; attempt++) {
            List<Point> waypoints = collectWaypoints(keypoints, obstacles, config, padding);

            if (waypoints == null) {
                break;
            }

            if (bestWaypoints == null) {
                bestWaypoints = waypoints;
            }

            PathCurve curve = fitSmooth(waypoints, startHeading, endHeading);

            if (isCurveClear(curve, obstacles, config, reliefs)) {
                return curve;
            }

            // Nothing fit, so keep the robot further away and try again.
            padding += 0.5;
        }

        return bestWaypoints != null ? buildPath(bestWaypoints, startHeading, endHeading) : sourceCurve;
    }

    /**
     * Builds a full list of waypoints by routing each original keypoint-to-keypoint
     * segment and concatenating the pieces.
     *
     * @param keypoints original key points to connect, in order
     * @param obstacles zones to avoid
     * @param config    geometry and tuning parameters
     * @param extra     additional padding to keep from obstacles
     * @return the combined waypoints, or {@code null} if any segment cannot be routed
     */
    private static List<Point> collectWaypoints(
            List<Point> keypoints, List<Zone> obstacles, PathConfig config, double extra) {
        List<Point> waypoints = new ArrayList<>();
        waypoints.add(keypoints.get(0));

        for (int i = 0; i < keypoints.size() - 1; i++) {
            Point segmentStart = keypoints.get(i);
            Point segmentEnd = keypoints.get(i + 1);

            List<Point> segmentWaypoints = routeSegmentWaypoints(segmentStart, segmentEnd, obstacles, config, extra);

            if (segmentWaypoints == null) {
                return null;
            }

            // Skip the shared start point; it was already added.
            for (int k = 1; k < segmentWaypoints.size(); k++) {
                waypoints.add(segmentWaypoints.get(k));
            }
        }

        return waypoints;
    }

    /**
     * Routes one straight segment, returning a list of points that begins at
     * {@code start} and ends at {@code end}.
     * <p>
     * If the segment is already clear, the segment endpoints are returned unchanged.
     * Clearance that an endpoint itself cannot meet is forgiven near that endpoint,
     * and an obstacle containing an endpoint is ignored (see {@link EndpointRelief}).
     *
     * @param start     segment start point
     * @param end       segment end point
     * @param obstacles zones to avoid
     * @param config    geometry and tuning parameters
     * @param extra     additional padding to keep from obstacles
     * @return the waypoints of the routed segment, or {@code null} if no route exists
     */
    private static List<Point> routeSegmentWaypoints(
            Point start, Point end, List<Zone> obstacles, PathConfig config, double extra) {
        EndpointRelief[] reliefs = {EndpointRelief.at(start, obstacles), EndpointRelief.at(end, obstacles)};

        if (isSegmentClear(start, end, obstacles, config, extra, reliefs)) {
            return Arrays.asList(start, end);
        }

        double approxHeading = Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());

        // Node 0 is the start and node 1 is the end; the rest are obstacle boundary samples.
        List<Point> nodes = new ArrayList<>();
        nodes.add(start);
        nodes.add(end);

        for (Zone zone : obstacles) {
            nodes.addAll(sampleBoundary(zone, approxHeading, config, extra));
        }

        return shortestPath(0, 1, nodes, obstacles, config, extra, reliefs);
    }

    /**
     * Checks that a single point keeps enough distance from every obstacle.
     *
     * @param p         robot-center point to test
     * @param heading   robot heading in radians
     * @param obstacles zones to test against
     * @param config    geometry parameters
     * @param extra     additional padding to keep
     * @param reliefs   endpoint reliefs that apply to this point
     * @return {@code true} if the point has enough clearance
     */
    private static boolean isPointClear(
            Point p,
            double heading,
            List<Zone> obstacles,
            PathConfig config,
            double extra,
            Iterable<EndpointRelief> reliefs) {
        for (int i = 0; i < obstacles.size(); i++) {
            Zone zone = obstacles.get(i);
            double requiredClearance = requiredMargin(p, heading, zone, config, extra);

            for (EndpointRelief relief : reliefs) {
                requiredClearance = Math.min(requiredClearance, relief.marginCap(i));
            }

            if (signedDist(zone, p) < requiredClearance - 1e-6) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks a segment by sampling it and testing each sample point.
     *
     * @param start     segment start point
     * @param end       segment end point
     * @param obstacles zones to test against
     * @param config    geometry parameters
     * @param extra     additional padding to keep
     * @param reliefs   reliefs of the endpoints this segment touches
     * @return {@code true} if every sample is clear
     */
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

    /**
     * Checks that a fitted curve stays clear of every obstacle. A cubic that starts
     * or ends on a key point is forgiven the clearance that key point cannot meet
     * itself; cubics between routing waypoints get no relief.
     *
     * @param curve     curve to test
     * @param obstacles zones to test against
     * @param config    geometry parameters
     * @param reliefs   one relief per key point of the route
     * @return {@code true} if the curve stays clear
     */
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

    /**
     * Returns the signed distance to a zone's boundary; negative when inside.
     *
     * @param zone zone to measure against
     * @param p    point to measure
     * @return the signed distance
     */
    private static double signedDist(Zone zone, Point p) {
        double distance = zone.distanceToBoundary(p);

        return zone.contains(p) ? -Math.abs(distance) : distance;
    }

    /**
     * Samples candidate waypoints around the boundary of an obstacle, offset far
     * enough out that the robot footprint plus clearance fits.
     * <p>
     * Circles are sampled uniformly. Polygons get points around each corner
     * (sweeping the exterior angle) plus one point at each edge midpoint. Composite
     * zones are sampled recursively.
     *
     * @param zone    obstacle whose boundary is sampled
     * @param heading approximate travel direction, used to size the footprint offset
     * @param config  geometry parameters
     * @param extra   additional padding
     * @return the sampled boundary waypoints
     */
    private static List<Point> sampleBoundary(Zone zone, double heading, PathConfig config, double extra) {
        List<Point> points = new ArrayList<>();
        double clearance = config.getClearance();

        if (zone instanceof CircleZone) {
            CircleZone circle = (CircleZone) zone;
            Point center = circle.getPosition();

            for (int i = 0; i < CIRCLE_SAMPLES; i++) {
                double angle = 2.0 * Math.PI * i / CIRCLE_SAMPLES;
                double offset = footprintRadius(angle - heading, config) + clearance + extra + SEED_PAD;

                // Divide by cos so samples on the circumscribed polygon still enclose the circle.
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

    /**
     * Returns the unit normal of edge {@code a -> b} that points away from
     * {@code center}.
     *
     * @param a      edge start point
     * @param b      edge end point
     * @param center polygon center used to choose the outward direction
     * @return the outward unit normal, or {@code (0, 0)} for a degenerate edge
     */
    private static Point outwardNormal(Point a, Point b, Point center) {
        double edgeX = b.getX() - a.getX();
        double edgeY = b.getY() - a.getY();
        double edgeLength = Math.hypot(edgeX, edgeY);

        if (edgeLength < 1e-9) {
            return new Point(0, 0);
        }

        // The two candidate normals, one of which points outward.
        double normalX = -edgeY / edgeLength;
        double normalY = edgeX / edgeLength;
        double altNormalX = edgeY / edgeLength;
        double altNormalY = -edgeX / edgeLength;

        double midpointX = (a.getX() + b.getX()) / 2.0;
        double midpointY = (a.getY() + b.getY()) / 2.0;

        double toCenterX = center.getX() - midpointX;
        double toCenterY = center.getY() - midpointY;

        // Pick the normal that points away from the polygon center.
        if (normalX * toCenterX + normalY * toCenterY < 0) {
            return new Point(normalX, normalY);
        }

        return new Point(altNormalX, altNormalY);
    }

    /**
     * Runs an A* search over a visibility graph of {@code nodes}, where two nodes are
     * connected when the segment between them is clear. The heuristic is the
     * straight-line distance to the end node, which is consistent, so the route found
     * is a shortest one.
     * <p>
     * Clearance that the start or end node cannot meet itself is forgiven on the
     * edges touching it (see {@link EndpointRelief}).
     *
     * @param startIdx  index of the start node (normally 0)
     * @param endIdx    index of the end node (normally 1)
     * @param nodes     visibility-graph nodes
     * @param obstacles zones to test line of sight against
     * @param config    geometry parameters
     * @param extra     additional padding to keep
     * @param reliefs   reliefs of the start node and the end node, in that order
     * @return the shortest route of points, or {@code null} if none exists
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

        // lineOfSight[i][j] is true when the straight segment i -> j is clear.
        boolean[][] lineOfSight = new boolean[nodeCount][nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            for (int j = i + 1; j < nodeCount; j++) {
                boolean clear = isSegmentClear(
                        nodes.get(i),
                        nodes.get(j),
                        obstacles,
                        config,
                        extra,
                        edgeReliefs(i, j, startIdx, endIdx, reliefs));
                lineOfSight[i][j] = clear;
                lineOfSight[j][i] = clear;
            }
        }

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
            // Pick the reached, unvisited node whose route to the end could be shortest.
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

            // Relax every visible neighbour.
            for (int neighbor = 0; neighbor < nodeCount; neighbor++) {
                if (visited[neighbor] || !lineOfSight[currentNode][neighbor]) {
                    continue;
                }

                double edgeLength = nodes.get(currentNode).distanceTo(nodes.get(neighbor));

                if (distances[currentNode] + edgeLength < distances[neighbor]) {
                    distances[neighbor] = distances[currentNode] + edgeLength;
                    previousNode[neighbor] = currentNode;
                }
            }
        }

        // No route reached the end.
        if (previousNode[endIdx] == -1 && startIdx != endIdx) {
            return null;
        }

        // Walk the predecessor chain backwards, then reverse it.
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
     * Fits a smooth cubic chain through the route. Interior headings are estimated
     * from central differences; the supplied end headings override the outer ones
     * when present.
     *
     * @param route        waypoints to fit through
     * @param startHeading start heading in radians, or {@link Double#NaN} to estimate it
     * @param endHeading   end heading in radians, or {@link Double#NaN} to estimate it
     * @return the fitted smooth curve
     */
    private static PathCurve fitSmooth(List<Point> route, double startHeading, double endHeading) {
        int pointCount = route.size();
        List<Point> controlPoints = new ArrayList<>();

        for (int i = 0; i < pointCount - 1; i++) {
            Point startPoint = route.get(i);
            Point endPoint = route.get(i + 1);

            double handleLength = startPoint.distanceTo(endPoint) / 3.0;

            double firstHeading = (i == 0 && !Double.isNaN(startHeading)) ? startHeading : headingAt(route, i);
            double secondHeading =
                    (i == pointCount - 2 && !Double.isNaN(endHeading)) ? endHeading : headingAt(route, i + 1);

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

    /**
     * Fits a cubic chain through the route using each segment's own direction as the
     * tangent. This tracks the route more tightly than {@link #fitSmooth} but turns
     * more abruptly.
     *
     * @param route        waypoints to fit through
     * @param startHeading start heading in radians, or {@link Double#NaN} to estimate it
     * @param endHeading   end heading in radians, or {@link Double#NaN} to estimate it
     * @return the fitted curve
     */
    private static PathCurve buildPath(List<Point> route, double startHeading, double endHeading) {
        int pointCount = route.size();
        List<Point> controlPoints = new ArrayList<>();

        for (int i = 0; i < pointCount - 1; i++) {
            Point startPoint = route.get(i);
            Point endPoint = route.get(i + 1);

            double handleLength = startPoint.distanceTo(endPoint) / 3.0;
            double segmentHeading =
                    Math.atan2(endPoint.getY() - startPoint.getY(), endPoint.getX() - startPoint.getX());

            double firstHeading = (i == 0 && !Double.isNaN(startHeading)) ? startHeading : segmentHeading;
            double secondHeading = (i == pointCount - 2 && !Double.isNaN(endHeading)) ? endHeading : segmentHeading;

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

    /**
     * Estimates the heading at route point {@code i} from its neighbors. End points
     * use the single adjacent segment; interior points use a central difference.
     *
     * @param route waypoints
     * @param i     index of the point to evaluate
     * @return the estimated heading in radians
     */
    private static double headingAt(List<Point> route, int i) {
        if (i == 0) {
            return Math.atan2(
                    route.get(1).getY() - route.get(0).getY(),
                    route.get(1).getX() - route.get(0).getX());
        }

        if (i == route.size() - 1) {
            return Math.atan2(
                    route.get(i).getY() - route.get(i - 1).getY(),
                    route.get(i).getX() - route.get(i - 1).getX());
        }

        return Math.atan2(
                route.get(i + 1).getY() - route.get(i - 1).getY(),
                route.get(i + 1).getX() - route.get(i - 1).getX());
    }
}
