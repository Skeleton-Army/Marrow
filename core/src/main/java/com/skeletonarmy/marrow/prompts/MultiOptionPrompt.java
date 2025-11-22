package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.internal.Button;

import java.util.ArrayList;
import java.util.List;

public class MultiOptionPrompt<T> extends Prompt<List<T>> {
    private final String header;
    private final T[] options;
    private final boolean requireSelection;
    private final boolean ordered;
    private final List<T> chosenOptions;

    private int cursorIndex = 0;
    private boolean showError = false;

    @SafeVarargs
    public MultiOptionPrompt(String header, boolean requireSelection, boolean ordered, T... options) {
        this.header = header;
        this.requireSelection = requireSelection;
        this.ordered = ordered;
        this.options = options;
        this.chosenOptions = new ArrayList<>();
    }

    @Override
    public List<T> process() {
        addLine("=== " + header + " ===");
        addLine("");

        for (int i = 0; i < options.length; i++) {
            T currentOption = options[i];

            String marker;
            if (chosenOptions.contains(currentOption)) {
                if (ordered) {
                    int index = chosenOptions.indexOf(currentOption) + 1;
                    marker = "[" + index + "]";
                } else {
                    marker = "[x]";
                }
            } else {
                marker = "[ ]";
            }

            String cursor = (i == cursorIndex) ? " <" : "";
            addLine(marker + " " + options[i] + cursor);
        }

        addLine("");
        addLine("-----------------");

        int doneIndex = options.length;
        String doneCursor = (cursorIndex == doneIndex) ? " <" : "";
        addLine("       DONE" + doneCursor);

        if (showError) {
            addLine("");
            addLine("! You must select at least one option !");
        }

        if (justPressed(Button.DPAD_UP)) {
            cursorIndex = (cursorIndex - 1 + (options.length + 1)) % (options.length + 1);
        } else if (justPressed(Button.DPAD_DOWN)) {
            cursorIndex = (cursorIndex + 1) % (options.length + 1);
        }

        if (justPressed(Button.A)) {
            if (cursorIndex < options.length) {
                T selectedOption = options[cursorIndex];

                // If it's in the list, remove it; otherwise, add it.
                if (chosenOptions.contains(selectedOption)) {
                    chosenOptions.remove(selectedOption);
                } else {
                    chosenOptions.add(selectedOption);
                }

                showError = false;
            } else { // DONE selected
                if (requireSelection && chosenOptions.isEmpty()) {
                    showError = true;
                } else {
                    return chosenOptions;
                }
            }
        }

        return null;
    }
}
