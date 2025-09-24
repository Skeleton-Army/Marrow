package com.skeletonarmy.marrow.bezierGeneration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    public static Point generateMidPoint(Pose begin, Pose end) {
        Point midPoint = adjustMidpointToAvoid(
                begin.getPosition(),
                end.getPosition(),
                begin.getHeadingRad(),
                end.getHeadingRad(),
                obstacles,
                BezierType.SHORTEST
        );

        if (midPoint == null) {
            midPoint = new Point((begin.x + end.x) / 2, (begin.y + end.y) / 2);
        }

        return midPoint;
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

    private static boolean isRobotOverlapping(Point robotCenter, double robotAngleRad, List<Obstacle> obstacles) {
        Point[] robotCorners = createRobotPolygon(robotCenter, robotAngleRad);

        for (Obstacle obs : obstacles) {
            if (obs instanceof PolygonObstacle) {
                PolygonObstacle polyObs = (PolygonObstacle) obs;
                for (Point corner : robotCorners) {
                    if (polyObs.isOverlapping(corner)) return true;
                }

                // Check if the two polygons are overlapping by checking the distance between them
                if (minDistanceBetweenPolygons(robotCorners, polyObs.corners) < -1) return true;
            } else if (obs instanceof CircleObstacle) {
                CircleObstacle circleObs = (CircleObstacle) obs;
                for (Point corner : robotCorners) {
                    if (circleObs.isOverlapping(corner)) return true;
                }
            }
        }

        // Check if the robot is outside the field boundary
        for (Point corner : robotCorners) {
            if (corner.x < 0 || corner.x > FIELD_SIZE || corner.y < 0 || corner.y > FIELD_SIZE) return true;
        }

        return false;
    }

    private static Point[] createRobotPolygon(Point center, double angleRad) {
        double halfWidth = robotWidth / 2.0;
        double halfHeight = robotHeight / 2.0;

        Point[] corners = new Point[]{
                new Point(-halfWidth, -halfHeight),
                new Point(halfWidth, -halfHeight),
                new Point(halfWidth, halfHeight),
                new Point(-halfWidth, halfHeight)
        };

        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);

        for (Point point : corners) {
            double x = point.x;
            double y = point.y;
            point.x = x * cos - y * sin + center.x;
            point.y = x * sin + y * cos + center.y;
        }

        return corners;
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
            if (obs instanceof PolygonObstacle) {
                PolygonObstacle polyObs = (PolygonObstacle) obs;
                for (Point vertex : polyObs.corners) {
                    double distToStart = start.distanceTo(vertex);
                    double distToEnd = end.distanceTo(vertex);
                    if (distToStart < minDistStart) minDistStart = distToStart;
                    if (distToEnd < minDistEnd) minDistEnd = distToEnd;
                }
            } else if (obs instanceof CircleObstacle) {
                CircleObstacle circleObs = (CircleObstacle) obs;
                double distToStart = start.distanceTo(circleObs.center) - circleObs.radius;
                double distToEnd = end.distanceTo(circleObs.center) - circleObs.radius;
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
            double startAngleRad, double endAngleRad,
            List<Obstacle> obstacles,
            BezierType type
    ) {
        Point direction = new Point(end.x - start.x, end.y - start.y);
        double mag = Math.hypot(direction.x, direction.y);
        direction.x /= mag;
        direction.y /= mag;

        Point perpendicular = new Point(-direction.y, direction.x);

        // Base midpoint shifted along the path direction
        double tBias = computeDynamicTBias(start, end, obstacles);
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

        offsetMagnitudes.sort(Comparator.comparingDouble(Math::abs));

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
                double angleRad = startAngleRad + (endAngleRad - startAngleRad) * i / candidatePath.size();
                if (isRobotOverlapping(pt, angleRad, obstacles)) {
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
                double minClearance = calculateMinClearance(testMid, obstacles);

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

    private static double calculateMinClearance(Point testMid, List<Obstacle> obstacles) {
        double minClearance = Double.POSITIVE_INFINITY;
        Point[] robotCorners = createRobotPolygon(testMid, 0);

        for (Obstacle obs : obstacles) {
            if (obs instanceof PolygonObstacle) {
                double clearance = minDistanceBetweenPolygons(robotCorners, ((PolygonObstacle) obs).corners);
                if (clearance < minClearance) minClearance = clearance;
            } else if (obs instanceof CircleObstacle) {
                double clearance = testMid.distanceTo(((CircleObstacle) obs).center) - ((CircleObstacle) obs).radius;
                if (clearance < minClearance) minClearance = clearance;
            }
        }
        return minClearance;
    }
}