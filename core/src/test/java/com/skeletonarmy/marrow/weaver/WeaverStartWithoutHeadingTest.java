package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests intake mode from a start pose with no heading.
 */
public class WeaverStartWithoutHeadingTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    private static boolean isFinite(Point point) {
        return !Double.isNaN(point.getX())
                && !Double.isInfinite(point.getX())
                && !Double.isNaN(point.getY())
                && !Double.isInfinite(point.getY());
    }

    private static void assertFiniteControlPoints(PathRoute route) {
        for (PathCurve segment : route.getSegments()) {
            for (Point controlPoint : segment.getControlPoints()) {
                assertTrue(controlPoint.toString(), isFinite(controlPoint));
            }
        }
    }

    @Test
    public void generate_startWithoutHeading_returnsFinitePath() {
        // Arrange
        Weaver.setConfig(new PathConfig().smoothing(false));
        WeaverBuilder builder = Weaver.builder().start(new Point(0, 0)).addTarget(new Point(30, 0));

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertFiniteControlPoints(actual);
        assertEquals(0.0, actual.getHeading(0.0), 1e-9);
    }

    @Test
    public void generate_startWithoutHeading_ordersTargetsByDistance() {
        // Arrange
        Point far = new Point(100, 0);
        Point near = new Point(10, 0);
        Point middle = new Point(50, 0);
        WeaverBuilder builder = Weaver.builder().start(new Point(0, 0)).targets(far, near, middle);

        // Act
        List<Point> actual = builder.generate().getVisitedTargets();

        // Assert
        assertEquals(Arrays.asList(near, middle, far), actual);
    }

    @Test
    public void generate_startWithoutHeadingAndGreedyOrdering_visitsEveryTarget() {
        // Arrange: more targets than the brute-force limit, so the greedy orderer runs.
        List<Point> targets = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            targets.add(new Point(10 * i, 0));
        }

        Weaver.setConfig(new PathConfig().bruteForceOrderLimit(3));
        WeaverBuilder builder = Weaver.builder().start(new Point(0, 0)).targets(targets);

        // Act
        List<Point> actual = builder.generate().getVisitedTargets();

        // Assert
        assertEquals(targets, actual);
    }

    @Test
    public void generate_startWithoutHeadingAndGreedyTargetLimit_visitsLimit() {
        // Arrange: more targets than the brute-force limit, so greedy selection runs.
        List<Point> targets = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            targets.add(new Point(10 * i, 0));
        }

        Weaver.setConfig(new PathConfig().bruteForceOrderLimit(3));
        WeaverBuilder builder =
                Weaver.builder().start(new Point(0, 0)).targets(targets).maxTargets(4);

        // Act
        List<Point> actual = builder.generate().getVisitedTargets();

        // Assert
        assertEquals(targets.subList(0, 4), actual);
    }

    @Test
    public void generate_startWithoutHeadingAndNoReachableTarget_reportsFiniteHeading() {
        // Arrange: the only target sits inside an obstacle, so it is excluded and no
        // target is reachable. The stationary route must still report a finite heading.
        Weaver.setConfig(new PathConfig().excludeBlockedTargets(true));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0))
                .addTarget(new Point(30, 0))
                .obstacles(new CircleZone(new Point(30, 0), 5));

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertTrue(actual.getVisitedTargets().isEmpty());
        assertFalse(Double.isNaN(actual.getSegmentEndHeadingsRad().get(0)));
    }

    @Test
    public void generate_startWithoutHeadingAndFirstTargetOnTheStart_returnsFinitePath() {
        // Arrange
        Weaver.setConfig(new PathConfig().intakeWidth(13).smoothing(false));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0))
                .targets(new Point(0, 0), new Point(10, 0))
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertFiniteControlPoints(actual);
    }
}
