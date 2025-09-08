package com.skeletonarmy.marrow.solverslib;

import com.rowanmcalpin.nextftc.core.command.Command;
import com.rowanmcalpin.nextftc.core.command.CommandManager;

import java.util.function.BooleanSupplier;

/**
 * A command that runs a given command and, if a condition is met,
 * retries it (or a different command) up to a specified number of times.
 * <p>
 * This command is useful for actions that may not succeed on the first attempt
 * and require re-running, such as vision alignment or precise mechanism movement.
 */
public class RetryCommand extends Command {
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
     * @param command        Supplies the command to run on the first attempt.
     * @param retryCommand   A function that takes the retry count (starting at 1) and returns the command for that attempt.
     * @param successCondition A condition that returns {@code true} if a retry should be attempted, or {@code false} if the command should finish without retrying.
     * @param maxRetries     The maximum number of retries allowed.
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
     * @param command        A supplier that creates a new instance of the command to run.
     * @param successCondition A condition that returns {@code true} if a retry should be attempted, or {@code false} if the command should finish without retrying.
     * @param maxRetries     The maximum number of retries allowed.
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
        CommandManager.INSTANCE.scheduleCommand(currentCommand);
        CommandManager.INSTANCE.scheduleCommands();
    }

    @Override
    public void update() {
        // If the sub-command is not finished and still running (not interrupted)
        if (!currentCommand.isDone() && CommandManager.INSTANCE.getRunningCommands().contains(currentCommand)) {
            return;
        }

        // Check if we should retry
        if (retryCount < maxRetries && !successCondition.getAsBoolean()) {
            retryCount++;
            currentCommand = retryCommand;
            CommandManager.INSTANCE.scheduleCommand(currentCommand);
        } else {
            isFinished = true;
        }
    }

    @Override
    public boolean isDone() {
        return isFinished;
    }
}