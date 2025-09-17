package com.skeletonarmy.marrow.prompts;

public class BooleanPrompt extends Prompt<Boolean> {
    private final String header;
    private boolean selectedValue;

    public BooleanPrompt(String header, boolean defaultValue) {
        this.header = header;
        this.selectedValue = defaultValue;
    }

    @Override
    public Boolean process() {
        addLine(header);
        addLine("");
        addLine("Current Value: " + (selectedValue ? "Yes" : "No"));

        if (anyJustPressed("dpad_up", "dpad_down", "dpad_left", "dpad_right")) {
            selectedValue = !selectedValue;
        }

        if (justPressed("a")) {
            return selectedValue;
        }

        return null;
    }
}
