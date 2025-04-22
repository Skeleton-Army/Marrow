package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.MarrowGamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public abstract class Prompt {
    protected final String key;
    protected final String header;

    public Prompt(String key, String header) {
        this.key = key;
        this.header = header;
    }

    public String getKey() {
        return key;
    }

    public abstract Object process(MarrowGamepad gamepad1, MarrowGamepad gamepad2, Telemetry telemetry);
}
