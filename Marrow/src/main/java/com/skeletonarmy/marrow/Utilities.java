package com.skeletonarmy.marrow;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.HashMap;
import java.util.List;

public class Utilities {
    private static final HashMap<String, Boolean> lastButtonStates = new HashMap<>();

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

    /**
     * Checks if an input was pressed (i.e., transitioned from false to true).
     *
     * @param input The current state of the input (true if pressed, false otherwise).
     * @return True if the input transitioned from false to true (was just pressed), false otherwise.
     */
    public static boolean isPressed(boolean input) {
        String uniqueKey = generateCallSiteID();

        // Get the previous state (default to false if not tracked yet)
        boolean lastState = Boolean.TRUE.equals(lastButtonStates.getOrDefault(uniqueKey, false));

        // Update the stored state
        lastButtonStates.put(uniqueKey, input);

        // Return true if the last state was false and the current state is true (just pressed)
        return !lastState && input;
    }

    /**
     * Checks if an input was released (i.e., transitioned from true to false).
     *
     * @param input The current state of the input (true if pressed, false otherwise).
     * @return True if the input transitioned from true to false (was just released), false otherwise.
     */
    public static boolean isReleased(boolean input) {
        String uniqueKey = generateCallSiteID();

        // Get the previous state (default to false if not tracked yet)
        boolean lastState = Boolean.TRUE.equals(lastButtonStates.getOrDefault(uniqueKey, false));

        // Update the stored state
        lastButtonStates.put(uniqueKey, input);

        // Return true if the last state was true and the current state is false (just released)
        return lastState && !input;
    }

    /**
     * Remaps a value from one range to another using linear interpolation.
     *
     * @param value  The input value to remap.
     * @param inMin  The lower bound of the input range.
     * @param inMax  The upper bound of the input range.
     * @param outMin The lower bound of the output range.
     * @param outMax The upper bound of the output range.
     * @return The remapped value in the output range.
     */
    public static double remap(double value, double inMin, double inMid, double inMax, double outMin, double outMid, double outMax) {
        if (value <= inMid) {
            return outMin + (value - inMin) * (outMid - outMin) / (inMid - inMin);
        } else {
            return outMid + (value - inMid) * (outMax - outMid) / (inMax - inMid);
        }
    }
}
