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
     * Checks if the specified button has been held long enough to trigger an initial action,
     * and then continues to return {@code true} at fixed intervals while the button remains held.
     *
     * @param button the button to check for press-and-hold behavior
     * @param initialDelayMs the delay in milliseconds before the first repeated trigger occurs
     *                       after the button is initially pressed
     * @param intervalMs the interval in milliseconds between subsequent triggers
     *                   while the button continues to be held
     * @return {@code true} if the button press should trigger an action at this time;
     *         {@code false} otherwise
     */
    protected boolean pressAndHold(Button button, long initialDelayMs, long intervalMs) {
        return input.pressAndHold(button, initialDelayMs, intervalMs);
    }

    /**
     * Checks if the specified button has been held long enough to trigger an initial action,
     * then repeatedly returns {@code true} at accelerating intervals while the button remains held.
     *
     * @param button          the button to check
     * @param initialDelayMs  the delay in milliseconds before auto-repeat begins
     * @param intervalMs      the initial repeat interval in milliseconds
     * @param speedupPercent  the percentage decrease in interval after each repeat
     *                        (e.g. {@code 10} means each repeat is 10% faster)
     */
    protected boolean pressAndHold(Button button, long initialDelayMs, long intervalMs, double speedupPercent) {
        return input.pressAndHold(button, initialDelayMs, intervalMs, speedupPercent);
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
