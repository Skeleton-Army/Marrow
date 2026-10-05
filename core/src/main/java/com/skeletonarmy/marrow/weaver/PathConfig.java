package com.skeletonarmy.marrow.weaver;

/**
 * Combined configuration for Bezier path generation and obstacle avoidance.
 * <p>
 * Example usage:
 * <pre>{@code
 * PathConfig config = new PathConfig()
 *         .intakeWidth(13.0)
 *         .robotWidth(16.0)
 *         .clearance(3.0);
 * }</pre>
 */
public class PathConfig {
    // --- Robot Geometry ---

    /**
     * Intake width used when reaching targets. Set to 0 to stop with the robot center
     * on the target. Set to the real intake width to sweep up game elements.
     */
    private double intakeWidth = 0.0;

    /**
     * Robot footprint width, measured along the robot's forward axis.
     */
    private double robotWidth = 18.0;

    /**
     * Robot footprint height, measured perpendicular to the forward axis.
     */
    private double robotHeight = 18.0;

    /**
     * Extra distance kept from obstacles, added on top of the robot footprint. Raise
     * for safer paths, lower to cut closer to obstacles.
     */
    private double clearance = 4.0;

    // --- Path Generation ---

    /**
     * When {@code true}, targets that an obstacle blocks the robot from occupying are
     * dropped so the path does not cut through the obstacle chasing them. Defaults on.
     */
    private boolean excludeBlockedTargets = true;

    /**
     * When {@code true}, generated curves are re-fitted with continuous tangents so
     * corners are rounded and curvature stays gradual. Disable for the raw Hermite
     * chain, which tracks target headings more tightly but turns more abruptly.
     * Defaults on.
     */
    private boolean smoothing = true;

    /**
     * Weight applied to turning cost when ordering waypoints. Raise to prefer
     * straighter routes over shorter distance.
     */
    private double turnCostWeight = 10.0;

    /**
     * Maximum number of targets ordered by brute force before falling back to the
     * greedy strategy. Raise to try more orderings (slower, usually better).
     */
    private int bruteForceOrderLimit = 8;

    /**
     * Returns the intake width used when reaching targets.
     *
     * @return the intake width, or {@code 0} to stop with the robot center on the target
     */
    public double getIntakeWidth() {
        return intakeWidth;
    }

    /**
     * Sets the intake width used when reaching targets.
     *
     * @param v intake width, or {@code 0} to stop with the robot center on the target
     * @return this config, for chaining
     */
    public PathConfig intakeWidth(double v) {
        this.intakeWidth = v;
        return this;
    }

    /**
     * Returns the robot footprint width, measured along the robot's forward axis.
     *
     * @return the robot width
     */
    public double getRobotWidth() {
        return robotWidth;
    }

    /**
     * Sets the robot footprint width, measured along the robot's forward axis.
     *
     * @param v robot width
     * @return this config, for chaining
     */
    public PathConfig robotWidth(double v) {
        this.robotWidth = v;
        return this;
    }

    /**
     * Returns the robot footprint height, measured perpendicular to the forward axis.
     *
     * @return the robot height
     */
    public double getRobotHeight() {
        return robotHeight;
    }

    /**
     * Sets the robot footprint height, measured perpendicular to the forward axis.
     *
     * @param v robot height
     * @return this config, for chaining
     */
    public PathConfig robotHeight(double v) {
        this.robotHeight = v;
        return this;
    }

    /**
     * Returns the extra distance kept from obstacles.
     *
     * @return the clearance in inches
     */
    public double getClearance() {
        return clearance;
    }

    /**
     * Sets the extra distance kept from obstacles, added on top of the robot footprint.
     *
     * @param v clearance in inches
     * @return this config, for chaining
     */
    public PathConfig clearance(double v) {
        this.clearance = v;
        return this;
    }

    /**
     * Returns whether targets blocked by an obstacle are dropped.
     *
     * @return {@code true} if unreachable targets are excluded
     */
    public boolean isExcludeBlockedTargets() {
        return excludeBlockedTargets;
    }

    /**
     * Sets whether targets blocked by an obstacle are dropped.
     *
     * @param v {@code true} to exclude unreachable targets
     * @return this config, for chaining
     */
    public PathConfig excludeBlockedTargets(boolean v) {
        this.excludeBlockedTargets = v;
        return this;
    }

    /**
     * Returns whether generated curves are re-fitted with continuous tangents.
     *
     * @return {@code true} if smoothing is enabled
     */
    public boolean isSmoothing() {
        return smoothing;
    }

    /**
     * Sets whether generated curves are re-fitted with continuous tangents.
     *
     * @param v {@code true} to round corners and keep curvature gradual
     * @return this config, for chaining
     */
    public PathConfig smoothing(boolean v) {
        this.smoothing = v;
        return this;
    }

    /**
     * Returns the weight applied to turning cost when ordering waypoints.
     *
     * @return the turn cost weight
     */
    public double getTurnCostWeight() {
        return turnCostWeight;
    }

    /**
     * Sets the weight applied to turning cost when ordering waypoints.
     *
     * @param v turn cost weight; higher values prefer straighter routes
     * @return this config, for chaining
     */
    public PathConfig turnCostWeight(double v) {
        this.turnCostWeight = v;
        return this;
    }

    /**
     * Returns the maximum number of targets ordered by brute force.
     *
     * @return the brute-force ordering limit
     */
    public int getBruteForceOrderLimit() {
        return bruteForceOrderLimit;
    }

    /**
     * Sets the maximum number of targets ordered by brute force before falling back
     * to the greedy strategy.
     *
     * @param v brute-force ordering limit
     * @return this config, for chaining
     */
    public PathConfig bruteForceOrderLimit(int v) {
        this.bruteForceOrderLimit = v;
        return this;
    }
}
