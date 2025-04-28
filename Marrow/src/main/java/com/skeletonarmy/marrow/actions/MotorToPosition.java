package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

@Config
public class MotorToPosition implements Action {
    private boolean initialized = false;

    private final DcMotorEx motor;
    private final int targetPos;
    private final double power;
    private final boolean holdPosition;
    private final int velocityThreshold;

    private boolean reachedVelocity;

    private final ElapsedTime timer = new ElapsedTime();

    public MotorToPosition(DcMotorEx motor, int targetPos, double power, int velocityThreshold, boolean holdPosition) {
        this.motor = motor;
        this.targetPos = targetPos;
        this.power = power;
        this.velocityThreshold = velocityThreshold;
        this.holdPosition = holdPosition;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        if (!initialized) {
            initialized = true;

            motor.setTargetPosition(targetPos);
            motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motor.setPower(power);

            timer.reset();
        }

        double currentVelocity = motor.getVelocity();
        boolean lowVelocity = Math.abs(currentVelocity) < velocityThreshold;

        if (!lowVelocity) {
            reachedVelocity = true;
        }

        boolean shouldStop = reachedVelocity && lowVelocity;

        // Reached target position / physically stopped (End of action)
        if (shouldStop) {
            if (holdPosition) {
                motor.setTargetPosition(motor.getCurrentPosition());
                motor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                motor.setPower(power / 2);
            } else {
                motor.setPower(0);
            }
        }

        return !shouldStop;
    }
}
