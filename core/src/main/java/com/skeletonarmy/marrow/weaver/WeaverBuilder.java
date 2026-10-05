package com.skeletonarmy.marrow.weaver;

import androidx.annotation.NonNull;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Fluent builder for path generation requests.
 * <p>
 * At minimum a start pose and either targets or a destination must be supplied.
 * The global configuration is read from {@link Weaver#getConfig()} when
 * {@link #generate()} is called.
 */
public class WeaverBuilder {
    private Point startPose;
    private Point destinationPose;
    private final List<Point> targets = new ArrayList<>();
    private final List<Zone> obstacles = new ArrayList<>();
    private boolean reorder = true;
    private int maxTargets = 0;

    /**
     * Sets the robot's start pose.
     *
     * @param start the start pose
     * @return this builder, for chaining
     */
    public WeaverBuilder start(Point start) {
        this.startPose = start;
        return this;
    }

    /**
     * Replaces all previously added targets.
     *
     * @param newTargets the targets to visit; may be {@code null} to clear them
     * @return this builder, for chaining
     */
    public WeaverBuilder targets(List<Point> newTargets) {
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
    public WeaverBuilder targets(Point... newTargets) {
        return targets(Arrays.asList(newTargets));
    }

    /**
     * Appends one target.
     *
     * @param target the target to append
     * @return this builder, for chaining
     */
    public WeaverBuilder addTarget(Point target) {
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
     * Caps how many targets the robot visits. A value of {@code 0} or less visits every target, which is the default.
     *
     * @param maxTargets maximum number of targets to visit
     * @return this builder, for chaining
     */
    public WeaverBuilder maxTargets(int maxTargets) {
        this.maxTargets = maxTargets;
        return this;
    }

    /**
     * Sets the destination pose for avoidance mode.
     *
     * @param destination the destination pose
     * @return this builder, for chaining
     */
    public WeaverBuilder end(Point destination) {
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
            throw new IllegalStateException("Start Point is required");
        }

        if (targets.isEmpty() && destinationPose == null) {
            throw new IllegalStateException("Either targets or a destination must be set");
        }

        return WeaverGenerator.generate(
                startPose, destinationPose, targets, obstacles, reorder, maxTargets, Weaver.getConfig());
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

    @NonNull
    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "WeaverBuilder(startPose=%s, destinationPose=%s, targets=%s, obstacles=%s, reorder=%s, maxTargets=%d)",
                startPose,
                destinationPose,
                targets,
                obstacles,
                reorder,
                maxTargets);
    }
}
