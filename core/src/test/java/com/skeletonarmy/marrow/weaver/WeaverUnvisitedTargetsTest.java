package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;

import com.skeletonarmy.marrow.zones.Point;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the unvisited targets reported for repeated targets.
 */
public class WeaverUnvisitedTargetsTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    @Test
    public void generate_sameTargetTwiceWithLimit_reportsEveryUnvisitedEntry() {
        // Arrange
        Point repeated = new Point(30, 0);
        Point other = new Point(60, 0);
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .targets(repeated, repeated, other)
                .maxTargets(1);

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertEquals(Collections.singletonList(repeated), actual.getVisitedTargets());
        assertEquals(Arrays.asList(repeated, other), actual.getUnvisitedTargets());
    }
}
