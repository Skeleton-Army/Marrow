package com.skeletonarmy.marrow;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import com.skeletonarmy.marrow.gamepads.MarrowGamepad;
import com.skeletonarmy.marrow.prompts.Prompt;
import com.skeletonarmy.marrow.gamepads.Button;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChoiceMenu {
    private final Telemetry telemetry;
    private final MarrowGamepad gamepad1;
    private final MarrowGamepad gamepad2;

    private final List<Prompt> prompts = new ArrayList<>();
    private final Map<String, Object> results = new HashMap<>();

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
    private void enqueuePrompt(Prompt prompt) {
        prompts.add(prompt);
    }

    /**
     * Gets the chosen value of a prompt from its key. Call this in the start function.
     *
     * @param key The prompt's key
     * @param defaultValue A default value if the value is null
     * @return The Object value of the prompt result
     */
    private Object getValueOf(String key, Object defaultValue) {
        if (results.get(key) == null) return defaultValue;
        return results.get(key);
    }

    /**
     * Gets the chosen value of a prompt from its key. Call this in the start function.
     *
     * @param key The prompt's key
     * @return The Object value of the prompt result
     */
    private Object getValueOf(String key) {
        return results.get(key);
    }

    /**
     * Handles the prompts and inputs. Call this in the init_loop function.
     * @return True if there are no more prompts to process
     */
    boolean processPrompts() {
        // Handle back navigation
        if ((gamepad1.justPressed(Button.B) || gamepad2.justPressed(Button.B)) && currentIndex > 0) {
            currentIndex--;
        }

        // Display results if no prompts left
        if (currentIndex >= prompts.size()) {
            for (Map.Entry<String, Object> entry : results.entrySet()) {
                telemetry.addData(entry.getKey(), entry.getValue());
            }
            return true;
        }

        // Get the current prompt
        Prompt currentPrompt = prompts.get(currentIndex);

        // Process prompt
        Object result = currentPrompt.process(gamepad1, gamepad2, telemetry);

        if (result != null) {
            results.put(currentPrompt.getKey(), result);
            currentIndex++;
        }

        return false;
    }

    /**
     * Prompts the user and waits for a result. This will block until the result is chosen.
     */
    public Object prompt(Prompt prompt) {
        enqueuePrompt(prompt);

        // Process the prompts until a result is selected for the current prompt
        while (!processPrompts()) {
            // This will keep processing until the current prompt has been answered
            // Optionally, you can add a timeout condition here if you need
        }

        // Return the result of the prompt
        return results.get(prompt.getKey());
    }
}
