package com.skeletonarmy.marrow.settings;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.skeletonarmy.marrow.internal.Button;
import com.skeletonarmy.marrow.internal.GamepadInput;
import com.skeletonarmy.marrow.prompts.Prompt;
import com.skeletonarmy.marrow.prompts.Prompter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SettingsOpMode extends OpMode {
    private static final String FILE_DIR = "FIRST";
    private static final String FILE_NAME = "marrow_settings.json";

    private static final Map<String, Object> results = new HashMap<>();
    private final List<Setting<?>> settingPrompts = new ArrayList<>();

    private GamepadInput gamepadInput;
    private State currentState = State.MENU;
    private Prompter prompter;
    private int cursorIndex = 0;

    public abstract void defineSettings();

    @Override
    public void init() {
        gamepadInput = new GamepadInput(gamepad1, gamepad2);

        defineSettings();
        SettingsFileHandler.loadFromFile(results, FILE_DIR, FILE_NAME);

        telemetry.addLine("Press START to enter the menu.");
        telemetry.update();
    }

    @Override
    public void loop() {
        switch (currentState) {
            case MENU:
                handleMenu();
                break;
            case PROMPT:
                handlePrompt();
        }

        gamepadInput.update();
        telemetry.update();
    }

    @Override
    public void stop() {
        SettingsFileHandler.saveToFile(results, FILE_DIR, FILE_NAME);
    }

    public <T> void add(String key, String name, Prompt<T> prompt) {
        for (Setting<?> existingSetting : settingPrompts) {
            if (existingSetting.getKey().equals(key)) {
                throw new IllegalArgumentException("Duplicate key.");
            }
        }

        settingPrompts.add(new Setting<>(key, name, prompt));
        results.put(key, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        if (!results.containsKey(key)) {
            SettingsFileHandler.loadFromFile(results, FILE_DIR, FILE_NAME);
        }

        Object value = results.get(key);

        // TODO: Handle nulls and defaults
        // TODO: Handle multiple settings opmodes having the same static results variable

        return (T) value;
    }

    private void handleMenu() {
        int i = 0;
        for (Setting<?> setting : settingPrompts) {
            String settingName = setting.getName();
            Object rawResult = get(setting.getKey());
            String result = (rawResult != null) ? rawResult.toString() : "N/A";
            String cursor = (i == cursorIndex) ? " <" : "";

            telemetry.addLine(settingName + ": " + result + cursor);
            i++;
        }

        int numberOfSettings = settingPrompts.size();

        if (gamepadInput.justPressed(Button.DPAD_UP)) {
            cursorIndex = (cursorIndex - 1 + numberOfSettings) % numberOfSettings;
        } else if (gamepadInput.justPressed(Button.DPAD_DOWN)) {
            cursorIndex = (cursorIndex + 1) % numberOfSettings;
        }

        if (gamepadInput.justPressed(Button.A)) {
            currentState = State.PROMPT;

            Setting<?> selected = settingPrompts.get(cursorIndex);

            prompter = new Prompter(this);
            prompter.prompt("_", selected.getPrompt())
                    .onComplete(() -> {
                        results.put(selected.getKey(), prompter.get("_"));
                        currentState = State.MENU;
                    });
        }
    }

    private void handlePrompt() {
        prompter.run();
    }

    private enum State {
        MENU,
        PROMPT
    }

    private static class Setting<T> {
        private final String key;
        private final String name;
        private final Prompt<T> prompt;

        public Setting(String key, String name, Prompt<T> prompt) {
            this.key = key;
            this.name = name;
            this.prompt = prompt;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public Prompt<T> getPrompt() {
            return prompt;
        }
    }
}
