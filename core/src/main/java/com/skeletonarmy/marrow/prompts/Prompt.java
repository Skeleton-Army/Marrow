package com.skeletonarmy.marrow.prompts;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public abstract class Prompt<T> {
    protected final String header;

    public Prompt(String header) {
        this.header = header;
    }

    public abstract T process(GamepadInput input, Telemetry telemetry);
}
