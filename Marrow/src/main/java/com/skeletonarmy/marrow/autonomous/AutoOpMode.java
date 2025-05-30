package com.skeletonarmy.marrow.autonomous;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.skeletonarmy.marrow.AdvancedDcMotor;
import com.skeletonarmy.marrow.MarrowGamepad;
import com.skeletonarmy.marrow.MarrowUtils;
import com.skeletonarmy.marrow.prompts.Prompt;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/*
    An OpMode that implements the choice menu and FSM (Finite State Machine).
    The base enhanced OpMode for autonomous programs.
 */
public abstract class AutoOpMode extends LinearOpMode {
  private static class StateEntry {
    Runnable runnable;
    double requiredTime;
    String timeoutState;
    double forceExitTime;

    StateEntry(Runnable runnable, double requiredTime, String timeoutState, double forceExitTime) {
      this.runnable = runnable;
      this.requiredTime = requiredTime;
      this.timeoutState = timeoutState;
      this.forceExitTime = forceExitTime;
    }
  }

  private final FtcDashboard dash = FtcDashboard.getInstance();

  private final Map<String, StateEntry> states = new HashMap<>();

  private List<Action> runningActions = new ArrayList<>();

  private String currentState = null;

  private ChoiceMenu choiceMenu;

  private Supplier<Boolean> fallbackCondition = () -> false;
  private Runnable fallbackFunction = () -> {};
  private boolean didFallback = false;

  private double autonomousDuration = 30;

  protected ElapsedTime runtime = new ElapsedTime();

  public MarrowGamepad gamepad1;
  public MarrowGamepad gamepad2;

  public abstract void onStateMachineStart();

  public abstract void preAutonomousSetup();

  public abstract void onInit();
  public void onInitLoop() {};
  public void onStart() {};
  public void onLoop() {};
  public void onStop() {};

  @Override
  public void runOpMode() {
    internalEarlyInit();
    onInit();
    internalLateInit();

    while (!isStarted() && !isStopRequested()) {
      onInitLoop();
      internalInitLoop();
    }

    waitForStart();

    onStart();
    internalStart();

    while (opModeIsActive() && !isStopRequested()) {
      internalLoop();
    }

    internalStop();
    onStop();
  }

  private void internalEarlyInit() {
    // Enable auto bulk reads
    MarrowUtils.setBulkReadsMode(hardwareMap, LynxModule.BulkCachingMode.AUTO);

    telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

    gamepad1 = new MarrowGamepad(this, super.gamepad1);
    gamepad2 = new MarrowGamepad(this, super.gamepad2);

    choiceMenu = new ChoiceMenu(this, gamepad1, gamepad2);

    registerStates();
  }

  private void internalLateInit() {
    new Thread(this::preAutonomousSetup).start();
  }

  private void internalInitLoop(){
    runAsyncTasks();
  }

  private void internalStart() {
    runtime.reset();

    onStateMachineStart();
  }

  private void internalLoop() {
    if (currentState != null) {
      StateEntry stateEntry = states.get(currentState);

      if (stateEntry != null) {
        telemetry.addData("State", currentState);
        stateEntry.runnable.run();
      } else {
        telemetry.addData("Error", "No handler for current state: " + currentState);
      }
    }

    runAsyncTasks();
  }

  private void internalStop() {
    runningActions.clear();
  }

  private void runAsyncTasks() {
    AdvancedDcMotor.updateAll();
    onLoop();
    runAsyncActions();
    telemetry.update();
  }

  private void registerStates() {
    for (var method : getClass().getDeclaredMethods()) {
      State ann = method.getAnnotation(State.class);

      if (ann != null) {
        method.setAccessible(true);

        Runnable runnable = () -> {
          try {
            method.invoke(this);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        };

        addState(
                method.getName(),
                new StateEntry(runnable, ann.requiredTime(), ann.timeoutState(), ann.forceExitTime())
        );
      }
    }
  }

  /**
   * Runs all queued actions.
   */
  private void runAsyncActions() {
    TelemetryPacket packet = new TelemetryPacket();

    // Update running actions
    List<Action> newActions = new ArrayList<>();

    for (Action action : runningActions) {
      action.preview(packet.fieldOverlay());
      if (action.run(packet)) {
        newActions.add(action);
      }
    }

    runningActions = newActions;

    dash.sendTelemetryPacket(packet);
  }

  /**
   * Runs an action in a blocking loop.
   */
  protected void runBlocking(Action action) {
    TelemetryPacket packet = new TelemetryPacket();

    StateEntry stateEntry = states.get(currentState);

    while (action.run(packet) && opModeIsActive()) {
      if (fallbackCondition.get() && !didFallback) {
        didFallback = true;
        fallbackFunction.run();

        requestOpModeStop();
        break;
      }

      if (stateEntry != null && !stateEntry.timeoutState.isEmpty() && !isEnoughTime(stateEntry.forceExitTime)) {
        if (states.get(stateEntry.timeoutState) == null) {
          throw new RuntimeException("State not found (timeoutState): " + stateEntry.timeoutState);
        }

        transition(stateEntry.timeoutState);

        break;
      }

      runAsyncTasks();
    }
  }

  private void addState(String stateName, StateEntry state) {
    states.put(stateName, state);
  }

  private void setCurrentState(String stateName) {
    currentState = stateName;
  }

  /**
   * Runs an action in a non-blocking loop.
   */
  protected void runAsync(Action action) {
    runningActions.add(action);
  }

  /**
   * Transitions to the specified state.
   * <p>
   * If there is insufficient time to complete the target state,
   * transitions to its configured timeout fallback state instead, if defined.
   * </p>
   *
   * @param stateName The name of the state to transition to
   */
  protected void transition(String stateName) {
    StateEntry stateEntry = states.get(stateName);

    if (stateEntry == null) {
      throw new RuntimeException("State not found: " + stateName);
    }

    // If not enough time, transition to timeoutState instead
    if (!stateEntry.timeoutState.isEmpty()) {
      if (states.get(stateEntry.timeoutState) == null) {
        throw new RuntimeException("State not found (timeoutState): " + stateEntry.timeoutState);
      }

      if (!isEnoughTime(stateName)) {
        stateName = stateEntry.timeoutState;
      }
    }

    setCurrentState(stateName);
  }

  /**
   * Transitions to the specified state if the given condition is true.
   *
   * @param condition The condition to evaluate
   * @param stateName The name of the state to transition to, if the condition is true
   */
  protected void conditionalTransition(boolean condition, String stateName) {
    if (condition) {
      transition(stateName);
    }
  }

  /**
   * Conditionally transitions to one of two states based on the given condition.
   * <p>
   * If the condition is true, transitions to {@code trueState}; otherwise, transitions to {@code falseState}.
   * </p>
   *
   * @param trueState The name of the state to transition to, if the condition is true
   * @param falseState The name of the state to transition to, if the condition is false
   * @param condition The condition to evaluate
   */
  protected void conditionalTransition(boolean condition, String trueState, String falseState) {
    transition(condition ? trueState : falseState);
  }

  /**
   * Gets the remaining time in the autonomous period in seconds.
   */
  protected double getRemainingTime() {
    return autonomousDuration - runtime.seconds();
  }

  /**
   * Determines whether there is enough remaining time to complete a task.
   *
   * @param requiredTime The time required (in seconds) to complete the task
   * @return {@code true} if the remaining time is sufficient to complete the task,
   *         {@code false} otherwise.
   */
  protected boolean isEnoughTime(double requiredTime) {
    return getRemainingTime() >= requiredTime;
  }

  /**
   * Determines whether there is enough remaining time to complete a state.
   *
   * @param stateName The state to check
   * @return {@code true} if the remaining time is sufficient to complete the state,
   *         {@code false} otherwise.
   */
  protected boolean isEnoughTime(String stateName) {
    StateEntry info = states.get(stateName);
    return (info == null) || isEnoughTime(info.requiredTime);
  }

  /**
   * Determines whether there is enough remaining time to complete the currently active state.
   *
   * @return {@code true} if the remaining time is sufficient to complete the state,
   *         {@code false} otherwise.
   */
  protected boolean isEnoughTime() {
    if (currentState == null) return true;
    return isEnoughTime(currentState);
  }

  /**
   * Sets a fallback state for the FSM.
   *
   * @param condition The condition to check
   * @param handler The function to run if the condition is true
   */
  protected void setFallbackState(Supplier<Boolean> condition, Runnable handler) {
    fallbackCondition = condition;
    fallbackFunction = handler;
  }

  /**
   * Prompts the user with a specified {@link Prompt} and waits for a selection.
   *
   * @param prompt The {@link Prompt} to present to the user
   * @return The selected result of type {@code T}
   */
  protected <T> T prompt(Prompt<T> prompt) {
    return choiceMenu.prompt(prompt);
  }

  /**
   * Sets the duration of the autonomous period, typically used for debugging purposes.
   * This value is used in {@link #isEnoughTime()}.
   * The default duration is 30 seconds.
   *
   * @param time The autonomous time in seconds.
   */
  protected void setAutonomousTime(double time) {
    autonomousDuration = time;
  }
}
