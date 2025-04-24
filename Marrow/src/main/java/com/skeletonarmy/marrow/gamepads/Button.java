package com.skeletonarmy.marrow.gamepads;

import com.qualcomm.robotcore.hardware.Gamepad;
import java.util.function.Predicate;

/**
 * Enum representing various gamepad buttons, each mapped to a predicate for easy state checking.
 */
public enum Button {
    A(g -> g.a),
    B(g -> g.b),
    X(g -> g.x),
    Y(g -> g.y),
    DPAD_UP(g -> g.dpad_up),
    DPAD_DOWN(g -> g.dpad_down),
    DPAD_LEFT(g -> g.dpad_left),
    DPAD_RIGHT(g -> g.dpad_right),
    LEFT_BUMPER(g -> g.left_bumper),
    RIGHT_BUMPER(g -> g.right_bumper),
    START(g -> g.start),
    BACK(g -> g.back),
    GUIDE(g -> g.guide),
    TOUCHPAD(g -> g.touchpad),
    TOUCHPAD_FINGER_1(g -> g.touchpad_finger_1),
    TOUCHPAD_FINGER_2(g -> g.touchpad_finger_2),
    RIGHT_STICK_BUTTON(g -> g.right_stick_button),
    LEFT_STICK_BUTTON(g -> g.left_stick_button),
    RIGHT_TRIGGER(g -> g.right_trigger > 0.1),
    LEFT_TRIGGER(g -> g.left_trigger > 0.1);

    private final Predicate<Gamepad> condition;

    Button(Predicate<Gamepad> condition) {
        this.condition = condition;
    }

    public boolean get(Gamepad gamepad) {
        return condition.test(gamepad);
    }
}
