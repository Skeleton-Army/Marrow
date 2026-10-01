package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

public class UnderlineModifier extends TelemetryModifier {
    @NonNull
    @Override
    public String format(String s) {
       return "<u>" + s + "</u>";
    }
}
