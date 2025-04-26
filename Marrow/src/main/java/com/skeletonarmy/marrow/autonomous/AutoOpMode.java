package com.skeletonarmy.marrow.autonomous;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.skeletonarmy.marrow.MarrowUtils;
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
  private final FtcDashboard dash = FtcDashboard.getInstance();

  private final Map<Runnable, Double> states = new HashMap<>();

  private List<Action> runningActions = new ArrayList<>();
  private final List<Runnable> runningFunctions = new ArrayList<>();

  private Runnable currentState = null;

  private ChoiceMenu choiceMenu;

  private Supplier<Boolean> fallbackCondition = () -> false;
  private Runnable fallbackFunction  = () -> {};
  private boolean didFallback = false;

  protected ElapsedTime runtime = new ElapsedTime();

  public abstract void preAutonomousSetup();
  public abstract void setInitialState();

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

    setInitialState();
  }

  private void internalLoop() {
    if (currentState != null) {
      // Retrieve the time and execute the state
      Double requiredTime = states.get(currentState);

      if (requiredTime != null) {
        telemetry.addData("State", currentState.toString());
        currentState.run();
      } else {
        telemetry.addData("Error", "No handler for current state: " + currentState.toString());
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

        addState(runnable, ann.requiredTime());
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

  private void addState(Runnable stateMethod, double requiredTime) {
    states.put(stateMethod, requiredTime);
  }

  private void setCurrentState(Runnable newState) {
    currentState = newState;
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
   * @param stateMethod The state to transition to
   */
  protected void transition(Runnable stateMethod) {
    setCurrentState(stateMethod);
  }

  /**
   * Adds a conditional transition to the FSM.
   * @param condition The condition to check
   * @param stateMethod The state to transition to if the condition is true
   */
  protected void conditionalTransition(boolean condition, Runnable stateMethod) {
    if (condition) {
      setCurrentState(stateMethod);
    }
  }

  /**
   * Adds a conditional transition to the FSM.
   * @param trueState The state to transition to if the condition is true
   * @param falseState The state to transition to if the condition is false
   * @param condition The condition to check
   */
  protected void conditionalTransition(boolean condition, Runnable trueState, Runnable falseState) {
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
   * @param stateMethod The state to check.
   * @return {@code true} if the remaining time is sufficient to complete the state,
   *         {@code false} if there is not enough time.
   */
  protected boolean isEnoughTime(Runnable stateMethod) {
    Double requiredTime = states.get(stateMethod);
    return (requiredTime == null) || isEnoughTime(requiredTime);
  }

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
