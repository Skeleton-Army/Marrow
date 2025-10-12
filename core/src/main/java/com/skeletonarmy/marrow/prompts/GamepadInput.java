package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;
import java.util.EnumMap;

/**
 * INTERNAL USE ONLY.
 * This class is not part of the public API and may change without notice.
 */
class GamepadInput {
    private final Gamepad gamepad1;
    private final Gamepad gamepad2;

    private final EnumMap<Button, Boolean> currentStates = new EnumMap<>(Button.class);
    private final EnumMap<Button, Boolean> previousStates = new EnumMap<>(Button.class);
    private final EnumMap<Button, Long> pressStartTimes = new EnumMap<>(Button.class);
    private final EnumMap<Button, Long> lastTriggerTimes = new EnumMap<>(Button.class);

    public GamepadInput(Gamepad gamepad1, Gamepad gamepad2) {
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;

        for (Button button : Button.values()) {
            currentStates.put(button, false);
            previousStates.put(button, false);
        }
    }

    /**
     * Should be called once per loop/frame to update the input state.
     */
    public void update() {
        for (Button button : Button.values()) {
            previousStates.put(button, currentStates.get(button));
            currentStates.put(button, getButtonState(button));
        }
    }

    /** Checks if a specific button is currently pressed. */
    public boolean isPressed(Button button) {
        return Boolean.TRUE.equals(currentStates.getOrDefault(button, false));
    }

    /** Checks if a specific button was just pressed. */
    public boolean justPressed(Button button) {
        return isPressed(button) && Boolean.FALSE.equals(previousStates.getOrDefault(button, false));
    }

    /** Checks if any of the specified buttons were just pressed. */
    public boolean anyJustPressed(Button... buttons) {
        for (Button button : buttons) {
            if (justPressed(button)) return true;
        }
        return false;
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
    public boolean pressAndHold(Button button, long initialDelayMs, long intervalMs) {
        long currentTime = System.currentTimeMillis();

        if (isPressed(button)) {
            Long pressStartTime = pressStartTimes.get(button);
            Long lastTriggerTime = lastTriggerTimes.get(button);

            if (pressStartTime == null || lastTriggerTime == null) {
                pressStartTimes.put(button, currentTime);
                lastTriggerTimes.put(button, currentTime);
                return true;
            }

            long timeSincePressStart = currentTime - pressStartTime;
            long timeSinceLastTrigger = currentTime - lastTriggerTime;

            if (timeSincePressStart > initialDelayMs && timeSinceLastTrigger > intervalMs) {
                lastTriggerTimes.put(button, currentTime);
                return true;
            }
        } else if (Boolean.TRUE.equals(previousStates.getOrDefault(button, false))) {
            // Only reset timers if the button was pressed last frame, i.e., actual release
            pressStartTimes.remove(button);
            lastTriggerTimes.remove(button);
        }

        return false;
    }

    /**
     * Checks if the specified button has been held long enough to trigger an initial action,
     * then repeatedly returns {@code true} at accelerating intervals while the button remains held.
     *
     * @param button          the button to check
     * @param initialDelayMs  the delay in milliseconds before auto-repeat begins
     * @param intervalMs      the initial repeat interval in milliseconds
     * @param speedupPercent  the percentage decrease in interval after each repeat (e.g. {@code 10} means each repeat is 10% faster)
     */
    public boolean pressAndHold(
            Button button,
            long initialDelayMs,
            long intervalMs,
            double speedupPercent
    ) {
        long now = System.currentTimeMillis();
        boolean pressed = Boolean.TRUE.equals(currentStates.get(button));
        boolean wasPressed = Boolean.TRUE.equals(previousStates.get(button));

        if (pressed) {
            long pressStart = pressStartTimes.getOrDefault(button, now);
            long lastTrigger = lastTriggerTimes.getOrDefault(button, now);

            if (!wasPressed) {
                pressStartTimes.put(button, now);
                lastTriggerTimes.put(button, now);
                return true;
            }

            long sinceStart = now - pressStart;
            long sinceLast = now - lastTrigger;

            // Compute easing for a soft first repeat before the full delay
            double delayProgress = Math.min(1.0, (double) sinceStart / initialDelayMs);
            long effectiveDelay = (long) (initialDelayMs * (1.0 - 0.5 * delayProgress));

            // Estimate how many repeats have occurred since delay ended
            long repeats = (sinceStart - initialDelayMs) / Math.max(intervalMs, 1);
            if (repeats < 0) repeats = 0;

            // Exponentially reduce interval based on repeat count
            double speedupFactor = Math.pow(1.0 - (speedupPercent / 100.0), repeats);
            long dynamicInterval = (long) (intervalMs * speedupFactor);

            // Trigger if delay has elapsed or easing allows early repeat
            if ((sinceStart >= effectiveDelay && sinceLast >= dynamicInterval)
                    || (sinceStart >= initialDelayMs && sinceLast >= dynamicInterval)) {
                lastTriggerTimes.put(button, now);
                return true;
            }

        } else if (wasPressed) {
            pressStartTimes.remove(button);
            lastTriggerTimes.remove(button);
        }

        return false;
    }

    private boolean getButtonState(Button button) {
        switch (button) {
            case A: return gamepad1.a || gamepad2.a;
            case B: return gamepad1.b || gamepad2.b;
            case X: return gamepad1.x || gamepad2.x;
            case Y: return gamepad1.y || gamepad2.y;
            case DPAD_UP: return gamepad1.dpad_up || gamepad2.dpad_up;
            case DPAD_DOWN: return gamepad1.dpad_down || gamepad2.dpad_down;
            case DPAD_LEFT: return gamepad1.dpad_left || gamepad2.dpad_left;
            case DPAD_RIGHT: return gamepad1.dpad_right || gamepad2.dpad_right;
            case GUIDE: return gamepad1.guide || gamepad2.guide;
            case START: return gamepad1.start || gamepad2.start;
            case BACK: return gamepad1.back || gamepad2.back;
            case LEFT_BUMPER: return gamepad1.left_bumper || gamepad2.left_bumper;
            case RIGHT_BUMPER: return gamepad1.right_bumper || gamepad2.right_bumper;
            case LEFT_STICK_BUTTON: return gamepad1.left_stick_button || gamepad2.left_stick_button;
            case RIGHT_STICK_BUTTON: return gamepad1.right_stick_button || gamepad2.right_stick_button;
            case CIRCLE: return gamepad1.circle || gamepad2.circle;
            case CROSS: return gamepad1.cross || gamepad2.cross;
            case TRIANGLE: return gamepad1.triangle || gamepad2.triangle;
            case SQUARE: return gamepad1.square || gamepad2.square;
            case SHARE: return gamepad1.share || gamepad2.share;
            case OPTIONS: return gamepad1.options || gamepad2.options;
            case PS: return gamepad1.ps || gamepad2.ps;
            default: return false;
        }
    }
}
