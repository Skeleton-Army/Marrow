package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.gamepads.MarrowGamepad;
import com.skeletonarmy.marrow.gamepads.Button;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class OptionPrompt extends Prompt {
    private final String[] options;
    private int selectedOptionIndex = 0;

    public OptionPrompt(String header, String... options) {
        super(header);
        this.options = options;
    }

    @Override
    public Object process(MarrowGamepad gamepad1, MarrowGamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();

        for (int i = 0; i < options.length; i++) {
            if (i == selectedOptionIndex) {
                telemetry.addLine((i + 1) + ") " + options[i] + " <");
            } else {
                telemetry.addLine((i + 1) + ") " + options[i]);
            }
        }

        if (gamepad1.justPressed(Button.DPAD_UP) || gamepad2.justPressed(Button.DPAD_UP)) {
            selectedOptionIndex = (selectedOptionIndex - 1 + options.length) % options.length;
        } else if (gamepad1.justPressed(Button.DPAD_DOWN) || gamepad2.justPressed(Button.DPAD_DOWN)) {
            selectedOptionIndex = (selectedOptionIndex + 1) % options.length;
        }

        if (gamepad1.justPressed(Button.A) || gamepad2.justPressed(Button.A)) {
            return options[selectedOptionIndex];
        }

        return null;
    }
}
