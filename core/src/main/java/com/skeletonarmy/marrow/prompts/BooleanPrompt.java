package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.MarrowGamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class BooleanPrompt extends Prompt<Boolean> {
    private boolean selectedValue;

    public BooleanPrompt(String header, boolean defaultValue) {
        super(header);
        this.selectedValue = defaultValue;
    }

    @Override
    public Boolean process(MarrowGamepad gamepad1, MarrowGamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();
        telemetry.addLine("Current Value: " + (selectedValue ? "Yes" : "No"));

        if (gamepad1.dpad_right.isJustPressed() || gamepad2.dpad_right.isJustPressed() || gamepad1.dpad_up.isJustPressed() || gamepad2.dpad_up.isJustPressed() || gamepad1.dpad_left.isJustPressed() || gamepad2.dpad_left.isJustPressed() || gamepad1.dpad_down.isJustPressed() || gamepad2.dpad_down.isJustPressed()) {
            selectedValue = !selectedValue;
        }

        if (gamepad1.a.isJustPressed() || gamepad2.a.isJustPressed()) {
            return selectedValue;
        }

        return null;
    }
}
