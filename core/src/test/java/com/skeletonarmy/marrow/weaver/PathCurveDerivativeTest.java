package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;

import com.skeletonarmy.marrow.zones.Point;
import java.util.Arrays;
import org.junit.Test;

/**
 * Tests that {@link PathCurve#derivative(double)} is the derivative of {@link PathCurve#get(double)}.
 */
public class PathCurveDerivativeTest {

    private static void assertMatchesFiniteDifference(PathCurve curve, double t) {
        double step = 1e-6;
        Point before = curve.get(t - step);
        Point after = curve.get(t + step);
        Point actual = curve.derivative(t);

        assertEquals((after.getX() - before.getX()) / (2 * step), actual.getX(), 1e-3);
        assertEquals((after.getY() - before.getY()) / (2 * step), actual.getY(), 1e-3);
    }

    @Test
    public void derivative_singleCubic_matchesFiniteDifference() {
        // Arrange
        PathCurve curve =
                new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 5), new Point(20, -5), new Point(30, 0)));

        // Act and assert
        assertMatchesFiniteDifference(curve, 0.3);
    }

    @Test
    public void derivative_twoCubicChain_matchesFiniteDifference() {
        // Arrange
        PathCurve curve = new PathCurve(Arrays.asList(
                new Point(0, 0),
                new Point(10, 0),
                new Point(20, 10),
                new Point(30, 10),
                new Point(40, 10),
                new Point(50, 0),
                new Point(60, 0)));

        // Act and assert: one sample inside each cubic.
        assertMatchesFiniteDifference(curve, 0.25);
        assertMatchesFiniteDifference(curve, 0.75);
    }

    @Test
    public void derivative_threeCubicChain_matchesFiniteDifference() {
        // Arrange
        PathCurve curve = new PathCurve(Arrays.asList(
                new Point(0, 0),
                new Point(5, 5),
                new Point(10, 5),
                new Point(15, 0),
                new Point(20, -5),
                new Point(25, -5),
                new Point(30, 0),
                new Point(35, 5),
                new Point(40, 5),
                new Point(45, 0)));

        // Act and assert
        assertMatchesFiniteDifference(curve, 0.2);
        assertMatchesFiniteDifference(curve, 0.5);
        assertMatchesFiniteDifference(curve, 0.9);
    }
}
