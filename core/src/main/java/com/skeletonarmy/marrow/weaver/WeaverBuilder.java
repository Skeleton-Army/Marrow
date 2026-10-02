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

    /** Sets the robot's start pose. */
    public WeaverBuilder start(PathPose start) {
        this.startPose = start;
        return this;
    }

    /** Replaces all previously added targets. */
    public WeaverBuilder targets(List<PathPose> newTargets) {
        this.targets.clear();

        if (newTargets != null) {
            this.targets.addAll(newTargets);
        }

        return this;
    }

    /** Replaces all previously added targets. */
    public WeaverBuilder targets(PathPose... newTargets) {
        this.targets.clear();

        if (newTargets != null) {
            this.targets.addAll(Arrays.asList(newTargets));
        }

        return this;
    }

    /** Appends one target. */
    public WeaverBuilder addTarget(PathPose target) {
        this.targets.add(target);
        return this;
    }

    /** Keeps the targets in the order they were added instead of letting the weaver reorder them. */
    public WeaverBuilder ordered() {
        this.reorder = false;
        return this;
    }

    /** Sets the destination pose for avoidance mode. */
    public WeaverBuilder end(PathPose destination) {
        this.destinationPose = destination;
        return this;
    }

    /** Replaces all previously added obstacles. */
    public WeaverBuilder obstacles(List<Zone> newObstacles) {
        this.obstacles.clear();

        if (newObstacles != null) {
            this.obstacles.addAll(newObstacles);
        }

        return this;
    }

    /** Replaces all previously added obstacles with the given ones. */
    public WeaverBuilder obstacles(Zone... newObstacles) {
        this.obstacles.clear();

        if (newObstacles != null) {
            this.obstacles.addAll(Arrays.asList(newObstacles));
        }

        return this;
    }

    /** Appends one obstacle. */
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

        return WeaverGenerator.generate(
                startPose,
                destinationPose,
                targets,
                obstacles,
                reorder,
                Weaver.getConfig()
        );
    }
}
