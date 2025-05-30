package com.skeletonarmy.marrow.actions;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.robotcore.hardware.CRServo;

/**
 * An {@link Action} that sets power to a continuous rotation servo immediately.
 */
public class SetCRServoPowerAction implements Action  {
    private final CRServo crServo;
    private final double power;

    public SetCRServoPowerAction(CRServo crServo, double power) {
        this.crServo = crServo;
        this.power = power;
    }

    @Override
    public boolean run(@NonNull TelemetryPacket telemetryPacket) {
        crServo.setPower(power);

        return false; // Finish immediately
    }
}
