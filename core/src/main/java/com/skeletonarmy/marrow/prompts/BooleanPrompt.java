package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.internal.Button;
import com.skeletonarmy.marrow.telemetry.FormatBuilder;
import com.skeletonarmy.marrow.telemetry.modifiers.ConditionalColorModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ConditionalModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.HtmlTextSize;

public class BooleanPrompt extends Prompt<Boolean> {
    private final String header;
    private boolean selectedValue;

    public BooleanPrompt(String header, boolean defaultValue) {
        if (header == null || header.isEmpty()) throw new IllegalArgumentException("Header cannot be empty.");

        this.header = formatHeader(header);
        this.selectedValue = defaultValue;
    }

    @Override
    public Boolean process() {
        addLine(header);
        addLine("");
        addLine(displayValue(selectedValue));

        if (anyJustPressed(
                Button.DPAD_UP,
                Button.DPAD_DOWN,
                Button.DPAD_LEFT,
                Button.DPAD_RIGHT
        )) {
            selectedValue = !selectedValue;
        }

        if (justPressed(Button.A)) {
            return selectedValue;
        }

        return null;
    }

    private String booleanString(boolean value) {
        return value ? "YES" : "NO";
    }

    private String displayValue(boolean value) {
        return new FormatBuilder(booleanString(value))
                .addConditionalColor(() -> value)
                .setSize(HtmlTextSize.BIG)
                .setPrefix("--- ")
                .setSuffix(" ---")
                .format();
    }
}
