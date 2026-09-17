package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderOptimizer {

    private OrderOptimizer() {
    }

    public static List<Waypoint> order(Point startPos, double startHeadingRad, List<Waypoint> targets, BezierConfig config) {
        if (targets.size() <= 1) {
            return new ArrayList<>(targets);
        }
        return targets.size() <= config.getBruteForceOrderLimit()
                ? bruteForce(startPos, startHeadingRad, targets, config)
                : greedy(startPos, startHeadingRad, targets, config);
    }

    public static double pathCost(Point startPos, double startHeadingRad, List<Waypoint> order, double turnCostWeight) {
        double cost = 0;
        Point currentPos = startPos;
        double currentHeading = startHeadingRad;

        for (Waypoint wp : order) {
            double travelBearing = wp.getHeading() != null ? wp.getHeading()
                    : Math.atan2(wp.getY() - currentPos.getY(), wp.getX() - currentPos.getX());

            cost += currentPos.distanceTo(new Point(wp.getX(), wp.getY())) + turnCostWeight * Math.abs(normalizeAngle(travelBearing - currentHeading));

            currentHeading = travelBearing;
            currentPos = new Point(wp.getX(), wp.getY());
        }
        return cost;
    }

    private static List<Waypoint> bruteForce(Point startPos, double startHeading, List<Waypoint> targets, BezierConfig config) {
        List<Waypoint> working = new ArrayList<>(targets);
        final List<Waypoint> bestOrder = new ArrayList<>(targets);
        final double[] minCost = {pathCost(startPos, startHeading, working, config.getTurnCostWeight())};

        permute(working, 0, perm -> {
            double cost = pathCost(startPos, startHeading, perm, config.getTurnCostWeight());
            if (cost < minCost[0]) {
                minCost[0] = cost;
                bestOrder.clear();
                bestOrder.addAll(perm);
            }
        });
        return bestOrder;
    }

    private static List<Waypoint> greedy(Point startPos, double startHeading, List<Waypoint> targets, BezierConfig config) {
        List<Waypoint> remaining = new ArrayList<>(targets);
        List<Waypoint> result = new ArrayList<>();
        Point currentPos = startPos;
        double currentHeading = startHeading;
        double k = config.getTurnCostWeight();

        while (!remaining.isEmpty()) {
            Waypoint bestWp = null;
            double bestCost = Double.MAX_VALUE;
            double bestHeading = 0;

            for (Waypoint wp : remaining) {
                double travelBearing = wp.getHeading() != null ? wp.getHeading()
                        : Math.atan2(wp.getY() - currentPos.getY(), wp.getX() - currentPos.getX());

                double cost = currentPos.distanceTo(new Point(wp.getX(), wp.getY())) + k * Math.abs(normalizeAngle(travelBearing - currentHeading));

                if (cost < bestCost) {
                    bestCost = cost;
                    bestWp = wp;
                    bestHeading = travelBearing;
                }
            }

            currentPos = new Point(bestWp.getX(), bestWp.getY());
            currentHeading = bestHeading;
            result.add(bestWp);
            remaining.remove(bestWp);
        }
        return result;
    }

    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    private interface PermutationVisitor {
        void visit(List<Waypoint> permutation);
    }

    private static void permute(List<Waypoint> arr, int k, PermutationVisitor visitor) {
        if (k == arr.size()) {
            visitor.visit(arr);
            return;
        }
        for (int i = k; i < arr.size(); i++) {
            Collections.swap(arr, k, i);
            permute(arr, k + 1, visitor);
            Collections.swap(arr, k, i);
        }
    }
}
