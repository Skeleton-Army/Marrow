package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.Point;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests where consecutive intake sweeps are placed.
 */
public class WeaverSweepPlacementTest {

    private static final double EPS = 1e-6;

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    /**
     * Whether the robot, facing along the route, passes {@code target} through its
     * intake mouth: the target crosses the robot's lateral axis within half the
     * intake width of its centre.
     */
    private static boolean passesThroughIntake(PathRoute route, Point target, double intakeWidth) {
        List<Point> samples = route.sample(2000);
        double previousAlong = Double.NaN;

        for (int i = 0; i < samples.size() - 1; i++) {
            Point here = samples.get(i);
            Point next = samples.get(i + 1);

            if (here.distanceTo(next) < EPS) {
                continue;
            }

            double heading = Math.atan2(next.getY() - here.getY(), next.getX() - here.getX());
            double relativeX = target.getX() - here.getX();
            double relativeY = target.getY() - here.getY();
            double along = relativeX * Math.cos(heading) + relativeY * Math.sin(heading);
            double lateral = -relativeX * Math.sin(heading) + relativeY * Math.cos(heading);
            boolean crossed = Math.abs(along) < 1e-3 || (previousAlong > 0 && along <= 0);

            if (crossed && Math.abs(lateral) <= intakeWidth / 2.0 + 1e-3) {
                return true;
            }

            previousAlong = along;
        }

        Point end = samples.get(samples.size() - 1);
        return end.distanceTo(target) < 1e-3;
    }

    @Test
    public void generate_sweepAfterSweep_passesEveryTargetThroughTheIntake() {
        // Arrange
        double intakeWidth = 10;
        List<Point> targets =
                Arrays.asList(new Point(50, 0), new Point(100, 0), new Point(100, 50), new Point(111, 55.5));
        Weaver.setConfig(new PathConfig().intakeWidth(intakeWidth).smoothing(false));
        WeaverBuilder builder =
                Weaver.builder().start(new Point(0, 0, 0)).targets(targets).ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        for (Point target : targets) {
            assertTrue(target.toString(), passesThroughIntake(actual, target, intakeWidth));
        }
    }

    @Test
    public void generate_sweepStartingOnAHeadedTarget_runsTowardTheNextTarget() {
        // Arrange: the sweep of (10, 0) and (30, 0) starts on the first target, which
        // the robot reaches facing pi/2.
        double intakeWidth = 10;
        List<Point> targets = Arrays.asList(new Point(10, 0, Math.PI / 2), new Point(10, 0), new Point(30, 0));
        Weaver.setConfig(new PathConfig().intakeWidth(intakeWidth).smoothing(false));
        WeaverBuilder builder =
                Weaver.builder().start(new Point(0, 0, 0)).targets(targets).ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        for (Point target : targets) {
            assertTrue(target.toString(), passesThroughIntake(actual, target, intakeWidth));
        }
    }

    @Test
    public void generate_sweepStartingOnTheStart_runsTowardTheNextTarget() {
        // Arrange: the robot faces pi/2, but the sweep runs along X.
        double intakeWidth = 13;
        List<Point> targets = Arrays.asList(new Point(0, 0), new Point(20, 0));
        Weaver.setConfig(new PathConfig().intakeWidth(intakeWidth).smoothing(false));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, Math.PI / 2))
                .targets(targets)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        for (Point target : targets) {
            assertTrue(target.toString(), passesThroughIntake(actual, target, intakeWidth));
        }
    }
}
