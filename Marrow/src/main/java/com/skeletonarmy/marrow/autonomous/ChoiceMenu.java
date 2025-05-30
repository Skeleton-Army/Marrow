package com.skeletonarmy.marrow.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import com.skeletonarmy.marrow.MarrowGamepad;
import com.skeletonarmy.marrow.prompts.Prompt;

import java.util.ArrayList;
import java.util.List;

public class ChoiceMenu {
    private final Telemetry telemetry;
    private final MarrowGamepad gamepad1;
    private final MarrowGamepad gamepad2;

    private final List<PromptResult<?>> promptResults = new ArrayList<>();
    private int currentIndex = 0;

    public ChoiceMenu(OpMode opMode, Gamepad gamepad1, Gamepad gamepad2) {
        this.telemetry = opMode.telemetry;
        this.gamepad1 = new MarrowGamepad(opMode, gamepad1);
        this.gamepad2 = new MarrowGamepad(opMode, gamepad2);
    }

    public ChoiceMenu(OpMode opMode, MarrowGamepad gamepad1, MarrowGamepad gamepad2) {
        this.telemetry = opMode.telemetry;
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
        if ((gamepad1.b.isJustPressed() || gamepad2.b.isJustPressed()) && currentIndex > 0) {
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
            gamepad1.update();
            gamepad2.update();
            telemetry.update();
            telemetry.clearAll();
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
