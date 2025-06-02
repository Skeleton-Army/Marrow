package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;

import java.util.function.Supplier;

/**
 * An {@link Action} that sleeps until a given condition becomes true.
 */
public class SleepUntilAction implements Action {
    private final Supplier<Boolean> condition;

    /**
     * Sleeps until the condition is true.
     */
    public SleepUntilAction(Supplier<Boolean> condition) {
        this.condition = condition;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        return !condition.get(); // Re-evaluate the condition each time
    }
}
