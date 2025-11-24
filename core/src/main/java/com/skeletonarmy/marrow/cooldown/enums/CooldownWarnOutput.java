package com.skeletonarmy.marrow.cooldown.enums;

import com.skeletonarmy.marrow.internal.LabeledEnum;

public enum CooldownWarnOutput implements LabeledEnum {
    STDOUT("stdout"),
    STDERR("stderr"),
    FILE("File"),
    ROBOT_GLOBAL_WARNING("Robot Global Warning");
    //ADVANTAGE_SCOPE("Advantage Scope"); coming soon (hopefully)
    private final String label;
    CooldownWarnOutput(String label) {
        this.label = label;
    }
    @Override
    public String getLabel() {
        return this.label;
    }
}
