package com.skeletonarmy.marrow.telemetry;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

public abstract class TelemetryModifier {
    /**
     * Formats a string based on the modifier and it's value.
     *
     * @param s string to be formatted
     * @return formatted string in a single line
     */
    @NotNull
    public abstract String format(String s);

    /**
     * a toString impl so standard addData won't print the memory addresses
     *
     * @return an empty string
     */
    @NonNull
    @Override
    public final String toString() {
        return "";
    }
}