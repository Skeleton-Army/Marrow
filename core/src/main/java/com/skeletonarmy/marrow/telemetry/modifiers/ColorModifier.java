package com.skeletonarmy.marrow.telemetry.modifiers;


import android.graphics.Color;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.HtmlColors;
import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

public class ColorModifier extends TelemetryModifier {
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
    public ColorModifier(HtmlColors color) {
        this(color.toHexString());
    }

    @NonNull
    @Override
    public String format(String s) {
        return String.format("<font color='%s'> %s </font>", color, s);
    }
}
