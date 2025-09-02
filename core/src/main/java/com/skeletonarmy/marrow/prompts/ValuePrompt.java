package com.skeletonarmy.marrow.prompts;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class ValuePrompt extends Prompt<Double> {
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private double selectedValue;

    public ValuePrompt(String header) {
        super(header);
        this.minValue = Double.MIN_VALUE;
        this.maxValue = Double.MAX_VALUE;
        this.increment = 1;
        this.selectedValue = 0;
    }

    public ValuePrompt(String header, double defaultValue) {
        super(header);
        this.minValue = Double.MIN_VALUE;
        this.maxValue = Double.MAX_VALUE;
        this.increment = 1;
        this.selectedValue = defaultValue;
    }

    public ValuePrompt(String header, double defaultValue, double increment) {
        super(header);
        this.minValue = Double.MIN_VALUE;
        this.maxValue = Double.MAX_VALUE;
        this.increment = increment;
        this.selectedValue = defaultValue;
    }

    public ValuePrompt(String header, double minValue, double maxValue, double defaultValue, double increment) {
        super(header);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.increment = increment;
        this.selectedValue = defaultValue;
    }

    @Override
    public Double process(GamepadInput input, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();

        telemetry.addLine("< " + selectedValue + " >");

        if (input.anyJustPressed("up", "right")) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (input.anyJustPressed("down", "left")) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (input.justPressed("a")) {
            return selectedValue;
        }

        return null;
    }
}
