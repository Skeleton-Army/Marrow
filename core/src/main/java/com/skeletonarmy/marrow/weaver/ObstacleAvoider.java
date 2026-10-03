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

        List<PathCurve> routedSegments = new ArrayList<>();

        for (PathCurve segment : path.getSegments()) {
            routedSegments.add(routeCurve(segment, obstacles, config));
        }

        return routedSegments.size() == 1
                ? new PathRoute(Collections.singletonList(routedSegments.get(0)))
                : new PathRoute(routedSegments);
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
     * Returns the largest distance any point of the robot could need from an
     * obstacle: the half diagonal of the footprint plus the clearance.
     *
     * @param config geometry parameters
     * @return the worst-case margin
     */
    private static double worstCaseMargin(PathConfig config) {
        return Math.hypot(config.getRobotWidth(), config.getRobotHeight()) / 2.0 + config.getClearance();
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
                - signedDist(zone, new Point(p.getX() - GRADIENT_EPS, p.getY()))) / (2 * GRADIENT_EPS);

        double gradientY = (signedDist(zone, new Point(p.getX(), p.getY() + GRADIENT_EPS))
                - signedDist(zone, new Point(p.getX(), p.getY() - GRADIENT_EPS))) / (2 * GRADIENT_EPS);

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

            if (isCurveClear(curve, obstacles, config, waypoints)) {
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
    private static List<Point> collectWaypoints(List<Point> keypoints, List<Zone> obstacles,
                                                PathConfig config, double extra) {
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
     * If either endpoint is inside an obstacle, or the segment is already clear,
     * the segment endpoints are returned unchanged.
     *
     * @param start     segment start point
     * @param end       segment end point
     * @param obstacles zones to avoid
     * @param config    geometry and tuning parameters
     * @param extra     additional padding to keep from obstacles
     * @return the waypoints of the routed segment, or {@code null} if no route exists
     */
    private static List<Point> routeSegmentWaypoints(Point start, Point end, List<Zone> obstacles,
                                                     PathConfig config, double extra) {
        if (insideObstacle(start, obstacles) || insideObstacle(end, obstacles)) {
            return Arrays.asList(start, end);
        }

        if (isSegmentClear(start, end, obstacles, config, extra)) {
            return Arrays.asList(start, end);
        }

        double approxHeading = Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());

        // How far inside its safety margin each endpoint sits. Relaxing to this
        // distance lets a route leave from a tight start or arrive at a tight goal.
        double startRelaxDistance = minSignedDist(start, obstacles);
        double endRelaxDistance = minSignedDist(end, obstacles);

        // Node 0 is the start and node 1 is the end; the rest are obstacle boundary samples.
        List<Point> nodes = new ArrayList<>();
        nodes.add(start);
        nodes.add(end);

        for (Zone zone : obstacles) {
            nodes.addAll(sampleBoundary(zone, approxHeading, config, extra));
        }

        return shortestPath(0, 1, nodes, obstacles, config, extra, startRelaxDistance, endRelaxDistance);
    }

    /**
     * Returns the smallest signed distance from {@code p} to any obstacle.
     *
     * @param p         point to measure
     * @param obstacles zones to measure against
     * @return the smallest signed distance; negative when inside an obstacle
     */
    private static double minSignedDist(Point p, List<Zone> obstacles) {
        double minimum = Double.MAX_VALUE;

        for (Zone zone : obstacles) {
            minimum = Math.min(minimum, signedDist(zone, p));
        }

        return minimum;
    }

    /**
     * Returns whether {@code p} is inside any obstacle.
     *
     * @param p         point to test
     * @param obstacles zones to test against
     * @return {@code true} if the point is inside an obstacle
     */
    private static boolean insideObstacle(Point p, List<Zone> obstacles) {
        for (Zone zone : obstacles) {
            if (zone.contains(p)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks that a single point keeps enough distance from every obstacle.
     *
     * @param p        robot-center point to test
     * @param heading  robot heading in radians
     * @param obstacles zones to test against
     * @param config   geometry parameters
     * @param extra    additional padding to keep
     * @param relaxCap upper bound on how much the required margin may be relaxed
     * @return {@code true} if the point has enough clearance
     */
    private static boolean isPointClear(Point p, double heading, List<Zone> obstacles, PathConfig config, double extra, double relaxCap) {
        for (Zone zone : obstacles) {
            double requiredClearance = Math.min(requiredMargin(p, heading, zone, config, extra), relaxCap);

            if (signedDist(zone, p) < requiredClearance - 1e-6) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks a segment without relaxing the clearance.
     *
     * @param start     segment start point
     * @param end       segment end point
     * @param obstacles zones to test against
     * @param config    geometry parameters
     * @param extra     additional padding to keep
     * @return {@code true} if the whole segment is clear
     */
    private static boolean isSegmentClear(Point start, Point end, List<Zone> obstacles, PathConfig config, double extra) {
        return isSegmentClear(start, end, obstacles, config, extra, Double.MAX_VALUE);
    }

    /**
     * Checks a segment by sampling it and testing each sample point.
     *
     * @param start    segment start point
     * @param end      segment end point
     * @param obstacles zones to test against
     * @param config   geometry parameters
     * @param extra    additional padding to keep
     * @param relaxCap upper bound on the relaxed margin, or {@link Double#MAX_VALUE}
     * @return {@code true} if every sample is clear
     */
    private static boolean isSegmentClear(Point start, Point end, List<Zone> obstacles, PathConfig config, double extra, double relaxCap) {
        double heading = Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());

        for (int i = 0; i <= SEGMENT_CHECKS; i++) {
            double progress = (double) i / SEGMENT_CHECKS;

            Point point = new Point(
                    start.getX() + (end.getX() - start.getX()) * progress,
                    start.getY() + (end.getY() - start.getY()) * progress);

            if (!isPointClear(point, heading, obstacles, config, extra, relaxCap)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks that a fitted curve stays clear of every obstacle.
     * <p>
     * Waypoints close to an obstacle are allowed a relaxed margin, so the path can
     * hug a corner it was deliberately routed around.
     *
     * @param curve     curve to test
     * @param obstacles zones to test against
     * @param config    geometry parameters
     * @param waypoints waypoints the curve was fitted through
     * @return {@code true} if the curve stays clear
     */
    private static boolean isCurveClear(PathCurve curve, List<Zone> obstacles, PathConfig config, List<Point> waypoints) {
        List<PathCurve> cubicSegments = curve.toCubicSegments();

        for (int segmentIndex = 0; segmentIndex < cubicSegments.size(); segmentIndex++) {
            PathCurve segment = cubicSegments.get(segmentIndex);

            // The relaxation cap is the tightest limit allowed by either endpoint of
            // this cubic, so a segment only relaxes near a waypoint it actually touches.
            double relaxationCap = Double.MAX_VALUE;

            if (segmentIndex < waypoints.size()) {
                relaxationCap = Math.min(relaxationCap, relaxDistance(waypoints.get(segmentIndex), obstacles, config));
            }

            if (segmentIndex + 1 < waypoints.size()) {
                relaxationCap = Math.min(relaxationCap,
                        relaxDistance(waypoints.get(segmentIndex + 1), obstacles, config));
            }

            List<Point> samplePoints = segment.sample(SMOOTH_SAMPLES);

            for (int i = 0; i < samplePoints.size(); i++) {
                double progress = samplePoints.size() > 1 ? (double) i / (samplePoints.size() - 1) : 0.0;
                double heading = segment.getHeading(progress);

                if (!isPointClear(samplePoints.get(i), heading, obstacles, config, 0.0, relaxationCap)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns the relaxed clearance allowed near a waypoint. Waypoints that already
     * sit inside the nominal margin get a small reduction; all others are unlimited.
     *
     * @param waypoint  waypoint to evaluate
     * @param obstacles zones to measure against
     * @param config    geometry parameters
     * @return the relaxed clearance, or {@link Double#MAX_VALUE} when unlimited
     */
    private static double relaxDistance(Point waypoint, List<Zone> obstacles, PathConfig config) {
        double nominal = worstCaseMargin(config);
        double distance = minSignedDist(waypoint, obstacles);

        return (distance < nominal) ? Math.max(0.0, distance - nominal * 0.2) : Double.MAX_VALUE;
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
                points.add(new Point(center.getX() + radius * Math.cos(angle),
                        center.getY() + radius * Math.sin(angle)));
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

                    points.add(new Point(corner.getX() + offset * Math.cos(theta),
                            corner.getY() + offset * Math.sin(theta)));
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

                points.add(new Point(midpoint.getX() + offset * normal.getX(),
                        midpoint.getY() + offset * normal.getY()));
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
     * Runs Dijkstra's algorithm over a visibility graph of {@code nodes}, where two
     * nodes are connected when the segment between them is clear.
     * <p>
     * The start and end nodes are allowed a relaxed clearance so a route can escape
     * a tight starting or ending position.
     *
     * @param startIdx    index of the start node (normally 0)
     * @param endIdx      index of the end node (normally 1)
     * @param nodes       visibility-graph nodes
     * @param obstacles   zones to test line of sight against
     * @param config      geometry parameters
     * @param extra       additional padding to keep
     * @param startRelax  relaxed clearance allowed at the start
     * @param endRelax    relaxed clearance allowed at the end
     * @return the shortest route of points, or {@code null} if none exists
     */
    private static List<Point> shortestPath(int startIdx, int endIdx, List<Point> nodes, List<Zone> obstacles,
                                            PathConfig config, double extra, double startRelax, double endRelax) {
        int nodeCount = nodes.size();
        double nominal = worstCaseMargin(config) + extra;

        boolean startRelaxed = startRelax < nominal;
        boolean endRelaxed = endRelax < nominal;

        // lineOfSight[i][j] is true when the straight segment i -> j is clear.
        boolean[][] lineOfSight = new boolean[nodeCount][nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            for (int j = i + 1; j < nodeCount; j++) {
                boolean touchStart = (i == startIdx && startRelaxed) || (j == startIdx && startRelaxed);
                boolean touchEnd = (i == endIdx && endRelaxed) || (j == endIdx && endRelaxed);

                double relaxCap = Double.MAX_VALUE;
                if (touchStart) {
                    relaxCap = Math.min(relaxCap, startRelax);
                }
                if (touchEnd) {
                    relaxCap = Math.min(relaxCap, endRelax);
                }

                boolean clear = isSegmentClear(nodes.get(i), nodes.get(j), obstacles, config, extra, relaxCap);
                lineOfSight[i][j] = clear;
                lineOfSight[j][i] = clear;
            }
        }

        double[] distances = new double[nodeCount];
        int[] previousNode = new int[nodeCount];
        boolean[] visited = new boolean[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            distances[i] = Double.MAX_VALUE;
            previousNode[i] = -1;
        }
        distances[startIdx] = 0;

        for (int iteration = 0; iteration < nodeCount; iteration++) {
            // Pick the unvisited node with the smallest tentative distance.
            int currentNode = -1;
            double bestDistance = Double.MAX_VALUE;

            for (int i = 0; i < nodeCount; i++) {
                if (!visited[i] && distances[i] < bestDistance) {
                    bestDistance = distances[i];
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
            double secondHeading = (i == pointCount - 2 && !Double.isNaN(endHeading))
                    ? endHeading
                    : headingAt(route, i + 1);

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
            double segmentHeading = Math.atan2(endPoint.getY() - startPoint.getY(),
                    endPoint.getX() - startPoint.getX());

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
            return Math.atan2(route.get(1).getY() - route.get(0).getY(),
                    route.get(1).getX() - route.get(0).getX());
        }

        if (i == route.size() - 1) {
            return Math.atan2(route.get(i).getY() - route.get(i - 1).getY(),
                    route.get(i).getX() - route.get(i - 1).getX());
        }

        return Math.atan2(route.get(i + 1).getY() - route.get(i - 1).getY(),
                route.get(i + 1).getX() - route.get(i - 1).getX());
    }
}
