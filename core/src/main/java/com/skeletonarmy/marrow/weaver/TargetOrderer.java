package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TargetOrderer {

    private TargetOrderer() {
    }

    public static List<PathPose> order(Point startPos, double startHeadingRad, List<PathPose> targets, PathConfig config) {
        if (targets.size() <= 1) {
            return new ArrayList<>(targets);
        }
        return targets.size() <= config.getBruteForceOrderLimit()
                ? bruteForce(startPos, startHeadingRad, targets, config)
                : greedy(startPos, startHeadingRad, targets, config);
    }

    public static double pathCost(Point startPos, double startHeadingRad, List<PathPose> order, double turnCostWeight) {
        double cost = 0;
        Point currentPos = startPos;
        double currentHeading = startHeadingRad;

        for (PathPose pose : order) {
            double travelBearing = !Double.isNaN(pose.getHeadingRad()) ? pose.getHeadingRad()
                    : Math.atan2(pose.getY() - currentPos.getY(), pose.getX() - currentPos.getX());

            cost += currentPos.distanceTo(new Point(pose.getX(), pose.getY())) + turnCostWeight * Math.abs(normalizeAngle(travelBearing - currentHeading));

            currentHeading = travelBearing;
            currentPos = new Point(pose.getX(), pose.getY());
        }
        return cost;
    }

    private static List<PathPose> bruteForce(Point startPos, double startHeading, List<PathPose> targets, PathConfig config) {
        List<PathPose> working = new ArrayList<>(targets);
        final List<PathPose> bestOrder = new ArrayList<>(targets);
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

    private static List<PathPose> greedy(Point startPos, double startHeading, List<PathPose> targets, PathConfig config) {
        List<PathPose> remaining = new ArrayList<>(targets);
        List<PathPose> result = new ArrayList<>();
        Point currentPos = startPos;
        double currentHeading = startHeading;
        double k = config.getTurnCostWeight();

        while (!remaining.isEmpty()) {
            PathPose bestPose = null;
            double bestCost = Double.MAX_VALUE;
            double bestHeading = 0;

            for (PathPose pose : remaining) {
                double travelBearing = !Double.isNaN(pose.getHeadingRad()) ? pose.getHeadingRad()
                        : Math.atan2(pose.getY() - currentPos.getY(), pose.getX() - currentPos.getX());

                double cost = currentPos.distanceTo(new Point(pose.getX(), pose.getY())) + k * Math.abs(normalizeAngle(travelBearing - currentHeading));

                if (cost < bestCost) {
                    bestCost = cost;
                    bestPose = pose;
                    bestHeading = travelBearing;
                }
            }

            currentPos = new Point(bestPose.getX(), bestPose.getY());
            currentHeading = bestHeading;
            result.add(bestPose);
            remaining.remove(bestPose);
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
        void visit(List<PathPose> permutation);
    }

    private static void permute(List<PathPose> arr, int k, PermutationVisitor visitor) {
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
