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
            cost += stepCost(currentPos, currentHeading, pose, turnCostWeight);
            currentHeading = arrivalHeading(currentPos, pose);
            currentPos = new Point(pose.getX(), pose.getY());
        }

        return cost;
    }

    /**
     * Cost of moving from a pose at {@code fromHeading} to {@code target}: travel
     * distance plus the weighted absolute heading change.
     *
     * @param fromPos          point the robot travels from
     * @param fromHeading      robot heading before the move, in radians
     * @param target           target being approached
     * @param turnCostWeight   weight applied to the heading change
     * @return the move cost; lower is better
     */
    private static double stepCost(Point fromPos, double fromHeading, Point target, double turnCostWeight) {
        double arrivalBearing = arrivalHeading(fromPos, target);
        double travelDistance = fromPos.distanceTo(new Point(target.getX(), target.getY()));
        double turn = Math.abs(normalizeAngle(arrivalBearing - fromHeading));

        return travelDistance + turnCostWeight * turn;
    }

    /**
     * Heading the robot holds on arrival at {@code target}: the target's explicit
     * heading when present, otherwise the direction of travel.
     *
     * @param fromPos point the robot travels from
     * @param target  target being approached
     * @return the arrival heading in radians
     */
    private static double arrivalHeading(Point fromPos, Point target) {
        if (target.hasHeading()) {
            return target.getHeadingRad();
        }

        return Math.atan2(target.getY() - fromPos.getY(), target.getX() - fromPos.getX());
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
     * Decides whether the robot may travel directly from one target to another.
     * Used by {@link #select} to avoid or drop unreachable targets.
     */
    public interface ReachabilityCheck {
        /**
         * @param from   point the robot travels from
         * @param target target the robot travels to
         * @return {@code true} if the move is allowed
         */
        boolean canReach(Point from, Point target);
    }

    /**
     * Selects and orders at most {@code maxTargets} of {@code targets}.
     * <p>
     * The largest reachable set wins first; among equal-sized sets the cheapest path
     * wins. Reachability is evaluated per move through {@code reachability}, so a
     * target blocked from one approach can still be chosen when reached from another.
     * Lists no longer than {@link PathConfig#getBruteForceOrderLimit()} are searched
     * exhaustively; longer lists use the greedy strategy.
     *
     * @param startPos        robot start position
     * @param startHeadingRad robot start heading in radians
     * @param targets         candidate targets
     * @param maxTargets      maximum number of targets to select
     * @param config          ordering parameters
     * @param reachability    per-move reachability test
     * @return the chosen targets in visit order
     */
    public static List<Point> select(
            Point startPos,
            double startHeadingRad,
            List<Point> targets,
            int maxTargets,
            PathConfig config,
            ReachabilityCheck reachability) {
        int limit = Math.min(maxTargets, targets.size());

        if (limit <= 0) {
            return new ArrayList<>();
        }

        if (targets.size() <= config.getBruteForceOrderLimit()) {
            return selectBruteForce(startPos, startHeadingRad, targets, limit, config, reachability);
        }

        return selectGreedy(startPos, startHeadingRad, targets, limit, config, reachability);
    }

    /**
     * Exhaustively searches every reachable ordered subset, keeping the longest and
     * cheapest one found.
     *
     * @param startPos        robot start position
     * @param startHeadingRad robot start heading in radians
     * @param targets         candidate targets
     * @param limit           maximum number of targets to select
     * @param config          ordering parameters
     * @param reachability    per-move reachability test
     * @return the chosen targets in visit order
     */
    private static List<Point> selectBruteForce(
            Point startPos,
            double startHeadingRad,
            List<Point> targets,
            int limit,
            PathConfig config,
            ReachabilityCheck reachability) {
        List<Point> best = new ArrayList<>();
        double[] bestCost = {Double.MAX_VALUE};

        search(
                startPos,
                startHeadingRad,
                targets,
                limit,
                config.getTurnCostWeight(),
                reachability,
                new boolean[targets.size()],
                new ArrayList<>(),
                0.0,
                best,
                bestCost);

        return best;
    }

    /**
     * Depth-first search over reachable ordered subsets, pruned when a partial order
     * can no longer beat the current best.
     *
     * @param currentPos     point the robot is currently at
     * @param currentHeading robot heading at {@code currentPos}
     * @param targets        candidate targets
     * @param limit          maximum number of targets to select
     * @param turnCostWeight weight applied to heading changes
     * @param reachability   per-move reachability test
     * @param used           flags for targets already in {@code current}
     * @param current        targets chosen so far, in order
     * @param cost           accumulated cost of {@code current}
     * @param best           best ordered subset found so far
     * @param bestCost       accumulated cost of {@code best}
     */
    private static void search(
            Point currentPos,
            double currentHeading,
            List<Point> targets,
            int limit,
            double turnCostWeight,
            ReachabilityCheck reachability,
            boolean[] used,
            List<Point> current,
            double cost,
            List<Point> best,
            double[] bestCost) {
        // A longer chain always wins; equal-length chains are compared by cost.
        if (current.size() > best.size() || (current.size() == best.size() && cost < bestCost[0])) {
            best.clear();
            best.addAll(current);
            bestCost[0] = cost;
        }

        if (current.size() >= limit) {
            return;
        }

        // Once the best chain has the maximum possible length, any partial order that
        // already costs at least as much cannot win, since cost only grows.
        if (limit <= best.size() && cost >= bestCost[0]) {
            return;
        }

        for (int i = 0; i < targets.size(); i++) {
            if (used[i]) {
                continue;
            }

            Point candidate = targets.get(i);

            if (!reachability.canReach(currentPos, candidate)) {
                continue;
            }

            used[i] = true;
            current.add(candidate);
            search(
                    new Point(candidate.getX(), candidate.getY()),
                    arrivalHeading(currentPos, candidate),
                    targets,
                    limit,
                    turnCostWeight,
                    reachability,
                    used,
                    current,
                    cost + stepCost(currentPos, currentHeading, candidate, turnCostWeight),
                    best,
                    bestCost);
            current.remove(current.size() - 1);
            used[i] = false;
        }
    }

    /**
     * Repeatedly picks the cheapest reachable target until {@code limit} are chosen or
     * none remain reachable.
     *
     * @param startPos        robot start position
     * @param startHeadingRad robot start heading in radians
     * @param targets         candidate targets
     * @param limit           maximum number of targets to select
     * @param config          ordering parameters
     * @param reachability    per-move reachability test
     * @return the chosen targets in visit order
     */
    private static List<Point> selectGreedy(
            Point startPos,
            double startHeadingRad,
            List<Point> targets,
            int limit,
            PathConfig config,
            ReachabilityCheck reachability) {
        List<Point> remaining = new ArrayList<>(targets);
        List<Point> result = new ArrayList<>();

        Point currentPos = startPos;
        double currentHeading = startHeadingRad;

        while (result.size() < limit && !remaining.isEmpty()) {
            Point bestTarget = null;
            double bestStepCost = Double.MAX_VALUE;

            for (Point candidate : remaining) {
                if (!reachability.canReach(currentPos, candidate)) {
                    continue;
                }

                double candidateCost = stepCost(currentPos, currentHeading, candidate, config.getTurnCostWeight());

                if (candidateCost < bestStepCost) {
                    bestStepCost = candidateCost;
                    bestTarget = candidate;
                }
            }

            if (bestTarget == null) {
                break;
            }

            currentHeading = arrivalHeading(currentPos, bestTarget);
            currentPos = new Point(bestTarget.getX(), bestTarget.getY());
            result.add(bestTarget);
            remaining.remove(bestTarget);
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
