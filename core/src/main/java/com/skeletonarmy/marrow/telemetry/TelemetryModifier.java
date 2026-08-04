package com.skeletonarmy.marrow.telemetry;

import org.jetbrains.annotations.NotNull;

public interface TelemetryModifier<T> {
    /**
     * Returns the value of the modifier, if modifier has a value.
     *
     * @return the value of the modifier, null if not applicable (e.g. bold modifier)
     */
    public T getValue();

    /**
     * Formats a string based on the modifier and it's value.
     *
     * @param s string to be formatted
     * @return formatted string
     */
    @NotNull
    public String format(String s);

}