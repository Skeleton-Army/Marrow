package com.skeletonarmy.marrow.telemetry;

import androidx.annotation.NonNull;

public abstract class TelemetryModifier {
    /**
     * Formats a string based on the modifier and it's value.
     *
     * @param s string to be formatted
     * @return formatted string in a single line
     */
    @NonNull
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

    /**
     * Determines if the modifier is a block element, which needs to be applied last for HTML correctness.
     *
     * @return whether the modifier is a block element
     */
    public boolean isBlockElement() {
        return false;
    }
}
