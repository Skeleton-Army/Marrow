package com.skeletonarmy.marrow.telemetry;

public enum HtmlColors {
    BLACK(0x000000),
    SILVER(0xC0C0C0),
    GRAY(0x808080),
    WHITE(0xFFFFFF),
    MAROON(0x800000),
    RED(0xFF0000),
    PURPLE(0x800080),
    FUCHSIA(0xFF00FF),
    GREEN(0x008000),
    LIME(0x00FF00),
    OLIVE(0x808000),
    YELLOW(0xFFFF00),
    NAVY(0x000080),
    BLUE(0x0000FF),
    TEAL(0x008080),
    AQUA(0x00FFFF);

    private final int rgb;

    HtmlColors(int rgb) {
        this.rgb = rgb;
    }

    public int getRgb() {
        return rgb;
    }

    public int getRed() {
        return (rgb >> 16) & 0xFF;
    }

    public int getGreen() {
        return (rgb >> 8) & 0xFF;
    }

    public int getBlue() {
        return rgb & 0xFF;
    }

    public String toHexString() {
        return String.format("#%06X", rgb);
    }
}
