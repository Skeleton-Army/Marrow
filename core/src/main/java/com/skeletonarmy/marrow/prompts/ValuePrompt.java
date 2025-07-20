package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;

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
    public Double process(Gamepad gamepad1, Gamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();

        telemetry.addData("Increment", increment);
        telemetry.addLine("[" + minValue + "] " + selectedValue + " [" + maxValue + "]");

        if (gamepad1.dpadRightWasPressed() || gamepad2.dpadRightWasPressed() || gamepad1.dpadUpWasPressed() || gamepad2.dpadUpWasPressed()) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (gamepad1.dpadLeftWasPressed() || gamepad2.dpadLeftWasPressed() || gamepad1.dpadDownWasPressed() || gamepad2.dpadDownWasPressed()) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (gamepad1.aWasPressed() || gamepad2.aWasPressed()) {
            return selectedValue;
        }

        return null;
    }
}
