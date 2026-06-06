package com.skeletonarmy.marrow.ivy;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * A command that runs a given command and, if a condition is met,
 * retries it (or a different command) up to a specified number of times.
 * <p>
 * This command is useful for actions that may not succeed on the first attempt
 * and require re-running, such as vision alignment or precise mechanism movement.
 */
public class RetryCommand implements Command {
    private final Command command;
    private final Command retryCommand;
    private final BooleanSupplier successCondition;
    private final int maxRetries;

    private Command currentCommand;
    private int retryCount = 0;
    private boolean isFinished = false;

    /**
     * Creates a new RetryCommand.
     *
     * @param command          The command to run on the first attempt.
     * @param retryCommand     The command to run on retry.
     * @param successCondition A condition that returns {@code false} if a retry should be
     *                         attempted, or {@code true} if the command should finish without retrying.
     * @param maxRetries       The maximum number of retries allowed.
     */
    public RetryCommand(
            Command command,
            Command retryCommand,
            BooleanSupplier successCondition,
            int maxRetries
    ) {
        this.command = command;
        this.retryCommand = retryCommand;
        this.successCondition = successCondition;
        this.maxRetries = maxRetries;
    }

    /**
     * Creates a new RetryCommand where the retry command is the same as the initial one.
     *
     * @param command          The command to run on the first attempt.
     * @param successCondition A condition that returns {@code false} if a retry should be
     *                         attempted, or {@code true} if the command should finish without retrying.
     * @param maxRetries       The maximum number of retries allowed.
     */
    public RetryCommand(
            Command command,
            BooleanSupplier successCondition,
            int maxRetries
    ) {
        this(command, command, successCondition, maxRetries);
    }

    @Override
    public void start() {
        isFinished = false;
        retryCount = 0;

        currentCommand = command;
        currentCommand.start();
    }

    @Override
    public void execute() {
        // If the sub-command is not finished, execute it
        if (!currentCommand.done()) {
            currentCommand.execute();
            return;
        }

        currentCommand.end(EndCondition.NATURALLY);

        // Check if we should retry
        if (retryCount < maxRetries && !successCondition.getAsBoolean()) {
            retryCount++;
            currentCommand = retryCommand;
            currentCommand.start();
        } else {
            isFinished = true;
        }
    }

    @Override
    public boolean done() {
        return isFinished;
    }

    @Override
    public void end(EndCondition endCondition) {
        // When RetryCommand is ended (for any reason), we must also end the sub-command it is currently managing
        if (currentCommand != null) {
            currentCommand.end(endCondition);
        }
    }

    @Override
    public Set<Object> requirements() {
        Set<Object> requirements = new HashSet<>();
        requirements.addAll(command.requirements());
        requirements.addAll(retryCommand.requirements());
        return requirements;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public InterruptedBehavior interruptedBehavior() {
        return InterruptedBehavior.END;
    }

    @Override
    public BlockedBehavior blockedBehavior() {
        return BlockedBehavior.CANCEL;
    }

    @Override
    public ConflictBehavior conflictBehavior() {
        return ConflictBehavior.OVERRIDE;
    }
}
