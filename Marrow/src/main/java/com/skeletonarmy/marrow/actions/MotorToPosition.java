package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.skeletonarmy.marrow.AdvancedDcMotor;

/**
 * An {@link Action} that commands a motor to move to a specific encoder position.
 * This action completes when the motor reaches the target position within the
 * motor's {@link DcMotorEx#getTargetPositionTolerance() target position tolerance},
 * or when an overcurrent condition is detected.
 */
@Config
public class MotorToPosition implements Action {
    private boolean initialized = false;

    private final DcMotorEx motor;
    private final int targetPos;

    private final ElapsedTime timer = new ElapsedTime();

    public MotorToPosition(DcMotorEx motor, int targetPos) {
        this.motor = motor;
        this.targetPos = targetPos;
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

        return !isAtPosition && !motor.isOverCurrent();
    }
}
