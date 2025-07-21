package com.skeletonarmy.marrow.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.skeletonarmy.marrow.autonomous.State;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class FiniteStateMachine {
    private final ElapsedTime runtime = new ElapsedTime();
    private final Map<String, StateEntry> states = new LinkedHashMap<>();
    private final OpMode opMode;
    private double duration;
    private String currentState;

    private Supplier<Boolean> fallbackCondition = () -> false;
    private String fallbackState = "";

    private boolean hasRunCurrentState = false;
    private boolean resetRuntime = false;

    public FiniteStateMachine(OpMode opMode, double duration) {
        this.opMode = opMode;
        this.duration = duration;

        registerStates();
    }

    public FiniteStateMachine(OpMode opMode) {
        this.opMode = opMode;
        this.duration = 30;

        registerStates();
    }

    /**
     * Registers all methods in the OpMode that are annotated with {@link State}.
     * These methods become states in the FSM.
     */
    private void registerStates() {
        for (Method method : opMode.getClass().getDeclaredMethods()) {
            State ann = method.getAnnotation(State.class);

            if (ann != null) {
                method.setAccessible(true);

                Runnable runnable = () -> {
                    try {
                        method.invoke(opMode);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };

                states.put(method.getName(), new StateEntry(runnable, ann.requiredTime(), ann.timeoutState(), ann.forceExitTime()));
            }
        }
    }

    /**
     * Transitions to the specified state.
     * <p>
     * If there is insufficient time to complete the target state,
     * transitions to its configured timeout fallback state instead, if defined.
     * </p>
     *
     * @param name The name of the state to transition to
     */
    public void transition(String name) {
        StateEntry entry = states.get(name);
        if (entry == null) throw new RuntimeException("State not found: " + name);

        // If not enough time, transition to timeoutState instead
        if (!entry.timeoutState.isEmpty() && !isEnoughTime(name)) {
            if (!states.containsKey(entry.timeoutState))
                throw new RuntimeException("Timeout state not found: " + entry.timeoutState);

            if (!isEnoughTime(name)) {
                name = entry.timeoutState;
            }
        }

        currentState = name;
        hasRunCurrentState = false; // Reset so state runs once on next call
    }

    /**
     * Runs the current state exactly once.
     */
    public void run() {
        if (!resetRuntime) {
            resetRuntime = true;
            runtime.reset();
        }

        runState(getCurrentState());
    }

    /**
     * Internal method that executes the given state's logic once.
     *
     * @param name The name of the state to run.
     */
    private void runState(String name) {
        if (!hasRunCurrentState) {
            StateEntry entry = states.get(name);

            if (entry != null) {
                hasRunCurrentState = true;
                entry.runnable.run();
                hasRunCurrentState = false;
            }
            else throw new RuntimeException("State not found: " + currentState);
        }
    }

    /**
     * Gets the current active state.
     * If none is set, it returns the first registered state.
     *
     * @return The name of the current state.
     */
    public String getCurrentState() {
        if (currentState == null)
            currentState = states.keySet().iterator().next(); // If state is null, get the first registered state
        return currentState;
    }

    /**
     * Checks the fallback condition, and if true, runs the fallback state.
     *
     * @return {@code true} if fallback occurred, {@code false} otherwise.
     */
    public boolean checkFallbackState() {
        if (fallbackCondition.get()) {
            StateEntry fallbackStateEntry = states.get(fallbackState);

            if (fallbackStateEntry != null) {
                currentState = fallbackState;
                fallbackStateEntry.runnable.run();
            } else {
                throw new RuntimeException("State not found: " + fallbackState);
            }

            return true;
        }

        return false;
    }

    /**
     * Checks whether the currently running state should be force-exited due to
     * insufficient time left. If so, transitions to the timeout fallback state.
     *
     * @return {@code true} if force exit was triggered, {@code false} otherwise.
     */
    public boolean checkForceExit() {
        StateEntry stateEntry = states.get(getCurrentState());

        if (stateEntry != null && !stateEntry.timeoutState.isEmpty() && !isEnoughTime(stateEntry.forceExitTime)) {
            if (!states.containsKey(stateEntry.timeoutState))
                throw new RuntimeException("Timeout state not found: " + stateEntry.timeoutState);

            transition(stateEntry.timeoutState);

            return true;
        }

        return false;
    }

    /**
     * Gets the remaining time in the autonomous period in seconds.
     */
    private double getRemainingTime() {
        return duration - runtime.seconds();
    }

    /**
     * Determines whether there is enough remaining time to complete a task.
     *
     * @param requiredTime The time required (in seconds) to complete the task
     * @return {@code true} if the remaining time is sufficient to complete the task,
     * {@code false} otherwise.
     */
    private boolean isEnoughTime(double requiredTime) {
        return getRemainingTime() >= requiredTime;
    }

    /**
     * Determines whether there is enough remaining time to complete a state.
     *
     * @param stateName The state to check
     * @return {@code true} if the remaining time is sufficient to complete the state,
     * {@code false} otherwise.
     */
    private boolean isEnoughTime(String stateName) {
        StateEntry info = states.get(stateName);
        return (info == null) || isEnoughTime(info.requiredTime);
    }

    /**
     * Determines whether there is enough remaining time to complete the currently active state.
     *
     * @return {@code true} if the remaining time is sufficient to complete the state,
     * {@code false} otherwise.
     */
    private boolean isEnoughTime() {
        if (currentState == null) return true;
        return isEnoughTime(currentState);
    }

    /**
     * Sets a fallback state for the FSM.
     *
     * @param condition The condition to check
     * @param stateName The state to transition to if the condition is true
     */
    public void setFallbackState(Supplier<Boolean> condition, String stateName) {
        fallbackCondition = condition;
        fallbackState = stateName;
    }

    /**
     * Sets the duration of the game period. This method is typically used for debugging purposes.
     * The default duration is 30 seconds.
     *
     * @param time The autonomous time in seconds.
     */
    public void setDuration(double time) {
        duration = time;
    }
}
