package com.skeletonarmy.marrow.internal;

import android.os.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.qualcomm.robotcore.util.RobotLog;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles JSON files I/O.
 *
 * <p><b>Internal API - Not Documented:</b> This class is public for
 * internal framework use. No formal documentation is provided
 * beyond these Javadoc comments. Contact the team in case you need support.
 *
 * <p><b>Warning:</b> Subject to change without notice.
 */
public class FileHandler {
    private static final ObjectMapper MAPPER = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Pretty print for human-readable JSON
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Allow Jackson to serialize the type into the JSON.
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);

        return mapper;
    }

    /**
     * Saves a map of Objects to JSON.
     * Jackson will automatically include type info for complex objects.
     *
     * @param map The map containing key-object pairs.
     * @param directoryName The directory name (e.g., "FIRST").
     * @param fileName The file name (e.g., "settings.json").
     */
    public static void saveToFile(Map<String, Object> map, String directoryName, String fileName) {
        File directory = new File(Environment.getExternalStorageDirectory().getPath(), directoryName);
        File file = new File(directory, fileName);

        if (!directory.exists() && !directory.mkdirs()) {
            RobotLog.addGlobalWarningMessage("Error: Could not create directory: " + directory.getAbsolutePath());
            return;
        }

        try {
            // Write the map directly. Jackson handles the types.
            MAPPER.writeValue(file, map);
        } catch (IOException e) {
            RobotLog.addGlobalWarningMessage("Error saving file: " + file.getAbsolutePath() + "\n" + e.getMessage());
        }
    }

    /**
     * Loads a JSON file into the provided map.
     *
     * @param map The map to load data into.
     * @param directoryName The directory name (e.g., "FIRST").
     * @param fileName The file name (e.g., "settings.json").
     */
    public static void loadFromFile(Map<String, Object> map, String directoryName, String fileName) {
        File directory = new File(Environment.getExternalStorageDirectory().getPath(), directoryName);
        File file = new File(directory, fileName);

        if (!file.exists()) return;

        try {
            // We use TypeReference to tell Jackson we are expecting a Map with String keys and Object values
            Map<String, Object> loadedMap = MAPPER.readValue(file, new TypeReference<HashMap<String, Object>>() {});

            if (loadedMap != null) {
                map.putAll(loadedMap);
            }
        } catch (IOException e) {
            RobotLog.addGlobalWarningMessage("Error loading file: " + file.getAbsolutePath() + "\n" + e.getMessage());
        }
    }
}
