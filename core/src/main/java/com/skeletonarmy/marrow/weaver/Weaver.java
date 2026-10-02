package com.skeletonarmy.marrow.weaver;

/**
 * Generator for Bezier paths with optional obstacle avoidance.
 * <p>
 * Example usage:
 * <pre>{@code
 * PathResult result = Weaver.builder()
 *         .start(new PathPose(72, 72, 0))
 *         .addTarget(new PathPose(100, 72))
 *         .generate();
 * }</pre>
 */
public class Weaver {
    private static PathConfig config = new PathConfig();

    private Weaver() {}

    /**
     * Replaces the global configuration used by subsequent generations.
     *
     * @param newConfig configuration to use
     */
    public static void setConfig(PathConfig newConfig) {
        config = newConfig;
    }

    /** @return the current global configuration */
    public static PathConfig getConfig() {
        return config;
    }

    /** Restores the global configuration to its default values. */
    public static void resetToDefaults() {
        config = new PathConfig();
    }

    /** @return a new builder for constructing a path */
    public static WeaverBuilder builder() {
        return new WeaverBuilder();
    }
}
