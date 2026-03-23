package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.internal.Button;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ValuePrompt<T extends Number> extends Prompt<T> {
    private final String header;
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private double selectedValue;
    private final Class<T> type;

    public ValuePrompt(String header, Class<T> type) {
        this(header, type, 0, getMaxForType(type), 0, 1);
    }

    public ValuePrompt(String header, Class<T> type, T defaultValue) {
        this(header, type, 0, getMaxForType(type), defaultValue.doubleValue(), 1);
    }

    public ValuePrompt(String header, Class<T> type, T defaultValue, T increment) {
        this(header, type, 0, getMaxForType(type), defaultValue.doubleValue(), increment.doubleValue());
    }

    public ValuePrompt(String header, Class<T> type, T minValue, T maxValue, T defaultValue, T increment) {
        this(header, type, minValue.doubleValue(), maxValue.doubleValue(), defaultValue.doubleValue(), increment.doubleValue());
    }

    private ValuePrompt(String header, Class<T> type, double minValue, double maxValue, double defaultValue, double increment) {
        if (header == null || header.isEmpty()) throw new IllegalArgumentException("Header cannot be empty.");
        if (minValue >= maxValue) throw new IllegalArgumentException("Max value must be greater than min value.");
        if (defaultValue < minValue || defaultValue > maxValue) throw new IllegalArgumentException("Default value must be between min and max value.");
        if (increment <= 0) throw new IllegalArgumentException("Increment must be greater than zero.");

        this.header = header;
        this.type = type;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.increment = increment;
        this.selectedValue = defaultValue;
    }

    @Override
    public T process() {
        addLine("=== " + header + " ===");
        addLine("");

        if (isIntegerType()) {
            addLine("< " + (long) selectedValue + " >");
        } else {
            addLine("< " + round(selectedValue, 2) + " >");
        }

        // Increase speedup based on range size and precision:
        // larger maxValue or smaller increment = faster acceleration
        double speedupPercent = Math.max(2, (maxValue / increment) / 50.0);

        if (pressAndHold(Button.DPAD_UP, 500, 50, speedupPercent)
                || pressAndHold(Button.DPAD_RIGHT, 500, 50, speedupPercent)) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (pressAndHold(Button.DPAD_DOWN, 500, 50, speedupPercent)
                || pressAndHold(Button.DPAD_LEFT, 500, 50, speedupPercent)) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (justPressed(Button.A)) {
            return cast(selectedValue);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private T cast(double value) {
        if (type == Integer.class) return (T) Integer.valueOf((int) value);
        if (type == Long.class)    return (T) Long.valueOf((long) value);
        if (type == Double.class)  return (T) Double.valueOf(value);
        if (type == Float.class)   return (T) Float.valueOf((float) value);
        if (type == Short.class)   return (T) Short.valueOf((short) value);
        if (type == Byte.class)    return (T) Byte.valueOf((byte) value);
        throw new IllegalStateException("Unsupported number type: " + type.getSimpleName());
    }

    private boolean isIntegerType() {
        return type == Integer.class || type == Long.class || type == Short.class || type == Byte.class;
    }

    private static double getMaxForType(Class<?> type) {
        if (type == Integer.class) return Integer.MAX_VALUE;
        if (type == Long.class)    return Long.MAX_VALUE;
        if (type == Short.class)   return Short.MAX_VALUE;
        if (type == Byte.class)    return Byte.MAX_VALUE;
        if (type == Float.class)   return Float.MAX_VALUE;
        return Double.MAX_VALUE;
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
