package com.skeletonarmy.marrow.gamepads;

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
}
