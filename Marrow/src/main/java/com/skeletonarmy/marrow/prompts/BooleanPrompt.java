package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.gamepads.MarrowGamepad;
import com.skeletonarmy.marrow.gamepads.Button;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class BooleanPrompt extends Prompt {
    private boolean selectedValue;

    public BooleanPrompt(String header, boolean defaultValue) {
        super(header);
        this.selectedValue = defaultValue;
    }

    @Override
    public Object process(MarrowGamepad gamepad1, MarrowGamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine("Current Value: " + (selectedValue ? "Yes" : "No"));

        if (gamepad1.justPressed(Button.DPAD_RIGHT) || gamepad2.justPressed(Button.DPAD_RIGHT) || gamepad1.justPressed(Button.DPAD_UP) || gamepad2.justPressed(Button.DPAD_UP) || gamepad1.justPressed(Button.DPAD_LEFT) || gamepad2.justPressed(Button.DPAD_LEFT) || gamepad1.justPressed(Button.DPAD_DOWN) || gamepad2.justPressed(Button.DPAD_DOWN)) {
            selectedValue = !selectedValue;
        }

        if (gamepad1.justPressed(Button.A) || gamepad2.justPressed(Button.A)) {
            return selectedValue;
        }

        return null;
    }
}
