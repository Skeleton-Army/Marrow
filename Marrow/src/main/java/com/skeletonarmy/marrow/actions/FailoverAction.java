package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;

/**
 * An {@link Action} that runs a primary action until a failover is triggered,
 * after which it runs a secondary failover action instead.
 */
public class FailoverAction implements Action {
    private final Action mainAction;
    private final Action failoverAction;
    private boolean failedOver = false;

    public FailoverAction(Action mainAction, Action failoverAction) {
        this.mainAction = mainAction;
        this.failoverAction = failoverAction;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        if (failedOver) {
            return failoverAction.run(telemetryPacket);
        }

        return mainAction.run(telemetryPacket);
    }

    public void failover() {
        failedOver = true;
    }
}
