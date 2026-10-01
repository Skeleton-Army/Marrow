package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Zone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WeaverBuilder {
    private PathPose startPose;
    private PathPose destinationPose;
    private final List<PathPose> targets = new ArrayList<>();
    private final List<Zone> obstacles = new ArrayList<>();
    private boolean reorder = true;

    public WeaverBuilder start(PathPose start) {
        this.startPose = start;
        return this;
    }

    public WeaverBuilder targets(List<PathPose> targets) {
        this.targets.clear();
        if (targets != null) {
            this.targets.addAll(targets);
        }
        return this;
    }

    public WeaverBuilder addTarget(PathPose target) {
        this.targets.add(target);
        return this;
    }

    public WeaverBuilder ordered() {
        this.reorder = false;
        return this;
    }

    public WeaverBuilder end(PathPose destination) {
        this.destinationPose = destination;
        return this;
    }

    public WeaverBuilder obstacles(List<Zone> obstacles) {
        this.obstacles.clear();
        if (obstacles != null) {
            this.obstacles.addAll(obstacles);
        }
        return this;
    }

    public WeaverBuilder obstacles(Zone... obstacles) {
        this.obstacles.clear();
        if (obstacles != null) {
            this.obstacles.addAll(Arrays.asList(obstacles));
        }
        return this;
    }

    public WeaverBuilder addObstacle(Zone obstacle) {
        this.obstacles.add(obstacle);
        return this;
    }

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
