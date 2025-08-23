package com.skeletonarmy.marrow.automation.solverslib;

import com.seattlesolvers.solverslib.command.Command;
import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.command.CommandGroupBase;
import com.seattlesolvers.solverslib.command.Subsystem;

import java.util.function.BooleanSupplier;

/**
 * A command that runs a given command and, if a condition is met,
 * retries it (or a different command) up to a specified number of times.
 * <p>
 * This command is useful for actions that may not succeed on the first attempt
 * and require re-running, such as vision alignment or precise mechanism movement.
 */
public class RetryCommand extends CommandBase {
    private final Command command;
    private final Command retryCommand;
    private final BooleanSupplier retryCondition;
    private final int maxRetries;

    private Command currentCommand;
    private int retryCount = 0;
    private boolean isFinished = false;

    /**
     * Creates a new RetryCommand.
     *
     * @param command        Supplies the command to run on the first attempt.
     * @param retryCommand   A function that takes the retry count (starting at 1) and returns the command for that attempt.
     * @param retryCondition A condition that returns {@code true} if a retry should be attempted, or {@code false} if the command should finish without retrying.
     * @param maxRetries     The maximum number of retries allowed.
     */
    public RetryCommand(
            Command command,
            Command retryCommand,
            BooleanSupplier retryCondition,
            int maxRetries
    ) {
        CommandGroupBase.requireUngrouped(command, retryCommand);

        this.command = command;
        this.retryCommand = retryCommand;
        this.retryCondition = retryCondition;
        this.maxRetries = maxRetries;

        addRequirements(command.getRequirements().toArray(new Subsystem[0]));
        addRequirements(retryCommand.getRequirements().toArray(new Subsystem[0]));
    }

    /**
     * Creates a new RetryCommand where the retry command is the same as the initial one.
     *
     * @param command        A supplier that creates a new instance of the command to run.
     * @param retryCondition A condition that returns {@code true} if a retry should be attempted, or {@code false} if the command should finish without retrying.
     * @param maxRetries     The maximum number of retries allowed.
     */
    public RetryCommand(
            Command command,
            BooleanSupplier retryCondition,
            int maxRetries
    ) {
        this(command, command, retryCondition, maxRetries);
    }

    @Override
    public void initialize() {
        isFinished = false;
        retryCount = 0;

        // Get the initial command and initialize it directly
        currentCommand = command;
        currentCommand.initialize();
    }

    @Override
    public void execute() {
        // If the sub-command is not finished, execute it
        if (!currentCommand.isFinished()) {
            currentCommand.execute();
            return;
        }

        // --- If we reach here, the currentCommand has just finished ---

        // Properly end the command that just finished
        currentCommand.end(false);

        // Check if we should retry
        if (retryCount < maxRetries && retryCondition.getAsBoolean()) {
            // Yes, so run the retry command
            retryCount++;
            currentCommand = retryCommand;
            currentCommand.initialize();
        } else {
            // No, so we are completely finished.
            isFinished = true;
        }
    }

    @Override
    public void end(boolean interrupted) {
        // When RetryCommand is ended (for any reason), we must also end the sub-command it is currently managing
        if (currentCommand != null) {
            currentCommand.end(interrupted);
        }
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }
}
