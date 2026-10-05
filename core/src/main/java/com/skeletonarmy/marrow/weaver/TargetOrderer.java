package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chooses the order in which targets should be visited.
 * <p>
 * Small target lists are ordered exactly by trying every permutation. Larger
 * lists fall back to a nearest-neighbor greedy pass to stay fast. Both strategies
 * minimize a cost that trades travel distance against turning.
 */
public class TargetOrderer {
    private TargetOrderer() {}

    /**
     * Returns the targets in the recommended visit order.
     *
     * <p>Lists no longer than {@link PathConfig#getBruteForceOrderLimit()} are ordered
     * exhaustively; longer lists use the greedy strategy.
     *
     * @param startPos        robot start position
     * @param startHeadingRad robot start heading in radians
     * @param targets         targets to order
     * @param config          ordering parameters
     * @return a new ordered list
     */
    public static List<Point> order(Point startPos, double startHeadingRad, List<Point> targets, PathConfig config) {
        if (targets.size() <= 1) {
            return new ArrayList<>(targets);
        }

        if (targets.size() <= config.getBruteForceOrderLimit()) {
            return bruteForce(startPos, startHeadingRad, targets, config);
        } else {
            return greedy(startPos, startHeadingRad, targets, config);
        }
    }

    /**
     * Computes the cost of visiting {@code order} from the given start.
     *
     * <p>The cost sums travel distance plus the weighted absolute heading change
     * between consecutive moves. A target with an explicit heading forces the robot
     * to face that heading; otherwise the robot faces its direction of travel.
     *
     * @param startPos        robot start position
     * @param startHeadingRad robot start heading in radians
     * @param order           candidate visit order
     * @param turnCostWeight  weight applied to heading changes
     * @return the total cost; lower is better
     */
    public static double pathCost(Point startPos, double startHeadingRad, List<Point> order, double turnCostWeight) {
        double cost = 0;
        Point currentPos = startPos;
        double currentHeading = startHeadingRad;

        for (Point pose : order) {
            double travelBearing;

            if (!pose.hasHeading()) {
                travelBearing = Math.atan2(pose.getY() - currentPos.getY(), pose.getX() - currentPos.getX());
            } else {
                travelBearing = pose.getHeadingRad();
            }

            Point targetPos = new Point(pose.getX(), pose.getY());
            double travelDistance = currentPos.distanceTo(targetPos);
            double turn = Math.abs(normalizeAngle(travelBearing - currentHeading));

            cost += travelDistance + turnCostWeight * turn;

            currentHeading = travelBearing;
            currentPos = targetPos;
        }

        return cost;
    }

    /**
     * Exhaustively tries every permutation and returns the cheapest order.
     *
     * @param startPos     robot start position
     * @param startHeading robot start heading in radians
     * @param targets      targets to order
     * @param config       ordering parameters
     * @return the cheapest visit order
     */
    private static List<Point> bruteForce(Point startPos, double startHeading, List<Point> targets, PathConfig config) {
        List<Point> working = new ArrayList<>(targets);
        final List<Point> bestOrder = new ArrayList<>(targets);
        final double[] minCost = {pathCost(startPos, startHeading, working, config.getTurnCostWeight())};

        permute(working, 0, permutation -> {
            double cost = pathCost(startPos, startHeading, permutation, config.getTurnCostWeight());

            if (cost < minCost[0]) {
                minCost[0] = cost;
                bestOrder.clear();
                bestOrder.addAll(permutation);
            }
        });

        return bestOrder;
    }

    /**
     * Repeatedly visits the cheapest remaining target.
     *
     * @param startPos     robot start position
     * @param startHeading robot start heading in radians
     * @param targets      targets to order
     * @param config       ordering parameters
     * @return the greedy visit order
     */
    private static List<Point> greedy(Point startPos, double startHeading, List<Point> targets, PathConfig config) {
        List<Point> remaining = new ArrayList<>(targets);
        List<Point> result = new ArrayList<>();

        Point currentPos = startPos;
        double currentHeading = startHeading;
        double turnCostWeight = config.getTurnCostWeight();

        while (!remaining.isEmpty()) {
            Point bestPose = null;
            double bestCost = Double.MAX_VALUE;
            double bestHeading = 0;

            // Pick the remaining target that adds the least cost from here.
            for (Point pose : remaining) {
                double travelBearing;

                if (!pose.hasHeading()) {
                    travelBearing = Math.atan2(pose.getY() - currentPos.getY(), pose.getX() - currentPos.getX());
                } else {
                    travelBearing = pose.getHeadingRad();
                }

                Point targetPos = new Point(pose.getX(), pose.getY());
                double travelDistance = currentPos.distanceTo(targetPos);
                double turn = Math.abs(normalizeAngle(travelBearing - currentHeading));
                double cost = travelDistance + turnCostWeight * turn;

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

    /**
     * Wraps an angle into the range {@code (-pi, pi]}.
     *
     * @param angle angle in radians
     * @return the equivalent angle in {@code (-pi, pi]}
     */
    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }

        while (angle < -Math.PI) {
            angle += 2 * Math.PI;
        }

        return angle;
    }

    /** Callback used by {@link #permute}. */
    private interface PermutationVisitor {
        /**
         * Called once for each complete ordering.
         *
         * @param permutation the complete permutation
         */
        void visit(List<Point> permutation);
    }

    /**
     * Generates every permutation of {@code arr} by swapping elements recursively,
     * calling the visitor for each complete ordering.
     *
     * @param arr     list being permuted in place
     * @param k       index the permutation starts from
     * @param visitor callback invoked for each complete ordering
     */
    private static void permute(List<Point> arr, int k, PermutationVisitor visitor) {
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
