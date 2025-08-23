package com.skeletonarmy.marrow;

import android.annotation.SuppressLint;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.List;
@SuppressWarnings("unused")
public class QoL {
    private static List<LynxModule> allHubs;

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
        // Cache all hubs
        allHubs = hardwareMap.getAll(LynxModule.class);

        for (LynxModule hub : allHubs) {
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
        // Cache all hubs if not cached already
        if (allHubs == null) {
            allHubs = hardwareMap.getAll(LynxModule.class);
        }

        for (LynxModule hub : allHubs) {
            hub.clearBulkCache();
        }
    }

    public enum logLevel {
        LOG,
        INFO,
        ERROR,
        CRITICAL
    }

    /**
     * Formats a log message with log level and optional timing information.
     *
     * @param message the log message content
     * @param level the log level
     * @param timer timer for elapsed time in seconds
     *              can be {@code null} to not include a timestamp
     *
     * @return formatted string like "[1.23]     [INFO] message" or "[INFO] message"
     */
    @SuppressLint("DefaultLocale")
    public static String formatLogMessage(String message, logLevel level, ElapsedTime timer) {
        if (timer != null) {
            return String.format("[%f]\t [%s] %s", timer.seconds(), level.name(), message);
        }
        else {
            return String.format("[%s] %s", level.name(), message);
        }
    }
}
