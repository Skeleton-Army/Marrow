package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests routes whose end tangents cannot be kept clear.
 */
public class ObstacleAvoiderTangentFallbackTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    /** A 1 x 38.8 in bar, 24 in left of the origin, running along Y. */
    private static List<Zone> bar() {
        return Collections.<Zone>singletonList(new PolygonZone(
                new Point(-25, -19.4), new Point(-24, -19.4), new Point(-24, 19.4), new Point(-25, 19.4)));
    }

    @Test
    public void generate_facingBarWithTargetBehindIt_turnsAndRoutesClear() {
        // Arrange: the robot faces the bar 15 in away with the target beyond it.
        // Leaving along its heading, the turn away would swing a corner into the bar,
        // but it has room to turn on the spot first.
        Weaver.setConfig(new PathConfig().robotWidth(18).robotHeight(18).clearance(2));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(-40, 0, 0))
                .addTarget(new Point(0, 0))
                .obstacles(bar());

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual.getPath(), bar(), Weaver.getConfig()));
    }

    @Test
    public void isTurnClear_cornerSwingsPastAWallBetweenSamples_isNotClear() {
        // Arrange: a 20 x 2 robot turning from 3 to 7 degrees, 10.548 in from a wall.
        // Both ends keep the 0.5 in clearance; part way through, at 5.7 degrees, the
        // corner reaches 10.05 + 0.5 in toward the wall.
        List<Zone> wall = Collections.<Zone>singletonList(
                new PolygonZone(new Point(10.548, -50), new Point(20, -50), new Point(20, 50), new Point(10.548, 50)));
        PathConfig config = new PathConfig().robotWidth(20).robotHeight(2).clearance(0.5);

        // Act
        boolean actual =
                ObstacleAvoider.isTurnClear(new Point(0, 0), Math.toRadians(3), Math.toRadians(7), wall, config);

        // Assert
        assertFalse(actual);
    }

    @Test
    public void isTurnClear_wallBeyondTheHalfDiagonal_isClear() {
        // Arrange
        List<Zone> wall = Collections.<Zone>singletonList(
                new PolygonZone(new Point(10.6, -50), new Point(20, -50), new Point(20, 50), new Point(10.6, 50)));
        PathConfig config = new PathConfig().robotWidth(20).robotHeight(2).clearance(0.5);

        // Act
        boolean actual =
                ObstacleAvoider.isTurnClear(new Point(0, 0), Math.toRadians(3), Math.toRadians(7), wall, config);

        // Assert
        assertTrue(actual);
    }

    @Test
    public void generate_noRoomToTurnOnTheSpot_keepsTheStartTangent() {
        // Arrange: a long, thin robot in a dead-end slot just wider than it is, facing
        // the closed end, with its target out of the open end behind it. The closed end
        // is too close to move toward, so no route keeps the start heading, and turning
        // round on the spot would hit the slot walls.
        List<Zone> slot = Arrays.<Zone>asList(
                new PolygonZone(new Point(-60, 2), new Point(-20, 2), new Point(-20, 30), new Point(-60, 30)),
                new PolygonZone(new Point(-60, -30), new Point(-20, -30), new Point(-20, -2), new Point(-60, -2)),
                new PolygonZone(new Point(-29, -2), new Point(-25, -2), new Point(-25, 2), new Point(-29, 2)));
        Weaver.setConfig(new PathConfig().robotWidth(20).robotHeight(2).clearance(0.5));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(-40, 0, 0))
                .addTarget(new Point(-80, 0))
                .obstacles(slot);

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertEquals(0.0, actual.getPath().getHeading(0.0), 1e-6);
    }
}
