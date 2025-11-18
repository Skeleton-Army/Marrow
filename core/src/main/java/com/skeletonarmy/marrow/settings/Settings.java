package com.skeletonarmy.marrow.settings;

import com.google.gson.Gson;
import com.skeletonarmy.marrow.internal.FileHandler;
import com.skeletonarmy.marrow.internal.FileHandler.Entry;

import java.util.HashMap;
import java.util.Map;

public final class Settings {
    private static final Gson GSON = new Gson();
    private static final Map<String, Entry> DATA = new HashMap<>();

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
     * @param value the value to store; may be {@code null}
     */
    public static void set(String key, Object value) {
        ensureLoaded();
        String normalized = key.toLowerCase();

        Entry e = new Entry();
        e.type = value != null ? value.getClass().getName() : Object.class.getName();
        e.value = serialize(value);

        DATA.put(normalized, e);
    }

    /**
     * Retrieves a typed setting.
     *
     * @param key          the case-insensitive key
     * @param type         expected type
     * @param defaultValue value to return if missing
     * @param <T>          generic type
     * @return the parsed and converted value, or {@code defaultValue}
     *
     * @throws IllegalArgumentException if the value exists but cannot be converted to the requested type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T get(String key, T defaultValue) {
        ensureLoaded();

        String normalized = key.toLowerCase();
        Entry entry = DATA.get(normalized);
        if (entry == null) return defaultValue;

        Object raw = entry.value;
        try {
            Class<?> storedType = Class.forName(entry.type);

            // Enum
            if (storedType.isEnum() && raw instanceof String) {
                return (T) Enum.valueOf((Class<Enum>) storedType, (String) raw);
            }

            // Number
            if (raw instanceof Number) {
                Number num = (Number) raw;
                if (storedType == Integer.class) return (T) Integer.valueOf(num.intValue());
                if (storedType == Long.class) return (T) Long.valueOf(num.longValue());
                if (storedType == Float.class) return (T) Float.valueOf(num.floatValue());
                if (storedType == Double.class) return (T) Double.valueOf(num.doubleValue());
            }

            // JSON deserialization for custom objects
            if (raw instanceof String && !storedType.equals(String.class)) {
                return (T) GSON.fromJson((String) raw, storedType);
            }

            // Direct cast
            if (storedType.isInstance(raw)) return (T) raw;

            // fallback: just return raw
            return (T) raw;

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Stored type not found: " + entry.type, e);
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            Map<String, Entry> fileData = new HashMap<>();
            FileHandler.loadFromFile(fileData, FILE_DIR, FILE_NAME);

            for (Map.Entry<String, Entry> e : fileData.entrySet()) {
                DATA.putIfAbsent(e.getKey(), e.getValue());
            }

            loaded = true;
        }
    }

    private static Object serialize(Object value) {
        if (value == null) return null;

        if (value.getClass().isEnum()) return value.toString();

        if (value instanceof Number || value instanceof Boolean || value instanceof String)
            return value;

        return GSON.toJson(value);
    }
}
