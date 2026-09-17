package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class BezierMathTest {

    @Before
    public void setup() {
        BezierPathGenerator.resetToDefaults();
    }

    private static final double EPS = 1e-9;

    @Test
    public void endpointsMatchControlPoints() {
        BezierCurve curve = new BezierCurve(Arrays.asList(
                new Point(0, 0), new Point(5, 10), new Point(15, 10), new Point(20, 0)
        ));

        assertEquals(0.0, curve.get(0.0).getX(), EPS);
        assertEquals(20.0, curve.get(1.0).getX(), EPS);
    }

    @Test
    public void linearCurveIsStraightLine() {
        BezierCurve line = new BezierCurve(Arrays.asList(new Point(0, 0), new Point(10, 10)));
        Point mid = line.get(0.5);

        assertEquals(5.0, mid.getX(), EPS);
        assertEquals(5.0, mid.getY(), EPS);
        assertEquals(Math.PI / 4, line.getHeading(0.5), 1e-6);
    }

    @Test
    public void derivativeMatchesTangent() {
        BezierCurve curve = new BezierCurve(Arrays.asList(new Point(0, 0), new Point(10, 0), new Point(10, 10)));
        // At start, tangent should be exactly along +x (towards second control point)
        assertEquals(0.0, curve.getHeading(0.0), 1e-6);
        // At end, tangent should be exactly along +y (from second to third control point)
        assertEquals(Math.PI / 2, curve.getHeading(1.0), 1e-6);
    }

    @Test
    public void samplingCoversFullRange() {
        BezierCurve curve = new BezierCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)));
        List<Point> pts = curve.sample(11);

        assertEquals(11, pts.size());
        assertEquals(0.0, pts.get(0).getX(), EPS);
        assertEquals(10.0, pts.get(10).getX(), EPS);
    }

    @Test
    public void approxLengthOfStraightLineIsCorrect() {
        BezierCurve line = new BezierCurve(Arrays.asList(new Point(0, 0), new Point(3, 4)));
        assertEquals(5.0, line.approxLength(50), 1e-3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooFewControlPointsThrows() {
        new BezierCurve(Collections.singletonList(new Point(0, 0)));
    }
}
