package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;

public class InstantAction implements Action {
    private final Action action;

    public InstantAction(Action action) {
        this.action = action;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        action.run(telemetryPacket);

        return false;
    }
}