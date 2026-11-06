package com.skeletonarmy.marrow.weaver;

import androidx.annotation.NonNull;
import java.util.Locale;

/** Geometry and tuning parameters for path generation and obstacle avoidance. */
public class PathConfig {
    /**
     * Largest target count that is ordered exhaustively; longer lists use the greedy strategy.
     * Seven targets is the most that reliably stays under a 100 ms budget on a Control Hub.
     */
    public static final int MAX_BRUTE_FORCE_ORDER_LIMIT = 7;

    /** Total intake width. Zero makes the robot stop with its center on each target. */
    private double intakeWidth = 0.0;

    /** Robot footprint size along the forward axis. */
    private double robotWidth = 18.0;

    /** Robot footprint size across the forward axis. */
    private double robotHeight = 18.0;

    /** Extra distance kept from obstacles on top of the footprint. */
    private double clearance = 4.0;

    /** Whether targets the robot cannot occupy without hitting an obstacle are dropped. */
    private boolean excludeBlockedTargets = true;

    /** Whether generated curves are re-fitted with continuous tangents. */
    private boolean smoothing = true;

    /** Weight applied to turning when ordering targets. */
    private double turnCostWeight = 10.0;

    /** Largest target list ordered exhaustively before switching to the greedy strategy. */
    private int bruteForceOrderLimit = MAX_BRUTE_FORCE_ORDER_LIMIT;

    public double getIntakeWidth() {
        return intakeWidth;
    }

    public PathConfig intakeWidth(double v) {
        this.intakeWidth = requireFinite("intakeWidth", v);
        return this;
    }

    public double getRobotWidth() {
        return robotWidth;
    }

    public PathConfig robotWidth(double v) {
        this.robotWidth = requireFinite("robotWidth", v);
        return this;
    }

    public double getRobotHeight() {
        return robotHeight;
    }

    public PathConfig robotHeight(double v) {
        this.robotHeight = requireFinite("robotHeight", v);
        return this;
    }

    public double getClearance() {
        return clearance;
    }

    public PathConfig clearance(double v) {
        this.clearance = requireFinite("clearance", v);
        return this;
    }

    public boolean isExcludeBlockedTargets() {
        return excludeBlockedTargets;
    }

    public PathConfig excludeBlockedTargets(boolean v) {
        this.excludeBlockedTargets = v;
        return this;
    }

    public boolean isSmoothing() {
        return smoothing;
    }

    public PathConfig smoothing(boolean v) {
        this.smoothing = v;
        return this;
    }

    public double getTurnCostWeight() {
        return turnCostWeight;
    }

    public PathConfig turnCostWeight(double v) {
        this.turnCostWeight = requireFinite("turnCostWeight", v);
        return this;
    }

    public int getBruteForceOrderLimit() {
        return bruteForceOrderLimit;
    }

    /** Sets the exhaustive ordering limit, clamped to {@code [0, MAX_BRUTE_FORCE_ORDER_LIMIT]}. */
    public PathConfig bruteForceOrderLimit(int v) {
        this.bruteForceOrderLimit = Math.max(0, Math.min(MAX_BRUTE_FORCE_ORDER_LIMIT, v));
        return this;
    }

    /** Rejects non-finite configuration values. */
    private static double requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be a finite number, got " + value);
        }

        return value;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "PathConfig(intakeWidth=%.3f, robotWidth=%.3f, robotHeight=%.3f, clearance=%.3f, "
                        + "excludeBlockedTargets=%s, smoothing=%s, turnCostWeight=%.3f, "
                        + "bruteForceOrderLimit=%d)",
                intakeWidth,
                robotWidth,
                robotHeight,
                clearance,
                excludeBlockedTargets,
                smoothing,
                turnCostWeight,
                bruteForceOrderLimit);
    }
}
