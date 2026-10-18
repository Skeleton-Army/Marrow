package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;

import com.skeletonarmy.marrow.zones.Point;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that target ordering handles very large headings.
 */
public class TargetOrdererAngleTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    @Test(timeout = 2000)
    public void generate_hugeTargetHeading_returnsPromptly() {
        // Arrange: wrapping 1e10 rad by repeated subtraction takes over a billion steps.
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .addTarget(new Point(10, 0, 1e10), HeadingPolicy.LOCKED)
                .addTarget(new Point(20, 0), HeadingPolicy.FREE);

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertEquals(2, actual.getVisitedTargets().size());
    }
}
