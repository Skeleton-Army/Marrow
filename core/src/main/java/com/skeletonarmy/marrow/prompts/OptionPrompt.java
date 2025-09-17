package com.skeletonarmy.marrow.prompts;

public class OptionPrompt<T> extends Prompt<T> {
    private final String header;
    private final T[] options;
    private int selectedOptionIndex = 0;

    @SafeVarargs
    public OptionPrompt(String header, T... options) {
        this.header = header;
        this.options = options;
    }

    @Override
    public T process() {
        addLine(header);
        addLine("");

        for (int i = 0; i < options.length; i++) {
            if (i == selectedOptionIndex) {
                addLine((i + 1) + ") " + options[i].toString() + " <");
            } else {
                addLine((i + 1) + ") " + options[i].toString());
            }
        }

        if (justPressed("dpad_up")) {
            selectedOptionIndex = (selectedOptionIndex - 1 + options.length) % options.length;
        } else if (justPressed("dpad_down")) {
            selectedOptionIndex = (selectedOptionIndex + 1) % options.length;
        }

        if (justPressed("a")) {
            return options[selectedOptionIndex];
        }

        return null;
    }
}
