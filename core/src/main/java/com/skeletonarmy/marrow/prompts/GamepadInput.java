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
     * Checks if the button has been held for more than the initial delay,
     * and continues to return true at a specified interval.
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
