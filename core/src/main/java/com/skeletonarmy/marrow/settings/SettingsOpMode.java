package com.skeletonarmy.marrow.settings;

import com.google.gson.Gson;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.skeletonarmy.marrow.internal.Button;
import com.skeletonarmy.marrow.internal.FileHandler;
import com.skeletonarmy.marrow.internal.GamepadInput;
import com.skeletonarmy.marrow.prompts.Prompt;
import com.skeletonarmy.marrow.prompts.Prompter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SettingsOpMode extends OpMode {
    private static final Gson GSON = new Gson();
    private static final Map<String, Object> RESULTS = new HashMap<>();

    private static String fileName;
    private static String fileDir;

    private static boolean loaded = false;

    private final List<Setting<?>> settingPrompts = new ArrayList<>();

    private State currentState = State.MENU;
    private Prompter prompter;
    private int cursorIndex = 0;

    public abstract void defineSettings();

    public String getFileName() {
        return getClass().getSimpleName() + ".json";
    }

    public String getFileDir() {
        return "FIRST/Settings";
    }

    @Override
    public void init() {
        fileName = getFileName();
        fileDir = getFileDir();

        if (!loaded) {
            FileHandler.loadFromFile(RESULTS, fileDir, fileName);
            loaded = true;
        }

        defineSettings();

        // Purge any orphaned keys loaded from the file
        Map<String, Object> cleanResults = new HashMap<>();

        for (Setting<?> setting : settingPrompts) {
            String key = setting.getKey();
            cleanResults.put(key, RESULTS.get(key));
        }

        RESULTS.clear();
        RESULTS.putAll(cleanResults);

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

        GamepadInput.update(gamepad1, gamepad2);
        telemetry.update();
    }

    @Override
    public void stop() {
        FileHandler.saveToFile(RESULTS, fileDir, fileName);
    }

    public <T> void add(String key, String name, Prompt<T> prompt) {
        String normalizedKey = key.toLowerCase();

        for (Setting<?> existingSetting : settingPrompts) {
            if (existingSetting.getKey().equals(normalizedKey)) {
                throw new IllegalArgumentException("Duplicate key: " + key);
            }
        }

        settingPrompts.add(new Setting<>(key, name, prompt));
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> type, T defaultValue) {
        if (!loaded) {
            FileHandler.loadFromFile(RESULTS, fileDir, fileName);
            loaded = true;
        }

        String normalizedKey = key.toLowerCase();
        Object value = RESULTS.get(normalizedKey);
        if (value == null) return defaultValue;

        // Enum conversion
        if (type.isEnum() && value instanceof String) {
            return (T) Enum.valueOf((Class<Enum>) type, (String) value);
        }

        // Primitive conversion (numbers)
        if (value instanceof Number) {
            Number number = (Number) value;
            if (type == Integer.class) return (T) Integer.valueOf(number.intValue());
            if (type == Long.class) return (T) Long.valueOf(number.longValue());
            if (type == Float.class) return (T) Float.valueOf(number.floatValue());
            if (type == Double.class) return (T) Double.valueOf(number.doubleValue());
        }

        // If value is JSON string and type is custom class
        if (!(type.isInstance(value)) && value instanceof String) {
            try {
                return GSON.fromJson((String) value, type);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse JSON for key '" + key + "'", e);
            }
        }

        // Normal cast (String, etc.)
        if (type.isInstance(value)) return (T) value;

        // TODO: Handle multiple settings opmodes having the same static results variable

        throw new IllegalArgumentException(
                "Cannot cast setting '" + key + "' of type " + value.getClass().getSimpleName() +
                        " to " + type.getSimpleName()
        );
    }

    private void handleMenu() {
        int i = 0;
        for (Setting<?> setting : settingPrompts) {
            String settingName = setting.getName();
            Object rawResult = get(setting.getKey(), Object.class, null);
            String result = (rawResult != null) ? rawResult.toString() : "N/A";
            String cursor = (i == cursorIndex) ? " <" : "";

            telemetry.addLine(settingName + ": " + result + cursor);
            i++;
        }

        int numberOfSettings = settingPrompts.size();

        if (GamepadInput.justPressed(Button.DPAD_UP)) {
            cursorIndex = (cursorIndex - 1 + numberOfSettings) % numberOfSettings;
        } else if (GamepadInput.justPressed(Button.DPAD_DOWN)) {
            cursorIndex = (cursorIndex + 1) % numberOfSettings;
        }

        if (GamepadInput.justPressed(Button.A)) {
            currentState = State.PROMPT;

            Setting<?> selected = settingPrompts.get(cursorIndex);

            prompter = new Prompter(this);
            prompter.prompt("_", selected.getPrompt())
                    .onComplete(() -> {
                        Object value = prompter.get("_");

                        // If enum, store as string
                        if (value != null && value.getClass().isEnum()) value = value.toString();

                        // If class, store as JSON string
                        else if (value != null && !(value instanceof Number) && !(value instanceof String)) {
                            value = GSON.toJson(value);
                        }

                        RESULTS.put(selected.getKey(), value);
                        currentState = State.MENU;
                    });
        }
    }

    private void handlePrompt() {
        prompter.run();

        if (GamepadInput.justPressed(Button.B)) {
            currentState = State.MENU;
            prompter = null;
        }
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
            this.key = key.toLowerCase();
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
