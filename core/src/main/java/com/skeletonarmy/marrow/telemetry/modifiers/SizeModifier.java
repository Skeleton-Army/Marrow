package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

public class SizeModifier extends TelemetryModifier {
    private final HtmlTextSize size;

    public SizeModifier(HtmlTextSize size) {
       this.size = size;
    }

    @NonNull
    @Override
    public String format(String s) {
        if (size == HtmlTextSize.BIG) {
            return "<big>" + s + "</big>";
        } else {
            return "<small>" + s + "</small>";
        }
    }
}
