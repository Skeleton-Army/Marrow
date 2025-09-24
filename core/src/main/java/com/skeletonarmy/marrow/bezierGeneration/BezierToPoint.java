package com.skeletonarmy.marrow.bezierGeneration;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BezierToPoint {
    public enum BezierType {
        SHORTEST,
        FASTEST,
        WIDEST
    }

    private static final double FIELD_SIZE = 144;

    private static double robotWidth;
    private static double robotHeight;
    private static List<Obstacle> obstacles;

    public static void initialize(double robotWidth, double robotHeight, Obstacle... obstacles) {
        BezierToPoint.robotWidth = robotWidth;
        BezierToPoint.robotHeight = robotHeight;
        BezierToPoint.obstacles = Arrays.asList(obstacles);
    }

    public static Point generateMidPoint(Pose3D begin, Pose3D end) {
        Position beginPose = begin.getPosition();
        Position endPose = end.getPosition();

        Point midPoint = adjustMidpointToAvoid(
                new Point(beginPose.x, beginPose.y),
                new Point(endPose.x, endPose.y),
                begin.getOrientation().getYaw(AngleUnit.DEGREES),
                end.getOrientation().getYaw(AngleUnit.DEGREES),
                obstacles,
                BezierType.SHORTEST
        );

        if (midPoint == null) {
            midPoint = new Point((beginPose.x + endPose.x) / 2, (beginPose.y + endPose.y) / 2);
        }

        return midPoint;
    }

    private static Point[] rotatePolygon(Point[] points, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        Point[] rotated = new Point[points.length];

        for (int i = 0; i < points.length; i++) {
            double x = points[i].x;
            double y = points[i].y;
            rotated[i].x = x * cos - y * sin;
            rotated[i].y = x * sin + y * cos;
        }

        return rotated;
    }

    private static boolean pointInPolygon(Point point, Point[] polygon) {
        int crossings = 0;
        for (int i = 0; i < polygon.length; i++) {
            Point a = polygon[i];
            Point b = polygon[(i + 1) % polygon.length];

            if (((a.y > point.y) != (b.y > point.y)) &&
                    (point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x)) {
                crossings++;
            }
        }
        return (crossings % 2 == 1);
    }

    private static double minDistanceBetweenPolygons(Point[] poly1, Point[] poly2) {
        double minDist = Double.MAX_VALUE;
        for (Point p1 : poly1) {
            for (Point p2 : poly2) {
                double dx = p1.x - p2.x;
                double dy = p1.y - p2.y;
                double dist = Math.hypot(dx, dy);
                if (dist < minDist) minDist = dist;
            }
        }
        return minDist;
    }

    private static boolean isOverlapping(Point center, double angle, List<Obstacle> obstacles) {
        double cx = center.x, cy = center.y;
        double dx = (robotWidth) / 2, dy = (robotHeight) / 2;
        Point[] corners = new Point[] {
                new Point(-dx, -dy),
                new Point(dx, -dy),
                new Point(dx, dy),
                new Point(-dx, dy)
        };

        Point[] rotated = rotatePolygon(corners, angle);
        for (Point point : rotated) {
            point.x += cx;
            point.y += cy;
        }

        for (Obstacle obs : obstacles) {
            for (Point corner : rotated) {
                if (pointInPolygon(corner, obs)) return true;
            }

            if (minDistanceBetweenPolygons(rotated, obs) < -1) return true;
        }

        for (Point corner : rotated) {
            if (corner.x < 0 || corner.x > FIELD_SIZE || corner.y < 0 || corner.y > FIELD_SIZE) return true;
        }

        return false;
    }

    private static Point bezierPoint(Point[] controlPoints, double t) {
        int n = controlPoints.length - 1;
        Point result = new Point(0, 0);
        for (int i = 0; i <= n; i++) {
            double binomial = binomialCoeff(n, i);
            double coeff = binomial * Math.pow(t, i) * Math.pow(1 - t, n - i);
            result.x += coeff * controlPoints[i].x;
            result.y += coeff * controlPoints[i].y;
        }
        return result;
    }

    private static double binomialCoeff(int n, int k) {
        double res = 1;
        for (int i = 0; i < k; ++i) {
            res *= (n - i);
            res /= (i + 1);
        }
        return res;
    }

    private static List<Point> bezierCurve(Point[] controlPoints, int numPoints) {
        List<Point> curve = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            double t = i / (double)(numPoints - 1);
            curve.add(bezierPoint(controlPoints, t));
        }
        return curve;
    }

    private static double computeDynamicTBias(Point start, Point end, List<Obstacle> obstacles) {
        double minDistStart = Double.MAX_VALUE;
        double minDistEnd = Double.MAX_VALUE;

        for (Obstacle obs : obstacles) {
            for (Point vertex : obs) {
                double distToStart = Math.hypot(start.x - vertex.x, start.y - vertex.y);
                double distToEnd = Math.hypot(end.x - vertex.x, end.y - vertex.y);

                if (distToStart < minDistStart) minDistStart = distToStart;
                if (distToEnd < minDistEnd) minDistEnd = distToEnd;
            }
        }

        double total = minDistStart + minDistEnd;
        if (total == 0) return 0.5; // avoid divide by zero

        // Inverse relationship: closer to end = lower tBias
        return minDistStart / total; // Normalized to [0, 1]
    }

    private static Point adjustMidpointToAvoid(
            Point start, Point end,
            double startAngle, double endAngle,
            List<Obstacle> obstacles,
            BezierType type
    ) {
        Point direction = new Point(end.x - start.x, end.y - start.y);
        double mag = Math.hypot(direction.x, direction.y);
        direction.x /= mag;
        direction.y /= mag;

        Point perpendicular = new Point(-direction.y, direction.x);
        // Step 1: Base midpoint shifted along the path direction
        double tBias = computeDynamicTBias(start, end, obstacles);
        System.out.println(tBias);
        Point mid = new Point(
                start.x * (1 - tBias) + end.x * tBias,
                start.y * (1 - tBias) + end.y * tBias
        );


        // Search offsets from center outwards, both left and right
        List<Double> offsetMagnitudes = new ArrayList<>();
        for (int i = 0; i <= 1000; i += 10) {
            offsetMagnitudes.add((double) i);
            if (i != 0) offsetMagnitudes.add((double) -i);
        }

        offsetMagnitudes.sort((a, b) -> Double.compare(Math.abs(a), Math.abs(b)));

        double bestShortest = Double.POSITIVE_INFINITY;
        double bestFastest = Double.POSITIVE_INFINITY;
        double bestClearance = 1;

        Point bestMidShortest = null;
        Point bestMidFastest = null;
        Point bestMidWidest = null;

        List<Point[]> validCandidates = new ArrayList<>();

        for (double offset : offsetMagnitudes) {
            Point testMid = new Point(mid.x + perpendicular.x * offset, mid.y + perpendicular.y * offset);
            Point[] ctrlPts = new Point[]{start, testMid, end};
            List<Point> candidatePath = bezierCurve(ctrlPts, 5000);

            boolean collision = false;

            for (int i = 0; i < candidatePath.size(); i++) {
                Point pt = candidatePath.get(i);
                double angle = startAngle + (endAngle - startAngle) * i / candidatePath.size();
                if (isOverlapping(pt, angle, obstacles)) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                double length = 0;
                double curvaturePenalty = 0;

                for (int i = 1; i < candidatePath.size(); i++) {
                    double dx1 = candidatePath.get(i).x - candidatePath.get(i - 1).x;
                    double dy1 = candidatePath.get(i).y - candidatePath.get(i - 1).y;
                    length += Math.hypot(dx1, dy1);
                    if (i > 1) {
                        double dx0 = candidatePath.get(i - 1).x - candidatePath.get(i - 2).x;
                        double dy0 = candidatePath.get(i - 1).y - candidatePath.get(i - 2).y;
                        double angle1 = Math.atan2(dy0, dx0);
                        double angle2 = Math.atan2(dy1, dx1);
                        double delta = Math.abs(angle2 - angle1);
                        if (delta > Math.PI) delta = 2 * Math.PI - delta;
                        curvaturePenalty += delta;
                    }
                }

                double fastCost = length + 100 * curvaturePenalty;

                // Clearance calculation (optional)
                double minClearance = Double.POSITIVE_INFINITY;
                for (Obstacle obs : obstacles) {
                    Point[] robotCorners = rotatePolygon(new Point[] {
                            new Point(-robotWidth / 2.0, -robotHeight / 2.0),
                            new Point(robotWidth / 2.0, -robotHeight / 2.0),
                            new Point(robotWidth / 2.0, robotHeight / 2.0),
                            new Point(-robotWidth / 2.0, robotHeight / 2.0)
                    }, 0);

                    for (Point robotCorner : robotCorners) {
                        robotCorner.x += testMid.x;
                        robotCorner.y += testMid.y;
                    }

                    double clearance = minDistanceBetweenPolygons(robotCorners, obs);
                    if (clearance < minClearance) minClearance = clearance;
                }

                validCandidates.add(ctrlPts);

                if (length < bestShortest) {
                    bestShortest = length;
                    bestMidShortest = testMid;
                }

                if (fastCost < bestFastest) {
                    bestFastest = fastCost;
                    bestMidFastest = testMid;
                }

                if (minClearance > bestClearance) {
                    bestClearance = minClearance;
                    bestMidWidest = testMid;
                }
            }
        }

        if (validCandidates.isEmpty()) return null;

        switch (type) {
            case FASTEST: return bestMidFastest;
            case SHORTEST: return bestMidShortest;
            case WIDEST: return bestMidWidest;
            default: return null;
        }
    }
}
