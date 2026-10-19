package com.skeletonarmy.marrow.weaver;

import androidx.annotation.NonNull;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Fluent builder for path generation requests.
 * <p>
 * At minimum a start pose and either targets or a destination must be supplied.
 * The global configuration is read from {@link Weaver#getConfig()} when
 * {@link #generate()} is called.
 * <p>
 * Targets default to {@link HeadingPolicy#FREE}, so a target heading is ignored
 * unless {@link HeadingPolicy#LOCKED} is selected explicitly.
 */
public class WeaverBuilder {
    private Point startPose;
    private Point destinationPose;
    private HeadingPolicy destinationHeadingPolicy = HeadingPolicy.FREE;
    private final List<TargetSpec> targets = new ArrayList<>();
    private final List<Zone> obstacles = new ArrayList<>();
    private boolean reorder = true;
    private int maxTargets = 0;
    private HeadingPolicy defaultHeadingPolicy = HeadingPolicy.FREE;

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
     * Sets the heading policy used by targets that do not specify one.
     *
     * @param policy policy to apply to subsequently added targets
     * @return this builder, for chaining
     */
    public WeaverBuilder defaultHeadingPolicy(HeadingPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Heading policy is required");
        }

        this.defaultHeadingPolicy = policy;
        return this;
    }

    /**
     * Replaces all previously added targets.
     *
     * @param newTargets the targets to visit; may be {@code null} to clear them
     * @return this builder, for chaining
     */
    public WeaverBuilder targets(List<Point> newTargets) {
        return targets(newTargets, defaultHeadingPolicy);
    }

    /**
     * Replaces all previously added targets with the given heading policy.
     *
     * @param newTargets the targets to visit; may be {@code null} to clear them
     * @param policy     heading policy for every target
     * @return this builder, for chaining
     */
    public WeaverBuilder targets(List<Point> newTargets, HeadingPolicy policy) {
        this.targets.clear();
        return addTargets(newTargets, policy);
    }

    /**
     * Replaces all previously added targets.
     *
     * @param newTargets the targets to visit; may be {@code null} to clear them
     * @return this builder, for chaining
     */
    public WeaverBuilder targets(Point... newTargets) {
        return targets(newTargets == null ? null : Arrays.asList(newTargets));
    }

    /**
     * Replaces all previously added targets, mapping each source element to a target.
     *
     * @param source  the elements to map; may be {@code null} to clear the targets
     * @param toPoint the mapping from a source element to its target
     * @param <T>     the source element type
     * @return this builder, for chaining
     */
    public <T> WeaverBuilder targets(Iterable<T> source, Function<T, Point> toPoint) {
        return targets(source, toPoint, defaultHeadingPolicy);
    }

    /**
     * Replaces all previously added targets, mapping each source element to a target
     * with the given heading policy.
     *
     * @param source  the elements to map; may be {@code null} to clear the targets
     * @param toPoint the mapping from a source element to its target
     * @param policy  heading policy for every target
     * @param <T>     the source element type
     * @return this builder, for chaining
     */
    public <T> WeaverBuilder targets(Iterable<T> source, Function<T, Point> toPoint, HeadingPolicy policy) {
        this.targets.clear();
        return addTargets(source, toPoint, policy);
    }

    /**
     * Appends one target using the default heading policy.
     *
     * @param target the target to append
     * @return this builder, for chaining
     */
    public WeaverBuilder addTarget(Point target) {
        return addTarget(target, defaultHeadingPolicy);
    }

    /**
     * Appends one target with an explicit heading policy.
     *
     * @param target the target to append
     * @param policy heading policy for the target
     * @return this builder, for chaining
     */
    public WeaverBuilder addTarget(Point target, HeadingPolicy policy) {
        this.targets.add(new TargetSpec(target, policy));
        return this;
    }

    /**
     * Appends all the given targets using the default heading policy.
     *
     * @param newTargets the targets to append; may be {@code null}
     * @return this builder, for chaining
     */
    public WeaverBuilder addTargets(List<Point> newTargets) {
        return addTargets(newTargets, defaultHeadingPolicy);
    }

    /**
     * Appends all the given targets with an explicit heading policy.
     *
     * @param newTargets the targets to append; may be {@code null}
     * @param policy     heading policy for every target
     * @return this builder, for chaining
     */
    public WeaverBuilder addTargets(List<Point> newTargets, HeadingPolicy policy) {
        if (newTargets != null) {
            for (Point target : newTargets) {
                addTarget(target, policy);
            }
        }

        return this;
    }

    /**
     * Appends targets, mapping each source element to a target.
     *
     * @param source  the elements to map; may be {@code null}
     * @param toPoint the mapping from a source element to its target
     * @param <T>     the source element type
     * @return this builder, for chaining
     */
    public <T> WeaverBuilder addTargets(Iterable<T> source, Function<T, Point> toPoint) {
        return addTargets(source, toPoint, defaultHeadingPolicy);
    }

    /**
     * Appends targets with an explicit heading policy, mapping each source element to
     * a target.
     *
     * @param source  the elements to map; may be {@code null}
     * @param toPoint the mapping from a source element to its target
     * @param policy  heading policy for every target
     * @param <T>     the source element type
     * @return this builder, for chaining
     */
    public <T> WeaverBuilder addTargets(Iterable<T> source, Function<T, Point> toPoint, HeadingPolicy policy) {
        if (source != null) {
            for (T element : source) {
                addTarget(toPoint.apply(element), policy);
            }
        }

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
     * Sets the destination pose for avoidance mode using the default heading policy.
     *
     * @param destination the destination pose
     * @return this builder, for chaining
     */
    public WeaverBuilder end(Point destination) {
        return end(destination, defaultHeadingPolicy);
    }

    /**
     * Sets the destination pose for avoidance mode with an explicit heading policy.
     *
     * @param destination the destination pose
     * @param policy      heading policy for the destination
     * @return this builder, for chaining
     */
    public WeaverBuilder end(Point destination, HeadingPolicy policy) {
        new TargetSpec(destination, policy);

        this.destinationPose = destination;
        this.destinationHeadingPolicy = policy;
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
                startPose,
                destinationPose,
                targets,
                destinationHeadingPolicy,
                obstacles,
                reorder,
                maxTargets,
                Weaver.getConfig());
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
                "WeaverBuilder(startPose=%s, destinationPose=%s, targets=%s, obstacles=%s, reorder=%s, "
                        + "maxTargets=%d, defaultHeadingPolicy=%s)",
                startPose,
                destinationPose,
                targets,
                obstacles,
                reorder,
                maxTargets,
                defaultHeadingPolicy);
    }
}
