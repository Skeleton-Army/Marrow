package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.prompts.internal.GamepadInput;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class ValuePrompt extends Prompt<Double> {
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private double selectedValue;

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

        telemetry.addData("Increment", increment);
        telemetry.addLine("[" + minValue + "] " + selectedValue + " [" + maxValue + "]");

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
