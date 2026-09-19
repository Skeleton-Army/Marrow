package com.skeletonarmy.marrow.weaver;

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
public class Weaver {

    private static PathConfig config = new PathConfig();

    private Weaver() {
    }

    public static void setConfig(PathConfig newConfig) {
        config = newConfig;
    }

    public static PathConfig getConfig() {
        return config;
    }

    public static void resetToDefaults() {
        config = new PathConfig();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PathPose startPose;
        private PathPose destinationPose;
        private final List<PathPose> targets = new ArrayList<>();
        private final List<Zone> obstacles = new ArrayList<>();
        private boolean reorder = true;

        public Builder start(PathPose start) {
            this.startPose = start;
            return this;
        }

        public Builder targets(List<PathPose> targets) {
            this.targets.clear();
            this.targets.addAll(targets);
            return this;
        }

        public Builder addTarget(PathPose target) {
            this.targets.add(target);
            return this;
        }

        public Builder ordered() {
            this.reorder = false;
            return this;
        }

        public Builder end(PathPose destination) {
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

        public PathResult generate() {
            if (startPose == null) {
                throw new IllegalStateException("Start PathPose is required");
            }

            if (!targets.isEmpty()) {
                return generateIntakeResult(startPose, targets, obstacles, reorder);
            }

            if (destinationPose != null) {
                return generateAvoidanceResult(
                        new Point(startPose.getX(), startPose.getY()),
                        new Point(destinationPose.getX(), destinationPose.getY()),
                        obstacles
                );
            }

            throw new IllegalStateException("Either targets or a destination must be set");
        }
    }

    private static PathResult generateIntakeResult(PathPose start, List<PathPose> targets, List<Zone> obstacles, boolean reorder) {
        Point startPoint = new Point(start.getX(), start.getY());
        List<PathPose> ordered = reorder 
                ? TargetOrderer.order(startPoint, start.getHeadingRad(), targets, config)
                : new ArrayList<>(targets);
        
        List<Point> keyPoints = new ArrayList<>();
        List<Double> headings = new ArrayList<>();
        
        keyPoints.add(startPoint);
        headings.add(start.getHeadingRad());

        double effectiveHalfWidth = config.getWidth() / 2.0;
        double reach = config.getReach();
        Point prevRaw = startPoint;

        for (int i = 0; i < ordered.size(); i++) {
            PathPose pose = ordered.get(i);
            Point target = new Point(pose.getX(), pose.getY());
            double heading = !Double.isNaN(pose.getHeadingRad()) ? pose.getHeadingRad()
                    : Math.atan2(target.getY() - prevRaw.getY(), target.getX() - prevRaw.getX());

            Point robotCenter;
            if (effectiveHalfWidth <= 1e-9 || i == 0 || i == ordered.size() - 1) {
                robotCenter = new Point(target.getX() - reach * Math.cos(heading),
                        target.getY() - reach * Math.sin(heading));
            } else {
                PathPose nextPose = ordered.get(i + 1);
                robotCenter = solveIntakeCapturePoint(target, heading, reach,
                        effectiveHalfWidth, prevRaw, new Point(nextPose.getX(), nextPose.getY()));
            }

            keyPoints.add(robotCenter);
            headings.add(heading);
            prevRaw = target;
        }

        PathCurve curve = cubicHermiteChain(keyPoints, headings);
        PathRoute path = new PathRoute(Collections.singletonList(curve));
        if (obstacles != null && !obstacles.isEmpty()) {
            path = preBowSegmentsAwayFromObstacles(path, obstacles, config);
            path = ObstacleAvoider.avoid(path, obstacles, config);
        }
        return new PathResult(path, Collections.singletonList(headings.get(headings.size() - 1)));
    }

    private static PathResult generateAvoidanceResult(Point start, Point end, List<Zone> obstacles) {
        int n = Math.max(config.getControlPointCount(), 4);
        List<Point> biased = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double t = (double) i / (n - 1);
            biased.add(new Point(start.getX() + (end.getX() - start.getX()) * t, start.getY() + (end.getY() - start.getY()) * t));
        }

        PathCurve curve = biasAwayFromObstacles(new PathCurve(biased), obstacles, config);
        PathRoute asPath = new PathRoute(Collections.singletonList(curve));
        PathRoute avoided = ObstacleAvoider.avoid(asPath, obstacles, config);
        
        return new PathResult(avoided, Collections.singletonList(avoided.getHeading(1.0)));
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

    private static PathRoute preBowSegmentsAwayFromObstacles(PathRoute path, List<Zone> obstacles, PathConfig config) {
        List<PathCurve> segments = new ArrayList<>();
        for (PathCurve segment : path.getSegments()) segments.add(biasAwayFromObstacles(segment, obstacles, config));
        return new PathRoute(segments);
    }

    private static PathCurve biasAwayFromObstacles(PathCurve curve, List<Zone> obstacles, PathConfig config) {
        if (obstacles == null || obstacles.isEmpty()) return curve;
        List<PathCurve> segments = new ArrayList<>();
        for (PathCurve seg : curve.toCubicSegments()) {
            segments.add(new PathCurve(biasControlPoints(seg.getControlPoints(), obstacles, config)));
        }
        return PathCurve.fromCubicSegments(segments);
    }

    private static List<Point> biasControlPoints(List<Point> controlPoints, List<Zone> obstacles, PathConfig config) {
        List<Point> pts = new ArrayList<>(controlPoints);
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

    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, double clearance, double robotSize, int samplesPerSegment) {
        for (PathCurve segment : path.getSegments()) if (!isPathClear(segment, obstacles, clearance, robotSize, samplesPerSegment)) return false;
        return true;
    }

    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, double clearance, double robotSize, int samples) {
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

    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, double clearance, int samplesPerSegment) { return isPathClear(path, obstacles, clearance, 0.0, samplesPerSegment); }
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, double clearance, int samples) { return isPathClear(curve, obstacles, clearance, 0.0, samples); }

    private static PathCurve twoPointCubic(Point start, Point end, double startHeading, double endHeading) {
        double d = start.distanceTo(end) / 3.0;
        return new PathCurve(Arrays.asList(
                start,
                new Point(start.getX() + d * Math.cos(startHeading), start.getY() + d * Math.sin(startHeading)),
                new Point(end.getX() - d * Math.cos(endHeading), end.getY() - d * Math.sin(endHeading)),
                end));
    }

    private static PathCurve cubicHermiteChain(List<Point> pts, List<Double> headings) {
        int n = pts.size();
        if (n == 2) {
            return twoPointCubic(pts.get(0), pts.get(1), headings.get(0), headings.get(1));
        }

        List<Point> flat = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            Point p0 = pts.get(i);
            Point p1 = pts.get(i + 1);
            double d = p0.distanceTo(p1) / 3.0;
            double h0 = headings.get(i);
            double h1 = headings.get(i + 1);
            Point cp1 = new Point(p0.getX() + d * Math.cos(h0), p0.getY() + d * Math.sin(h0));
            Point cp2 = new Point(p1.getX() - d * Math.cos(h1), p1.getY() - d * Math.sin(h1));
            if (i == 0) flat.add(p0);
            flat.add(cp1);
            flat.add(cp2);
            flat.add(p1);
        }
        return new PathCurve(flat);
    }
}
