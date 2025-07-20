package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class BooleanPrompt extends Prompt<Boolean> {
    private boolean selectedValue;

    private boolean prevButtonPressed;

    public BooleanPrompt(String header, boolean defaultValue) {
        super(header);
        this.selectedValue = defaultValue;
    }

    @Override
    public Boolean process(Gamepad gamepad1, Gamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();
        telemetry.addLine("Current Value: " + (selectedValue ? "Yes" : "No"));

        if (!prevButtonPressed && (gamepad1.dpad_right || gamepad2.dpad_right || gamepad1.dpad_up || gamepad2.dpad_up || gamepad1.dpad_left || gamepad2.dpad_left || gamepad1.dpad_down || gamepad2.dpad_down)) {
            selectedValue = !selectedValue;
        }

        if (!prevButtonPressed && (gamepad1.a || gamepad2.a)) {
            return selectedValue;
        }

        prevButtonPressed = gamepad1.a || gamepad2.a || gamepad1.dpad_right || gamepad2.dpad_right || gamepad1.dpad_up || gamepad2.dpad_up || gamepad1.dpad_left || gamepad2.dpad_left || gamepad1.dpad_down || gamepad2.dpad_down;

        return null;
    }
}
