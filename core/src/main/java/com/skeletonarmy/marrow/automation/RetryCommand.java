package com.skeletonarmy.marrow.automation;

import com.seattlesolvers.solverslib.command.Command;
import com.seattlesolvers.solverslib.command.CommandBase;
import com.seattlesolvers.solverslib.command.Subsystem;

import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * A command that runs a given command and, if a condition is met,
 * retries it (or a different command) up to a specified number of times.
 * <p>
 * This command is useful for actions that may not succeed on the first attempt
 * and require re-running, such as vision alignment or precise mechanism movement.
 */
public class RetryCommand extends CommandBase {

    private final Supplier<Command> initialCommandSupplier;
    private final IntFunction<Command> retryCommandSupplier;
    private final BooleanSupplier retryCondition;
    private final int maxRetries;

    private Command currentCommand;
    private int retryCount = 0;
    private boolean isFinished = false;

    /**
     * Creates a new RetryCommand.
     *
     * @param initialCommandSupplier Supplies the command to run on the first attempt
     * @param retryCommandSupplier   A function that takes the retry count (starting at 1) and returns the command for that attempt
     * @param retryCondition         A condition that returns true if a retry should be attempted
     * @param maxRetries             The maximum number of retries allowed
     * @param requirements           The subsystems used by all commands
     */
    public RetryCommand(
            Supplier<Command> initialCommandSupplier,
            IntFunction<Command> retryCommandSupplier,
            BooleanSupplier retryCondition,
            int maxRetries,
            Subsystem... requirements
    ) {
        this.initialCommandSupplier = initialCommandSupplier;
        this.retryCommandSupplier = retryCommandSupplier;
        this.retryCondition = retryCondition;
        this.maxRetries = maxRetries;

        addRequirements(requirements);
    }

    /**
     * Creates a new RetryCommand.
     *
     * @param commandSupplier A supplier that creates a new instance of the command to run
     * @param retryCondition  A condition that returns true if a retry should be attempted
     * @param maxRetries      The maximum number of retries allowed
     * @param requirements    The subsystems used by the command
     */
    public RetryCommand(
            Supplier<Command> commandSupplier,
            BooleanSupplier retryCondition,
            int maxRetries,
            Subsystem... requirements
    ) {
        this(commandSupplier, i -> commandSupplier.get(), retryCondition, maxRetries, requirements);
    }

    @Override
    public void initialize() {
        isFinished = false;
        retryCount = 0;
        scheduleInitialCommand();
    }

    @Override
    public void execute() {
        if (currentCommand != null && !currentCommand.isScheduled()) {
            handleCommandCompletion();
        }
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted && currentCommand != null && currentCommand.isScheduled()) {
            currentCommand.cancel();
        }
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    private void handleCommandCompletion() {
        if (shouldRetry()) {
            executeRetry();
        } else {
            finishCommand();
        }
    }

    private boolean shouldRetry() {
        return retryCount < maxRetries && retryCondition.getAsBoolean();
    }

    private void scheduleInitialCommand() {
        currentCommand = initialCommandSupplier.get();
        currentCommand.schedule();
    }

    private void executeRetry() {
        retryCount++;
        currentCommand = retryCommandSupplier.apply(retryCount);
        currentCommand.schedule();
    }

    private void finishCommand() {
        isFinished = true;
    }
}
