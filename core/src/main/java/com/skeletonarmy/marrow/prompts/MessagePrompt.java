package com.skeletonarmy.marrow.prompts;

import com.skeletonarmy.marrow.internal.Button;
import com.skeletonarmy.marrow.telemetry.FormatBuilder;

public class MessagePrompt extends Prompt<Boolean> {
    private final String message;
    private final String ctlMsg;
    public MessagePrompt(String message) {
        this.message = message;
        ctlMsg = new FormatBuilder()
                .setPrefix("Press")
                .setBase("CROSS/A")
                .setSuffix("to continue")
                .bold()
                .italic()
                .format();
    }

    @Override
    public Boolean process() {
        addLine(message);
        addLine("");
        addLine(ctlMsg);

        return justPressed(Button.A) ? true : null;
    }
}
