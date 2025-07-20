package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;

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
    public T process(Gamepad gamepad1, Gamepad gamepad2, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();

        for (int i = 0; i < options.length; i++) {
            if (i == selectedOptionIndex) {
                telemetry.addLine((i + 1) + ") " + options[i].toString() + " <");
            } else {
                telemetry.addLine((i + 1) + ") " + options[i].toString());
            }
        }

        if (gamepad1.dpadUpWasPressed() || gamepad2.dpadUpWasPressed()) {
            selectedOptionIndex = (selectedOptionIndex - 1 + options.length) % options.length;
        } else if (gamepad1.dpadDownWasPressed() || gamepad2.dpadDownWasPressed()) {
            selectedOptionIndex = (selectedOptionIndex + 1) % options.length;
        }

        if (gamepad1.aWasPressed() || gamepad2.aWasPressed()) {
            return options[selectedOptionIndex];
        }

        return null;
    }
}
