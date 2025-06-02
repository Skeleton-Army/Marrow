package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.MarrowGamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public abstract class Prompt<T> {
    protected final String header;

    public Prompt(String header) {
        this.header = header;
    }

    public abstract T process(MarrowGamepad gamepad1, MarrowGamepad gamepad2, Telemetry telemetry);
}
