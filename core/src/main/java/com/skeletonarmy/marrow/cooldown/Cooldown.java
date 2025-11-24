package com.skeletonarmy.marrow.cooldown;

import android.annotation.SuppressLint;
import android.os.Environment;

import com.qualcomm.robotcore.util.RobotLog;
import com.skeletonarmy.marrow.cooldown.enums.CooldownWarnOutput;
import com.skeletonarmy.marrow.cooldown.enums.OpModeStatus;
import com.skeletonarmy.marrow.cooldown.enums.SaveFormat;
import com.skeletonarmy.marrow.cooldown.enums.TimestampOutput;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

//Name Needs to changed before release
public class Cooldown {
    private SaveFormat saveFormat;
    private TimestampOutput timestampOutput;
    private CooldownWarnOutput cooldownWarnOutput;
    private File outputFile;
    String opModeName;

    // How much time after the last OpMode that wasn't ignored stopped for the robot to be considered cool.
    private double cooldownPeriod; // In Min


    // Not sure how to name this, if the last OpMode in less then this time, then it is ignored.
    private double ignoreOpModeTime; // In Sec


    // on OpMode stop also keep OpMode.getRuntime();

    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SS");

    public Cooldown(Builder builder) throws IOException {
        this(builder.saveFormat, builder.timestampOutput, builder.cooldownWarnOutput, builder.outputFile, builder.cooldownPeriod, builder.ignoreOpModeTime, builder.opModeName);
        
    }
    private Cooldown(SaveFormat saveFormat, TimestampOutput timestampOutput, CooldownWarnOutput cooldownWarnOutput,
                     File outputFile, double cooldownPeriod, double ignoreOpModeTime, String opModeName
    ) throws IOException {
        this.saveFormat = saveFormat;
        this.timestampOutput = timestampOutput;
        this.cooldownWarnOutput = cooldownWarnOutput;
        if (outputFile != null) {
            validateFile(outputFile);
        }
        this.outputFile = outputFile;
        this.cooldownPeriod = cooldownPeriod;
        this.ignoreOpModeTime = ignoreOpModeTime;
        this.opModeName = opModeName;

        if (this.saveFormat == SaveFormat.CSV) {
            FileWriter fileWriter = new FileWriter(this.outputFile);
            fileWriter.write("Time,OpMode,Status,Runtime\n");
            fileWriter.close();
        }
    }
    public Cooldown(File optionFile){
        // Should accept a TOML formated file, and get default options from it.
        // I will implement this contractor to reduce the mess in code when manually creating this object across multiple OpModes with non-default options.
    }
    private void validateFile(File file) throws IOException {
        if (!file.isFile()) {
            throw new IllegalArgumentException(String.format("Provided output file %s, isn't a file", file.getAbsolutePath() + File.pathSeparator + file.getName()));
        }
        if (!file.mkdirs()) {
            throw new RuntimeException("Could not create directory: " + file.getAbsolutePath());
        }
        if (!file.exists()) {
            if(!file.createNewFile()) {
                throw new RuntimeException("Could not create file: " + file.getAbsolutePath() + File.pathSeparator + file.getName());
            }
        }
        if (!file.canWrite() && !file.canRead()) {
            throw new RuntimeException("Cannot read/write to file: " + file.getAbsolutePath() + File.pathSeparator + file.getName());
        }
    }
    @SuppressLint("DefaultLocale")
    public void logOpMode(OpModeStatus status, Double opModeTime) throws IOException {
        switch (this.timestampOutput) {
            case FILE:
                logToFile(status, opModeTime);
                break;
            case STDOUT:
                logToStdout(status, opModeTime);
                break;
        }
    }
    private void logToFile(OpModeStatus status, Double opModeTime) throws IOException {
        FileWriter fileWriter = new FileWriter(this.outputFile, true);
        Calendar calendar = Calendar.getInstance();
        if (this.saveFormat == SaveFormat.TXT) {
            if(status == OpModeStatus.START) {
                fileWriter.write(String.format("%s | %s OpMode %s...\n",
                        timeFormat.format(calendar.getTime()),
                        status.getLabel(),
                        this.opModeName));
            } else if (status == OpModeStatus.STOP) {
                fileWriter.write(String.format("%s | %s OpMode %s... | OpMode %s took %f\n",
                        timeFormat.format(calendar.getTime()),
                        status.getLabel(),
                        this.opModeName,
                        this.opModeName,
                        opModeTime));
            }
        } else if (this.saveFormat == SaveFormat.CSV) {
            if (status == OpModeStatus.START) {
                fileWriter.write(String.format("%s,%s,%s,0.0",
                        timeFormat.format(calendar.getTime()), this.opModeName, status));
            } else if (status == OpModeStatus.STOP) {
                fileWriter.write(String.format("%s,%s,%s,%f",
                        timeFormat.format(calendar.getTime()), this.opModeName, status, opModeTime));
            }
        }
        fileWriter.close();
    }
    private void logToStdout(OpModeStatus status, Double opModeTime) {
        Calendar calendar = Calendar.getInstance();
        if (this.saveFormat == SaveFormat.TXT) {
            if (status == OpModeStatus.START) {
                System.out.printf("%s | %s OpMode %s...%n\n",
                        timeFormat.format(calendar.getTime()),
                        status.getLabel(),
                        this.opModeName);
            } else if (status == OpModeStatus.STOP) {
                System.out.printf("%s | %s OpMode %s... | OpMode %s took %f\n",
                        timeFormat.format(calendar.getTime()),
                        status.getLabel(),
                        this.opModeName,
                        this.opModeName,
                        opModeTime);
            }
        } else if (this.saveFormat == SaveFormat.CSV) {
            System.out.println("It's not recommended to use save CSV to stdout, it can cause a confusing output");
            if (status == OpModeStatus.START) {
                System.out.printf("%s,%s,%s,0.0\n",
                        timeFormat.format(calendar.getTime()), this.opModeName, status);
            } else if (status == OpModeStatus.STOP) {
                System.out.printf("%s,%s,%s,%f\n",
                        timeFormat.format(calendar.getTime()), this.opModeName, status, opModeTime);
            }
        }
    }
    
    public static void clearWarning() {
        RobotLog.clearGlobalWarningMsg();
    }
    @SuppressWarnings("unused")
    public static class Builder {
        private SaveFormat saveFormat;
        private TimestampOutput timestampOutput;
        private CooldownWarnOutput cooldownWarnOutput;
        private File outputFile = null;
        private double cooldownPeriod;
        private double ignoreOpModeTime;
        private String opModeName;

        public Builder saveFormat(SaveFormat saveFormat) {
            this.saveFormat = saveFormat;
            return this;
        }
        public Builder timestampOutput(TimestampOutput timestampOutput) {
            this.timestampOutput = timestampOutput;
            return this;
        }
        public Builder cooldownWarnOutput(CooldownWarnOutput cooldownWarnOutput) {
            this.cooldownWarnOutput = cooldownWarnOutput;
            return this;
        }
        public Builder outputFile(File outputFile) {
            this.outputFile = outputFile;
            return this;
        }
        public Builder outputFile(String path, String name) {
            this.outputFile = new File(path, name);
            return this;
        }
        public Builder cooldownPeriod(double cooldownPeriod) {
            this.cooldownPeriod = cooldownPeriod;
            return this;
        }
        public Builder ignoreOpModeTime(double ignoreOpModeTime) {
            this.ignoreOpModeTime = ignoreOpModeTime;
            return this;
        }
        public Builder opModeName(String opModeName) {
            this.opModeName = opModeName;
            return this;
        }
        public Cooldown build() throws IOException {
            return new Cooldown(this);
        }

        public Cooldown buildDefault() throws IOException {
            return new Cooldown(SaveFormat.TXT,TimestampOutput.FILE,CooldownWarnOutput.ROBOT_GLOBAL_WARNING,
                    new File(Environment.getExternalStorageDirectory() + "/Marrow/marrow_robot_cooldown.log"), 5,35, "Default OpMode");
        }
    }
}
