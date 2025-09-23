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
                addLine((i + 1) + ") " + options[i] + " <");
            } else {
                addLine((i + 1) + ") " + options[i]);
            }
        }

        if (justPressed(Button.DPAD_UP)) {
            selectedOptionIndex = (selectedOptionIndex - 1 + options.length) % options.length;
        } else if (justPressed(Button.DPAD_DOWN)) {
            selectedOptionIndex = (selectedOptionIndex + 1) % options.length;
        }

        if (justPressed(Button.A)) {
            return options[selectedOptionIndex];
        }

        return null;
    }
}
