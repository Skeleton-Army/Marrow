package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.MarrowGamepad;

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
    public Double process(MarrowGamepad gamepad1, MarrowGamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();

        telemetry.addData("Increment", increment);
        telemetry.addLine("[" + minValue + "] " + selectedValue + " [" + maxValue + "]");

        if (gamepad1.dpad_right.isJustPressed() || gamepad2.dpad_right.isJustPressed() || gamepad1.dpad_up.isJustPressed() || gamepad2.dpad_up.isJustPressed()) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (gamepad1.dpad_left.isJustPressed() || gamepad2.dpad_left.isJustPressed() || gamepad1.dpad_down.isJustPressed() || gamepad2.dpad_down.isJustPressed()) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (gamepad1.a.isJustPressed() || gamepad2.a.isJustPressed()) {
            return selectedValue;
        }

        return (double) 0;
    }
}
