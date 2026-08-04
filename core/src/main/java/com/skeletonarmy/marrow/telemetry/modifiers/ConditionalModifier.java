package com.skeletonarmy.marrow.telemetry.modifiers;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

import java.util.function.BooleanSupplier;

public class ConditionalModifier implements TelemetryModifier<BooleanSupplier> {
    private final BooleanSupplier value;
    private final Pair <TelemetryModifier<?>, TelemetryModifier<?>> conditionalPair;

    public ConditionalModifier(BooleanSupplier value, TelemetryModifier<?> onTure, TelemetryModifier<?> onFalse) {
        this.value = value;
        conditionalPair = new Pair<>(onTure, onFalse);
    }

    @Override
    public BooleanSupplier getValue() {
        return value;
    }

    @NonNull
    @Override
    public String format(String s) {
        if (value.getAsBoolean()) {
            return conditionalPair.first.format(s);
        } else {
            return conditionalPair.second.format(s);
        }
    }
}
