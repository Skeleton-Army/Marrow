package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;

import java.util.HashMap;
import java.util.Map;

/**
 * INTERNAL USE ONLY.
 * This class is not part of the public API and may change without notice.
 */
class GamepadInput {
    private final Gamepad gamepad1;
    private final Gamepad gamepad2;

    private final Map<String, Boolean> previousStates = new HashMap<>();
    private final Map<String, Long> pressStartTimes = new HashMap<>();
    private final Map<String, Long> lastTriggerTimes = new HashMap<>();

    public GamepadInput(Gamepad gamepad1, Gamepad gamepad2) {
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;
    }

    /**
     * Checks if a specific button is currently pressed.
     *
     * @param key The button key to check.
     * @return true if the button is pressed on either gamepad, false otherwise.
     */
    public boolean isPressed(String key) {
        return getButtonState(key);
    }

    /**
     * Checks if a specific button was just pressed.
     *
     * @param key The button key to check.
     * @return true if the button was just pressed, false otherwise.
     */
    public boolean justPressed(String key) {
        boolean current = isPressed(key);
        boolean previous = Boolean.TRUE.equals(previousStates.getOrDefault(key, false));
        previousStates.put(key, current);
        return current && !previous;
    }

    /**
     * Checks if any of the specified buttons were just pressed.
     *
     * @param keys The button keys to check.
     * @return true if any of the buttons were just pressed, false otherwise.
     */
    public boolean anyJustPressed(String... keys) {
        for (String key : keys) {
            if (justPressed(key)) return true;
        }
        return false;
    }

    /**
     * Checks if the button has been held for more than the initial delay,
     * and continues to return true at a specified interval.
     *
     * @param key The button key to check.
     * @param initialDelayMs The initial time (in milliseconds) before the first trigger.
     * @param intervalMs The time (in milliseconds) between subsequent triggers.
     * @return True if the button is held and triggers on the interval, otherwise false.
     */
    public boolean isHeld(String key, long initialDelayMs, long intervalMs) {
        boolean isCurrentlyPressed = isPressed(key);
        long currentTime = System.currentTimeMillis();

        if (isCurrentlyPressed) {
            // Get the start and last trigger times.
            Long pressStartTime = pressStartTimes.get(key);
            Long lastTriggerTime = lastTriggerTimes.get(key);

            // This is the very first time the button is pressed.
            if (pressStartTime == null || lastTriggerTime == null) {
                pressStartTimes.put(key, currentTime);
                lastTriggerTimes.put(key, currentTime);
                return true;
            }

            // Calculate time elapsed since the press started and since the last trigger.
            long timeSincePressStart = currentTime - pressStartTime;
            long timeSinceLastTrigger = currentTime - lastTriggerTime;

            // Return true if the initial delay and subsequent interval conditions are met.
            if (timeSincePressStart > initialDelayMs && timeSinceLastTrigger > intervalMs) {
                lastTriggerTimes.put(key, currentTime);
                return true;
            }
        } else {
            // Button is not pressed, so reset its state for the next press.
            pressStartTimes.remove(key);
            lastTriggerTimes.remove(key);
        }

        return false;
    }

    private boolean getButtonState(String key) {
        switch (key) {
            case "a":
                return gamepad1.a || gamepad2.a;
            case "b":
                return gamepad1.b || gamepad2.b;
            case "x":
                return gamepad1.x || gamepad2.x;
            case "y":
                return gamepad1.y || gamepad2.y;
            case "dpad_up":
                return gamepad1.dpad_up || gamepad2.dpad_up;
            case "dpad_down":
                return gamepad1.dpad_down || gamepad2.dpad_down;
            case "dpad_left":
                return gamepad1.dpad_left || gamepad2.dpad_left;
            case "dpad_right":
                return gamepad1.dpad_right || gamepad2.dpad_right;
            case "guide":
                return gamepad1.guide || gamepad2.guide;
            case "start":
                return gamepad1.start || gamepad2.start;
            case "back":
                return gamepad1.back || gamepad2.back;
            case "left_bumper":
                return gamepad1.left_bumper || gamepad2.left_bumper;
            case "right_bumper":
                return gamepad1.right_bumper || gamepad2.right_bumper;
            case "left_stick_button":
                return gamepad1.left_stick_button || gamepad2.left_stick_button;
            case "right_stick_button":
                return gamepad1.right_stick_button || gamepad2.right_stick_button;
            case "circle":
                return gamepad1.circle || gamepad2.circle;
            case "cross":
                return gamepad1.cross || gamepad2.cross;
            case "triangle":
                return gamepad1.triangle || gamepad2.triangle;
            case "square":
                return gamepad1.square || gamepad2.square;
            case "share":
                return gamepad1.share || gamepad2.share;
            case "options":
                return gamepad1.options || gamepad2.options;
            case "touchpad":
                return gamepad1.touchpad || gamepad2.touchpad;
            case "touchpad_finger_1":
                return gamepad1.touchpad_finger_1 || gamepad2.touchpad_finger_1;
            case "touchpad_finger_2":
                return gamepad1.touchpad_finger_2 || gamepad2.touchpad_finger_2;
            case "ps":
                return gamepad1.ps || gamepad2.ps;
            default:
                return false;
        }
    }
}
