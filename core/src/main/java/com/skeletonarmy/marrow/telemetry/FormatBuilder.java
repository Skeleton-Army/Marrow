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

public class FormatBuilder {
    private String base = ""; //to remove potential null pointers
    private final List<TelemetryModifier<?>> modifiers = new ArrayList<>();

    public String getBase() {
        return base;
    }

    public List<TelemetryModifier<?>> getModifiers() {
        return modifiers;
    }

    public FormatBuilder setBase(String base) {
        this.base = base;
        return this;
    }

    public FormatBuilder setColor(String hex) {
        modifiers.add(new ColorModifier(hex));
        return this;
    }

    public FormatBuilder setColor(int r, int g, int b) {
        modifiers.add(new ColorModifier(r, g, b));
        return this;
    }

    public FormatBuilder bold() {
        modifiers.add(new BoldModifier());
        return this;
    }

    public FormatBuilder italic() {
        modifiers.add(new ItalicModifier());
        return this;
    }

    public FormatBuilder addConditional(BooleanSupplier condition, TelemetryModifier<?> onTrue, TelemetryModifier<?> onFalse) {
        modifiers.add(new ConditionalModifier(condition, onTrue, onFalse));
        return this;
    }

    public FormatBuilder addMultiConditional(Supplier<Integer> supplier, TelemetryModifier<?> defaultModifier, TelemetryModifier<?>... modifiers) {
        this.modifiers.add(new MultiConditionalModifier(supplier, defaultModifier, modifiers));
        return this;
    }

    public FormatBuilder addModifier(TelemetryModifier<?> modifier) {
        modifiers.add(modifier);
        return this;
    }

    public TelemetryFormatter build() {
        return new TelemetryFormatter(base, modifiers);
    }

    public String format() {
        return build().format();
    } 
}
