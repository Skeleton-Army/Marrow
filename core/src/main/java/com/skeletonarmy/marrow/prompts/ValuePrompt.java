package com.skeletonarmy.marrow.prompts;

public class ValuePrompt extends Prompt<Double> {
    private final String header;
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private double selectedValue;

    public ValuePrompt(String header) {
        this(header, Double.MIN_VALUE, Double.MAX_VALUE, 0, 1);
    }

    public ValuePrompt(String header, double defaultValue) {
        this(header, Double.MIN_VALUE, Double.MAX_VALUE, defaultValue, 1);
    }

    public ValuePrompt(String header, double defaultValue, double increment) {
        this(header, Double.MIN_VALUE, Double.MAX_VALUE, defaultValue, increment);
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

        if (pressAndHold(Button.DPAD_UP, 500, 100) || pressAndHold(Button.DPAD_RIGHT, 500, 100)) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (pressAndHold(Button.DPAD_DOWN, 500, 100) || pressAndHold(Button.DPAD_LEFT, 500, 100)) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (justPressed(Button.A)) {
            return selectedValue;
        }

        return null;
    }
}
