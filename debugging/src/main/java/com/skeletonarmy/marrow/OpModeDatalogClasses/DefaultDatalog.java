package com.skeletonarmy.marrow.OpModeDatalogClasses;

import com.skeletonarmy.marrow.Datalogger;
import com.skeletonarmy.marrow.Datalogger.GenericField;

import java.io.File;

public class DefaultDatalog {
    private final Datalogger datalogger;
    public GenericField opModeStatus = new GenericField("OpModeStatus");
    public GenericField loopCounter  = new GenericField("Loop Counter");
    public GenericField gamepad1X = new GenericField("Gameoad 1 X");
    public GenericField gamepad1Y = new GenericField("Gamepad 2 Y");
    public GenericField gamepad2X = new GenericField("Gamepad 2 X");
    public GenericField gamepad2Y = new GenericField("Gamepad 2 Y");
    public GenericField battery = new GenericField("Battery");
    public GenericField robotPosX = new GenericField("Robot Position X");
    public GenericField robotPosY = new GenericField("Robot Position Y");
    public GenericField robotAngle = new GenericField("Robot Angle");
    public GenericField yaw = new GenericField("Yaw");
    public GenericField pitch = new GenericField("Pitch");
    public GenericField roll = new GenericField("Roll");
    public DefaultDatalog(File logFile) {
        datalogger = Datalogger.builder()
                .setFilename(logFile)
                .setAutoTimestamp(Datalogger.AutoTimestamp.DECIMAL_SECONDS)
                .setFields(
                        opModeStatus,
                        loopCounter,
                        gamepad1X,
                        gamepad1Y,
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
