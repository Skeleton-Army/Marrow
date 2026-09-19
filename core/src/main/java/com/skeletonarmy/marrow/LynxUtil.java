package com.skeletonarmy.marrow;

import android.content.Context;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegistrar;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.List;

/**
 * Utility for managing REV Hub bulk caching.
 * <p>
 * This system automatically enables bulk reading for all OpModes based on the global
 * {@link BulkCachingSettings}.
 */
public class LynxUtil {
    private static List<LynxModule> cachedHubs = null;
    private static HardwareMap cachedHardwareMap = null;

    /**
     * Automatically registers a listener to handle bulk caching for all OpModes.
     * This is called by the SDK during OpMode registration.
     */
    @OpModeRegistrar
    public static void register(Context context, OpModeManager manager) {
        if (manager instanceof OpModeManagerImpl) {
            ((OpModeManagerImpl) manager).registerListener(new OpModeManagerNotifier.Notifications() {
                @Override
                public void onOpModePreInit(OpMode opMode) {
                    // Check for annotation override first
                    BulkCaching annotation = opMode.getClass().getAnnotation(BulkCaching.class);
                    
                    // Fallback to global setting (which defaults to AUTO)
                    LynxModule.BulkCachingMode mode = (annotation != null) ? annotation.mode() : BulkCachingSettings.defaultMode;

                    if (mode != LynxModule.BulkCachingMode.OFF) {
                        setBulkCachingMode(opMode.hardwareMap, mode);
                    }
                }

                @Override
                public void onOpModePreStart(OpMode opMode) {}

                @Override
                public void onOpModePostStop(OpMode opMode) {
                    // Clean up to prevent hardware map leaks between runs
                    cachedHubs = null;
                    cachedHardwareMap = null;
                }
            });
        }
    }

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
