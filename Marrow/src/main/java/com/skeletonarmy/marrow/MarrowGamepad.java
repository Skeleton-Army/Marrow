package com.skeletonarmy.marrow;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * <b>MarrowGamepad</b> is an extension of the Gamepad class that provides additional functionality
 * for detecting button transitions, specifically when a button is just pressed or just released.
 */
public class MarrowGamepad extends Gamepad {
    // The previous state of the Gamepad, used to detect transitions
    private final Gamepad previous = new Gamepad();

    /**
     * Creates a new MarrowGamepad with the given Gamepad.
     *
     * @param gamepad The Gamepad instance to initialize this MarrowGamepad with.
     */
    public MarrowGamepad(Gamepad gamepad) {
        super();
        this.copy(gamepad);
    }

    /**
     * Copies the state of the specified Gamepad into the current Gamepad instance.
     * The current state is saved into the 'previous' Gamepad before copying the new state.
     *
     * @param other The Gamepad whose state should be copied into this Gamepad
     */
    @Override
    public void copy(Gamepad other) {
        // Save current state into previous before copying new one
        previous.copy(this);
        super.copy(other);
    }

    /**
     * Checks if a specific button was just pressed (transitioned from not pressed to pressed).
     *
     * @param button The button to check for the just-pressed transition
     * @return True if the button was just pressed, false otherwise
     */
    public boolean justPressed(Button button) {
        return !button.get(previous) && button.get(this);
    }

    /**
     * Checks if a specific button was just released (transitioned from pressed to not pressed).
     *
     * @param button The button to check for the just-released transition
     * @return True if the button was just released, false otherwise
     */
    public boolean justReleased(Button button) {
        return button.get(previous) && !button.get(this);
    }

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
}
