package com.skeletonarmy.marrow.telemetry.modifiers;

import androidx.annotation.NonNull;

import com.skeletonarmy.marrow.telemetry.FormatBuilder;
import com.skeletonarmy.marrow.telemetry.TelemetryModifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wraps a series of independently-formatted {@link FormatBuilder} items in an
 * HTML unordered list ({@code <ul><li>...</li></ul>}).
 * <p>
 * Unlike the other modifiers, each item carries its own base string and its
 * own modifier chain (color, bold, alignment, etc.), so list items can be
 * styled individually rather than sharing one formatting chain.
 */
public class UnorderedListModifier extends TelemetryModifier {
    private final List<FormatBuilder> items;

    public UnorderedListModifier(FormatBuilder... items) {
        this.items = new ArrayList<>(Arrays.asList(items));
    }

    public UnorderedListModifier(List<FormatBuilder> items) {
        this.items = new ArrayList<>(items);
    }

    /**
     * Wraps a series of independently-formatted {@link FormatBuilder} items in an unordered list
     *
     * @param s string to be formatted
     * @return {@code s} with HTML formatting applied
     */
    @NonNull
    @Override
    public String format(String s) {
        StringBuilder sb = new StringBuilder("<ul>");

        if (!s.isEmpty()) {
            sb.append("<li>").append(s).append("</li>");
        }

        for (FormatBuilder item : items) {
            sb.append("<li>").append(item.format()).append("</li>");
        }

        sb.append("</ul>");
        return sb.toString();
    }
}