package com.skeletonarmy.marrow.internal;

import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.qualcomm.robotcore.util.RobotLog;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class FileHandler {
    private static final Gson GSON = new Gson();
    private static final Type RESULTS_MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    /**
     * Saves the provided map to a JSON file.
     * @param map The map containing key-value pairs.
     * @param directoryName The directory name (e.g., "FIRST").
     * @param fileName The file name (e.g., "marrow_settings.json").
     */
    public static void saveToFile(Map<String, Object> map, String directoryName, String fileName) {
        File directory = new File(Environment.getExternalStorageDirectory().getPath(), directoryName);
        File file = new File(directory, fileName);

        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                RobotLog.addGlobalWarningMessage("Error: Could not create directory: " + directory.getAbsolutePath());
                return;
            }
        }

        try (FileWriter fw = new FileWriter(file)) {
            String json = GSON.toJson(map);
            fw.write(json);
        } catch (IOException e) {
            RobotLog.addGlobalWarningMessage("Error saving file: " + file.getAbsolutePath() + "\n" + e.getMessage());
        }
    }

    /**
     * Loads a JSON file into the provided map.
     * @param map The map to load the data into (it will be cleared first).
     * @param directoryName The directory name (e.g., "FIRST").
     * @param fileName The file name (e.g., "marrow_settings.json").
     */
    public static void loadFromFile(Map<String, Object> map, String directoryName, String fileName) {
        File directory = new File(Environment.getExternalStorageDirectory().getPath(), directoryName);
        File file = new File(directory, fileName);

        if (!file.exists()) {
            return;
        }

        try (FileReader fr = new FileReader(file)) {
            Map<String, Object> loadedMap = GSON.fromJson(fr, RESULTS_MAP_TYPE);

            if (loadedMap != null) {
                map.putAll(loadedMap);
            }
        } catch (IOException e) {
            RobotLog.addGlobalWarningMessage("Error loading file: " + file.getAbsolutePath() + "\n" + e.getMessage());
        } catch (JsonSyntaxException e) {
            RobotLog.addGlobalWarningMessage("Error parsing file (corrupted JSON): " + file.getAbsolutePath() + "\n" + e.getMessage());
        }
    }
}