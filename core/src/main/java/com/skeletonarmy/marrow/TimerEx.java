package com.skeletonarmy.marrow;

import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.concurrent.TimeUnit;

public class TimerEx {
    private final TimeUnit unit;
    private final double duration;

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

    public TimerEx(double duration) {
        this(duration, TimeUnit.SECONDS);
    }

    public TimerEx(double duration, TimeUnit unit) {
        this.timer = new ElapsedTime();
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

    public double getElapsed() {
        return isOn ? nanoToUnit(timer.nanoseconds(), unit) : nanoToUnit(pauseTime, unit);
    }

    public double getRemaining() {
        return duration - getElapsed();
    }

    public boolean isLessThan(double timeLeft) {
        return getRemaining() < timeLeft;
    }

    public boolean isMoreThan(double timeLeft) {
        return getRemaining() > timeLeft;
    }

    public boolean isDone() {
        return getRemaining() <= 0;
    }

    public boolean isOn() {
        return isOn;
    }

    public double getDuration() {
        return duration;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    private double nanoToUnit(long nanos, TimeUnit unit) {
        // How many nanoseconds are in one of the requested units
        double nanosPerUnit = TimeUnit.NANOSECONDS.convert(1, unit);
        return nanos / nanosPerUnit;
    }
}
