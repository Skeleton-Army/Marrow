package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
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
        Point prevRaw = startPoint;

        for (int i = 0; i < ordered.size(); i++) {
            PathPose pose = ordered.get(i);
            Point target = new Point(pose.getX(), pose.getY());
            double heading = !Double.isNaN(pose.getHeadingRad()) ? pose.getHeadingRad()
                    : Math.atan2(target.getY() - prevRaw.getY(), target.getX() - prevRaw.getX());

            Point robotCenter;
            if (effectiveHalfWidth <= 1e-9 || i == ordered.size() - 1) {
                robotCenter = target;
            } else {
                PathPose nextPose = ordered.get(i + 1);
                robotCenter = solveIntakeCapturePoint(target, heading,
                        effectiveHalfWidth, prevRaw, new Point(nextPose.getX(), nextPose.getY()));
            }

            keyPoints.add(robotCenter);
            headings.add(heading);
            prevRaw = target;
        }

        PathCurve curve = cubicHermiteChain(keyPoints, headings);
        PathRoute path = new PathRoute(Collections.singletonList(curve));
        if (obstacles != null && !obstacles.isEmpty()) {
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

        PathCurve curve = new PathCurve(biased);
        PathRoute asPath = new PathRoute(Collections.singletonList(curve));
        PathRoute avoided = ObstacleAvoider.avoid(asPath, obstacles, config);
        
        return new PathResult(avoided, Collections.singletonList(avoided.getHeading(1.0)));
    }

    private static Point solveIntakeCapturePoint(Point target, double heading, double halfWidth, Point prevRaw, Point nextRaw) {
        double cos = Math.cos(heading), sin = Math.sin(heading);
        double ux = sin, uy = -cos, vx = nextRaw.getX() - prevRaw.getX(), vy = nextRaw.getY() - prevRaw.getY();
        double denom = ux * vy - uy * vx, s = 0;
        if (Math.abs(denom) > 1e-9) s = ((prevRaw.getX() - target.getX()) * vy - (prevRaw.getY() - target.getY()) * vx) / denom;
        s = Math.max(-halfWidth, Math.min(halfWidth, s));
        return new Point(target.getX() + s * ux, target.getY() + s * uy);
    }

    public static boolean isCapturedByIntake(Point robotCenter, double headingRad, double intakeWidth, Point target) {
        double cos = Math.cos(headingRad), sin = Math.sin(headingRad);
        double relX = target.getX() - robotCenter.getX(), relY = target.getY() - robotCenter.getY();
        return Math.abs(relX * cos + relY * sin) < 1e-6 && Math.abs(relX * -sin + relY * cos) <= intakeWidth / 2.0 + 1e-6;
    }

    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, double clearance, double robotWidth, double robotHeight, int samplesPerSegment) {
        for (PathCurve segment : path.getSegments()) if (!isPathClear(segment, obstacles, clearance, robotWidth, robotHeight, samplesPerSegment)) return false;
        return true;
    }

    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, double clearance, double robotWidth, double robotHeight, int samples) {
        for (int i = 0; i < samples; i++) {
            double t = samples == 1 ? 0 : (double) i / (samples - 1);
            Point p = curve.get(t);
            if (robotWidth <= 0 || robotHeight <= 0) {
                for (Zone zone : obstacles) if (zone.contains(p) || zone.distanceToBoundary(p) < clearance) return false;
            } else {
                Zone footprint = RobotFootprint.asZone(p, curve.getHeading(t), robotWidth, robotHeight);
                for (Zone zone : obstacles) if (zone.isInside(footprint) || zone.distanceTo(footprint) < clearance) return false;
            }
        }
        return true;
    }

    public static boolean isPathClear(PathRoute path, List<Zone> obstacles, double clearance, int samplesPerSegment) { return isPathClear(path, obstacles, clearance, 0.0, 0.0, samplesPerSegment); }
    public static boolean isPathClear(PathCurve curve, List<Zone> obstacles, double clearance, int samples) { return isPathClear(curve, obstacles, clearance, 0.0, 0.0, samples); }

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
