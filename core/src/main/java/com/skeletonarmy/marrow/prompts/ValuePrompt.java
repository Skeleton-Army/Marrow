package com.skeletonarmy.marrow.prompts;

public class ValuePrompt extends Prompt<Double> {
    private final String header;
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private double selectedValue;

    public ValuePrompt(String header) {
        this.header = header;
        this.minValue = Double.MIN_VALUE;
        this.maxValue = Double.MAX_VALUE;
        this.increment = 1;
        this.selectedValue = 0;
    }

    public ValuePrompt(String header, double defaultValue) {
        this.header = header;
        this.minValue = Double.MIN_VALUE;
        this.maxValue = Double.MAX_VALUE;
        this.increment = 1;
        this.selectedValue = defaultValue;
    }

    public ValuePrompt(String header, double defaultValue, double increment) {
        this.header = header;
        this.minValue = Double.MIN_VALUE;
        this.maxValue = Double.MAX_VALUE;
        this.increment = increment;
        this.selectedValue = defaultValue;
    }

    public ValuePrompt(String header, double minValue, double maxValue, double defaultValue, double increment) {
        this.header = header;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.increment = increment;
        this.selectedValue = defaultValue;
    }

    @Override
    public Double process() {
        addLine(header);
        addLine("");

        addLine("< " + selectedValue + " >");

        if (anyJustPressed("dpad_up", "dpad_right")) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (anyJustPressed("dpad_down", "dpad_left")) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (justPressed("a")) {
            return selectedValue;
        }

        return null;
    }
}
