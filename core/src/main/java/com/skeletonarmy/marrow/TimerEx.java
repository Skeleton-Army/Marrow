package com.skeletonarmy.marrow;

import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.concurrent.TimeUnit;

public class TimerEx {
    private final TimeUnit unit;
    private final long duration;

    private ElapsedTime timer;
    private long pauseTime;
    private boolean isOn;
    private boolean started;

    public TimerEx() {
        this(0, TimeUnit.SECONDS);
    }

    public TimerEx(TimeUnit unit) {
        this(0, unit);
    }

    public TimerEx(long duration) {
        this(duration, TimeUnit.SECONDS);
    }

    public TimerEx(long duration, TimeUnit unit) {
        this.timer = new ElapsedTime();
        this.timer.reset();
        this.duration = duration;
        this.unit = unit;
    }

    public void start() {
        if (!started) {
            restart();
            started = true;
        }
    }

    public void pause() {
        if (isOn) {
            pauseTime = timer.nanoseconds();
            isOn = false;
        }
    }

    public void resume() {
        if (!isOn) {
            timer = new ElapsedTime(System.nanoTime() - pauseTime);
            isOn = true;
        }
    }

    public void restart() {
        timer.reset();
        pauseTime = 0;
        isOn = true;
    }

    public long getElapsed() {
        return isOn ? timer.time(unit) : unit.convert(pauseTime, TimeUnit.NANOSECONDS);
    }

    public long getRemaining() {
        return duration - getElapsed();
    }

    public boolean isLessThan(long timeLeft) {
        return getRemaining() < timeLeft;
    }

    public boolean isMoreThan(long timeLeft) {
        return getRemaining() > timeLeft;
    }

    public boolean isDone() {
        return getRemaining() <= 0;
    }

    public boolean isOn() {
        return isOn;
    }

    public long getDuration() {
        return duration;
    }

    public TimeUnit getUnit() {
        return unit;
    }
}
