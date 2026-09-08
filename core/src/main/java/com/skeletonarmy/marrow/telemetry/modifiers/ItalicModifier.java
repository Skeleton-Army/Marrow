package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

public class ItalicModifier extends TelemetryModifier<Object> {

    @Override
    public Object getValue() {
        return null;
    }

    @NonNull
    @Override
    public String format(String s) {
        return "<i>\n" +
                '\t' + s + '\n' +
                "</i>";
    }
}
