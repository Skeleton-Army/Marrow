package com.skeletonarmy.marrow.prompts;

public class ValuePrompt extends Prompt<Number> {
    private final String header;
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private Double selectedValue;

    // Flag that tells us whether this ValuePrompt should be treated as an integer prompt.
    // If true → values are displayed/returned as integers (no decimals).
    // If false → values are treated as doubles (can have decimals).
    private final boolean isInteger;

    public ValuePrompt(String header) {
        this(header, 0, Double.POSITIVE_INFINITY, 0, 1);
    }

    public ValuePrompt(String header, double defaultValue) {
        this(header, 0, Double.POSITIVE_INFINITY, defaultValue, 1);
    }

    /*
    Commented out for test writing purposes. Will be reverted before merging.

    public ValuePrompt(String header, double defaultValue, double increment) {
        this(header, 0, Double.POSITIVE_INFINITY, defaultValue, increment);
    }
     */

    // The temporary constructor for testing. Will be removed before merging.
    public ValuePrompt(String header, double maxValue, double increment) {
        this(header, 0, maxValue, 0, increment);
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

        this.isInteger = isIntegerLike(minValue) &&
                isIntegerLike(maxValue) &&
                isIntegerLike(defaultValue) &&
                isIntegerLike(increment);
    }

    @Override
    public Number process() {

        // Temporary variables to speedup testing. Once final ones are decided should be inlined.
        int speedupDivisor = 13;
        int minIntervalMs = 4;
        double speedupPercent = maxValue/speedupDivisor;

        addLine("=== " + header + " ===");
        addLine("");

        if (isInteger) {
            addLine("< " + selectedValue.intValue() + " >");
        } else {
            addLine("< " + selectedValue + " >");
        }

        // IntervalMs is different in DPAD_UP and DPAD_RIGHT for purposes of testing another variable.
        if (pressAndHold(Button.DPAD_UP, 500, 50, speedupPercent, minIntervalMs)
                || pressAndHold(Button.DPAD_RIGHT, 500, 75, speedupPercent, minIntervalMs)) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (pressAndHold(Button.DPAD_DOWN, 500, 100, speedupPercent, minIntervalMs)
                || pressAndHold(Button.DPAD_LEFT, 500, 100, speedupPercent, minIntervalMs)) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (justPressed(Button.A)) {
            if (isInteger) {
                return selectedValue.intValue();
            } else {
                return selectedValue;
            }
        }

        return null;
    }

    /**
     * Checks whether a double value is a whole number (integer).
     */
    private boolean isIntegerLike(double value) {
        return Double.isInfinite(value) || value == (int)value;
    }
}
