package com.skeletonarmy.marrow.prompts;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ValuePrompt extends Prompt<Number> {
    private final String header;
    private final double minValue;
    private final double maxValue;
    private final double increment;

    // If bigIncrement isn't used it should be set to the same value as increment.
    private final double bigIncrement;
    private Double selectedValue;

    // Flag that tells us whether this ValuePrompt should be treated as an integer prompt.
    // If true → values are displayed/returned as integers (no decimals).
    // If false → values are treated as doubles (can have decimals).
    private final boolean isInteger;

    public ValuePrompt(String header) {
        this(header, 0, Double.POSITIVE_INFINITY, 0, 1, 1);
    }

    public ValuePrompt(String header, double defaultValue) {
        this(header, 0, Double.POSITIVE_INFINITY, defaultValue, 1, 1);
    }

    public ValuePrompt(String header, double defaultValue, double increment) {
        this(header, 0, Double.POSITIVE_INFINITY, defaultValue, increment, increment);
    }

    public ValuePrompt(String header, double minValue, double maxValue, double defaultValue, double increment) {
        this(header, minValue, maxValue, defaultValue, increment, increment);
    }
    public ValuePrompt(String header, double minValue, double maxValue, double defaultValue, double increment, double bigIncrement) {
        if (minValue >= maxValue) throw new IllegalArgumentException("Max value must be greater than min value.");
        if (defaultValue < minValue || defaultValue > maxValue) throw new IllegalArgumentException("Default value must be between min and max value.");
        if (increment <= 0) throw new IllegalArgumentException("Increment must be greater than zero.");
        if (bigIncrement <= 0) throw new IllegalArgumentException("Big Increment must be greater then zero.");

        this.header = header;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.increment = increment;
        this.bigIncrement = bigIncrement;
        this.selectedValue = defaultValue;

        this.isInteger = isIntegerLike(minValue) &&
                         isIntegerLike(maxValue) &&
                         isIntegerLike(defaultValue) &&
                         isIntegerLike(increment) &&
                         isIntegerLike(bigIncrement);
    }

    @Override
    public Number process() {
        addLine("=== " + header + " ===");
        addLine("");

        if (isInteger) {
            addLine("< " + selectedValue.intValue() + " >");
        } else {
            addLine("< " + round(selectedValue, 2) + " >");
        }

        if (pressAndHold(Button.DPAD_UP, 500, 100)) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (pressAndHold(Button.DPAD_RIGHT, 500, 100)) {
            selectedValue = Math.min(maxValue, selectedValue + bigIncrement);
        } else if (pressAndHold(Button.DPAD_DOWN, 500, 100)) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        } else if (pressAndHold(Button.DPAD_LEFT, 500, 100)) {
            selectedValue = Math.min(minValue, selectedValue - bigIncrement);
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
    private static boolean isIntegerLike(double value) {
        return Double.isInfinite(value) || value == (int)value;
    }

    /**
     * Rounds a double to the specified number of decimal {@code places}.
     */
    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
