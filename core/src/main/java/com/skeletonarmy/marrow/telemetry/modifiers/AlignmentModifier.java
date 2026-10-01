package com.skeletonarmy.marrow.telemetry.modifiers;

import android.text.Layout;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

public class AlignmentModifier extends TelemetryModifier {
    private final Alignment alignment;

    public AlignmentModifier(Alignment alignment) {
        this.alignment = alignment;
    }

    @NonNull
    @Override
    public String format(String s) {
        return "<div style=\"text-align:" + alignment.toString().toLowerCase() + ";\">"
                + s
                + "</div>";
    }

    public enum Alignment {
        NORMAL,
        CENTER,
        END;
    }

}
