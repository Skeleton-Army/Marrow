package com.skeletonarmy.marrow.weaver;

/**
 * Combined configuration for Bezier path generation and obstacle avoidance.
 */
public class PathConfig {

    // --- Robot Geometry ---

    // Intake width used when reaching targets.
    // Set to 0 to stop with the robot center on the target.
    // Set to half your intake width if you want to intake game elements.
    private double width = 0.0;

    // Robot footprint dimensions used for clearance checks.
    // Width runs along the robot's forward axis, height perpendicular to it.
    private double robotWidth = 18.0;
    private double robotHeight = 18.0;

    // Extra distance kept from obstacles, added to the robot footprint.
    // Raise for safer paths, lower to cut closer to obstacles.
    private double clearance = 4.0;

    // --- Waypoint Ordering ---

    // Weight applied to turning cost when ordering waypoints.
    // Raise to prefer straighter routes over shorter distance.
    private double turnCostWeight = 10.0;

    // Max targets ordered by brute force before falling back to greedy.
    // Raise to try more orderings (slower, better results).
    private int bruteForceOrderLimit = 8;

    public double getWidth() { return width; }
    public PathConfig width(double v) { this.width = v; return this; }

    public double getRobotWidth() { return robotWidth; }
    public PathConfig robotWidth(double v) { this.robotWidth = v; return this; }

    public double getRobotHeight() { return robotHeight; }
    public PathConfig robotHeight(double v) { this.robotHeight = v; return this; }

    public double getClearance() { return clearance; }
    public PathConfig clearance(double v) { this.clearance = v; return this; }

    public double getTurnCostWeight() { return turnCostWeight; }
    public PathConfig turnCostWeight(double v) { this.turnCostWeight = v; return this; }

    public int getBruteForceOrderLimit() { return bruteForceOrderLimit; }
    public PathConfig bruteForceOrderLimit(int v) { this.bruteForceOrderLimit = v; return this; }
}
