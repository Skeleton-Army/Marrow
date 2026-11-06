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
 *
 * <p>A start pose and at least one target or destination are required. When both are supplied,
 * the targets are visited first and the route ends at the destination. Targets are
 * {@link HeadingPolicy#FREE} unless a different policy is supplied.
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

    /** Sets the robot's start pose. */
    public WeaverBuilder start(Point start) {
        if (start != null) {
            TargetSpec.requireFinitePose(start);
        }

        this.startPose = start;
        return this;
    }

    public WeaverBuilder defaultHeadingPolicy(HeadingPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("Heading policy is required");
        }

        this.defaultHeadingPolicy = policy;
        return this;
    }

    public WeaverBuilder targets(List<Point> newTargets) {
        return targets(newTargets, defaultHeadingPolicy);
    }

    public WeaverBuilder targets(List<Point> newTargets, HeadingPolicy policy) {
        this.targets.clear();
        return addTargets(newTargets, policy);
    }

    public WeaverBuilder targets(Point... newTargets) {
        return targets(newTargets == null ? null : Arrays.asList(newTargets));
    }

    public <T> WeaverBuilder targets(Iterable<T> source, Function<T, Point> toPoint) {
        return targets(source, toPoint, defaultHeadingPolicy);
    }

    public <T> WeaverBuilder targets(Iterable<T> source, Function<T, Point> toPoint, HeadingPolicy policy) {
        this.targets.clear();
        return addTargets(source, toPoint, policy);
    }

    public WeaverBuilder addTarget(Point target) {
        return addTarget(target, defaultHeadingPolicy);
    }

    public WeaverBuilder addTarget(Point target, HeadingPolicy policy) {
        this.targets.add(new TargetSpec(target, policy));
        return this;
    }

    public WeaverBuilder addTargets(List<Point> newTargets) {
        return addTargets(newTargets, defaultHeadingPolicy);
    }

    public WeaverBuilder addTargets(List<Point> newTargets, HeadingPolicy policy) {
        if (newTargets != null) {
            for (Point target : newTargets) {
                addTarget(target, policy);
            }
        }

        return this;
    }

    public <T> WeaverBuilder addTargets(Iterable<T> source, Function<T, Point> toPoint) {
        return addTargets(source, toPoint, defaultHeadingPolicy);
    }

    /** Adds a target for each element produced by {@code source}. */
    public <T> WeaverBuilder addTargets(Iterable<T> source, Function<T, Point> toPoint, HeadingPolicy policy) {
        if (toPoint == null) {
            throw new IllegalArgumentException("A point converter is required");
        }

        if (source != null) {
            for (T element : source) {
                addTarget(toPoint.apply(element), policy);
            }
        }

        return this;
    }

    /** Keeps targets in insertion order instead of letting the weaver reorder them. */
    public WeaverBuilder ordered() {
        this.reorder = false;
        return this;
    }

    /** Caps the number of targets visited; {@code 0} or less visits all of them. */
    public WeaverBuilder maxTargets(int maxTargets) {
        this.maxTargets = maxTargets;
        return this;
    }

    public WeaverBuilder end(Point destination) {
        return end(destination, defaultHeadingPolicy);
    }

    public WeaverBuilder end(Point destination, HeadingPolicy policy) {
        TargetSpec.validate(destination, policy);

        this.destinationPose = destination;
        this.destinationHeadingPolicy = policy;
        return this;
    }

    /** Replaces the obstacle list; null clears it. */
    public WeaverBuilder obstacles(List<Zone> newObstacles) {
        if (newObstacles != null) {
            for (Zone obstacle : newObstacles) {
                if (obstacle == null) {
                    throw new IllegalArgumentException("Obstacle must not be null");
                }
            }
        }

        this.obstacles.clear();

        if (newObstacles != null) {
            this.obstacles.addAll(newObstacles);
        }

        return this;
    }

    /** Replaces the obstacle list; null clears it. */
    public WeaverBuilder obstacles(Zone... newObstacles) {
        return obstacles(newObstacles == null ? null : Arrays.asList(newObstacles));
    }

    /** Adds one obstacle. */
    public WeaverBuilder addObstacle(Zone obstacle) {
        if (obstacle == null) {
            throw new IllegalArgumentException("Obstacle must not be null");
        }

        this.obstacles.add(obstacle);
        return this;
    }

    /** Builds the path for the current settings. */
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

    /** Alias for {@link #generate()}. */
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
