package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that sideways intake offsets keep the robot clear of obstacles.
 */
public class WeaverIntakeOffsetTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    private static PathConfig obstacleConfig() {
        return new PathConfig().robotWidth(16).robotHeight(16).clearance(2);
    }

    @Test
    public void generate_intakeOffsetTowardObstacle_keepsRobotClear() {
        // Arrange: lining the intake up with the first target would shift the robot
        // centre about 4 in toward the circle, to about (40.5, 1.0), where the footprint
        // overlaps the circle.
        List<Zone> obstacles = Collections.<Zone>singletonList(new CircleZone(new Point(40, -15), 9));
        Weaver.setConfig(obstacleConfig().intakeWidth(8));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .targets(new Point(40, 5), new Point(80, 0))
                .obstacles(obstacles)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, obstacles, obstacleConfig()));
    }

    @Test
    public void generate_mergedSweepOffsetTowardObstacle_keepsRobotClear() {
        // Arrange: sweeping both targets in one pass would put the robot centre at
        // (40, 4), into the circle's clearance.
        List<Zone> obstacles = Collections.<Zone>singletonList(new CircleZone(new Point(40, 15), 2));
        Weaver.setConfig(obstacleConfig().intakeWidth(8));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .targets(new Point(40, 0), new Point(80, 8))
                .obstacles(obstacles)
                .ordered();

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, obstacles, obstacleConfig()));
    }
}
