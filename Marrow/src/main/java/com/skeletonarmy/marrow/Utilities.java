package com.skeletonarmy.marrow;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.HashMap;
import java.util.List;

public class Utilities {
    /**
     * Sets the bulk caching mode for all Lynx modules in the hardware map.
     * This method iterates through all available Lynx modules and applies the specified
     * bulk caching mode to each one.
     *
     * @param hardwareMap The hardware map containing all the Lynx modules to be updated.
     * @param mode The desired bulk caching mode to be set on all Lynx modules.
     */
    public static void setBulkReadsMode(HardwareMap hardwareMap, LynxModule.BulkCachingMode mode) {
        List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);

        for (LynxModule hub : allHubs) {
            hub.setBulkCachingMode(mode);
        }
    }

    /**
     * Generates an ID representing the location the method is called from.
     * The ID will include the class name, method name, and line number for each element in the stack trace.
     *
     * @return A string representing the full stack trace composition.
     */
    public static String generateCallSiteID() {
        // Get the stack trace of the current thread
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // StringBuilder is used here to efficiently build the final string
        StringBuilder idBuilder = new StringBuilder();

        // Loop through all stack trace elements and append their details to the ID
        for (int i = 1; i < stackTrace.length; i++) { // Start from 1 to skip the 'getStackTrace' method
            StackTraceElement caller = stackTrace[i];
            idBuilder.append(caller.getClassName())
                    .append("#")
                    .append(caller.getMethodName())
                    .append(":")
                    .append(caller.getLineNumber());

            if (i < stackTrace.length - 1) {
                idBuilder.append(" -> ");
            }
        }

        return idBuilder.toString();
    }
}
