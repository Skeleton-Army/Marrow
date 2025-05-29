package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;

import java.util.function.Supplier;

/**
 * An {@link Action} that dynamically retrieves and runs an action supplied at runtime.
 * This allows using the latest action instance each time it starts running.
 */
public class DynamicAction implements Action {
    private final Supplier<Action> actionSupplier;
    private Action currentAction;

    public DynamicAction(Supplier<Action> actionSupplier) {
        this.actionSupplier = actionSupplier;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        if (currentAction == null) {
            currentAction = actionSupplier.get();
        }

        return currentAction.run(telemetryPacket);
    }
}
