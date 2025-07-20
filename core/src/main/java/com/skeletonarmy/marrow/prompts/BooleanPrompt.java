package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class BooleanPrompt extends Prompt<Boolean> {
    private boolean selectedValue;

    public BooleanPrompt(String header, boolean defaultValue) {
        super(header);
        this.selectedValue = defaultValue;
    }

    @Override
    public Boolean process(Gamepad gamepad1, Gamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();
        telemetry.addLine("Current Value: " + (selectedValue ? "Yes" : "No"));

        if (gamepad1.dpadRightWasPressed() || gamepad2.dpadRightWasPressed() || gamepad1.dpadUpWasPressed() || gamepad2.dpadUpWasPressed() || gamepad1.dpadLeftWasPressed() || gamepad2.dpadLeftWasPressed() || gamepad1.dpadDownWasPressed() || gamepad2.dpadDownWasPressed()) {
            selectedValue = !selectedValue;
        }

        if (gamepad1.aWasPressed() || gamepad2.aWasPressed()) {
            return selectedValue;
        }

        return null;
    }
}
