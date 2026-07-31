package com.skeletonarmy.marrow.apriltags;

import android.annotation.SuppressLint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonApriltagFamily {
    private final File jsonFile;
    private final ObjectMapper objectMapper;

    @SuppressLint("SdCardPath")
    public JsonApriltagFamily(String fileName) {
        File temp = new File(fileName);

        if (temp.isAbsolute()) {
            jsonFile = temp;
        } else {
            jsonFile = new File("/sdcard/FIRST/Marrow/apriltags/" + fileName);
        }

        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static boolean isValidJson(String json) throws JsonProcessingException {
        try {
            new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(json);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private List<AprilTagMetadata> parseTags() {
        List<AprilTagMetadata> tags = new ArrayList<>();

        try {
            List<InternalApriltag> internalApriltags = objectMapper.readValue(jsonFile, new TypeReference<List<InternalApriltag>>() {});
            for (InternalApriltag tag : internalApriltags) {
                tags.add(tag.toAprilTagMetadata());
            }
        } catch (IOException e) {
            RobotLog.addGlobalWarningMessage("[MARROW]: Error loading file: " + jsonFile.getAbsolutePath() + "\n" + e.getMessage());
        }

        return tags;
    }
    public AprilTagLibrary getFamily() {
        AprilTagLibrary.Builder builder = new AprilTagLibrary.Builder();

        List<AprilTagMetadata> tagList = parseTags();

        for (AprilTagMetadata tag : tagList) {
            builder.addTag(tag);
        }

        return builder.build();
    }
}