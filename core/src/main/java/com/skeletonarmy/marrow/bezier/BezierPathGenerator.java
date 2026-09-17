package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Generator for Bezier paths with optional obstacle avoidance.
 */
public class BezierPathGenerator {

    private static BezierConfig config = new BezierConfig();

    private BezierPathGenerator() {
    }

    public static void setConfig(BezierConfig newConfig) {
        config = newConfig;
    }

    public static BezierConfig getConfig() {
        return config;
    }

    public static void resetToDefaults() {
        config = new BezierConfig();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Pose startPose;
        private Pose destinationPose;
        private final List<Waypoint> waypoints = new ArrayList<>();
        private final List<Zone> obstacles = new ArrayList<>();
        private boolean reorder = true;

        public Builder start(Pose start) {
            this.startPose = start;
            return this;
        }

        public Builder waypoints(List<Waypoint> targets) {
            this.waypoints.clear();
            this.waypoints.addAll(targets);
            return this;
        }

        public Builder addWaypoint(Waypoint target) {
            this.waypoints.add(target);
            return this;
        }

        public Builder ordered() {
            this.reorder = false;
            return this;
        }

        public Builder to(Pose destination) {
            this.destinationPose = destination;
            return this;
        }

        public Builder obstacles(List<Zone> obstacles) {
            this.obstacles.clear();
            this.obstacles.addAll(obstacles);
            return this;
        }

        public Builder addObstacle(Zone obstacle) {
            this.obstacles.add(obstacle);
            return this;
        }

        public BezierResult generate() {
            if (startPose == null) {
                throw new IllegalStateException("Start Pose is required");
            }

            if (!waypoints.isEmpty()) {
                return generateIntakeResult(startPose, waypoints, obstacles, reorder);
            }

            if (destinationPose != null) {
                return generateAvoidanceResult(
                        new Point(startPose.getX(), startPose.getY()),
                        new Point(destinationPose.getX(), destinationPose.getY()),
                        obstacles
                );
            }

            throw new IllegalStateException("Either waypoints or a destination must be set");
        }
    }

    private static BezierResult generateIntakeResult(Pose start, List<Waypoint> targets, List<Zone> obstacles, boolean reorder) {
        Point startPoint = new Point(start.getX(), start.getY());
        List<Waypoint> ordered = reorder 
                ? OrderOptimizer.order(startPoint, start.getHeadingRad(), targets, config)
                : new ArrayList<>(targets);
        
        List<Point> keyPoints = new ArrayList<>();
        List<Double> headings = new ArrayList<>();
        
        keyPoints.add(startPoint);
        headings.add(start.getHeadingRad());

        double effectiveHalfWidth = config.getWidth() / 2.0;
        double reach = config.getReach();
        Point prevRaw = startPoint;

        for (int i = 0; i < ordered.size(); i++) {
            Waypoint wp = ordered.get(i);
            Point target = new Point(wp.getX(), wp.getY());
            double heading = wp.getHeading() != null ? wp.getHeading()
                    : Math.atan2(target.getY() - prevRaw.getY(), target.getX() - prevRaw.getX());

            Point robotCenter;
            if (effectiveHalfWidth <= 1e-9 || i == 0 || i == ordered.size() - 1) {
                robotCenter = new Point(target.getX() - reach * Math.cos(heading),
                        target.getY() - reach * Math.sin(heading));
            } else {
                Waypoint nextWp = ordered.get(i + 1);
                robotCenter = solveIntakeCapturePoint(target, heading, reach,
                        effectiveHalfWidth, prevRaw, new Point(nextWp.getX(), nextWp.getY()));
            }

            keyPoints.add(robotCenter);
            headings.add(heading);
            prevRaw = target;
        }

        BezierPath path = applyAntiWhip(catmullRomToBezier(keyPoints, headings, config.getTension()), headings.get(headings.size() - 1));
        if (obstacles != null && !obstacles.isEmpty()) {
            path = preBowSegmentsAwayFromObstacles(path, obstacles, config);
            path = CollisionAvoider.avoid(path, obstacles, config);
        }
        return new BezierResult(path, new ArrayList<>(headings.subList(1, headings.size())));
    }

    private static BezierResult generateAvoidanceResult(Point start, Point end, List<Zone> obstacles) {
        int n = Math.max(config.getControlPointCount(), 4);
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double t = (double) i / (n - 1);
            points.add(new Point(start.getX() + (end.getX() - start.getX()) * t, start.getY() + (end.getY() - start.getY()) * t));
        }

        List<Point> biased = biasAwayFromObstacles(points, obstacles, config);
        BezierCurve curve = new BezierCurve(biased);
        BezierPath asPath = new BezierPath(Collections.singletonList(curve));
        BezierPath avoided = CollisionAvoider.avoid(asPath, obstacles, config);
        
        return new BezierResult(avoided, Collections.singletonList(avoided.getHeading(1.0)));
    }

    private static Point solveIntakeCapturePoint(Point target, double heading, double offset, double halfWidth, Point prevRaw, Point nextRaw) {
        double cos = Math.cos(heading), sin = Math.sin(heading);
        Point nominalCenter = new Point(target.getX() - offset * cos, target.getY() - offset * sin);
        double ux = sin, uy = -cos, vx = nextRaw.getX() - prevRaw.getX(), vy = nextRaw.getY() - prevRaw.getY();
        double denom = ux * vy - uy * vx, s = 0;
        if (Math.abs(denom) > 1e-9) s = ((prevRaw.getX() - nominalCenter.getX()) * vy - (prevRaw.getY() - nominalCenter.getY()) * vx) / denom;
        s = Math.max(-halfWidth, Math.min(halfWidth, s));
        return new Point(nominalCenter.getX() + s * ux, nominalCenter.getY() + s * uy);
    }

    public static boolean isCapturedByIntake(Point robotCenter, double headingRad, double intakeOffset, double intakeWidth, Point target) {
        double cos = Math.cos(headingRad), sin = Math.sin(headingRad);
        double intakeCenterX = robotCenter.getX() + intakeOffset * cos, intakeCenterY = robotCenter.getY() + intakeOffset * sin;
        double relX = target.getX() - intakeCenterX, relY = target.getY() - intakeCenterY;
        return Math.abs(relX * cos + relY * sin) < 1e-6 && Math.abs(relX * -sin + relY * cos) <= intakeWidth / 2.0 + 1e-6;
    }

    private static BezierPath preBowSegmentsAwayFromObstacles(BezierPath path, List<Zone> obstacles, BezierConfig config) {
        List<BezierCurve> segments = new ArrayList<>();
        for (BezierCurve segment : path.getSegments()) segments.add(new BezierCurve(biasAwayFromObstacles(segment.getControlPoints(), obstacles, config)));
        return new BezierPath(segments);
    }

    private static List<Point> biasAwayFromObstacles(List<Point> controlPoints, List<Zone> obstacles, BezierConfig config) {
        List<Point> pts = new ArrayList<>(controlPoints);
        if (obstacles == null || obstacles.isEmpty()) return pts;
        Point start = pts.get(0), end = pts.get(pts.size() - 1);
        double dx = end.getX() - start.getX(), dy = end.getY() - start.getY(), len = Math.hypot(dx, dy);
        if (len < 1e-9) return pts;

        double targetClearance = (config.getClearance() + config.getRobotSize() * Math.sqrt(2) / 2.0) * 1.05 + 0.05;
        for (Zone zone : obstacles) {
            Point closest = closestPointOnSegment(start, end, zone.getPosition());
            double dist = closest.distanceTo(zone.getPosition());
            if (zone.contains(closest) || dist < targetClearance) {
                double side = (dx * (zone.getPosition().getY() - start.getY()) - dy * (zone.getPosition().getX() - start.getX())) >= 0 ? -1 : 1;
                double push = Math.max(targetClearance - dist, 0) + targetClearance;
                for (int i = 1; i < pts.size() - 1; i++) {
                    double t = (double) i / (pts.size() - 1);
                    double weight = 1.0 - Math.abs(t - 0.5) * 2;
                    pts.set(i, new Point(pts.get(i).getX() + side * (-dy / len) * push * weight, pts.get(i).getY() + side * (dx / len) * push * weight));
                }
            }
        }
        return pts;
    }

    private static Point closestPointOnSegment(Point a, Point b, Point p) {
        double dx = b.getX() - a.getX(), dy = b.getY() - a.getY(), lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-9) return a;
        double t = Math.max(0, Math.min(1, ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) / lenSq));
        return new Point(a.getX() + t * dx, a.getY() + t * dy);
    }

    public static boolean isPathClear(BezierPath path, List<Zone> obstacles, double clearance, double robotSize, int samplesPerSegment) {
        for (BezierCurve segment : path.getSegments()) if (!isPathClear(segment, obstacles, clearance, robotSize, samplesPerSegment)) return false;
        return true;
    }

    public static boolean isPathClear(BezierCurve curve, List<Zone> obstacles, double clearance, double robotSize, int samples) {
        for (int i = 0; i < samples; i++) {
            Point p = curve.get(samples == 1 ? 0 : (double) i / (samples - 1));
            if (robotSize <= 0) {
                for (Zone zone : obstacles) if (zone.contains(p) || zone.distanceToBoundary(p) < clearance) return false;
            } else {
                PolygonZone footprint = RobotFootprint.asZone(p, curve.getHeading(samples == 1 ? 0 : (double) i / (samples - 1)), robotSize);
                for (Zone zone : obstacles) if (zone.isInside(footprint) || zone.distanceTo(footprint) < clearance) return false;
            }
        }
        return true;
    }

    public static boolean isPathClear(BezierPath path, List<Zone> obstacles, double clearance, int samplesPerSegment) { return isPathClear(path, obstacles, clearance, 0.0, samplesPerSegment); }
    public static boolean isPathClear(BezierCurve curve, List<Zone> obstacles, double clearance, int samples) { return isPathClear(curve, obstacles, clearance, 0.0, samples); }

    private static BezierPath catmullRomToBezier(List<Point> pts, List<Double> headings, double tension) {
        int n = pts.size();
        if (n == 2) return new BezierPath(Collections.singletonList(twoPointCubic(pts.get(0), pts.get(1), headings.get(0), headings.get(1))));
        List<BezierCurve> segments = new ArrayList<>();
        double alpha = 1 - tension;
        for (int i = 0; i < n - 1; i++) {
            Point p0 = pts.get(Math.max(i - 1, 0)), p1 = pts.get(i), p2 = pts.get(i + 1), p3 = pts.get(Math.min(i + 2, n - 1));
            Point t1 = new Point((p2.getX() - p0.getX()) * alpha / 2.0, (p2.getY() - p0.getY()) * alpha / 2.0), t2 = new Point((p3.getX() - p1.getX()) * alpha / 2.0, (p3.getY() - p1.getY()) * alpha / 2.0);
            segments.add(new BezierCurve(Arrays.asList(p1, new Point(p1.getX() + t1.getX() / 3.0, p1.getY() + t1.getY() / 3.0), new Point(p2.getX() - t2.getX() / 3.0, p2.getY() - t2.getY() / 3.0), p2)));
        }
        return new BezierPath(segments);
    }

    private static BezierCurve twoPointCubic(Point start, Point end, double startHeading, double endHeading) {
        double d = start.distanceTo(end) / 3.0;
        return new BezierCurve(Arrays.asList(start, new Point(start.getX() + d * Math.cos(startHeading), start.getY() + d * Math.sin(startHeading)), new Point(end.getX() - d * Math.cos(endHeading), end.getY() - d * Math.sin(endHeading)), end));
    }

    private static BezierPath applyAntiWhip(BezierPath path, double finalHeading) {
        List<BezierCurve> segments = new ArrayList<>(path.getSegments());
        BezierCurve last = segments.get(segments.size() - 1);
        List<Point> cps = new ArrayList<>(last.getControlPoints());
        Point end = cps.get(cps.size() - 1);
        cps.set(cps.size() - 2, new Point(end.getX() - end.distanceTo(cps.get(cps.size() - 2)) * Math.cos(finalHeading), end.getY() - end.distanceTo(cps.get(cps.size() - 2)) * Math.sin(finalHeading)));
        segments.set(segments.size() - 1, new BezierCurve(cps));
        return new BezierPath(segments);
    }
}
