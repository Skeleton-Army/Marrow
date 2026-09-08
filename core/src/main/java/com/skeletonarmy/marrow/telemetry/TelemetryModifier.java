package com.skeletonarmy.marrow.telemetry;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TelemetryModifier<T> {
    /**
     * Returns the value of the modifier, if modifier has a value.
     *
     * @return the value of the modifier, null if not applicable (e.g. bold modifier)
     */
    @Nullable
    public abstract T getValue();

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