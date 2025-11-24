package com.skeletonarmy.marrow.cooldown.enums;

import com.skeletonarmy.marrow.internal.LabeledEnum;

public enum TimestampOutput implements LabeledEnum {
    STDOUT("stdout"),
    FILE("File");
    //ADVANTAGE_SCOPE("Advantage Scope"); coming soon (hopefully)
    private final String label;
    TimestampOutput(String label) {
        this.label = label;
    }
    @Override
    public String getLabel() {
        return this.label;
    }
}
