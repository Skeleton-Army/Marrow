package com.skeletonarmy.marrow.phases;

import androidx.annotation.NonNull;

/*
 * Callback for phase transitions.
 *
 * Example: {@code addPhaseListener(phase -> { if ("Endgame".equals(phase.getName())) {...} })}
 */

@FunctionalInterface
public interface PhaseListener {

    /**
     * Called when entering a new phase.
     */
    void onPhaseEntered(@NonNull Phase newPhase);
}
