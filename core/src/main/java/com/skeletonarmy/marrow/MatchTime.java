package com.skeletonarmy.marrow;

import com.qualcomm.robotcore.util.ElapsedTime;

public class MatchTime {
    private final ElapsedTime timer = new ElapsedTime();
    private final double periodDuration;

    private boolean running = false;

    public MatchTime(double periodDuration) {
        this.periodDuration = periodDuration;
    }

    public void start() {
        if (running) return;
        timer.reset();
        running = true;
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

    public boolean isRunning() {
        return running;
    }
}
