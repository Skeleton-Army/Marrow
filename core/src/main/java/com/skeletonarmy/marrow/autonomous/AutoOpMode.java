package com.skeletonarmy.marrow.autonomous;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.skeletonarmy.marrow.AdvancedDcMotor;
import com.skeletonarmy.marrow.MarrowGamepad;
import com.skeletonarmy.marrow.MarrowUtils;
import com.skeletonarmy.marrow.prompts.ChoiceMenu;

import java.util.function.Supplier;

/*
    An OpMode that implements a choice menu and a FSM (Finite State Machine).
    The base enhanced OpMode for autonomous programs.
 */
public abstract class AutoOpMode extends LinearOpMode {
  public MarrowGamepad gamepad1, gamepad2;
  public ChoiceMenu choiceMenu;

  private FiniteStateMachine stateMachine;
  private ActionRunner actionRunner;

  private double lastOpModeTime = -1;

  public abstract void onInit();
  public void onInitLoop() {}
  public void onStart() {}
  public void onLoop() {}
  public void onStop() {}

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

    stateMachine = new FiniteStateMachine(this);
    actionRunner = new ActionRunner();
  }

  private void internalLateInit() {}

  private void internalInitLoop(){
    runAsyncTasks();
  }

  private void internalStart() {
    stateMachine.start();
  }

  private void internalLoop() {
    stateMachine.runCurrentState();

    if (time != lastOpModeTime) {
      lastOpModeTime = time;

      telemetry.addData("Running State", stateMachine.getCurrentState());
      telemetry.addLine();

      onLoop();
    }

    runAsyncTasks();
  }

  private void internalStop() {}

  private void runAsyncTasks() {
    actionRunner.runAsyncActions();
    telemetry.update();
    AdvancedDcMotor.updateAll();
  }

  /**
   * Runs an {@link Action} in a blocking loop.
   *
   * @param action The {@link Action} to be run.
   */
  public void runBlocking(Action action) {
    while (actionRunner.runAction(action) && opModeIsActive()) {
      if (stateMachine.checkFallbackState()) return;
      if (stateMachine.checkForceExit()) return;

      internalLoop();
    }
  }

  /**
   * Runs an {@link Action} in a non-blocking loop.
   *
   * @param action The {@link Action} to be run.
   */
  protected void runAsync(Action action) {
    actionRunner.runAsync(action);
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
    stateMachine.transition(name);
  }

  /**
   * Sets a fallback state for the FSM.
   *
   * @param condition The condition to check
   * @param stateName The state to transition to if the condition is true
   */
  public void setFallbackState(Supplier<Boolean> condition, String stateName) {
    stateMachine.setFallbackState(condition, stateName);
  }

  /**
   * Sets the duration of the autonomous period. This method is typically used for debugging purposes.
   * The default duration is 30 seconds.
   *
   * @param time The autonomous time in seconds.
   */
  public void setAutonomousDuration(double time) {
    stateMachine.setDuration(time);
  }
}
