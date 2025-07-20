package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public abstract class Prompt<T> {
    protected final String header;

    public Prompt(String header) {
        this.header = header;
    }

    public abstract T process(Gamepad gamepad1, Gamepad gamepad2, Telemetry telemetry);
}
