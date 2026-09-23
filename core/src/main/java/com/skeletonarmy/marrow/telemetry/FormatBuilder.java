package com.skeletonarmy.marrow.telemetry;

import com.skeletonarmy.marrow.telemetry.modifiers.BoldModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ColorModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ConditionalColorModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ConditionalModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.HtmlTextSize;
import com.skeletonarmy.marrow.telemetry.modifiers.ItalicModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.MonospaceModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.MultiConditionalModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.NewlineModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.SizeModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.StrikethroughModifier;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class FormatBuilder {
    private String base;
    private String prefix = "";
    private String suffix = "";

    private final List<TelemetryModifier> modifiers = new ArrayList<>();

    public String getBase() {
        return base;
    }

    public List<TelemetryModifier> getModifiers() {
        return modifiers;
    }

    public FormatBuilder() {
        this.base = "";
    }

    public FormatBuilder(Object base) {
        this.base = String.valueOf(base);
    }

    public FormatBuilder setBase(Object base) {
        this.base = String.valueOf(base);
        return this;
    }
    public FormatBuilder setColor(String hex) {
        modifiers.add(new ColorModifier(hex));
        return this;
    }

    public FormatBuilder setColor(int r, int g, int b) {
        modifiers.add(new ColorModifier(r, g, b));
        return this;
    }

    public FormatBuilder setColor(HtmlColor color) {
        modifiers.add(new ColorModifier(color));
        return this;
    }

    public FormatBuilder bold() {
        modifiers.add(new BoldModifier());
        return this;
    }

    public FormatBuilder italic() {
        modifiers.add(new ItalicModifier());
        return this;
    }

    public FormatBuilder addConditional(BooleanSupplier condition, TelemetryModifier onTrue, TelemetryModifier onFalse) {
        modifiers.add(new ConditionalModifier(condition, onTrue, onFalse));
        return this;
    }

    public FormatBuilder addMultiConditional(Supplier<Integer> supplier, TelemetryModifier defaultModifier, TelemetryModifier... modifiers) {
        this.modifiers.add(new MultiConditionalModifier(supplier, defaultModifier, modifiers));
        return this;
    }

    public FormatBuilder monospace() {
        modifiers.add(new MonospaceModifier());
        return this;
    }

    // Kinda useless for a builder, but it's probably better it's here than not. it also helps shuts up Android Studio when commiting
    public FormatBuilder addNewline() {
        modifiers.add(new NewlineModifier());
        return this;
    }

    public FormatBuilder setSize(HtmlTextSize size) {
        modifiers.add(new SizeModifier(size));
        return this;
    }

    public FormatBuilder strikethrough() {
        modifiers.add(new StrikethroughModifier());
        return this;
    }

    public FormatBuilder addConditionalColor(BooleanSupplier condition) {
        modifiers.add(new ConditionalColorModifier(condition));
        return this;
    }


    public FormatBuilder addModifier(TelemetryModifier modifier) {
        modifiers.add(modifier);
        return this;
    }

    public FormatBuilder addModifiers(TelemetryModifier... modifiers) {
        this.modifiers.addAll(Arrays.asList(modifiers));
        return this;
    }

    /**
     * Sets prefix to the final formatted string, not base string.
     *
     * @param prefix the prefix
     * @return this
     */
    public FormatBuilder setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Sets suffix to the final formatted string, not base string.
     *
     * @param suffix the suffix
     * @return this
     */
    public FormatBuilder setSuffix(String suffix) {
        this.suffix = suffix;
        return this;
    }

    private TelemetryFormatter build() {
        return new TelemetryFormatter(this);
    }

    public String format() {
        return prefix + build().format() + suffix;
    }

    public Telemetry.Item printData(Telemetry telemetry, String caption) {
        return telemetry.addData(caption, format());
    }

    public Telemetry.Line printLine(Telemetry telemetry) {
        return telemetry.addLine(format());
    }
}
