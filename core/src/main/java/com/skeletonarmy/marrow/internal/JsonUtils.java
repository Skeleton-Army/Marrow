package com.skeletonarmy.marrow.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.Util;

import org.apache.commons.io.FileUtils;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Handles JSON files I/O.
 *
 * <p><b>Internal API - Not Documented:</b> This class is public for
 * internal framework use. No formal documentation is provided
 * beyond these Javadoc comments. Contact the team in case you need support.
 *
 * <p><b>Warning:</b> Subject to change without notice.
 */

public class JsonUtils {
    private final ObjectMapper mapper;
    private final File file;
    public final File MARROW_FOLDER = new File(AppUtil.FIRST_FOLDER + "/marrow");
    private boolean fileError = false;

    public JsonUtils(String fileName) {
        this.mapper = new ObjectMapper();
        file = new File(MARROW_FOLDER, fileName);

        if (!Objects.requireNonNull(file.getParentFile()).exists() && !file.getParentFile().mkdirs()) {
            RobotLog.addGlobalWarningMessage("[Marrow] Error: Unable to create directory %s\n" +
                                             "[Marrow] Error: please try to create the directory manually", file.getParent()
            );

            fileError = true;
        }

        // Pretty print for human-readable JSON
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Ignore unknown fields to prevent crashes when loading objects with extra or read-only properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Allow Jackson to serialize the type into the JSON
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
    }

    public void save(Map<String, Object> data) {
        if (fileError) {
            return;
        }

        try {
            mapper.writeValue(file, data);
        } catch (Exception e) {
            RobotLog.addGlobalWarningMessage("[Marrow] Error: Unable to save file %s | Exception: %s",
                    file.getAbsolutePath(), e.getMessage());
        }
    }


    //WARNING: May return null
    public Map<String, Object> load() {
        if (fileError) {
            return null;
        }

        try {
            return mapper.readValue(file, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            RobotLog.addGlobalWarningMessage("[Marrow] Error: Unable to load file %s | Exception: %s",
                    file.getAbsolutePath(), e.getMessage());
        }

        return null;
    }

    public boolean deleteFile() {
        return FileUtils.deleteQuietly(file);
    }
}
