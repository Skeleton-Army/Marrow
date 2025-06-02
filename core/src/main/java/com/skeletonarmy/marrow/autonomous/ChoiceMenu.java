package com.skeletonarmy.marrow.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import com.skeletonarmy.marrow.MarrowGamepad;
import com.skeletonarmy.marrow.prompts.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ChoiceMenu {
    private final OpMode opMode;
    private final Telemetry telemetry;
    private final MarrowGamepad gamepad1;
    private final MarrowGamepad gamepad2;

    private final List<KeyPromptPair<?>> prompts = new ArrayList<>();
    private final Map<String, Object> results = new HashMap<>();

    private int currentIndex = 0;
    private double lastOpModeTime = -1;

    public ChoiceMenu(OpMode opMode, Gamepad gamepad1, Gamepad gamepad2) {
        this.opMode = opMode;
        this.telemetry = opMode.telemetry;
        this.gamepad1 = new MarrowGamepad(opMode, gamepad1);
        this.gamepad2 = new MarrowGamepad(opMode, gamepad2);
    }

    public ChoiceMenu(OpMode opMode, MarrowGamepad gamepad1, MarrowGamepad gamepad2) {
        this.opMode = opMode;
        this.telemetry = opMode.telemetry;
        this.gamepad1 = gamepad1;
        this.gamepad2 = gamepad2;
    }

    /**
     * Add a prompt to the queue.
     */
    public <T> void enqueuePrompt(String key, Prompt<T> prompt) {
        prompts.add(new KeyPromptPair<>(key, prompt));
    }

    /**
     * Add a prompt to the queue.
     */
    public <T> void enqueuePrompt(String key, Supplier<Prompt<T>> promptSupplier) {
        prompts.add(new KeyPromptPair<>(key, promptSupplier));
    }


    /**
     * Gets the chosen value of a prompt from its key.
     *
     * @param key The prompt's key
     * @return The value of the prompt result
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) results.get(key);
    }

    /**
     * Gets the chosen value of a prompt from its key.
     *
     * @param key The prompt's key
     * @param defaultValue A default value if the value doesn't exist
     * @return The value of the prompt result
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        return (T) results.getOrDefault(key, defaultValue);
    }

    /**
     * Handles the prompts and inputs. Should be called in a loop.
     * @return True if there are no more prompts to process, false otherwise
     */
    private boolean processPrompts() {
        // Handle back navigation
        if ((gamepad1.b.isJustPressed() || gamepad2.b.isJustPressed()) && currentIndex > 0) {
            currentIndex--;
        }

        // No prompts left
        if (currentIndex >= prompts.size()) {
            return true;
        }

        KeyPromptPair<?> current = prompts.get(currentIndex);
        Prompt<?> prompt = current.getPrompt();

        // Skip if prompt is null
        if (prompt == null) {
            currentIndex++;
            return false;
        }

        Object result = prompt.process(gamepad1, gamepad2, telemetry);
        if (result != null) {
            results.put(current.getKey(), result);
            currentIndex++;
        }

        return false;
    }

    /**
     * Runs all queued prompts in a blocking loop until all prompts are complete.
     */
    public void run() {
        boolean finished = false;

        while (!finished) {
            if (opMode.time == lastOpModeTime) continue; // Skip if it was already called this frame. This is so it runs only once per loop.

            finished = processPrompts();
            lastOpModeTime = opMode.time;
        }
    }

    private static class KeyPromptPair<T> {
        private final String key;
        private final Prompt<T> prompt;
        private final Supplier<Prompt<T>> promptSupplier;

        public KeyPromptPair(String key, Prompt<T> prompt) {
            this.key = key;
            this.prompt = prompt;
            this.promptSupplier = null;
        }

        public KeyPromptPair(String key, Supplier<Prompt<T>> promptSupplier) {
            this.key = key;
            this.prompt = null;
            this.promptSupplier = promptSupplier;
        }

        public String getKey() {
            return key;
        }

        public Prompt<T> getPrompt() {
            if (prompt != null) return prompt;
            if (promptSupplier != null) return promptSupplier.get();
            return null;
        }
    }
}
