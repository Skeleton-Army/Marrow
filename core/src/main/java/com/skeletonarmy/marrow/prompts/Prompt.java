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

    /**
     * Checks if a specific button is currently pressed.
     */
    protected boolean isPressed(String key) {
        return input.isPressed(key);
    }

    /**
     * Checks if a specific button was just pressed.
     */
    protected boolean justPressed(String key) {
        return input.justPressed(key);
    }

    /**
     * Checks if any of the specified buttons were just pressed.
     */
    protected boolean anyJustPressed(String... keys) {
        return input.anyJustPressed(keys);
    }

    /**
     * Checks if the button has been held for more than the initial delay,
     * and continues to return true at a specified interval.
     */
    protected boolean isHeld(String key, long initialDelayMs, long intervalMs) {
        return input.isHeld(key, initialDelayMs, intervalMs);
    }

    /**
     * Adds a line to the Telemetry.
     */
    protected void addLine(String lineCaption) {
        telemetry.addLine(lineCaption);
    }

    /**
     * Adds data to the Telemetry.
     */
    protected void addData(String caption, Object value) {
        telemetry.addData(caption, value);
    }
}
