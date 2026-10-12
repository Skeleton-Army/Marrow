package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.CompositeZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the clearance relief given to route endpoints near obstacles.
 */
public class ObstacleAvoiderEndpointReliefTest {

    @Before
    public void setup() {
        Weaver.resetToDefaults();
    }

    private static PathConfig obstacleConfig() {
        return new PathConfig().robotWidth(16).robotHeight(16).clearance(2);
    }

    @Test
    public void generate_obstacleBesideStart_keepsFullClearanceFromOtherObstacles() {
        // Arrange
        Zone besideStart = new CircleZone(new Point(0, -9), 0.5);
        Zone midRoute = new CircleZone(new Point(30, 9.5), 1);
        Weaver.setConfig(obstacleConfig());
        WeaverBuilder builder =
                Weaver.builder().start(new Point(0, 0, 0)).end(new Point(60, 0)).obstacles(besideStart, midRoute);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, Collections.singletonList(midRoute), obstacleConfig()));
    }

    @Test
    public void generate_destinationInsideObstacle_stillAvoidsObstaclesOnTheWay() {
        // Arrange
        Zone onTheWay = new CircleZone(new Point(0, 0), 8);
        Zone aroundDestination = new CircleZone(new Point(60, 0), 3);
        Weaver.setConfig(obstacleConfig());
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(-60, 0, 0))
                .end(new Point(60, 0))
                .obstacles(onTheWay, aroundDestination);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, Collections.singletonList(onTheWay), obstacleConfig()));
    }

    @Test
    public void generate_routeBetweenSeveralObstacles_keepsFullClearance() {
        // Arrange: found by fuzzing. The old relief rules let the fitted curve through
        // with less than the required clearance.
        List<Zone> obstacles = Arrays.<Zone>asList(
                new CircleZone(new Point(47.9, 8.3), 7.5),
                new CircleZone(new Point(-39.2, -2.2), 8.9),
                new CircleZone(new Point(-2.5, -21.3), 7.8),
                new CircleZone(new Point(23.7, -38.3), 6.9),
                new CircleZone(new Point(-48.5, 25.0), 6.3));
        Weaver.setConfig(obstacleConfig());
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(20.08, 41.35, 0))
                .end(new Point(-50.9, -24.37))
                .obstacles(obstacles);

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, obstacles, obstacleConfig()));
    }

    @Test
    public void generate_startInsideOnePartOfACompositeObstacle_stillAvoidsTheOtherPart() {
        // Arrange
        Zone aroundStart = new CircleZone(new Point(0, 0), 3);
        Zone onTheWay = new CircleZone(new Point(30, 0), 5);
        Weaver.setConfig(obstacleConfig());
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(0, 0, 0))
                .end(new Point(60, 0))
                .obstacles(new CompositeZone(aroundStart, onTheWay));

        // Act
        PathRoute actual = builder.generate().getPath();

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, Collections.singletonList(onTheWay), obstacleConfig()));
    }
}
