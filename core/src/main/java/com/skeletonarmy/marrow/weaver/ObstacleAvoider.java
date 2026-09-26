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

public class ObstacleAvoider {
    private ObstacleAvoider() {}

    private static final int CIRCLE_SAMPLES = 24;
    private static final int CORNER_SAMPLES = 8;
    private static final int SEGMENT_CHECKS = 12;
    private static final int SMOOTH_SAMPLES = 100;
    private static final int MAX_INFLATE_ATTEMPTS = 10;
    private static final double GRADIENT_EPS = 1e-3;
    private static final double SEED_PAD = 0.75;

    public static PathRoute avoid(PathRoute path, List<Zone> obstacles, PathConfig config) {
        if (obstacles == null || obstacles.isEmpty()) {
            return path;
        }

        List<PathCurve> out = new ArrayList<>();
        for (PathCurve top : path.getSegments()) {
            out.add(routeCurve(top, obstacles, config));
        }

        return out.size() == 1
                ? new PathRoute(Collections.singletonList(out.get(0)))
                : new PathRoute(out);
    }

    private static double footprintRadius(double relativeAngle, PathConfig config) {
        double hw = config.getRobotWidth() / 2.0;
        double hh = config.getRobotHeight() / 2.0;
        return hw * Math.abs(Math.cos(relativeAngle)) + hh * Math.abs(Math.sin(relativeAngle));
    }

    private static double worstCaseMargin(PathConfig config) {
        return Math.hypot(config.getRobotWidth(), config.getRobotHeight()) / 2.0 + config.getClearance();
    }

    private static double obstacleDirection(Zone zone, Point p) {
        double gx = (signedDist(zone, new Point(p.getX() + GRADIENT_EPS, p.getY()))
                - signedDist(zone, new Point(p.getX() - GRADIENT_EPS, p.getY()))) / (2 * GRADIENT_EPS);
        double gy = (signedDist(zone, new Point(p.getX(), p.getY() + GRADIENT_EPS))
                - signedDist(zone, new Point(p.getX(), p.getY() - GRADIENT_EPS))) / (2 * GRADIENT_EPS);
        if (Math.abs(gx) < 1e-9 && Math.abs(gy) < 1e-9) {
            return 0.0;
        }
        return Math.atan2(gy, gx);
    }

    private static double requiredMargin(Point p, double heading, Zone zone, PathConfig config, double extra) {
        double relative = obstacleDirection(zone, p) - heading;
        return footprintRadius(relative, config) + config.getClearance() + extra;
    }

    private static PathCurve routeCurve(PathCurve top, List<Zone> obstacles, PathConfig config) {
        double startHeading = top.getHeading(0.0);
        double endHeading = top.getHeading(1.0);
        List<PathCurve> cubics = top.toCubicSegments();

        List<Point> keypoints = new ArrayList<>();
        keypoints.add(cubics.get(0).getControlPoints().get(0));
        for (PathCurve c : cubics) {
            List<Point> cps = c.getControlPoints();
            keypoints.add(cps.get(cps.size() - 1));
        }

        List<Point> bestWaypoints = null;
        double extra = 0.0;
        for (int attempt = 0; attempt < MAX_INFLATE_ATTEMPTS; attempt++) {
            List<Point> waypoints = collectWaypoints(keypoints, obstacles, config, extra);
            if (waypoints == null) break;
            if (bestWaypoints == null) bestWaypoints = waypoints;

            PathCurve curve = fitSmooth(waypoints, startHeading, endHeading);
            if (isCurveClear(curve, obstacles, config, waypoints)) {
                return curve;
            }

            extra += 0.5;
        }

        return bestWaypoints != null ? buildPath(bestWaypoints, startHeading, endHeading) : top;
    }

    private static List<Point> collectWaypoints(List<Point> keypoints, List<Zone> obstacles, PathConfig config, double extra) {
        List<Point> waypoints = new ArrayList<>();
        waypoints.add(keypoints.get(0));
        for (int i = 0; i < keypoints.size() - 1; i++) {
            Point a = keypoints.get(i);
            Point b = keypoints.get(i + 1);
            List<Point> seg = routeSegmentWaypoints(a, b, obstacles, config, extra);
            if (seg == null) return null;
            for (int k = 1; k < seg.size(); k++) waypoints.add(seg.get(k));
        }
        return waypoints;
    }

    private static List<Point> routeSegmentWaypoints(Point start, Point end, List<Zone> obstacles, PathConfig config, double extra) {
        if (insideObstacle(start, obstacles) || insideObstacle(end, obstacles)) {
            return Arrays.asList(start, end);
        }

        if (isSegmentClear(start, end, obstacles, config, extra)) {
            return Arrays.asList(start, end);
        }

        double approxHeading = Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());

        double startRelax = minSignedDist(start, obstacles);
        double endRelax = minSignedDist(end, obstacles);

        List<Point> nodes = new ArrayList<>();
        nodes.add(start);
        nodes.add(end);
        for (Zone zone : obstacles) {
            nodes.addAll(sampleBoundary(zone, approxHeading, config, extra));
        }

        return shortestPath(0, 1, nodes, obstacles, config, extra, startRelax, endRelax);
    }

    private static double minSignedDist(Point p, List<Zone> obstacles) {
        double min = Double.MAX_VALUE;
        for (Zone zone : obstacles) {
            min = Math.min(min, signedDist(zone, p));
        }
        return min;
    }

    private static boolean insideObstacle(Point p, List<Zone> obstacles) {
        for (Zone zone : obstacles) {
            if (zone.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPointClear(Point p, double heading, List<Zone> obstacles, PathConfig config, double extra, double relaxCap) {
        for (Zone zone : obstacles) {
            double req = Math.min(requiredMargin(p, heading, zone, config, extra), relaxCap);
            if (signedDist(zone, p) < req - 1e-6) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSegmentClear(Point a, Point b, List<Zone> obstacles, PathConfig config, double extra) {
        return isSegmentClear(a, b, obstacles, config, extra, Double.MAX_VALUE);
    }

    private static boolean isSegmentClear(Point a, Point b, List<Zone> obstacles, PathConfig config, double extra, double relaxCap) {
        double heading = Math.atan2(b.getY() - a.getY(), b.getX() - a.getX());
        for (int i = 0; i <= SEGMENT_CHECKS; i++) {
            double t = (double) i / SEGMENT_CHECKS;
            Point p = new Point(a.getX() + (b.getX() - a.getX()) * t, a.getY() + (b.getY() - a.getY()) * t);
            if (!isPointClear(p, heading, obstacles, config, extra, relaxCap)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCurveClear(PathCurve curve, List<Zone> obstacles, PathConfig config, List<Point> waypoints) {
        List<PathCurve> segs = curve.toCubicSegments();
        for (int si = 0; si < segs.size(); si++) {
            PathCurve seg = segs.get(si);
            double relaxCap = Double.MAX_VALUE;
            if (si < waypoints.size()) relaxCap = Math.min(relaxCap, relaxDistance(waypoints.get(si), obstacles, config));
            if (si + 1 < waypoints.size()) relaxCap = Math.min(relaxCap, relaxDistance(waypoints.get(si + 1), obstacles, config));

            List<Point> pts = seg.sample(SMOOTH_SAMPLES);
            for (int i = 0; i < pts.size(); i++) {
                double t = pts.size() > 1 ? (double) i / (pts.size() - 1) : 0.0;
                double heading = seg.getHeading(t);
                if (!isPointClear(pts.get(i), heading, obstacles, config, 0.0, relaxCap)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double relaxDistance(Point waypoint, List<Zone> obstacles, PathConfig config) {
        double nominal = worstCaseMargin(config);
        double d = minSignedDist(waypoint, obstacles);
        return (d < nominal) ? Math.max(0.0, d - nominal * 0.2) : Double.MAX_VALUE;
    }

    private static double signedDist(Zone zone, Point p) {
        double d = zone.distanceToBoundary(p);
        return zone.contains(p) ? -Math.abs(d) : d;
    }

    private static List<Point> sampleBoundary(Zone zone, double heading, PathConfig config, double extra) {
        List<Point> pts = new ArrayList<>();
        double clearance = config.getClearance();
        if (zone instanceof CircleZone) {
            CircleZone c = (CircleZone) zone;
            Point center = c.getPosition();
            for (int i = 0; i < CIRCLE_SAMPLES; i++) {
                double a = 2.0 * Math.PI * i / CIRCLE_SAMPLES;
                double offset = footprintRadius(a - heading, config) + clearance + extra + SEED_PAD;
                double r = (c.getRadius() + offset) / Math.cos(Math.PI / CIRCLE_SAMPLES);
                pts.add(new Point(center.getX() + r * Math.cos(a), center.getY() + r * Math.sin(a)));
            }
        } else if (zone instanceof PolygonZone) {
            PolygonZone poly = (PolygonZone) zone;
            Point[] corners = poly.getCorners();
            Point center = poly.getPosition();
            int n = corners.length;
            for (int i = 0; i < n; i++) {
                Point prev = corners[(i - 1 + n) % n];
                Point cur = corners[i];
                Point next = corners[(i + 1) % n];
                Point n1 = outwardNormal(prev, cur, center);
                Point n2 = outwardNormal(cur, next, center);
                double a1 = Math.atan2(n1.getY(), n1.getX());
                double a2 = Math.atan2(n2.getY(), n2.getX());
                double sweep = a2 - a1;
                while (sweep <= 0) sweep += 2 * Math.PI;
                for (int k = 0; k <= CORNER_SAMPLES; k++) {
                    double theta = a1 + sweep * k / CORNER_SAMPLES;
                    double offset = footprintRadius(theta - heading, config) + clearance + extra + SEED_PAD;
                    pts.add(new Point(cur.getX() + offset * Math.cos(theta), cur.getY() + offset * Math.sin(theta)));
                }
            }
            for (int i = 0; i < n; i++) {
                Point a = corners[i], b = corners[(i + 1) % n];
                Point mid = new Point((a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0);
                Point nrm = outwardNormal(a, b, center);
                double normalAngle = Math.atan2(nrm.getY(), nrm.getX());
                double offset = footprintRadius(normalAngle - heading, config) + clearance + extra + SEED_PAD;
                pts.add(new Point(mid.getX() + offset * nrm.getX(), mid.getY() + offset * nrm.getY()));
            }
        } else if (zone instanceof CompositeZone) {
            for (Zone z : ((CompositeZone) zone).getZones()) {
                pts.addAll(sampleBoundary(z, heading, config, extra));
            }
        }
        return pts;
    }

    private static Point outwardNormal(Point a, Point b, Point center) {
        double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
        double len = Math.hypot(dx, dy);
        if (len < 1e-9) return new Point(0, 0);
        double n1x = -dy / len, n1y = dx / len;
        double n2x = dy / len, n2y = -dx / len;
        double mx = (a.getX() + b.getX()) / 2.0, my = (a.getY() + b.getY()) / 2.0;
        double cx = center.getX() - mx, cy = center.getY() - my;
        if (n1x * cx + n1y * cy < 0) return new Point(n1x, n1y);
        return new Point(n2x, n2y);
    }

    private static List<Point> shortestPath(int startIdx, int endIdx, List<Point> nodes, List<Zone> obstacles, PathConfig config, double extra, double startRelax, double endRelax) {
        int n = nodes.size();
        double nominal = worstCaseMargin(config) + extra;
        boolean startRelaxed = startRelax < nominal;
        boolean endRelaxed = endRelax < nominal;
        boolean[][] visible = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                boolean touchStart = (i == startIdx && startRelaxed) || (j == startIdx && startRelaxed);
                boolean touchEnd = (i == endIdx && endRelaxed) || (j == endIdx && endRelaxed);
                double relaxCap = Double.MAX_VALUE;
                if (touchStart) relaxCap = Math.min(relaxCap, startRelax);
                if (touchEnd) relaxCap = Math.min(relaxCap, endRelax);
                visible[i][j] = visible[j][i] = isSegmentClear(nodes.get(i), nodes.get(j), obstacles, config, extra, relaxCap);
            }
        }

        double[] dist = new double[n];
        int[] prev = new int[n];
        boolean[] visited = new boolean[n];
        for (int i = 0; i < n; i++) {
            dist[i] = Double.MAX_VALUE;
            prev[i] = -1;
        }
        dist[startIdx] = 0;

        for (int iter = 0; iter < n; iter++) {
            int u = -1;
            double best = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (!visited[i] && dist[i] < best) {
                    best = dist[i];
                    u = i;
                }
            }
            if (u < 0) break;
            visited[u] = true;
            if (u == endIdx) break;

            for (int v = 0; v < n; v++) {
                if (visited[v] || !visible[u][v]) continue;
                double w = nodes.get(u).distanceTo(nodes.get(v));
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w;
                    prev[v] = u;
                }
            }
        }

        if (prev[endIdx] == -1 && startIdx != endIdx) {
            return null;
        }

        List<Point> route = new ArrayList<>();
        int cur = endIdx;
        while (cur != -1) {
            route.add(nodes.get(cur));
            cur = prev[cur];
        }
        Collections.reverse(route);
        return route;
    }

    private static PathCurve fitSmooth(List<Point> route, double startHeading, double endHeading) {
        int n = route.size();
        List<Point> flat = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            Point p0 = route.get(i);
            Point p1 = route.get(i + 1);
            double d = p0.distanceTo(p1) / 3.0;
            double h0 = (i == 0 && !Double.isNaN(startHeading)) ? startHeading : headingAt(route, i);
            double h1 = (i == n - 2 && !Double.isNaN(endHeading)) ? endHeading : headingAt(route, i + 1);
            if (i == 0) flat.add(p0);
            flat.add(new Point(p0.getX() + d * Math.cos(h0), p0.getY() + d * Math.sin(h0)));
            flat.add(new Point(p1.getX() - d * Math.cos(h1), p1.getY() - d * Math.sin(h1)));
            flat.add(p1);
        }
        return new PathCurve(flat);
    }

    private static PathCurve buildPath(List<Point> route, double startHeading, double endHeading) {
        int n = route.size();
        List<Point> flat = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            Point p0 = route.get(i);
            Point p1 = route.get(i + 1);
            double d = p0.distanceTo(p1) / 3.0;
            double segHeading = Math.atan2(p1.getY() - p0.getY(), p1.getX() - p0.getX());
            double h0 = (i == 0 && !Double.isNaN(startHeading)) ? startHeading : segHeading;
            double h1 = (i == n - 2 && !Double.isNaN(endHeading)) ? endHeading : segHeading;
            if (i == 0) flat.add(p0);
            flat.add(new Point(p0.getX() + d * Math.cos(h0), p0.getY() + d * Math.sin(h0)));
            flat.add(new Point(p1.getX() - d * Math.cos(h1), p1.getY() - d * Math.sin(h1)));
            flat.add(p1);
        }
        return new PathCurve(flat);
    }

    private static double headingAt(List<Point> route, int i) {
        if (i == 0) {
            return Math.atan2(route.get(1).getY() - route.get(0).getY(), route.get(1).getX() - route.get(0).getX());
        }
        if (i == route.size() - 1) {
            return Math.atan2(route.get(i).getY() - route.get(i - 1).getY(), route.get(i).getX() - route.get(i - 1).getX());
        }
        return Math.atan2(route.get(i + 1).getY() - route.get(i - 1).getY(), route.get(i + 1).getX() - route.get(i - 1).getX());
    }
}
