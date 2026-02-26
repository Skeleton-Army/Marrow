package com.skeletonarmy.marrow;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.robot.RobotState;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

public class OpModeManager {
    /**
     * Retrieves the internal OpModeManagerImpl instance.
     *
     * @return The active OpModeManagerImpl instance
     * @throws IllegalStateException if the activity or the OpModeManagerImpl are not yet available.
     */
    public static @NonNull OpModeManagerImpl getManager() {
        Activity activity = AppUtil.getInstance().getActivity();

        if (activity == null) {
            throw new IllegalStateException("OpModeManager called before activity is available. Ensure this runs after the robot is initialized.");
        }

        OpModeManagerImpl internalManager = OpModeManagerImpl.getOpModeManagerOfActivity(activity);

        if (internalManager == null) {
            throw new IllegalStateException("OpModeManagerImpl is null. The OpMode system may not be fully initialized.");
        }

        return internalManager;
    }

    /**
     * Retrieves the currently active OpMode.
     *
     * @return The active OpMode, or null if no OpMode is running.
     */
    public static @NonNull OpMode getActiveOpMode() {
        return getManager().getActiveOpMode();
    }

    /**
     * Retrieves the name of the currently active OpMode.
     *
     * @return The name of the active OpMode.
     */
    public static @NonNull String getActiveOpModeName() {
        return getManager().getActiveOpModeName();
    }

    /**
     * Retrieves the current state of the robot controller.
     *
     * @return The current RobotState enum (e.g., INIT, RUNNING).
     */
    public static @NonNull RobotState getRobotState() {
        return getManager().getRobotState();
    }

    /**
     * Retrieves the current HardwareMap.
     *
     * @return The active HardwareMap object.
     */
    public static @NonNull HardwareMap getHardwareMap() {
        return getManager().getHardwareMap();
    }

    /**
     * Retrieves the current Telemetry.
     *
     * @return The active Telemetry object.
     */
    public static @NonNull Telemetry getTelemetry() {
        return getActiveOpMode().telemetry;
    }

    /**
     * Registers a listener to receive notifications when the OpMode lifecycle changes (e.g., init, start, stop).
     *
     * @param listener The listener implementing OpModeManagerNotifier.Notifications
     * @return The active OpMode at the time of registration, or null
     */
    public static @NonNull OpMode registerListener(OpModeManagerNotifier.Notifications listener) {
        OpMode opMode = getManager().registerListener(listener);
        if (opMode == null) {
            throw new IllegalStateException("OpMode is not initialized!");
        }
        return opMode;
    }

    /**
     * Unregisters a listener previously registered for OpMode lifecycle notifications.
     *
     * @param listener The listener to remove
     */
    public static void unregisterListener(OpModeManagerNotifier.Notifications listener) {
        getManager().unregisterListener(listener);
    }
}
