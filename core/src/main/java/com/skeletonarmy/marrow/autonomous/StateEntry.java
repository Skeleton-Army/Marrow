package com.skeletonarmy.marrow.autonomous;

public class StateEntry {
    public Runnable runnable;
    public double requiredTime;
    public String timeoutState;
    public double forceExitTime;

    public StateEntry(Runnable runnable, double requiredTime, String timeoutState, double forceExitTime) {
        this.runnable = runnable;
        this.requiredTime = requiredTime;
        this.timeoutState = timeoutState;
        this.forceExitTime = forceExitTime;
    }
}
