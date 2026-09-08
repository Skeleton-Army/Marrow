package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

/**
 * An empty modifier that does nothing, useful for avoiding null checks for many branches.
 */
public class EmptyModifier extends TelemetryModifier<Object> {
    @Override
    public Object getValue() {
        return null;
    }

    @NonNull
    @Override
    public String format(String s) {
        return s;
    }
}
