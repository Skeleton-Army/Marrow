package com.skeletonarmy.marrow.weaver;

import androidx.annotation.NonNull;
import java.util.Locale;

/** Geometry and tuning parameters for path generation and obstacle avoidance. */
public class PathConfig {
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
    private int bruteForceOrderLimit = 8;

    public double getIntakeWidth() {
        return intakeWidth;
    }

    public PathConfig intakeWidth(double v) {
        this.intakeWidth = v;
        return this;
    }

    public double getRobotWidth() {
        return robotWidth;
    }

    public PathConfig robotWidth(double v) {
        this.robotWidth = v;
        return this;
    }

    public double getRobotHeight() {
        return robotHeight;
    }

    public PathConfig robotHeight(double v) {
        this.robotHeight = v;
        return this;
    }

    public double getClearance() {
        return clearance;
    }

    public PathConfig clearance(double v) {
        this.clearance = v;
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
        this.turnCostWeight = v;
        return this;
    }

    public int getBruteForceOrderLimit() {
        return bruteForceOrderLimit;
    }

    public PathConfig bruteForceOrderLimit(int v) {
        this.bruteForceOrderLimit = v;
        return this;
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
