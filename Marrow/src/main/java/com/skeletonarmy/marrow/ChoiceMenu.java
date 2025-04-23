package com.skeletonarmy.marrow;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import com.skeletonarmy.marrow.gamepads.MarrowGamepad;
import com.skeletonarmy.marrow.prompts.Prompt;
import com.skeletonarmy.marrow.gamepads.Button;

import java.util.ArrayList;
import java.util.List;

public class ChoiceMenu {
    private final Telemetry telemetry;
    private final MarrowGamepad gamepad1;
    private final MarrowGamepad gamepad2;

    private final List<PromptResult<?>> promptResults = new ArrayList<>();
    private int currentIndex = 0;

    public ChoiceMenu(Telemetry telemetry, Gamepad gamepad1, Gamepad gamepad2) {
        this.telemetry = telemetry;
        this.gamepad1 = new MarrowGamepad(gamepad1);
        this.gamepad2 = new MarrowGamepad(gamepad2);
    }

    public ChoiceMenu(Telemetry telemetry, MarrowGamepad gamepad1, MarrowGamepad gamepad2) {
        this.telemetry = telemetry;
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;
    }

    /**
     * Add a prompt to the queue.
     */
    private <T> void enqueuePrompt(Prompt<T> prompt) {
        promptResults.add(new PromptResult<>(prompt));
    }

    /**
     * Handles the prompts and inputs. Should be called in a loop.
     * @return True if there are no more prompts to process
     */
    private boolean processPrompts() {
        // Handle back navigation
        if ((gamepad1.justPressed(Button.B) || gamepad2.justPressed(Button.B)) && currentIndex > 0) {
            currentIndex--;
        }

        // No prompts left
        if (currentIndex >= promptResults.size()) {
            return true;
        }

        PromptResult current = promptResults.get(currentIndex);
        Object result = current.prompt.process(gamepad1, gamepad2, telemetry);

        if (result != null) {
            current.result = result;
            currentIndex++;
        }

        return false;
    }

    /**
     * Prompts the user and waits for a result. This will block until the result is chosen.
     */
    @SuppressWarnings("unchecked")
    public <T> T prompt(Prompt<T> prompt) {
        enqueuePrompt(prompt);

        // Process the prompts until a result is selected for the current prompt
        while (!processPrompts()) {
            // This will keep processing until the current prompt has been answered
            // Optionally, you can add a timeout condition here if you need
        }

        // Return the result of the prompt
        return (T) promptResults.get(promptResults.size() - 1).result;
    }

    /**
     * A helper class to keep prompt and its result together.
     */
    private static class PromptResult<T> {
        public final Prompt<T> prompt;
        public T result;

        public PromptResult(Prompt<T> prompt) {
            this.prompt = prompt;
        }
    }
}
