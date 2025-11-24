package com.skeletonarmy.marrow.cooldown.enums;

import com.skeletonarmy.marrow.internal.LabeledEnum;

public enum SaveFormat implements LabeledEnum {
    CSV("csv"),
    TXT("txt");
    //ADVANTAGE_SCOPE("Advantage Scope"); coming soon (hopefully)
    private final String label;
    SaveFormat(String label) {
        this.label = label;
    }
    @Override
    public String getLabel() {
        return this.label;
    }
}
