package com.skeletonarmy.marrow.autonomous;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;

import java.util.ArrayList;
import java.util.List;

public class ActionRunner {
    private final FtcDashboard dash = FtcDashboard.getInstance();

    private List<Action> asyncActions = new ArrayList<>();

    /**
     * Runs an {@link Action} in a non-blocking loop.
     *
     * @param action The {@link Action} to be run.
     */
    public void runAsync(Action action) {
        asyncActions.add(action);
    }

    /**
     * Runs a single {@link Action} synchronously for one cycle.
     * Also updates and runs any queued async actions.
     *
     * @param action The action to run.
     * @return {@code true} if the action is still running and should be called again, {@code false} if it is finished.
     */
    public boolean runAction(Action action) {
        TelemetryPacket packet = new TelemetryPacket();

        boolean result = action.run(packet);
        dash.sendTelemetryPacket(packet);

        runAsyncActions();

        return result;
    }

    /**
     * Runs all queued actions.
     */
    public void runAsyncActions() {
        TelemetryPacket packet = new TelemetryPacket();

        // Update running actions
        List<Action> newActions = new ArrayList<>();

        for (Action action : asyncActions) {
            action.preview(packet.fieldOverlay());
            if (action.run(packet)) {
                newActions.add(action);
            }
        }

        asyncActions = newActions;

        dash.sendTelemetryPacket(packet);
    }

    /**
     * Clears all currently running asynchronous actions.
     */
    public void clear() {
        asyncActions.clear();
    }
}