package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.internal.Button;

public class MessagePrompt extends Prompt<Boolean> {
    private final String message;

    public MessagePrompt(String message) {
        this.message = message;
    }

    @Override
    public Boolean process() {
        addLine(message);
        addLine("");
        addLine("Press CROSS/A to continue");

        return justPressed(Button.A) ? true : null;
    }
}
