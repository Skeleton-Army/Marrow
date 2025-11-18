package com.skeletonarmy.marrow.settings;

import com.google.gson.Gson;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.skeletonarmy.marrow.internal.Button;
import com.skeletonarmy.marrow.internal.GamepadInput;
import com.skeletonarmy.marrow.prompts.Prompt;
import com.skeletonarmy.marrow.prompts.Prompter;

import java.util.ArrayList;
import java.util.List;

public abstract class SettingsOpMode extends OpMode {
    private static final Gson GSON = new Gson();

    private final List<Setting<?>> options = new ArrayList<>();

    private enum State { MENU, PROMPT }
    private State state = State.MENU;
    private Prompter prompter;
    private int cursor = 0;

    public abstract void defineSettings();

    @Override
    public void init() {
        defineSettings();
        telemetry.addLine("Press START to enter the menu.");
        telemetry.update();
    }

    @Override
    public void loop() {
        switch (state) {
            case MENU:
                drawMenu();
                break;
            case PROMPT:
                runPrompt();
                break;
        }

        GamepadInput.update(gamepad1, gamepad2);
        telemetry.update();
    }

    @Override
    public void stop() {
        Settings.save();
    }

    protected <T> void add(String key, String name, Prompt<T> prompt) {
        String normalizedKey = key.toLowerCase();

        if (key.isEmpty()) throw new IllegalArgumentException("Key cannot be empty.");
        if (options.stream().anyMatch(p -> p.getKey().equals(normalizedKey))) throw new IllegalArgumentException("Duplicate key found: " + key);

        options.add(new Setting<>(key, name, prompt));
    }

    private void drawMenu() {
        for (int i = 0; i < options.size(); i++) {
            Setting<?> s = options.get(i);
            Object value = Settings.get(s.getKey(), null);
            telemetry.addLine(s.getName() + ": " + formatValue(value) + (i == cursor ? " <" : ""));
        }

        if (GamepadInput.justPressed(Button.DPAD_UP))
            cursor = (cursor - 1 + options.size()) % options.size();

        if (GamepadInput.justPressed(Button.DPAD_DOWN))
            cursor = (cursor + 1) % options.size();

        if (GamepadInput.justPressed(Button.A)) {
            Setting<?> s = options.get(cursor);
            prompter = new Prompter(this);
            state = State.PROMPT;

            prompter.prompt("_", s.getPrompt())
                    .onComplete(() -> {
                        Object v = prompter.get("_");
                        Settings.set(s.getKey(), v);
                        state = State.MENU;
                    });
        }
    }

    private void runPrompt() {
        prompter.run();

        if (GamepadInput.justPressed(Button.B)) {
            state = State.MENU;
            prompter = null;
        }
    }

    private String formatValue(Object value) {
        if (value == null) return "N/A";
        Class<?> clazz = value.getClass();

        if (clazz.isPrimitive() || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value.toString();
        }

        // JSON pretty-print
        try {
            return GSON.toJson(value);
        } catch (Exception e) {
            return value.toString();
        }
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
