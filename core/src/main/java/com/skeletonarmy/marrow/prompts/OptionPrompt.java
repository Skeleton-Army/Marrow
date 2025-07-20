package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class OptionPrompt<T> extends Prompt<T> {
    private final T[] options;
    private int selectedOptionIndex = 0;

    private boolean prevButtonPressed;

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

        if (!prevButtonPressed && (gamepad1.dpad_up || gamepad2.dpad_up)) {
            selectedOptionIndex = (selectedOptionIndex - 1 + options.length) % options.length;
        } else if (!prevButtonPressed && (gamepad1.dpad_down || gamepad2.dpad_down)) {
            selectedOptionIndex = (selectedOptionIndex + 1) % options.length;
        }

        if (!prevButtonPressed && (gamepad1.a || gamepad2.a)) {
            return options[selectedOptionIndex];
        }

        prevButtonPressed = gamepad1.a || gamepad2.a || gamepad1.dpad_up || gamepad2.dpad_up || gamepad1.dpad_down || gamepad2.dpad_down;

        return null;
    }
}
