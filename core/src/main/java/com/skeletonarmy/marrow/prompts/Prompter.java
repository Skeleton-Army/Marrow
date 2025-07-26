package com.skeletonarmy.marrow.prompts;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.skeletonarmy.marrow.prompts.internal.GamepadInput;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Prompter {
    private final Telemetry telemetry;
    private final GamepadInput gamepadInput;

    private final List<KeyPromptPair<?>> prompts = new ArrayList<>();
    private final Map<String, Object> results = new HashMap<>();

    private int currentIndex = 0;

    public Prompter(OpMode opMode) {
        this.telemetry = opMode.telemetry;
        this.gamepadInput = new GamepadInput(opMode.gamepad1, opMode.gamepad2);
    }

    /**
     * Add a prompt to the queue.
     */
    public <T> Prompter prompt(String key, Prompt<T> prompt) {
        prompts.add(new KeyPromptPair<>(key, () -> prompt));
        return this; // For method chaining
    }

    /**
     * Add a prompt to the queue.
     */
    public <T> Prompter prompt(String key, Supplier<Prompt<T>> promptSupplier) {
        prompts.add(new KeyPromptPair<>(key, promptSupplier));
        return this; // For method chaining
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
        if (gamepadInput.justPressed("b") && currentIndex > 0) {
            do {
                prompts.get(currentIndex).reset(); // Reset prompt so it will get a fresh prompt every time
                currentIndex--;
            } while (prompts.get(currentIndex).getPrompt() == null && currentIndex > 0); // Skip all null prompts
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

        Object result = prompt.process(gamepadInput, telemetry);
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
            finished = processPrompts();
            telemetry.update();
        }
    }

    private static class KeyPromptPair<T> {
        private final String key;
        private final Supplier<Prompt<T>> promptSupplier;

        private Prompt<T> prompt;

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
            if (promptSupplier != null) {
                // Save the created prompt so it doesn't reset every loop
                prompt = promptSupplier.get();
                return prompt;
            }
            return null;
        }

        public void reset() {
            // Only clear if it's a supplier-based prompt
            if (promptSupplier != null) {
                prompt = null;
            }
        }
    }
}
