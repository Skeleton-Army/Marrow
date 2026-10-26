package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chooses the order in which targets are visited.
 *
 * <p>Small lists are ordered exactly; larger lists use a nearest-neighbor pass
 * improved by 2-opt. Both minimize travel distance weighted against turning.
 */
public class TargetOrderer {
    /** Number of 2-opt sweeps run over a greedy order. */
    private static final int TWO_OPT_PASSES = 8;

    /** Largest order the 2-opt sweeps are run on. */
    private static final int TWO_OPT_LIMIT = 200;

    private TargetOrderer() {}

    /** Returns the targets in the recommended visit order, ignoring headings. */
    public static List<Point> order(Point startPos, double startHeadingRad, List<Point> targets, PathConfig config) {
        return toPoints(orderSpecs(startPos, startHeadingRad, freeSpecs(targets), config));
    }

    static List<TargetSpec> orderSpecs(
            Point startPos, double startHeadingRad, List<TargetSpec> targets, PathConfig config) {
        if (targets.size() <= 1) {
            return new ArrayList<>(targets);
        }

        if (targets.size() <= config.getBruteForceOrderLimit()) {
            return bruteForce(startPos, startHeadingRad, targets, config);
        }

        return greedy(startPos, startHeadingRad, targets, config);
    }

    /** Returns the cost of visiting {@code order} from the start. */
    public static double pathCost(Point startPos, double startHeadingRad, List<Point> order, double turnCostWeight) {
        return pathCostSpecs(startPos, startHeadingRad, freeSpecs(order), turnCostWeight);
    }

    static double pathCostSpecs(Point startPos, double startHeadingRad, List<TargetSpec> order, double turnCostWeight) {
        double cost = 0;
        Point currentPos = startPos;
        double currentHeading = startHeadingRad;

        for (TargetSpec target : order) {
            cost += stepCost(currentPos, currentHeading, target, turnCostWeight);
            currentHeading = arrivalHeading(currentPos, target);
            currentPos = target.position();
        }

        return cost;
    }

    private static double stepCost(Point fromPos, double fromHeading, TargetSpec target, double turnCostWeight) {
        double arrivalBearing = arrivalHeading(fromPos, target);
        double travelDistance = fromPos.distanceTo(target.position());
        double turn = Double.isNaN(fromHeading) ? 0.0 : Math.abs(normalizeAngle(arrivalBearing - fromHeading));

        return travelDistance + turnCostWeight * turn;
    }

    private static double arrivalHeading(Point fromPos, TargetSpec target) {
        if (target.hasLockedHeading()) {
            return target.getHeadingRad();
        }

        return Math.atan2(target.getY() - fromPos.getY(), target.getX() - fromPos.getX());
    }

    private static List<TargetSpec> bruteForce(
            Point startPos, double startHeading, List<TargetSpec> targets, PathConfig config) {
        List<TargetSpec> working = new ArrayList<>(targets);
        List<TargetSpec> bestOrder = new ArrayList<>(targets);
        double[] minCost = {pathCostSpecs(startPos, startHeading, working, config.getTurnCostWeight())};

        permute(working, 0, permutation -> {
            double cost = pathCostSpecs(startPos, startHeading, permutation, config.getTurnCostWeight());

            if (cost < minCost[0]) {
                minCost[0] = cost;
                bestOrder.clear();
                bestOrder.addAll(permutation);
            }
        });

        return bestOrder;
    }

    private static List<TargetSpec> greedy(
            Point startPos, double startHeading, List<TargetSpec> targets, PathConfig config) {
        List<TargetSpec> remaining = new ArrayList<>(targets);
        List<TargetSpec> result = new ArrayList<>();
        Point currentPos = startPos;

        while (!remaining.isEmpty()) {
            TargetSpec nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (TargetSpec target : remaining) {
                double distance = currentPos.distanceTo(target.position());

                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = target;
                }
            }

            currentPos = nearest.position();
            result.add(nearest);
            remaining.remove(nearest);
        }

        return twoOpt(result, startPos, startHeading, config.getTurnCostWeight());
    }

    private static List<TargetSpec> twoOpt(
            List<TargetSpec> order, Point startPos, double startHeading, double turnCostWeight) {
        List<TargetSpec> best = new ArrayList<>(order);

        if (best.size() > TWO_OPT_LIMIT) {
            return best;
        }

        double bestCost = pathCostSpecs(startPos, startHeading, best, turnCostWeight);

        for (int pass = 0; pass < TWO_OPT_PASSES; pass++) {
            boolean improved = false;

            for (int i = 0; i < best.size() - 1; i++) {
                for (int j = i + 1; j < best.size(); j++) {
                    List<TargetSpec> candidate = new ArrayList<>(best);
                    Collections.reverse(candidate.subList(i, j + 1));

                    double cost = pathCostSpecs(startPos, startHeading, candidate, turnCostWeight);

                    if (cost < bestCost - 1e-9) {
                        best = candidate;
                        bestCost = cost;
                        improved = true;
                    }
                }
            }

            if (!improved) {
                break;
            }
        }

        return best;
    }

    /** Decides whether the robot may travel directly from one target to another. */
    public interface ReachabilityCheck {
        boolean canReach(Point from, Point target);
    }

    interface TargetReachability {
        boolean canReach(Point from, TargetSpec target);
    }

    /** Selects and orders at most {@code maxTargets} targets, ignoring headings. */
    public static List<Point> select(
            Point startPos,
            double startHeadingRad,
            List<Point> targets,
            int maxTargets,
            PathConfig config,
            ReachabilityCheck reachability) {
        List<TargetSpec> selected = selectSpecs(
                startPos,
                startHeadingRad,
                freeSpecs(targets),
                maxTargets,
                config,
                (from, target) -> reachability.canReach(from, target.getPoint()));

        return toPoints(selected);
    }

    static List<TargetSpec> selectSpecs(
            Point startPos,
            double startHeadingRad,
            List<TargetSpec> targets,
            int maxTargets,
            PathConfig config,
            TargetReachability reachability) {
        int limit = Math.min(maxTargets, targets.size());

        if (limit <= 0) {
            return new ArrayList<>();
        }

        if (targets.size() <= config.getBruteForceOrderLimit()) {
            return selectBruteForce(startPos, startHeadingRad, targets, limit, config, reachability);
        }

        return selectGreedy(startPos, startHeadingRad, targets, limit, config, reachability);
    }

    private static List<TargetSpec> selectBruteForce(
            Point startPos,
            double startHeadingRad,
            List<TargetSpec> targets,
            int limit,
            PathConfig config,
            TargetReachability reachability) {
        List<TargetSpec> best = new ArrayList<>();
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

    private static void search(
            Point currentPos,
            double currentHeading,
            List<TargetSpec> targets,
            int limit,
            double turnCostWeight,
            TargetReachability reachability,
            boolean[] used,
            List<TargetSpec> current,
            double cost,
            List<TargetSpec> best,
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

        // Cost only grows, so a partial order that cannot beat the best is pruned.
        if (limit <= best.size() && cost >= bestCost[0]) {
            return;
        }

        for (int i = 0; i < targets.size(); i++) {
            if (used[i]) {
                continue;
            }

            TargetSpec candidate = targets.get(i);

            if (!reachability.canReach(currentPos, candidate)) {
                continue;
            }

            used[i] = true;
            current.add(candidate);
            search(
                    candidate.position(),
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

    private static List<TargetSpec> selectGreedy(
            Point startPos,
            double startHeading,
            List<TargetSpec> targets,
            int limit,
            PathConfig config,
            TargetReachability reachability) {
        List<TargetSpec> remaining = new ArrayList<>(targets);
        List<TargetSpec> result = new ArrayList<>();
        Point currentPos = startPos;
        double currentHeading = startHeading;

        while (result.size() < limit && !remaining.isEmpty()) {
            TargetSpec bestTarget = null;
            double bestStepCost = Double.MAX_VALUE;

            for (TargetSpec candidate : remaining) {
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
            currentPos = bestTarget.position();
            result.add(bestTarget);
            remaining.remove(bestTarget);
        }

        return result;
    }

    private static List<TargetSpec> freeSpecs(List<Point> points) {
        List<TargetSpec> specs = new ArrayList<>();

        for (Point point : points) {
            specs.add(new TargetSpec(point, HeadingPolicy.FREE));
        }

        return specs;
    }

    private static List<Point> toPoints(List<TargetSpec> specs) {
        List<Point> points = new ArrayList<>();

        for (TargetSpec spec : specs) {
            points.add(spec.getPoint());
        }

        return points;
    }

    private static double normalizeAngle(double angle) {
        return Math.IEEEremainder(angle, 2 * Math.PI);
    }

    private interface PermutationVisitor {
        void visit(List<TargetSpec> permutation);
    }

    private static void permute(List<TargetSpec> arr, int k, PermutationVisitor visitor) {
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
