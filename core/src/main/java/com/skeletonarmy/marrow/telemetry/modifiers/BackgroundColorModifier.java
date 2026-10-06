package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;
import com.skeletonarmy.marrow.telemetry.HtmlColor;
import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

public class BackgroundColorModifier extends TelemetryModifier {
    private final String color;

    public BackgroundColorModifier(String hex) {
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }

        color = hex.toUpperCase();
    }

    public BackgroundColorModifier(int r, int g, int b) {
        this(String.format("#%02x%02x%02x", r, g, b));
    }

    public BackgroundColorModifier(HtmlColor color) {
        this(color.getHexCode());
    }

    @NonNull
    @Override
    public String format(String s) {
        return "<span style=\"background:" + color + ";\">" + s + "<span>";
    }
}
