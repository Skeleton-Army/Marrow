package com.skeletonarmy.marrow.prompts;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class BooleanPrompt extends Prompt<Boolean> {
    private boolean selectedValue;

    public BooleanPrompt(String header, boolean defaultValue) {
        super(header);
        this.selectedValue = defaultValue;
    }

    @Override
    public Boolean process(GamepadInput input, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();
        telemetry.addLine("Current Value: " + (selectedValue ? "Yes" : "No"));

        if (input.anyJustPressed("up", "down", "left", "right")) {
            selectedValue = !selectedValue;
        }

        if (input.justPressed("a")) {
            return selectedValue;
        }

        return null;
    }
}
