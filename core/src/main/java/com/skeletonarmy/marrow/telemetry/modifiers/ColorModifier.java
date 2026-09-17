package com.skeletonarmy.marrow.telemetry.modifiers;


import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.HtmlColor;
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
    public ColorModifier(HtmlColor color) {
        this(color.getHexCode());
    }

    @NonNull
    @Override
    public String format(String s) {
        return String.format("<font color='%s'>%s</font>", color, s);
    }
}
