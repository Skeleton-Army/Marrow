package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * An action that runs a given command and, if a condition is met,
 * retries it (or a different action) up to a specified number of times.
 * <p>
 * This is useful for actions that may not succeed on the first attempt
 * and require re-running, such as vision alignment or precise mechanism movement.
 */
public class RetryAction implements Action {
    private final Supplier<Action> initialActionSupplier;
    private final Supplier<Action> retryActionSupplier;
    private final BooleanSupplier successCondition;
    private final int maxRetries;

    private Action currentAction;
    private int retryCount = 0;
    private boolean initialized = false;

    /**
     * Creates a new RetryAction.
     *
     * @param initialAction    Supplies the action to run on the first attempt.
     * @param retryAction      Supplies the action to run on retry.
     * @param successCondition A condition that returns {@code false} if a retry should be attempted, or {@code true} if the action should finish without retrying.
     * @param maxRetries       The maximum number of retries allowed.
     */
    public RetryAction(
            Supplier<Action> initialAction,
            Supplier<Action> retryAction,
            BooleanSupplier successCondition,
            int maxRetries
    ) {
        this.initialActionSupplier = initialAction;
        this.retryActionSupplier = retryAction;
        this.successCondition = successCondition;
        this.maxRetries = maxRetries;
    }

    /**
     * Creates a new RetryAction where the retry action is the same as the initial one.
     *
     * @param action           A supplier that creates a new instance of the action to run.
     * @param successCondition A condition that returns {@code false} if a retry should be attempted, or {@code true} if the action should finish without retrying.
     * @param maxRetries       The maximum number of retries allowed.
     */
    public RetryAction(
            Supplier<Action> action,
            BooleanSupplier successCondition,
            int maxRetries
    ) {
        this(action, action, successCondition, maxRetries);
    }

    @Override
    public boolean run(@NonNull TelemetryPacket packet) {
        if (!initialized) {
            currentAction = initialActionSupplier.get();
            initialized = true;
        }

        boolean running = currentAction.run(packet);

        if (running) {
            return true;
        }

        if (!successCondition.getAsBoolean()) {
            return false;
        }

        if (retryCount < maxRetries) {
            retryCount++;
            currentAction = retryActionSupplier.get();
            return true;
        }

        return false;
    }
}