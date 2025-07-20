package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class ValuePrompt extends Prompt<Double> {
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private double selectedValue;

    private boolean prevButtonPressed;

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

        if (!prevButtonPressed && (gamepad1.dpad_right || gamepad2.dpad_right || gamepad1.dpad_up || gamepad2.dpad_up)) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (!prevButtonPressed && (gamepad1.dpad_left || gamepad2.dpad_left || gamepad1.dpad_down|| gamepad2.dpad_down)) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (!prevButtonPressed && (gamepad1.a || gamepad2.a)) {
            return selectedValue;
        }

        prevButtonPressed = gamepad1.a || gamepad2.a || gamepad1.dpad_right || gamepad2.dpad_right || gamepad1.dpad_up || gamepad2.dpad_up || gamepad1.dpad_left || gamepad2.dpad_left || gamepad1.dpad_down || gamepad2.dpad_down;

        return null;
    }
}
