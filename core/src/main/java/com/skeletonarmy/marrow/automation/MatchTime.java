package com.skeletonarmy.marrow.automation;

import com.qualcomm.robotcore.util.ElapsedTime;

public class MatchTime {
    private final ElapsedTime timer = new ElapsedTime();
    private final double periodDuration;

    private boolean started = false;

    public MatchTime(double periodDuration) {
        this.periodDuration = periodDuration;
    }

    public void start() {
        if (started) return;
        timer.reset();
        started = true;
    }

    public void restart() {
        timer.reset();
    }

    public double getElapsed() {
        return timer.seconds();
    }

    public double getRemaining() {
        return periodDuration - timer.seconds();
    }

    public boolean isLessThan(double secondsLeft) {
        return getRemaining() < secondsLeft;
    }

    public boolean isMoreThan(double secondsLeft) {
        return getRemaining() > secondsLeft;
    }

    public boolean hasStarted() {
        return started;
    }
}
