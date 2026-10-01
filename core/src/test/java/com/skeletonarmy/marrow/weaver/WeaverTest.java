package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class WeaverTest {

    private static final double EPS = 1e-6;
    private static final double START_X = 72;
    private static final double START_Y = 72;

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    // --- Global configuration ---

    @Test
    public void getConfig_defaults_returnsDefaultConfig() {
        // Arrange & Act
        PathConfig actual = Weaver.getConfig();

        // Assert
        assertNotNull(actual);
        assertEquals(0.0, actual.getWidth(), EPS);
    }

    @Test
    public void setConfig_customConfig_replacesGlobalConfig() {
        // Arrange
        PathConfig custom = new PathConfig().width(99);

        // Act
        Weaver.setConfig(custom);

        // Assert
        assertSame(custom, Weaver.getConfig());
        assertEquals(99.0, Weaver.getConfig().getWidth(), EPS);
    }

    @Test
    public void resetToDefaults_afterCustomConfig_restoresDefaultConfig() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(99));

        // Act
        Weaver.resetToDefaults();

        // Assert
        assertEquals(0.0, Weaver.getConfig().getWidth(), EPS);
    }

    // --- Builder argument validation ---

    @Test
    public void generate_startMissing_throwsIllegalStateException() {
        // Arrange
        Weaver.Builder builder = Weaver.builder().addTarget(new PathPose(START_X + 20, START_Y));

        // Act
        IllegalStateException actual = assertThrows(IllegalStateException.class, builder::generate);

        // Assert
        assertEquals("Start PathPose is required", actual.getMessage());
    }

    @Test
    public void generate_noTargetsOrDestination_throwsIllegalStateException() {
        // Arrange
        Weaver.Builder builder = Weaver.builder().start(new PathPose(START_X, START_Y, 0));

        // Act
        IllegalStateException actual = assertThrows(IllegalStateException.class, builder::generate);

        // Assert
        assertEquals("Either targets or a destination must be set", actual.getMessage());
    }

    // --- Pure avoidance (destination) mode ---

    @Test
    public void generate_destinationOnly_returnsPathEndingAtDestination() {
        // Arrange
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(10, 10, 0))
                .end(new PathPose(30, 10));

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertEquals(1, actual.getSegments().size());
        assertEquals(30.0, actual.getPath().get(1.0).getX(), EPS);
        assertEquals(10.0, actual.getPath().get(1.0).getY(), EPS);
    }

    @Test
    public void generate_destinationOnly_returnsEndHeadingAlongTravel() {
        // Arrange
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(10, 10, 0))
                .end(new PathPose(30, 10));

        // Act
        List<Double> actualHeadings = builder.generate().getSegmentEndHeadingsRad();

        // Assert
        assertEquals(Collections.singletonList(0.0), actualHeadings);
    }

    @Test
    public void generate_destinationWithObstacle_returnsObstacleClearPath() {
        // Arrange
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(20, 10), 3));
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(10, 10))
                .end(new PathPose(30, 10))
                .addObstacle(obstacles.get(0));

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertTrue(Weaver.isPathClear(actual, obstacles, Weaver.getConfig().getClearance(), 200));
    }

    // --- Intake mode ---

    @Test
    public void generate_singleTarget_endsAtTarget() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0));
        PathPose target = new PathPose(START_X + 20, START_Y);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(target);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(target.getX(), actual.get(1.0).getX(), EPS);
        assertEquals(target.getY(), actual.get(1.0).getY(), EPS);
    }

    @Test
    public void generate_multipleTargets_reordersToEndAtFurthestTarget() {
        // Arrange
        PathPose far = new PathPose(START_X + 50, START_Y);
        PathPose near = new PathPose(START_X + 10, START_Y);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(far)
                .addTarget(near);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(far.getX(), actual.get(1.0).getX(), EPS);
    }

    @Test
    public void generate_orderedTargets_endsAtLastAddedTarget() {
        // Arrange
        PathPose far = new PathPose(START_X + 50, START_Y);
        PathPose near = new PathPose(START_X + 10, START_Y);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(far)
                .addTarget(near)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(near.getX(), actual.get(1.0).getX(), EPS);
    }

    @Test
    public void generate_zeroWidth_passesThroughEveryTarget() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0));
        PathPose first = new PathPose(START_X + 20, START_Y + 5);
        PathPose second = new PathPose(START_X + 50, START_Y - 5);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(first)
                .addTarget(second)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(first.getX(), actual.get(0.5).getX(), EPS);
        assertEquals(first.getY(), actual.get(0.5).getY(), EPS);
        assertEquals(second.getX(), actual.get(1.0).getX(), EPS);
        assertEquals(second.getY(), actual.get(1.0).getY(), EPS);
    }

    @Test
    public void generate_wideIntake_clustersPerpendicularTargets_keepsPathStraight() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(18));
        List<PathPose> targets = Arrays.asList(
                new PathPose(START_X + 20, START_Y),
                new PathPose(START_X + 40, START_Y - 5),
                new PathPose(START_X + 40, START_Y + 5));
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .targets(targets)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(START_Y, actual.get(0.25).getY(), 1e-3);
        assertEquals(START_Y, actual.get(0.5).getY(), 1e-3);
        assertEquals(START_Y, actual.get(0.75).getY(), 1e-3);
        assertEquals(START_Y, actual.get(1.0).getY(), 1e-3);
    }

    @Test
    public void generate_wideIntake_separateTargets_solvesCapturePoints() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(18));
        PathPose first = new PathPose(START_X + 40, START_Y - 30);
        PathPose second = new PathPose(START_X + 80, START_Y + 30);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(first)
                .addTarget(second)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(second.getX(), actual.get(1.0).getX(), EPS);
        assertEquals(second.getY(), actual.get(1.0).getY(), EPS);
    }

    // --- Blocked targets ---

    @Test
    public void generate_allTargetsBlocked_returnsIdlePathAtStart() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0));
        Zone obstacle = new CircleZone(new Point(START_X + 30, START_Y), 8);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(START_X + 30, START_Y))
                .ordered()
                .addObstacle(obstacle);

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertEquals(START_X, actual.getPath().get(1.0).getX(), EPS);
        assertEquals(START_Y, actual.getPath().get(1.0).getY(), EPS);
        assertEquals(Collections.singletonList(0.0), actual.getSegmentEndHeadingsRad());
    }

    @Test
    public void generate_allTargetsBlocked_reportsSkippedTargets() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0));
        Zone obstacle = new CircleZone(new Point(START_X + 30, START_Y), 8);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(START_X + 30, START_Y))
                .ordered()
                .addObstacle(obstacle);

        // Act
        int actual = builder.generate().getSkippedTargets();

        // Assert
        assertEquals(1, actual);
    }

    @Test
    public void generate_someTargetsBlocked_reportsSkippedTargets() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0));
        Zone obstacle = new CircleZone(new Point(START_X + 30, START_Y), 8);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(START_X + 30, START_Y))
                .addTarget(new PathPose(START_X + 60, START_Y))
                .addObstacle(obstacle);

        // Act
        int actual = builder.generate().getSkippedTargets();

        // Assert
        assertEquals(1, actual);
    }

    @Test
    public void generate_blockedTargetExclusionDisabled_reportsNoSkippedTargets() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0).excludeBlockedTargets(false));
        Zone obstacle = new CircleZone(new Point(START_X + 30, START_Y), 8);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(START_X + 30, START_Y))
                .ordered()
                .addObstacle(obstacle);

        // Act
        int actual = builder.generate().getSkippedTargets();

        // Assert
        assertEquals(0, actual);
    }

    @Test
    public void generate_destinationMode_reportsNoSkippedTargets() {
        // Arrange
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(10, 10, 0))
                .end(new PathPose(30, 10));

        // Act
        int actual = builder.generate().getSkippedTargets();

        // Assert
        assertEquals(0, actual);
    }

    @Test
    public void generate_blockedTargetExclusionDisabled_reachesBlockedTarget() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0).excludeBlockedTargets(false));
        Zone obstacle = new CircleZone(new Point(START_X + 30, START_Y), 8);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(START_X + 30, START_Y))
                .ordered()
                .addObstacle(obstacle);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(START_X + 30, actual.get(1.0).getX(), EPS);
        assertEquals(START_Y, actual.get(1.0).getY(), EPS);
    }

    @Test
    public void generate_someTargetsBlocked_reachesFreeTarget() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0));
        Zone obstacle = new CircleZone(new Point(START_X + 30, START_Y), 8);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(new PathPose(START_X + 30, START_Y))
                .addTarget(new PathPose(START_X + 60, START_Y))
                .addObstacle(obstacle);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(START_X + 60, actual.get(1.0).getX(), EPS);
        assertEquals(START_Y, actual.get(1.0).getY(), EPS);
    }

    // --- Builder collection replacement ---

    @Test
    public void builderTargets_replacesPreviouslyAddedTargets() {
        // Arrange
        PathPose discarded = new PathPose(START_X + 50, START_Y);
        PathPose kept = new PathPose(START_X + 20, START_Y);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(discarded)
                .targets(Collections.singletonList(kept))
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(kept.getX(), actual.get(1.0).getX(), EPS);
    }

    @Test
    public void builderObstacles_replacesPreviouslyAddedObstacles() {
        // Arrange
        Zone discarded = new CircleZone(new Point(20, 10), 3);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(10, 10))
                .end(new PathPose(30, 10))
                .addObstacle(discarded)
                .obstacles(Collections.<Zone>emptyList());

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(20.0, actual.get(0.5).getX(), EPS);
        assertEquals(10.0, actual.get(0.5).getY(), EPS);
    }

    // --- isCapturedByIntake ---

    @Test
    public void isCapturedByIntake_withinIntakeWidth_returnsTrue() {
        // Arrange
        Point robotCenter = new Point(0, 0);
        Point target = new Point(0, 1);

        // Act
        boolean actual = Weaver.isCapturedByIntake(robotCenter, 0, 4, target);

        // Assert
        assertTrue(actual);
    }

    @Test
    public void isCapturedByIntake_atExactHalfWidth_returnsTrue() {
        // Arrange
        Point robotCenter = new Point(0, 0);
        Point target = new Point(0, 2);

        // Act
        boolean actual = Weaver.isCapturedByIntake(robotCenter, 0, 4, target);

        // Assert
        assertTrue(actual);
    }

    @Test
    public void isCapturedByIntake_beyondIntakeWidth_returnsFalse() {
        // Arrange
        Point robotCenter = new Point(0, 0);
        Point target = new Point(0, 3);

        // Act
        boolean actual = Weaver.isCapturedByIntake(robotCenter, 0, 4, target);

        // Assert
        assertFalse(actual);
    }

    @Test
    public void isCapturedByIntake_alongHeadingAxis_returnsFalse() {
        // Arrange
        Point robotCenter = new Point(0, 0);
        Point target = new Point(1, 0);

        // Act
        boolean actual = Weaver.isCapturedByIntake(robotCenter, 0, 4, target);

        // Assert
        assertFalse(actual);
    }

    // --- isPathClear ---

    @Test
    public void isPathClear_routeClearOfObstacle_returnsTrue() {
        // Arrange
        PathRoute route = new PathRoute(Collections.singletonList(
                new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)))));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(5, 10), 1));

        // Act
        boolean actual = Weaver.isPathClear(route, obstacles, 0.5, 50);

        // Assert
        assertTrue(actual);
    }

    @Test
    public void isPathClear_routeThroughObstacle_returnsFalse() {
        // Arrange
        PathRoute route = new PathRoute(Collections.singletonList(
                new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)))));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(5, 0), 1));

        // Act
        boolean actual = Weaver.isPathClear(route, obstacles, 0.5, 50);

        // Assert
        assertFalse(actual);
    }

    @Test
    public void isPathClear_curveWithRobotFootprintViolatingClearance_returnsFalse() {
        // Arrange
        PathCurve curve = new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(5, 0.9), 0.1));

        // Act
        boolean actual = Weaver.isPathClear(curve, obstacles, 0.5, 3.0, 1.0, 50);

        // Assert
        assertFalse(actual);
    }

    @Test
    public void isPathClear_noObstacles_returnsTrue() {
        // Arrange
        PathCurve curve = new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)));

        // Act
        boolean actual = Weaver.isPathClear(curve, Collections.<Zone>emptyList(), 1.0, 10);

        // Assert
        assertTrue(actual);
    }

    @Test
    public void isPathClear_withPathConfig_returnsCorrectResult() {
        // Arrange
        PathRoute route = new PathRoute(Collections.singletonList(
                new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)))));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(5, 0), 1));
        PathConfig config = new PathConfig().clearance(0.5).robotWidth(0).robotHeight(0);

        // Act & Assert
        assertFalse(Weaver.isPathClear(route, obstacles, config, 50));
        assertFalse(Weaver.isPathClear(route, obstacles, config));
    }

    @Test
    public void isPathClear_curveWithPathConfig_returnsCorrectResult() {
        // Arrange
        PathCurve curve = new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(5, 10), 1));
        PathConfig config = new PathConfig().clearance(0.5).robotWidth(0).robotHeight(0);

        // Act & Assert
        assertTrue(Weaver.isPathClear(curve, obstacles, config, 50));
        assertTrue(Weaver.isPathClear(curve, obstacles, config));
    }

    @Test
    public void isCapturedByIntake_withPathConfig_returnsCorrectResult() {
        // Arrange
        Point center = new Point(0, 0);
        Point target = new Point(0, 5);
        PathConfig config = new PathConfig().width(12.0);

        // Act & Assert
        assertTrue(Weaver.isCapturedByIntake(center, 0, config, target));

        Weaver.setConfig(config);
        assertTrue(Weaver.isCapturedByIntake(center, 0, target));
        Weaver.resetToDefaults();
    }

    @Test
    public void isPathClear_usingPathConfig_returnsCorrectResult() {
        // Arrange
        PathRoute route = new PathRoute(Collections.singletonList(
                new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)))));
        PathCurve curve = new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0)));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(5, 10), 1));
        PathConfig config = new PathConfig().clearance(0.5).robotWidth(0).robotHeight(0);

        // Act & Assert
        assertTrue(Weaver.isPathClear(route, obstacles, config));
        assertTrue(Weaver.isPathClear(curve, obstacles, config));
        assertTrue(Weaver.isPathClear(route, obstacles, config, 50));
        assertTrue(Weaver.isPathClear(curve, obstacles, config, 50));
    }

    // --- Smoothing toggle ---

    @Test
    public void generate_smoothingDisabled_endsAtTarget() {
        // Arrange
        Weaver.setConfig(new PathConfig().width(0).smoothing(false));
        PathPose target = new PathPose(START_X + 20, START_Y);
        Weaver.Builder builder = Weaver.builder()
                .start(new PathPose(START_X, START_Y, 0))
                .addTarget(target);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertEquals(target.getX(), actual.get(1.0).getX(), EPS);
        assertEquals(target.getY(), actual.get(1.0).getY(), EPS);
    }
}
