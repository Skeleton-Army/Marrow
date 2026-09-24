package com.skeletonarmy.marrow;

import com.qualcomm.hardware.lynx.LynxModule;

/**
 * Global settings for the Marrow bulk caching system.
 */
public class BulkCachingSettings {
    /**
     * The default bulk caching mode for all OpModes.
     * <p>
     * Set this to {@link LynxModule.BulkCachingMode#OFF} to disable bulk caching globally
     * by default (opt-in only via {@link BulkCaching} annotation).
     * <p>
     * Defaults to {@link LynxModule.BulkCachingMode#OFF}.
     */
    public static LynxModule.BulkCachingMode defaultMode = LynxModule.BulkCachingMode.OFF;
}
