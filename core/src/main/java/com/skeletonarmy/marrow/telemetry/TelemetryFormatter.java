package com.skeletonarmy.marrow.telemetry;


import com.skeletonarmy.marrow.telemetry.modifiers.BoldModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ColorModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ConditionalModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ItalicModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.MultiConditionalModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class TelemetryFormatter {
    private final String base;
    private final List<TelemetryModifier<?>> modifiers;

    public TelemetryFormatter(String base, List<TelemetryModifier<?>> modifiers) {
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
