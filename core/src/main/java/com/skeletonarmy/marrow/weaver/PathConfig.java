package com.skeletonarmy.marrow.weaver;

/**
 * Combined configuration for Bezier path generation and obstacle avoidance.
 * <p>
 * Example usage:
 * <pre>{@code
 * PathConfig config = new PathConfig()
 *         .width(13.0)
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
    private double width = 0.0;

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

    public double getWidth() { return width; }
    public PathConfig width(double v) { this.width = v; return this; }

    public double getRobotWidth() { return robotWidth; }
    public PathConfig robotWidth(double v) { this.robotWidth = v; return this; }

    public double getRobotHeight() { return robotHeight; }
    public PathConfig robotHeight(double v) { this.robotHeight = v; return this; }

    public double getClearance() { return clearance; }
    public PathConfig clearance(double v) { this.clearance = v; return this; }

    public boolean isExcludeBlockedTargets() { return excludeBlockedTargets; }
    public PathConfig excludeBlockedTargets(boolean v) { this.excludeBlockedTargets = v; return this; }

    public boolean isSmoothing() { return smoothing; }
    public PathConfig smoothing(boolean v) { this.smoothing = v; return this; }

    public double getTurnCostWeight() { return turnCostWeight; }
    public PathConfig turnCostWeight(double v) { this.turnCostWeight = v; return this; }

    public int getBruteForceOrderLimit() { return bruteForceOrderLimit; }
    public PathConfig bruteForceOrderLimit(int v) { this.bruteForceOrderLimit = v; return this; }
}
