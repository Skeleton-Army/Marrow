package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.gamepads.MarrowGamepad;
import com.skeletonarmy.marrow.gamepads.Button;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class ValuePrompt extends Prompt {
    private final double minValue;
    private final double maxValue;
    private final double increment;
    private double selectedValue;

    public ValuePrompt(String key, String header, double minValue, double maxValue, double defaultValue, double increment) {
        super(key, header);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.increment = increment;
        this.selectedValue = defaultValue;
    }

    @Override
    public Object process(MarrowGamepad gamepad1, MarrowGamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();

        telemetry.addData("Increment", increment);
        telemetry.addLine("[" + minValue + "] " + selectedValue + " [" + maxValue + "]");

        if (gamepad1.justPressed(Button.DPAD_RIGHT) || gamepad2.justPressed(Button.DPAD_RIGHT) || gamepad1.justPressed(Button.DPAD_UP) || gamepad2.justPressed(Button.DPAD_UP)) {
            selectedValue = Math.min(maxValue, selectedValue + increment);
        } else if (gamepad1.justPressed(Button.DPAD_LEFT) || gamepad2.justPressed(Button.DPAD_LEFT) || gamepad1.justPressed(Button.DPAD_DOWN) || gamepad2.justPressed(Button.DPAD_DOWN)) {
            selectedValue = Math.max(minValue, selectedValue - increment);
        }

        if (gamepad1.justPressed(Button.A) || gamepad2.justPressed(Button.A)) {
            return selectedValue;
        }

        return null;
    }
}
