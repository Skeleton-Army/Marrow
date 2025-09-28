package com.skeletonarmy.marrow.prompts;

public class OptionPrompt<T> extends Prompt<T> {
    private final String header;
    private final T[] options;
    private int cursorIndex = 0;

    @SafeVarargs
    public OptionPrompt(String header, T... options) {
        this.header = header;
        this.options = options;
    }

    @Override
    public T process() {
        addLine("=== " + header + " ===");
        addLine("");

        for (int i = 0; i < options.length; i++) {
            String cursor = (i == cursorIndex) ? " <" : "";
            addLine(" - " + options[i] + cursor);
        }

        if (justPressed(Button.DPAD_UP)) {
            cursorIndex = (cursorIndex - 1 + options.length) % options.length;
        } else if (justPressed(Button.DPAD_DOWN)) {
            cursorIndex = (cursorIndex + 1) % options.length;
        }

        if (justPressed(Button.A)) {
            return options[cursorIndex];
        }

        return null;
    }
}
