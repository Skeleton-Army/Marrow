package com.skeletonarmy.marrow.prompts;

public class ValuePrompt extends Prompt<Double> {
    private final String header;
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private double selectedValue;

    public ValuePrompt(String header) {
        this(header, 0, Double.POSITIVE_INFINITY, 0, 1);
    }

    public ValuePrompt(String header, double defaultValue) {
        this(header, 0, Double.POSITIVE_INFINITY, defaultValue, 1);
    }

    public ValuePrompt(String header, double defaultValue, double increment) {
        this(header, 0, Double.POSITIVE_INFINITY, defaultValue, increment);
    }

    public ValuePrompt(String header, double minValue, double maxValue, double defaultValue, double increment) {
        if (minValue >= maxValue) throw new IllegalArgumentException("Max value must be greater than min value.");
        if (defaultValue < minValue || defaultValue > maxValue) throw new IllegalArgumentException("Default value must be between min and max value.");
        if (increment <= 0) throw new IllegalArgumentException("Increment must be greater than zero.");

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
