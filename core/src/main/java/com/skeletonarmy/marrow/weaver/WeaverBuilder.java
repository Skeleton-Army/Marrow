package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Zone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent builder for path generation requests.
 * <p>
 * At minimum a start pose and either targets or a destination must be supplied.
 * The global configuration is read from {@link Weaver#getConfig()} when
 * {@link #generate()} is called.
 */
public class WeaverBuilder {
    private PathPose startPose;
    private PathPose destinationPose;
    private final List<PathPose> targets = new ArrayList<>();
    private final List<Zone> obstacles = new ArrayList<>();
    private boolean reorder = true;

    /**
     * Sets the robot's start pose.
     *
     * @param start the start pose
     * @return this builder, for chaining
     */
    public WeaverBuilder start(PathPose start) {
        this.startPose = start;
        return this;
    }

    /**
     * Replaces all previously added targets.
     *
     * @param newTargets the targets to visit; may be {@code null} to clear them
     * @return this builder, for chaining
     */
    public WeaverBuilder targets(List<PathPose> newTargets) {
        this.targets.clear();

        if (newTargets != null) {
            this.targets.addAll(newTargets);
        }

        return this;
    }

    /**
     * Replaces all previously added targets.
     *
     * @param newTargets the targets to visit; may be {@code null} to clear them
     * @return this builder, for chaining
     */
    public WeaverBuilder targets(PathPose... newTargets) {
        return targets(Arrays.asList(newTargets));
    }

    /**
     * Appends one target.
     *
     * @param target the target to append
     * @return this builder, for chaining
     */
    public WeaverBuilder addTarget(PathPose target) {
        this.targets.add(target);
        return this;
    }

    /**
     * Keeps the targets in the order they were added instead of letting the weaver
     * reorder them.
     *
     * @return this builder, for chaining
     */
    public WeaverBuilder ordered() {
        this.reorder = false;
        return this;
    }

    /**
     * Sets the destination pose for avoidance mode.
     *
     * @param destination the destination pose
     * @return this builder, for chaining
     */
    public WeaverBuilder end(PathPose destination) {
        this.destinationPose = destination;
        return this;
    }

    /**
     * Replaces all previously added obstacles.
     *
     * @param newObstacles the obstacles to avoid; may be {@code null} to clear them
     * @return this builder, for chaining
     */
    public WeaverBuilder obstacles(List<Zone> newObstacles) {
        this.obstacles.clear();

        if (newObstacles != null) {
            this.obstacles.addAll(newObstacles);
        }

        return this;
    }

    /**
     * Replaces all previously added obstacles with the given ones.
     *
     * @param newObstacles the obstacles to avoid; may be {@code null} to clear them
     * @return this builder, for chaining
     */
    public WeaverBuilder obstacles(Zone... newObstacles) {
        return obstacles(Arrays.asList(newObstacles));
    }

    /**
     * Appends one obstacle.
     *
     * @param obstacle the obstacle to append
     * @return this builder, for chaining
     */
    public WeaverBuilder addObstacle(Zone obstacle) {
        this.obstacles.add(obstacle);
        return this;
    }

    /**
     * Generates the path from the configured inputs.
     *
     * @return the generated path and metadata
     * @throws IllegalStateException if no start pose, or neither targets nor a
     *                               destination, were supplied
     */
    public PathResult generate() {
        if (startPose == null) {
            throw new IllegalStateException("Start PathPose is required");
        }

        if (targets.isEmpty() && destinationPose == null) {
            throw new IllegalStateException("Either targets or a destination must be set");
        }

        return WeaverGenerator.generate(startPose, destinationPose, targets, obstacles, reorder, Weaver.getConfig());
    }

    /**
     * Generates the path from the configured inputs.
     *
     * @return the generated path and metadata
     * @throws IllegalStateException if no start pose, or neither targets nor a
     *                               destination, were supplied
     */
    public PathResult build() {
        return generate();
    }
}
