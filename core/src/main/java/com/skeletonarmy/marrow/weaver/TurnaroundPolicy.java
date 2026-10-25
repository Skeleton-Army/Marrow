package com.skeletonarmy.marrow.weaver;

/**
 * Controls how a route leaves a pose whose heading points away from where it has to go.
 * <p>
 * A robot that starts facing away from its first target cannot follow a curve fitted
 * straight through the reversal: the control points collapse onto one line and the
 * tangent flips inside a fraction of an inch. Whichever way it is resolved, the route
 * still passes through every target.
 */
public enum TurnaroundPolicy {
    /**
     * Keep the heading the pose asks for and sweep a real arc to turn around, of radius
     * {@code max(robotWidth, robotHeight) / 4}. The path swings out to the side before
     * it comes back, so the tangent heading turns at a rate the robot can follow. Use it
     * when there is room in front of the robot to spend.
     */
    ARC,

    /**
     * Leave toward the first key point and let the follower rotate the robot onto the
     * path in place. The tightest route and the shortest way round, but the pose's own
     * heading no longer shapes the curve, so the robot spins where it stands instead of
     * driving the turn.
     */
    IN_PLACE
}
