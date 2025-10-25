package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.internal.Button;

public class BooleanPrompt extends Prompt<Boolean> {
    private final String header;
    private boolean selectedValue;

    public BooleanPrompt(String header, boolean defaultValue) {
        this.header = header;
        this.selectedValue = defaultValue;
    }

    @Override
    public Boolean process() {
        addLine("=== " + header + " ===");
        addLine("");
        addLine("--- " + (selectedValue ? "YES" : "NO") + " ---");

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
}
