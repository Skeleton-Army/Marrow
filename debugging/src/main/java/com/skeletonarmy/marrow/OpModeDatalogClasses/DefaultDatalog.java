package com.skeletonarmy.marrow.OpModeDatalogClasses;

import com.skeletonarmy.marrow.Datalogger;

import java.io.File;

public class DefaultDatalog {
    private final Datalogger datalogger;
    public Datalogger.GenericField opModeStatus = new Datalogger.GenericField("OpModeStatus");
    public Datalogger.GenericField loopCounter  = new Datalogger.GenericField("Loop Counter");
    public Datalogger.GenericField intakePos = new Datalogger.GenericField("Intake Position");
    public Datalogger.GenericField intakeVel = new Datalogger.GenericField("Intake Velocity");
    public Datalogger.GenericField outtakePos = new Datalogger.GenericField("Outtake Position");
    public Datalogger.GenericField outtakeVel = new Datalogger.GenericField("Outtake Velocity");
    public Datalogger.GenericField specArmPos = new Datalogger.GenericField("SpecimenArm Position");
    public Datalogger.GenericField hangPos = new Datalogger.GenericField("Hang Position");
    public Datalogger.GenericField outtakeLimit = new Datalogger.GenericField("Outtake Limit Switch");
    public Datalogger.GenericField gamepad2X = new Datalogger.GenericField("Gamepad 2 X");
    public Datalogger.GenericField gamepad2Y = new Datalogger.GenericField("Gamepad 2 Y");
    public Datalogger .GenericField battery = new Datalogger.GenericField("Battery");
    public Datalogger.GenericField robotPosX = new Datalogger.GenericField("Robot Position X");
    public Datalogger.GenericField robotPosY = new Datalogger.GenericField("Robot Position Y");
    public Datalogger.GenericField robotAngle = new Datalogger.GenericField("Robot Angle");
    public Datalogger.GenericField yaw          = new Datalogger.GenericField("Yaw");
    public Datalogger.GenericField pitch        = new Datalogger.GenericField("Pitch");
    public Datalogger.GenericField roll         = new Datalogger.GenericField("Roll");
    public DefaultDatalog(File logFile) {
        datalogger = Datalogger.builder()
                .setFilename(logFile)
                .setAutoTimestamp(Datalogger.AutoTimestamp.DECIMAL_SECONDS)
                .setFields(
                        opModeStatus,
                        loopCounter,
                        intakePos,
                        intakeVel,
                        outtakePos,
                        outtakeVel,
                        specArmPos,
                        hangPos,
                        outtakeLimit,
                        gamepad2X,
                        gamepad2Y,
                        battery,
                        robotAngle,
                        robotPosX,
                        robotPosY,
                        yaw,
                        pitch,
                        roll
                )
                .build();
    }
    public void writeLine() {
        datalogger.writeLine();
    }
}
