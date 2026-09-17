package com.skeletonarmy.marrow.bezier;

/**
 * Combined configuration for Bezier path generation and obstacle avoidance.
 */
public class BezierConfig {

    // Intake & Ordering
    private double turnCostWeight = 10.0;
    private double reach = 5.0;
    private double width = 8.0;
    private double tension = 0.0;
    private int bruteForceOrderLimit = 8;

    // Obstacle Avoidance
    private double clearance = 4.0;
    private int controlPointCount = 6;
    private int maxIterations = 80;
    private double stepSize = 0.5;
    private int samplesPerCheck = 25;
    private double robotSize = 18.0;

    // Getters and fluent setters for Intake
    public double getTurnCostWeight() { return turnCostWeight; }
    public BezierConfig turnCostWeight(double v) { this.turnCostWeight = v; return this; }

    public double getReach() { return reach; }
    public BezierConfig reach(double v) { this.reach = v; return this; }

    public double getWidth() { return width; }
    public BezierConfig width(double v) { this.width = v; return this; }

    public double getTension() { return tension; }
    public BezierConfig tension(double v) { this.tension = v; return this; }

    public int getBruteForceOrderLimit() { return bruteForceOrderLimit; }
    public BezierConfig bruteForceOrderLimit(int v) { this.bruteForceOrderLimit = v; return this; }

    // Getters and fluent setters for Avoidance
    public double getClearance() { return clearance; }
    public BezierConfig clearance(double v) { this.clearance = v; return this; }

    public int getControlPointCount() { return controlPointCount; }
    public BezierConfig controlPointCount(int v) { this.controlPointCount = v; return this; }

    public int getMaxIterations() { return maxIterations; }
    public BezierConfig maxIterations(int v) { this.maxIterations = v; return this; }

    public double getStepSize() { return stepSize; }
    public BezierConfig stepSize(double v) { this.stepSize = v; return this; }

    public int getSamplesPerCheck() { return samplesPerCheck; }
    public BezierConfig samplesPerCheck(int v) { this.samplesPerCheck = v; return this; }

    public double getRobotSize() { return robotSize; }
    public BezierConfig robotSize(double v) { this.robotSize = v; return this; }
}
