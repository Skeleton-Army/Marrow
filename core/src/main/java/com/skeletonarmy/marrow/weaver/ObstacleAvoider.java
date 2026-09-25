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
    private static final int SEGMENT_CHECKS = 12;
    private static final int SMOOTH_SAMPLES = 100;
    private static final int MAX_INFLATE_ATTEMPTS = 10;

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

    private static double margin(PathConfig config) {
        return config.getRobotSize() / 2.0 + config.getClearance();
    }

    private static PathCurve routeCurve(PathCurve top, List<Zone> obstacles, PathConfig config) {
        double required = margin(config);
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
        double m = required;
        for (int attempt = 0; attempt < MAX_INFLATE_ATTEMPTS; attempt++) {
            List<Point> waypoints = collectWaypoints(keypoints, obstacles, config, m);
            if (waypoints == null) break;
            if (bestWaypoints == null) bestWaypoints = waypoints;

            PathCurve curve = fitSmooth(waypoints, startHeading, endHeading);
            if (isCurveClear(curve, obstacles, required, waypoints)) {
                return curve;
            }

            m += 0.5;
        }

        return bestWaypoints != null ? buildPath(bestWaypoints, startHeading, endHeading) : top;
    }

    private static List<Point> collectWaypoints(List<Point> keypoints, List<Zone> obstacles, PathConfig config, double m) {
        List<Point> waypoints = new ArrayList<>();
        waypoints.add(keypoints.get(0));
        for (int i = 0; i < keypoints.size() - 1; i++) {
            Point a = keypoints.get(i);
            Point b = keypoints.get(i + 1);
            List<Point> seg = routeSegmentWaypoints(a, b, obstacles, config, m);
            if (seg == null) return null;
            for (int k = 1; k < seg.size(); k++) waypoints.add(seg.get(k));
        }
        return waypoints;
    }

    private static List<Point> routeSegmentWaypoints(Point start, Point end, List<Zone> obstacles, PathConfig config, double m) {
        if (insideObstacle(start, obstacles) || insideObstacle(end, obstacles)) {
            return Arrays.asList(start, end);
        }

        if (isSegmentClear(start, end, obstacles, m)) {
            return Arrays.asList(start, end);
        }

        double startRelax = minSignedDist(start, obstacles);
        double endRelax = minSignedDist(end, obstacles);

        List<Point> nodes = new ArrayList<>();
        nodes.add(start);
        nodes.add(end);
        for (Zone zone : obstacles) {
            nodes.addAll(sampleBoundary(zone, m));
        }

        return shortestPath(0, 1, nodes, obstacles, m, startRelax, endRelax);
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

    private static boolean isPointClear(Point p, List<Zone> obstacles, double margin) {
        for (Zone zone : obstacles) {
            if (signedDist(zone, p) < margin - 1e-6) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSegmentClear(Point a, Point b, List<Zone> obstacles, double margin) {
        for (int i = 0; i <= SEGMENT_CHECKS; i++) {
            double t = (double) i / SEGMENT_CHECKS;
            Point p = new Point(a.getX() + (b.getX() - a.getX()) * t, a.getY() + (b.getY() - a.getY()) * t);
            if (!isPointClear(p, obstacles, margin)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCurveClear(PathCurve curve, List<Zone> obstacles, double margin, List<Point> waypoints) {
        List<PathCurve> segs = curve.toCubicSegments();
        for (int si = 0; si < segs.size(); si++) {
            double m = margin;
            if (si < waypoints.size()) m = Math.min(m, relaxDistance(waypoints.get(si), obstacles, margin));
            if (si + 1 < waypoints.size()) m = Math.min(m, relaxDistance(waypoints.get(si + 1), obstacles, margin));
            for (Point p : segs.get(si).sample(SMOOTH_SAMPLES)) {
                if (!isPointClear(p, obstacles, m)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double relaxDistance(Point waypoint, List<Zone> obstacles, double margin) {
        double d = minSignedDist(waypoint, obstacles);
        return (d < margin) ? Math.max(0.0, d - margin * 0.2) : margin;
    }

    private static double signedDist(Zone zone, Point p) {
        double d = zone.distanceToBoundary(p);
        return zone.contains(p) ? -Math.abs(d) : d;
    }

    private static List<Point> sampleBoundary(Zone zone, double margin) {
        List<Point> pts = new ArrayList<>();
        if (zone instanceof CircleZone) {
            CircleZone c = (CircleZone) zone;
            Point center = c.getPosition();
            double r = (c.getRadius() + margin) / Math.cos(Math.PI / CIRCLE_SAMPLES);
            for (int i = 0; i < CIRCLE_SAMPLES; i++) {
                double a = 2.0 * Math.PI * i / CIRCLE_SAMPLES;
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
                double dot = n1.getX() * n2.getX() + n1.getY() * n2.getY();
                double denom = 1.0 + dot;
                if (denom < 1e-9) {
                    pts.add(new Point(cur.getX() + margin * n1.getX(), cur.getY() + margin * n1.getY()));
                } else {
                    pts.add(new Point(
                            cur.getX() + margin * (n1.getX() + n2.getX()) / denom,
                            cur.getY() + margin * (n1.getY() + n2.getY()) / denom));
                }
            }
            for (int i = 0; i < n; i++) {
                Point a = corners[i], b = corners[(i + 1) % n];
                Point mid = new Point((a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0);
                Point nrm = outwardNormal(a, b, center);
                pts.add(new Point(mid.getX() + margin * nrm.getX(), mid.getY() + margin * nrm.getY()));
            }
        } else if (zone instanceof CompositeZone) {
            for (Zone z : ((CompositeZone) zone).getZones()) {
                pts.addAll(sampleBoundary(z, margin));
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

    private static List<Point> shortestPath(int startIdx, int endIdx, List<Point> nodes, List<Zone> obstacles, double margin, double startRelax, double endRelax) {
        int n = nodes.size();
        boolean startRelaxed = !isPointClear(nodes.get(startIdx), obstacles, margin);
        boolean endRelaxed = !isPointClear(nodes.get(endIdx), obstacles, margin);
        boolean[][] visible = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                boolean touchStart = (i == startIdx && startRelaxed) || (j == startIdx && startRelaxed);
                boolean touchEnd = (i == endIdx && endRelaxed) || (j == endIdx && endRelaxed);
                double m = margin;
                if (touchStart) m = Math.min(m, startRelax);
                if (touchEnd) m = Math.min(m, endRelax);
                visible[i][j] = visible[j][i] = isSegmentClear(nodes.get(i), nodes.get(j), obstacles, m);
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
