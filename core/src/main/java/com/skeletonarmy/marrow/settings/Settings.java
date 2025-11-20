package com.skeletonarmy.marrow.settings;

import com.skeletonarmy.marrow.internal.FileHandler;

import java.util.HashMap;
import java.util.Map;

public final class Settings {
    private static final Map<String, Object> DATA = new HashMap<>();

    private static final String FILE_DIR = "FIRST/Settings";
    private static final String FILE_NAME = "settings.json";

    private static boolean loaded = false;

    private Settings() {}

    /**
     * Saves all currently loaded settings to file.
     */
    public static void save() {
        ensureLoaded();
        FileHandler.saveToFile(DATA, FILE_DIR, FILE_NAME);
    }

    /**
     * Stores a value under the specified key.
     *
     * @param key   the case-insensitive key
     * @param value the value to store
     */
    public static void set(String key, Object value) {
        ensureLoaded();
        String normalized = key.toLowerCase();
        DATA.put(normalized, value);
    }

    /**
     * Retrieves a typed setting.
     *
     * @param key          the case-insensitive key
     * @param defaultValue value to return if missing or if casting fails
     * @param <T>          generic type
     * @return the value cast to T, or defaultValue
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, T defaultValue) {
        ensureLoaded();

        String normalized = key.toLowerCase();
        Object raw = DATA.get(normalized);
        if (raw == null) return defaultValue;

        try {
            // Because Jackson already deserialized the object into its specific class
            // (e.g. Integer, Double, CustomObject), we only need a direct cast.
            return (T) raw;
        } catch (ClassCastException e) {
            // Fallback if the type in the file doesn't match T
            return defaultValue;
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            FileHandler.loadFromFile(DATA, FILE_DIR, FILE_NAME);
            loaded = true;
        }
    }
}