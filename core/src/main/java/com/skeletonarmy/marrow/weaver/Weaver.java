package com.skeletonarmy.marrow.weaver;

/**
 * Generator for Bezier paths with optional obstacle avoidance.
 */
public class Weaver {

    private static PathConfig config = new PathConfig();

    private Weaver() {
    }

    public static void setConfig(PathConfig newConfig) {
        config = newConfig;
    }

    public static PathConfig getConfig() {
        return config;
    }

    public static void resetToDefaults() {
        config = new PathConfig();
    }

    public static WeaverBuilder builder() {
        return new WeaverBuilder();
    }
}
