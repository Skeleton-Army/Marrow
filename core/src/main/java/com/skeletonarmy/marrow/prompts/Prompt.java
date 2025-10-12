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
    protected boolean isPressed(Button button) {
        return input.isPressed(button);
    }

    /**
     * Checks if a specific button was just pressed.
     */
    protected boolean justPressed(Button button) {
        return input.justPressed(button);
    }

    /**
     * Checks if any of the specified buttons were just pressed.
     */
    protected boolean anyJustPressed(Button... buttons) {
        return input.anyJustPressed(buttons);
    }

    /**
     * Checks if the button has been held for more than the initial delay,
     * and continues to return true at a specified interval.
     */
    protected boolean pressAndHold(Button button, long initialDelayMs, long intervalMs) {
        return input.pressAndHold(button, initialDelayMs, intervalMs);
    }

    protected boolean pressAndHold(Button button, long initialDelayMs, long intervalMs, double speedupPercent, int minIntervalMs) {
        return input.pressAndHold(button, initialDelayMs, intervalMs, speedupPercent, minIntervalMs);
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
