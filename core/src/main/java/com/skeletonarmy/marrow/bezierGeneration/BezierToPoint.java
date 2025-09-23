package com.skeletonarmy.marrow.bezierGeneration;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;
import org.firstinspires.ftc.robotcore.external.navigation.Position;

import java.util.ArrayList;
import java.util.List;

public class BezierToPoint {

    public Position beginPose;
    public Position endPose;

    public Point midPoint;

    public Telemetry telemetry;
    public static boolean useTelemetry;
    public static List<Point> path;
    public static Point testMid;
    static double width;
    static double height;
    static final double fieldSize = 144;
    public BezierToPoint(Pose3D begin, Pose3D end, double width, double height , List<double[][]> obstacles, boolean useTelemetry , Telemetry telemetry) {
        this.beginPose = begin.getPosition();
        this.endPose = end.getPosition();
        this.width = width;
        this.height = height;

        // starting mid point
        midPoint = new Point((beginPose.x + endPose.x) / 2, (beginPose.y + endPose.y) / 2);

        midPoint = adjustMidpointToAvoid(
                new Point(beginPose.x, beginPose.y),
                new Point(endPose.x, endPose.y),
                begin.getOrientation().getYaw(AngleUnit.DEGREES),
                end.getOrientation().getYaw(AngleUnit.DEGREES),
                obstacles,
                "shortest", // <-- try "fastest", "shortest", or "widest"
                5,
                telemetry,
                "right"
        );


        if (midPoint == null) {
            midPoint = new Point((beginPose.x + endPose.x) / 2, (beginPose.y + endPose.y) / 2);
        }

        BezierToPoint.useTelemetry = useTelemetry;
        path = bezierCurve(new Point[]{new Point(beginPose.x, beginPose.y), midPoint, new Point(endPose.x, endPose.y)}, 1000);

        if (useTelemetry && telemetry != null) {
            telemetry.addData("Final Midpoint", "x: %.2f, y: %.2f", midPoint.x, midPoint.y);
            telemetry.addData("Path Points", path.size());
            telemetry.update();
        }
    }

    public static List<Point> getPath() {
        return path;
    }

    public static class Point {
        public double x, y;
        public Point(double x, double y) { this.x = x; this.y = y; }

        public double distanceTo(Point other) {
            return Math.hypot(this.x - other.x, this.y - other.y);
        }
    }

    public static double[][] rotatePolygon(double[][] points, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double[][] rotated = new double[points.length][2];

        for (int i = 0; i < points.length; i++) {
            double x = points[i][0];
            double y = points[i][1];
            rotated[i][0] = x * cos - y * sin;
            rotated[i][1] = x * sin + y * cos;
        }

        return rotated;
    }

    public static boolean pointInPolygon(double[] point, double[][] polygon) {
        int crossings = 0;
        for (int i = 0; i < polygon.length; i++) {
            double[] a = polygon[i];
            double[] b = polygon[(i + 1) % polygon.length];

            if (((a[1] > point[1]) != (b[1] > point[1])) &&
                    (point[0] < (b[0] - a[0]) * (point[1] - a[1]) / (b[1] - a[1]) + a[0])) {
                crossings++;
            }
        }
        return (crossings % 2 == 1);
    }

    public static double minDistanceBetweenPolygons(double[][] poly1, double[][] poly2) {
        double minDist = Double.MAX_VALUE;
        for (double[] p1 : poly1) {
            for (double[] p2 : poly2) {
                double dx = p1[0] - p2[0];
                double dy = p1[1] - p2[1];
                double dist = Math.hypot(dx, dy);
                if (dist < minDist) minDist = dist;
            }
        }
        return minDist;
    }

    public static boolean isOverlapping(Point center, double angle, List<double[][]> obstacles) {
        double cx = center.x, cy = center.y;
        double dx = (width) / 2, dy = (height) / 2;
        double[][] corners = new double[][] {
                {-dx, -dy}, {dx, -dy}, {dx, dy}, {-dx, dy}
        };

        double[][] rotated = rotatePolygon(corners, angle);
        for (int i = 0; i < rotated.length; i++) {
            rotated[i][0] += cx;
            rotated[i][1] += cy;
        }

        for (double[][] obs : obstacles) {
            for (double[] corner : rotated) {
                if (pointInPolygon(corner, obs)) return true;
            }

            if (minDistanceBetweenPolygons(rotated, obs) < -1) return true;
        }

        for (double[] corner : rotated) {
            if (corner[0] < 0 || corner[0] > fieldSize || corner[1] < 0 || corner[1] > fieldSize) return true;
        }

        return false;
    }

    public static Point bezierPoint(Point[] controlPoints, double t) {
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

    public static double binomialCoeff(int n, int k) {
        double res = 1;
        for (int i = 0; i < k; ++i) {
            res *= (n - i);
            res /= (i + 1);
        }
        return res;
    }

    public static List<Point> bezierCurve(Point[] controlPoints, int numPoints) {
        List<Point> curve = new ArrayList<>();
        for (int i = 0; i < numPoints; i++) {
            double t = i / (double)(numPoints - 1);
            curve.add(bezierPoint(controlPoints, t));
        }
        return curve;
    }

    public static double computeDynamicTBias(Point start, Point end, List<double[][]> obstacles) {
        double minDistStart = Double.MAX_VALUE;
        double minDistEnd = Double.MAX_VALUE;

        for (double[][] obs : obstacles) {
            for (double[] vertex : obs) {
                double distToStart = Math.hypot(start.x - vertex[0], start.y - vertex[1]);
                double distToEnd = Math.hypot(end.x - vertex[0], end.y - vertex[1]);

                if (distToStart < minDistStart) minDistStart = distToStart;
                if (distToEnd < minDistEnd) minDistEnd = distToEnd;
            }
        }

        double total = minDistStart + minDistEnd;
        if (total == 0) return 0.5; // avoid divide by zero

        // Inverse relationship: closer to end = lower tBias
        return minDistStart / total; // Normalized to [0, 1]
    }

    public static Point adjustMidpointToAvoid(
            Point start, Point end,
            double startAngle, double endAngle,
            List<double[][]> obstacles,
            String preference,
            int topN,
            Telemetry telemetry,
            String preferredSide  // You can keep this in signature, but it's no longer used
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
            testMid = new Point(mid.x + perpendicular.x * offset, mid.y + perpendicular.y * offset);
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
                for (double[][] obs : obstacles) {
                    double[][] robotCorners = rotatePolygon(new double[][]{
                            {-width / 2.0, -height / 2.0},
                            {width / 2.0, -height / 2.0},
                            {width / 2.0, height / 2.0},
                            {-width / 2.0, height / 2.0}
                    }, 0);

                    for (int j = 0; j < robotCorners.length; j++) {
                        robotCorners[j][0] += testMid.x;
                        robotCorners[j][1] += testMid.y;
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

        // Telemetry output
        if (useTelemetry && telemetry != null) {
            telemetry.addData("Checked Candidates", offsetMagnitudes.size());
            telemetry.addData("Valid Midpoints", validCandidates.size());

            for (int i = 0; i < Math.min(topN, validCandidates.size()); i++) {
                Point midpt = validCandidates.get(i)[1];
                telemetry.addData("Midpoint #" + (i + 1), "x: %.2f, y: %.2f", midpt.x, midpt.y);
            }

            if (validCandidates.isEmpty()) {
                telemetry.addLine("No valid path candidates found!");
            }

            telemetry.update();
        }

        if (validCandidates.isEmpty()) return null;

        // Return based on selected preference
        if ("fastest".equals(preference) && bestMidFastest != null) return bestMidFastest;
        if ("shortest".equals(preference) && bestMidShortest != null) return bestMidShortest;
        if ("widest".equals(preference) && bestMidWidest != null) return bestMidWidest;

        // Fallback
        if (bestMidFastest != null) return bestMidFastest;
        if (bestMidShortest != null) return bestMidShortest;
        if (bestMidWidest != null) return bestMidWidest;

        return null;
    }


}

