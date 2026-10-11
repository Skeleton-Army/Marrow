package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;

import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests which targets are merged into one intake sweep.
 */
public class WeaverSweepGroupingTest {

    private static final double EPS = 1e-6;

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    private static List<PathCurve> cubics(PathRoute route) {
        List<PathCurve> cubics = new ArrayList<>();

        for (PathCurve segment : route.getSegments()) {
            cubics.addAll(segment.toCubicSegments());
        }

        return cubics;
    }

    @Test
    public void generate_targetsOnOppositeSides_reachesFirstTargetBeforeSecond() {
        // Arrange
        Point ahead = new Point(50, 0);
        Point behind = new Point(-50, 0);
        Weaver.setConfig(new PathConfig().intakeWidth(10).smoothing(false));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .targets(ahead, behind)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        Point firstStop = cubics(actual).get(0).get(1.0);
        assertEquals(50.0, firstStop.getX(), EPS);
    }

    @Test
    public void generate_orderedTargetBackAlongTheLine_isNotMergedIntoTheSweep() {
        // Arrange
        Point far = new Point(40, 0);
        Point near = new Point(20, 0);
        Weaver.setConfig(new PathConfig().intakeWidth(10).smoothing(false));
        WeaverBuilder builder =
                Weaver.builder().start(new Point(0, 0, 0)).targets(far, near).ordered();

        // Act
        PathResult actual = builder.generate();

        // Assert: the key points follow the visited order, far first.
        assertEquals(Arrays.asList(far, near), actual.getVisitedTargets());
        assertEquals(40.0, cubics(actual.getPath()).get(0).get(1.0).getX(), EPS);
    }
}
