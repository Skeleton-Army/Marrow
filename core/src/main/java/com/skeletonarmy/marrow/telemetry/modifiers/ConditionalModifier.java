package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;
import com.skeletonarmy.marrow.telemetry.TelemetryModifier;
import java.util.function.BooleanSupplier;

public class ConditionalModifier extends TelemetryModifier {
    private final BooleanSupplier value;
    private final TelemetryModifier onTrue;
    private final TelemetryModifier onFalse;

    public ConditionalModifier(BooleanSupplier value, TelemetryModifier onTrue, TelemetryModifier onFalse) {
        this.value = value;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    @NonNull
    @Override
    public String format(String s) {
        if (value.getAsBoolean()) {
            return onTrue.format(s);
        } else {
            return onFalse.format(s);
        }
    }

    @Override
    public boolean isBlockElement() {
        if (value.getAsBoolean()) {
            return onTrue != null && onTrue.isBlockElement();
        } else {
            return onFalse != null && onFalse.isBlockElement();
        }
    }
}
