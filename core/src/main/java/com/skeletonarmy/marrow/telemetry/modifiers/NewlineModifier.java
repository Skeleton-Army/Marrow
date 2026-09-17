package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

public class NewlineModifier extends TelemetryModifier {
    @NonNull
    @Override
    public String format(String s) {
        return s + "<br>";
    }
}
