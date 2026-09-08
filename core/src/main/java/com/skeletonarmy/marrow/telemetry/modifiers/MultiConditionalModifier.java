package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;


public class MultiConditionalModifier extends TelemetryModifier<Supplier<Integer>> {
    private final Supplier<Integer> supplier;
    private final TelemetryModifier<?> defaultModifier;
    private final List<TelemetryModifier<?>> modifiers;

    /**
     * Gets a {@link Supplier<Integer>} whose value is the index of the desired modifier.
     *
     * @param supplier the supplier
     * @param defaultModifier default modifier, can be {@code null} if nonexistent
     * @param modifiers the modifiers. if empty the default modifier will be used or returns a non-formated string if {@code defaultModifier} is {@code null}
     */
    public MultiConditionalModifier(Supplier<Integer> supplier, TelemetryModifier<?> defaultModifier, TelemetryModifier<?>... modifiers) {
        this.supplier = supplier;

        if (defaultModifier == null) {
            defaultModifier = new EmptyModifier();
        }
        this.defaultModifier = defaultModifier;
        this.modifiers = new ArrayList<>(Arrays.asList(modifiers));
    }

    @Override
    public Supplier<Integer> getValue() {
        return supplier;
    }

    @NonNull
    @Override
    public String format(String s) {
        if (modifiers.isEmpty()) {
            return defaultModifier.format(s);
        }

        int index = supplier.get();
        if (index < 0 || index >= modifiers.size()) {
            return defaultModifier.format(s);
        }

        return modifiers.get(index).format(s);
    }
}
