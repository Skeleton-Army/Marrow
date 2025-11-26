package com.skeletonarmy.marrow;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.List;

public class LynxUtil {
    private static List<LynxModule> cachedHubs = null;
    private static HardwareMap cachedHardwareMap = null;

    /**
     * Sets the bulk caching mode for all Lynx modules in the hardware map.
     * <p>
     * <b>From GM0:</b> When in {@link LynxModule.BulkCachingMode#MANUAL} mode, if the cache is not cleared appropriately,
     * stale values will be returned. For that reason, if you are not quite sure what you are doing, we recommend
     * {@link LynxModule.BulkCachingMode#AUTO} mode; while {@link LynxModule.BulkCachingMode#MANUAL} mode can have some
     * performance improvements if {@link LynxModule.BulkCachingMode#AUTO} mode is not used optimally, it has less room
     * for catastrophic error.
     *
     * @param hardwareMap The hardware map.
     * @param mode The desired bulk caching mode to be set.
     */
    public static void setBulkCachingMode(HardwareMap hardwareMap, LynxModule.BulkCachingMode mode) {
        for (LynxModule hub : getHubs(hardwareMap)) {
            hub.setBulkCachingMode(mode);
        }
    }

    /**
     * Clears the bulk data cache for all Lynx modules in the hardware map.
     * <p>
     * This is typically used when operating in {@link LynxModule.BulkCachingMode#MANUAL} mode to ensure fresh sensor and
     * hardware values are retrieved. If the cache is not cleared in {@link LynxModule.BulkCachingMode#MANUAL} mode, stale
     * data may be returned.
     *
     * @param hardwareMap The hardware map.
     */
    public static void clearBulkCache(HardwareMap hardwareMap) {
        for (LynxModule hub : getHubs(hardwareMap)) {
            hub.clearBulkCache();
        }
    }

    private static List<LynxModule> getHubs(HardwareMap hardwareMap) {
        if (cachedHubs == null || cachedHardwareMap != hardwareMap) {
            cachedHubs = hardwareMap.getAll(LynxModule.class);
            cachedHardwareMap = hardwareMap;
        }
        return cachedHubs;
    }
}
