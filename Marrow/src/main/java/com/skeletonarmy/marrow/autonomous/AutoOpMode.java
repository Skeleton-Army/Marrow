package com.skeletonarmy.marrow.autonomous;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.github.meanbeanlib.mirror.Executables;
import com.github.meanbeanlib.mirror.SerializableLambdas.SerializableConsumer0;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
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

    StateEntry(Runnable runnable, double requiredTime, String timeoutState) {
      this.runnable = runnable;
      this.requiredTime = requiredTime;
      this.timeoutState = timeoutState;
    }
  }

  private final FtcDashboard dash = FtcDashboard.getInstance();

  private final Map<String, StateEntry> states = new HashMap<>();

  private List<Action> runningActions = new ArrayList<>();
  private final List<Runnable> runningFunctions = new ArrayList<>();

  private String currentState = null;

  private ChoiceMenu choiceMenu;

  private Supplier<Boolean> fallbackCondition = () -> false;
  private Runnable fallbackFunction = () -> {};
  private boolean didFallback = false;

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
      onLoop();
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
    // Run in a separate thread to avoid blocking the main thread
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
    runningFunctions.clear();
  }

  private void runAsyncTasks() {
    runAsyncActions();
    runAsyncFunctions();
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
                new StateEntry(runnable, ann.requiredTime(), ann.timeoutState())
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
   * Runs all queued functions.
   */
  private void runAsyncFunctions() {
    for (Runnable func : runningFunctions) {
      func.run();
    }
  }

  /**
   * Runs an action in a blocking loop.
   */
  protected void runBlocking(Action action) {
    TelemetryPacket packet = new TelemetryPacket();

    while (action.run(packet) && opModeIsActive()) {
      if (fallbackCondition.get() && !didFallback) {
        didFallback = true;
        fallbackFunction.run();

        requestOpModeStop();
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
   * Runs a function in a non-blocking loop.
   *
   * @param func The function to run asynchronously
   */
  protected void runAsync(Runnable func) {
    runningFunctions.add(func);
  }

  /**
   * Transitions to the specified state.
   *
   * @param stateReference A method reference representing the state to transition to. (Example: this::state)
   */
  protected void transition(SerializableConsumer0 stateReference) {
    transition(getMethodName(stateReference));
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
    if (stateEntry.timeoutState != null && !stateEntry.timeoutState.isEmpty()) {
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
   * @param stateReference A method reference representing the state to transition to, if the condition is true
   */
  protected void conditionalTransition(boolean condition, SerializableConsumer0 stateReference) {
    if (condition) {
      transition(stateReference);
    }
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
   * @param trueState A method reference representing the state to transition to, if the condition is true
   * @param falseState A method reference representing the state to transition to, if the condition is false
   * @param condition The condition to evaluate
   */
  protected void conditionalTransition(boolean condition, SerializableConsumer0 trueState, SerializableConsumer0 falseState) {
    transition(condition ? trueState : falseState);
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
    return 30 - runtime.seconds();
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
   * Determines whether there is enough remaining time to complete a state.
   *
   * @param stateReference A method reference representing the state to check.
   * @return {@code true} if the remaining time is sufficient to complete the state,
   *         {@code false} otherwise.
   */
  protected boolean isEnoughTime(SerializableConsumer0 stateReference) {
    return isEnoughTime(getMethodName(stateReference));
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
   * Retrieves the method name from a given method reference. (Example: this::myFunction -> returns: "myFunction")
   *
   * @param method The {@link SerializableConsumer0} instance representing the method reference.
   * @return The name of the method referenced by the {@code SerializableConsumer0}.
   * @throws RuntimeException if the method cannot be found or invoked via reflection.
   */
  private static String getMethodName(SerializableConsumer0 method) {
    try {
      Method foundMethod = Executables.findMethod(method);
      return foundMethod.getName();
    } catch (Exception e) {
      throw new RuntimeException("Failed to get method name from SerializableConsumer0", e);
    }
  }
}
