package com.skeletonarmy.marrow.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.telemetry.modifiers.AlignmentModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.BoldModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ConditionalColorModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.ConditionalModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.EmptyModifier;
import com.skeletonarmy.marrow.telemetry.modifiers.MultiConditionalModifier;
import java.util.List;
import org.junit.Test;

public class TelemetryFormatterTest {

    @Test
    public void testConditionalIsBlockElement() {
        ConditionalModifier inlineCond = new ConditionalModifier(() -> true, new BoldModifier(), new EmptyModifier());
        assertFalse(inlineCond.isBlockElement());

        ConditionalModifier blockCond = new ConditionalModifier(
                () -> true, new AlignmentModifier(AlignmentModifier.Alignment.CENTER), new EmptyModifier());
        assertTrue(blockCond.isBlockElement());

        ConditionalColorModifier condColor = new ConditionalColorModifier(() -> true);
        assertFalse(condColor.isBlockElement());

        MultiConditionalModifier multiInline =
                new MultiConditionalModifier(() -> 0, new EmptyModifier(), new BoldModifier());
        assertFalse(multiInline.isBlockElement());

        MultiConditionalModifier multiBlock = new MultiConditionalModifier(
                () -> 0, new EmptyModifier(), new AlignmentModifier(AlignmentModifier.Alignment.CENTER));
        assertTrue(multiBlock.isBlockElement());
    }

    @Test
    public void testBlockElementsPlacedAtEnd() {
        FormatBuilder builder = new FormatBuilder("Test")
                .align(AlignmentModifier.Alignment.CENTER)
                .bold()
                .setColor(HtmlColor.RED);

        TelemetryFormatter formatter = new TelemetryFormatter(builder);
        List<TelemetryModifier> modifiers = formatter.getModifiers();
        String formatted = formatter.format();

        assertFalse(modifiers.get(0).isBlockElement());
        assertFalse(modifiers.get(1).isBlockElement());
        assertTrue(modifiers.get(2).isBlockElement());

        assertEquals("<div style=\"text-align:center;\"><font color='#FF0000'><b>Test</b></font></div>", formatted);
    }
}
