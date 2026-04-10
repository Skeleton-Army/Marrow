package com.skeletonarmy.marrow.phases;

import androidx.annotation.NonNull;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.util.RobotLog;
import com.skeletonarmy.marrow.OpModeManager;
import com.skeletonarmy.marrow.TimerEx;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages match phases and automatically transitions based on elapsed time.
 * <p>
 * Create an instance with phases, then call {@link #update()} each loop iteration. Use
 * {@link #getCurrentPhase()} for time-aware logic or {@link #addPhaseListener} for callbacks.
 * <p>
 * Typical usage:
 * <pre>
 *     PhaseManager phaseManager = new PhaseManager.Builder()
 *             .addPhase("Auto", 30)
 *             .addPhase("TeleOp", 2, TimeUnit.MINUTES)
 *             .build();
 *
 * while (opModeIsActive()) {
 *     manager.update();
 *     if (manager.isCurrentPhase("Park")) { ... }
 * }
 * </pre>
 *
 * @see Phase
 */
public class PhaseManager {
    private final TimerEx matchTimer;

    // Configured phases in order
    private final List<Phase> phases;

    // Current state
    private Phase currentPhase;
    private Phase previousPhase;

    // Listeners
    private final List<PhaseListener> phaseListeners;

    private final PrintStream printStream;

    /**
     * Creates a phase manager with phases in order.
     * <p>
     * Call once before {@code waitForStart()}.
     *
     * @param phases phases to transition through in order
     * @throws IllegalArgumentException if no phases are provided
     */
    private PhaseManager(PrintStream printStream, @NonNull List<Phase> phases) {
        if (phases.isEmpty()) {
            throw new IllegalArgumentException("At least one phase must be provided");
        }
        this.phases = new ArrayList<>();
        this.phases.addAll(phases);
        this.matchTimer = new TimerEx(getTotalDuration(phases.toArray(new Phase[0])), TimeUnit.SECONDS);
        this.currentPhase = this.phases.get(0);
        this.previousPhase = null;
        this.phaseListeners = new ArrayList<>();
        OpModeManager.getManager().registerListener(timerPhasesListener);

        if (printStream != null) {
            this.printStream = printStream;
        } else {
            this.printStream = System.out;
        }

    }

    /**
     * Sums phase durations in seconds.
     */
    private static double getTotalDuration(@NonNull Phase[] phases) {
        double total = 0;
        for (Phase phase : phases) {
            total += phase.getDurationSeconds();
        }
        return total;
    }

    /**
     * Updates the current phase based on elapsed time. Call once per loop.
     */
    public void update() {
        if (phases.isEmpty()) {
            return;
        }

        double elapsedSeconds = matchTimer.getElapsed();

        // Find which phase we're in based on elapsed time
        double timeAccumulated = 0;
        Phase newPhase = phases.get(0);
        for (Phase phase : phases) {
            double phaseEnd = timeAccumulated + phase.getDurationSeconds();

            if (elapsedSeconds < phaseEnd) {
                newPhase = phase;
                break;
            }

            timeAccumulated = phaseEnd;
            newPhase = phase; // Stay on last phase if exceeded
        }

        // Notify listeners of phase transitions
        if (!newPhase.equals(currentPhase) && previousPhase != null) {
            previousPhase = currentPhase;
            currentPhase = newPhase;
            notifyPhaseChange();
        } else if (previousPhase == null) {
            previousPhase = currentPhase;
            currentPhase = newPhase;
        } else {
            currentPhase = newPhase;
        }
    }

    /**
     * Gets the current phase.
     */
    public @NonNull Phase getCurrentPhase() {
        return currentPhase != null ? currentPhase : phases.get(0);
    }

    public boolean isCurrentPhase(String phaseName) {
        return getCurrentPhase().getName().equals(phaseName);
    }

    /**
     * Gets elapsed time in seconds since the match started (0 if not started).
     */
    public double getElapsedTime() {
        return matchTimer.getElapsed();
    }

    /**
     * Gets remaining time in the match (in seconds).
     */
    public double getTimeRemaining() {
        double remaining = matchTimer.getRemaining();
        return Math.max(0, remaining);
    }

    /**
     * Gets remaining time in the current phase (in seconds).
     */
    public double getPhaseTimeRemaining() {
        if (currentPhase == null || !matchTimer.isOn()) {
            return currentPhase != null ? currentPhase.getDurationSeconds() : 0;
        }

        double phaseStartTime = getPhaseStartTime();
        double elapsedInPhase = getElapsedTime() - phaseStartTime;
        return Math.max(0, currentPhase.getDurationSeconds() - elapsedInPhase);
    }

    /**
     * Calculates when the current phase started (elapsed seconds from match start).
     */
    private double getPhaseStartTime() {
        double start = 0;
        for (Phase phase : phases) {
            if (phase.equals(currentPhase)) {
                break;
            }
            start += phase.getDurationSeconds();
        }
        return start;
    }

    /**
     * Gets total match duration (sum of all phase durations, in seconds).
     */
    public double getTotalMatchDuration() {
        double total = 0;
        for (Phase phase : phases) {
            total += phase.getDurationSeconds();
        }
        return total;
    }

    public void addPhaseListener(@NonNull PhaseListener listener) {
        phaseListeners.add(listener);
    }

    /**
     * Unregisters a phase listener.
     */
    public void removePhaseListener(@NonNull PhaseListener listener) {
        phaseListeners.remove(listener);
    }

    /**
     * Clears all phase listeners.
     */
    public void clearPhaseListeners() {
        phaseListeners.clear();
    }

    private void notifyPhaseChange() {
        for (PhaseListener listener : phaseListeners) {
            try {
                listener.onPhaseEntered(currentPhase);
            } catch (Exception e) {
                RobotLog.addGlobalWarningMessage("[ERROR]: Failed to call listener: %s", e.getMessage());
                printStream.printf("[ERROR]: Failed to call listener: %s\n", e.getMessage());
            }
        }
    }

    private final OpModeManagerNotifier.Notifications timerPhasesListener = new OpModeManagerNotifier.Notifications() {
        @Override
        public void onOpModePreInit(OpMode opMode) {

        }

        @Override
        public void onOpModePreStart(OpMode opMode) {
            //While this listener isn't the most optimal, its the best one the SDK provides.
            matchTimer.start();
        }

        @Override
        public void onOpModePostStop(OpMode opMode) {
            OpModeManager.getManager().unregisterListener(timerPhasesListener);
        }
    };

    public static class Builder {
        private final List<Phase> phaseList = new ArrayList<>();
        private PrintStream printStream = null;

        public Builder addPhase(String name, double duration) {
            phaseList.add(new Phase(name, duration));
            return this;
        }

        public Builder addPhase(String name, double duration, TimeUnit timeUnit) {
            phaseList.add(new Phase(name, duration, timeUnit));
            return this;
        }

        public Builder setPrintStream(PrintStream printStream) {
            this.printStream = printStream;
            return this;
        }

        public PhaseManager build() {
            return new PhaseManager(printStream, phaseList);
        }
    }
}