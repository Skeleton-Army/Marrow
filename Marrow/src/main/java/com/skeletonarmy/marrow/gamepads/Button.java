package com.skeletonarmy.marrow.gamepads;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Enum representing various gamepad buttons.
 * Each enum value corresponds to a specific button and provides a method to get
 * the state of that button on a given Gamepad instance.
 */
public enum Button {
    A {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.a;
        }
    },
    B {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.b;
        }
    },
    X {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.x;
        }
    },
    Y {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.y;
        }
    },
    DPAD_UP {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.dpad_up;
        }
    },
    DPAD_DOWN {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.dpad_down;
        }
    },
    DPAD_LEFT {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.dpad_left;
        }
    },
    DPAD_RIGHT {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.dpad_right;
        }
    },
    LEFT_BUMPER {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.left_bumper;
        }
    },
    RIGHT_BUMPER {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.right_bumper;
        }
    },
    START {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.start;
        }
    },
    BACK {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.back;
        }
    },
    GUIDE {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.guide;
        }
    },
    TOUCHPAD {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.touchpad;
        }
    },
    TOUCHPAD_FINGER_1 {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.touchpad_finger_1;
        }
    },
    TOUCHPAD_FINGER_2 {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.touchpad_finger_2;
        }
    },
    RIGHT_STICK_BUTTON {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.right_stick_button;
        }
    },
    LEFT_STICK_BUTTON {
        @Override
        public boolean get(Gamepad gamepad) {
            return gamepad.left_stick_button;
        }
    };

    /**
     * Abstract method for retrieving the state of a specific button on the given Gamepad.
     *
     * @param gamepad The Gamepad instance to check the button state on
     * @return The state of the button (true if pressed, false if not)
     */
    public abstract boolean get(Gamepad gamepad);
}
