package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.internal.Button;

import java.util.ArrayList;
import java.util.List;

public class MultiOptionPrompt<T> extends Prompt<List<T>> {
    private final String header;
    private final T[] options;
    private final boolean requireSelection;
    private final boolean ordered;
    private final int maxSelections;
    private final List<T> chosenOptions;

    private int cursorIndex = 0;
    private boolean showError = false;
    private String errorMessage = "";

    @SafeVarargs
    public MultiOptionPrompt(String header, boolean requireSelection, boolean ordered, int maxSelections, T... options) {
        if (header == null || header.isEmpty()) throw new IllegalArgumentException("Header cannot be empty.");
        if (options == null || options.length == 0) throw new IllegalArgumentException("Options cannot be null or empty.");
        if (maxSelections < 0) throw new IllegalArgumentException("Max selections cannot be negative.");

        this.header = header;
        this.requireSelection = requireSelection;
        this.ordered = ordered;
        this.options = options.clone();
        this.maxSelections = maxSelections;
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
            addLine(marker + " " + currentOption + cursor);
        }

        addLine("");
        addLine("-----------------");

        int doneIndex = options.length;
        String doneCursor = (cursorIndex == doneIndex) ? " <" : "";
        addLine("       DONE" + doneCursor);

        // Show error message, if any
        if (showError) {
            addLine("");
            addLine("! " + errorMessage + " !");
        }

        if (justPressed(Button.DPAD_UP)) {
            cursorIndex = (cursorIndex - 1 + (options.length + 1)) % (options.length + 1);
        } else if (justPressed(Button.DPAD_DOWN)) {
            cursorIndex = (cursorIndex + 1) % (options.length + 1);
        }

        if (justPressed(Button.A)) {
            if (cursorIndex < options.length) {
                T selectedOption = options[cursorIndex];

                if (chosenOptions.contains(selectedOption)) {
                    // Remove selection
                    chosenOptions.remove(selectedOption);
                    showError = false;
                } else {
                    // Enforce limit
                    if (maxSelections > 0 && chosenOptions.size() >= maxSelections) {
                        showError = true;
                        errorMessage = "You may only select up to " + maxSelections + " option(s)";
                    } else {
                        chosenOptions.add(selectedOption);
                        showError = false;
                    }
                }

            } else { // DONE
                if (requireSelection && chosenOptions.isEmpty()) {
                    showError = true;
                    errorMessage = "You must select at least one option";
                } else {
                    return chosenOptions;
                }
            }
        }

        return null;
    }
}
