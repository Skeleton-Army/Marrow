package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.gamepads.MarrowGamepad;
import com.skeletonarmy.marrow.gamepads.Button;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class OptionPrompt<T> extends Prompt<T> {
    private final T[] options;
    private int selectedOptionIndex = 0;

    @SafeVarargs
    public OptionPrompt(String header, T... options) {
        super(header);
        this.options = options;
    }

    @Override
    public T process(MarrowGamepad gamepad1, MarrowGamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();

        for (int i = 0; i < options.length; i++) {
            if (i == selectedOptionIndex) {
                telemetry.addLine((i + 1) + ") " + options[i].toString() + " <");
            } else {
                telemetry.addLine((i + 1) + ") " + options[i].toString());
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
