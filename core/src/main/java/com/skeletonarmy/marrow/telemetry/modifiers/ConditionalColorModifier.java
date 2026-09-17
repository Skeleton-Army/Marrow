package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.HtmlColor;
import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

import java.util.function.BooleanSupplier;

public class ConditionalColorModifier extends TelemetryModifier {
    private final ConditionalModifier modifier;

    public ConditionalColorModifier(BooleanSupplier value) {
        modifier = new ConditionalModifier(value, new ColorModifier(HtmlColor.LIME_GREEN), new ColorModifier(HtmlColor.RED));
    }

    @NonNull
    @Override
    public String format(String s) {
        return modifier.format(s);
    }
}
