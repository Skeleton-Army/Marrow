package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.skeletonarmy.marrow.AdvancedDcMotor;

@Config
public class MotorToPosition implements Action {
    private boolean initialized = false;

    private final DcMotorEx motor;
    private final int targetPos;
    private final boolean holdPosition;

    private boolean reachedVelocity;

    private final ElapsedTime timer = new ElapsedTime();

    public MotorToPosition(DcMotorEx motor, int targetPos, boolean holdPosition) {
        this.motor = motor;
        this.targetPos = targetPos;
        this.holdPosition = holdPosition;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        if (!initialized) {
            initialized = true;

            motor.setTargetPosition(targetPos);
            motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motor.setPower(1);

            timer.reset();
        }

        AdvancedDcMotor.updateAll();

        boolean isAtPosition = Math.abs(targetPos - motor.getCurrentPosition()) <= motor.getTargetPositionTolerance();

        // Reached target position
        if (isAtPosition) {
            if (holdPosition) {
                motor.setTargetPosition(motor.getCurrentPosition());
            } else {
                motor.setPower(0);
            }
        }

        return !isAtPosition;
    }
}
