package com.skeletonarmy.marrow.autonomous;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.skeletonarmy.marrow.MarrowUtils;
import com.skeletonarmy.marrow.fsm.State;
import com.skeletonarmy.marrow.prompts.Prompt;

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

    StateEntry(Runnable runnable, double requiredTime) {
      this.runnable = runnable;
      this.requiredTime = requiredTime;
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

  public abstract String initialState();

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

    choiceMenu = new ChoiceMenu(telemetry, gamepad1, gamepad2);

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

    setCurrentState(initialState());
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

        addState(method.getName(), new StateEntry(runnable, ann.requiredTime()));
      }
    }
  }

  /**
   * Run all queued actions.
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
   * Run all queued functions.
   */
  private void runAsyncFunctions() {
    for (Runnable func : runningFunctions) {
      func.run();
    }
  }

  /**
   * Run an action in a blocking loop.
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
   * Run an action in a non-blocking loop.
   */
  protected void runAsync(Action action) {
    runningActions.add(action);
  }

  /**
   * Run a function in a non-blocking loop.
   * @param func The function to run asynchronously
   */
  protected void runAsync(Runnable func) {
    runningFunctions.add(func);
  }

  /**
   * Adds a transition to the FSM.
   * @param stateName The state to transition to
   */
  protected void transition(String stateName) {
    setCurrentState(stateName);
  }

  /**
   * Adds a conditional transition to the FSM.
   * @param condition The condition to check
   * @param stateName The state to transition to if the condition is true
   */
  protected void conditionalTransition(boolean condition, String stateName) {
    if (condition) {
      setCurrentState(stateName);
    }
  }

  /**
   * Adds a conditional transition to the FSM.
   * @param trueState The state to transition to if the condition is true
   * @param falseState The state to transition to if the condition is false
   * @param condition The condition to check
   */
  protected void conditionalTransition(boolean condition, String trueState, String falseState) {
    setCurrentState(condition ? trueState : falseState);
  }

  /**
   * Gets the remaining time in the autonomous period in seconds.
   */
  protected double getRemainingTime() {
    return 30 - runtime.seconds();
  }

  /**
   * Checks if the remaining time in the autonomous period is sufficient to complete a given task.
   *
   * @param requiredTime The amount of time (in seconds) required to complete the task.
   * @return {@code true} if the remaining time is sufficient to complete the task,
   *         {@code false} if there is not enough time.
   */
  protected boolean isEnoughTime(double requiredTime) {
    return getRemainingTime() >= requiredTime;
  }

  /**
   * Checks if the remaining time in the autonomous period is sufficient to complete the state.
   *
   * @param stateName The state to check.
   * @return {@code true} if the remaining time is sufficient to complete the state,
   *         {@code false} if there is not enough time.
   */
  protected boolean isEnoughTime(String stateName) {
    StateEntry info = states.get(stateName);
    return (info == null) || isEnoughTime(info.requiredTime);
  }

//  /**
//   * Checks if the remaining time in the autonomous period is sufficient to complete the state.
//   *
//   * @param stateMethod The state to check.
//   * @return {@code true} if the remaining time is sufficient to complete the state,
//   *         {@code false} if there is not enough time.
//   */
//  protected boolean isEnoughTime(Runnable stateMethod) {
//    return isEnoughTime(getMethodName(stateMethod));
//  }

  /**
   * Checks if the remaining time in the autonomous period is sufficient to complete the currently active state.
   *
   * @return {@code true} if the remaining time is sufficient to complete the current state,
   *         {@code false} if there is not enough time.
   */
  protected boolean isEnoughTime() {
    if (currentState == null) return true;
    return isEnoughTime(currentState);
  }

  /**
   * Sets a fallback state for the FSM.
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
   * @param prompt the {@link Prompt} to present to the user
   * @return the selected result of type {@code T}
   */
  protected <T> T prompt(Prompt<T> prompt) {
    return choiceMenu.prompt(prompt);
  }
}
