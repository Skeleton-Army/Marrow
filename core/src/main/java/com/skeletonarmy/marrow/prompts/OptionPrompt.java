package com.skeletonarmy.marrow.prompts;

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
    public T process(GamepadInput input, Telemetry telemetry) {
        telemetry.addLine(header);
        telemetry.addLine();

        for (int i = 0; i < options.length; i++) {
            if (i == selectedOptionIndex) {
                telemetry.addLine((i + 1) + ") " + options[i].toString() + " <");
            } else {
                telemetry.addLine((i + 1) + ") " + options[i].toString());
            }
        }

        if (input.justPressed("up")) {
            selectedOptionIndex = (selectedOptionIndex - 1 + options.length) % options.length;
        } else if (input.justPressed("down")) {
            selectedOptionIndex = (selectedOptionIndex + 1) % options.length;
        }

        if (input.justPressed("a")) {
            return options[selectedOptionIndex];
        }

        return null;
    }
}
