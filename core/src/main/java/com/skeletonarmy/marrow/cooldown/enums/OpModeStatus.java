package com.skeletonarmy.marrow.cooldown.enums;

import com.skeletonarmy.marrow.internal.LabeledEnum;

public enum OpModeStatus implements LabeledEnum {
    START("Staring"),
    STOP("Stopping");
    private final String label;
    OpModeStatus(String label) {this.label = label;}
    @Override
    public String getLabel() {
        return this.label;
    }
}
