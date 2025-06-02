package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;

import java.util.function.Supplier;

/**
 * An {@link Action} that runs a given action only if the specified condition is true.
 */
public class ConditionAction implements Action {
    private final Action action;
    private final Supplier<Boolean> condition;

    private boolean shouldRun = false;

    public ConditionAction(Action action, Supplier<Boolean> condition) {
        this.action = action;
        this.condition = condition;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        if (!shouldRun) {
            shouldRun = condition.get();
        }

        if (shouldRun) {
            return action.run(telemetryPacket);
        }

        return false;
    }
}
