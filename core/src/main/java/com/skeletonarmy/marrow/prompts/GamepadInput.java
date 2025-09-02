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

    public GamepadInput(Gamepad gamepad1, Gamepad gamepad2) {
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;
    }

    public boolean isPressed(String key) {
        switch (key) {
            case "a":
                return gamepad1.a || gamepad2.a;
            case "b":
                return gamepad1.b || gamepad2.b;
            case "up":
                return gamepad1.dpad_up || gamepad2.dpad_up;
            case "down":
                return gamepad1.dpad_down || gamepad2.dpad_down;
            case "left":
                return gamepad1.dpad_left || gamepad2.dpad_left;
            case "right":
                return gamepad1.dpad_right || gamepad2.dpad_right;
            default:
                return false;
        }
    }

    public boolean justPressed(String key) {
        boolean current = isPressed(key);
        boolean previous = Boolean.TRUE.equals(previousStates.getOrDefault(key, false));
        previousStates.put(key, current);
        return current && !previous;
    }

    public boolean anyJustPressed(String... keys) {
        for (String key : keys) {
            if (justPressed(key)) return true;
        }
        return false;
    }
}
