package com.skeletonarmy.marrow.telemetry.modifiers;


import android.graphics.Color;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

public class ColorModifier extends TelemetryModifier<String> {
    private final String color;

    public ColorModifier(String hex) {
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }

        color = hex.toUpperCase();
    }

    public ColorModifier(int r, int g, int b) {
        this(String.format("#%02x%02x%02x", r, g, b));
    }

    @Override
    public String getValue() {
       return color;
    }

    @NonNull
    @Override
    public String format(String s) {
        return String.format("<font color='%s'> %s </font>", color, s);
    }
}
