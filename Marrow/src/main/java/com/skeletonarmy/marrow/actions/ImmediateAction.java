package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;

/**
 * An {@link Action} that runs a given action once immediately without waiting for its completion.
 */
public class ImmediateAction implements Action {
    private final Action action;

    public ImmediateAction(Action action) {
        this.action = action;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        action.run(telemetryPacket);

        return false;
    }
}