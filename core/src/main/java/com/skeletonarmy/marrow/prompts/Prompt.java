package com.skeletonarmy.marrow.prompts;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public abstract class Prompt<T> {
    private GamepadInput input;
    private Telemetry telemetry;

    public abstract T process();

    /**
     * Injects dependencies into the prompt.
     * This method is package-private to allow Prompter to configure the prompt
     * without exposing these dependencies publicly.
     */
    void configure(GamepadInput input, Telemetry telemetry) {
        this.input = input;
        this.telemetry = telemetry;
    }

    // Helper functions

    protected boolean isPressed(String key) {
        return input.isPressed(key);
    }

    protected boolean justPressed(String key) {
        return input.justPressed(key);
    }

    protected boolean anyJustPressed(String... keys) {
        return input.anyJustPressed(keys);
    }

    protected void addLine(String lineCaption) {
        telemetry.addLine(lineCaption);
    }

    protected void addData(String caption, Object value) {
        telemetry.addData(caption, value);
    }
}
