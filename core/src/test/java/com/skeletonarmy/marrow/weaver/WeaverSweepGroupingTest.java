package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    /** Index of the first route sample close enough to {@code target}. */
    private static int firstReach(PathRoute route, Point target) {
        List<Point> samples = route.sample(2000);

        for (int i = 0; i < samples.size(); i++) {
            if (samples.get(i).distanceTo(target) <= 0.5) {
                return i;
            }
        }

        return -1;
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

        // Assert: the sweep runs ahead first, then behind. The route turns around before
        // the first stop because the merged sweep heads back west.
        int aheadIndex = firstReach(actual, ahead);
        int behindIndex = firstReach(actual, behind);
        assertTrue("ahead=" + aheadIndex + " behind=" + behindIndex, aheadIndex >= 0);
        assertTrue("ahead=" + aheadIndex + " behind=" + behindIndex, behindIndex > aheadIndex);
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
