package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.MarrowGamepad;

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

        if (gamepad1.dpad_up.isJustPressed() || gamepad2.dpad_up.isJustPressed()) {
            selectedOptionIndex = (selectedOptionIndex - 1 + options.length) % options.length;
        } else if (gamepad1.dpad_down.isJustPressed() || gamepad2.dpad_down.isJustPressed()) {
            selectedOptionIndex = (selectedOptionIndex + 1) % options.length;
        }

        if (gamepad1.a.isJustPressed() || gamepad2.a.isJustPressed()) {
            return options[selectedOptionIndex];
        }

        return null;
    }
}
