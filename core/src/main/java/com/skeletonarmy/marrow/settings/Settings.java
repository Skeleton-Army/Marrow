package com.skeletonarmy.marrow.settings;

import com.skeletonarmy.marrow.internal.FileHandler;

import java.util.HashMap;
import java.util.Map;

public final class Settings {
    private static final Map<String, Object> DATA = new HashMap<>(); // Saved to file
    private static final Map<String, Object> SESSION_DATA = new HashMap<>(); // Not saved

    private static final String FILE_DIR = "FIRST/marrow";
    private static final String FILE_NAME = "settings.json";

    private static boolean loaded = false;

    private Settings() {}

    /**
     * Wipes all saved settings from memory and deletes the content of the file.
     */
    public static void clear() {
        ensureLoaded();
        DATA.clear();
        SESSION_DATA.clear();
        save();
    }

    /**
     * Saves all currently loaded settings to file.
     */
    public static void save() {
        ensureLoaded();
        FileHandler.saveToFile(DATA, FILE_DIR, FILE_NAME);
    }

    /**
     * Stores a value under the specified key and immediately saves to file.
     *
     * @param key   the case-insensitive key
     * @param value the value to store
     */
    public static void set(String key, Object value) {
        set(key, value, true);
    }

    /**
     * Stores a value under the specified key.
     * Saves the settings to file if {@code save} is true.
     *
     * @param key   case-insensitive key
     * @param value value to store
     * @param save  whether to save to file immediately
     */
    public static void set(String key, Object value, boolean save) {
        ensureLoaded();
        String normalized = key.toLowerCase();
        Object valueToStore = (value instanceof Enum<?>) ? ((Enum<?>) value).name() : value;

        if (save) {
            DATA.put(normalized, valueToStore);
            save();
        } else {
            SESSION_DATA.put(normalized, valueToStore);
        }
    }

    /**
     * Retrieves a typed setting.
     *
     * @param key          the case-insensitive key
     * @param defaultValue value to return if missing or if casting fails
     * @param <T>          generic type
     * @return the value cast to T, or defaultValue
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T get(String key, T defaultValue) {
        ensureLoaded();

        String normalized = key.toLowerCase();

        Object raw = SESSION_DATA.containsKey(normalized) ? SESSION_DATA.get(normalized) : DATA.get(normalized);
        if (raw == null) return defaultValue;

        try {
            // If defaultValue is an enum, convert the stored string back to the enum
            if (defaultValue instanceof Enum<?> && raw instanceof String) {
                Class<? extends Enum> enumClass = defaultValue.getClass().asSubclass(Enum.class);
                return (T) Enum.valueOf(enumClass, (String) raw);
            }

            return (T) raw;
        } catch (ClassCastException | IllegalArgumentException e) {
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