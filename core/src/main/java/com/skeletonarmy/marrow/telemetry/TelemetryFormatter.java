package com.skeletonarmy.marrow.telemetry;


import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TelemetryFormatter {
    private final String base;
    private final List<TelemetryModifier<?>> modifiers;

    public TelemetryFormatter(@NonNull String base, List<TelemetryModifier<?>> modifiers) {
        this.base = base;
        this.modifiers = new ArrayList<>(modifiers);
    }

    public TelemetryFormatter(String base) {
        this(base, new ArrayList<>());
    }

    public TelemetryFormatter(FormatBuilder builder) {
        this(builder.getBase(), builder.getModifiers());
    }

    public String getBase() {
        return base;
    }

    public List<TelemetryModifier<?>> getModifiers() {
        return modifiers;
    }

    public void addModifier(TelemetryModifier<?> modifier) {
        modifiers.add(modifier);
    }

    public String format() {
        String result = base;
        for (TelemetryModifier<?> modifier : modifiers) {
            result = modifier.format(result);
        }
        return result;
    }

}
