package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.CompositeZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ObstacleAvoiderTest {

    private static final double EPS = 1e-6;

    // --- avoid ---

    @Test
    public void avoid_nullObstacles_returnsSamePath() {
        // Arrange
        PathRoute route = straightRoute(new Point(0, 0), new Point(10, 0));

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, null, new PathConfig());

        // Assert
        assertSame(route, actual);
    }

    @Test
    public void avoid_emptyObstacles_returnsSamePath() {
        // Arrange
        PathRoute route = straightRoute(new Point(0, 0), new Point(10, 0));

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, Collections.emptyList(), new PathConfig());

        // Assert
        assertSame(route, actual);
    }

    @Test
    public void avoid_circleObstacleOnPath_returnsObstacleClearPath() {
        // Arrange
        PathRoute route = straightRoute(new Point(10, 10), new Point(30, 10));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(20, 10), 3));
        PathConfig config = new PathConfig().robotWidth(0).robotHeight(0);

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, obstacles, config);

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, obstacles, config, 200));
    }

    @Test
    public void avoid_polygonObstacleOnPath_returnsObstacleClearPath() {
        // Arrange
        PathRoute route = straightRoute(new Point(72, 72), new Point(120, 72));
        List<Zone> obstacles = Collections.singletonList(new PolygonZone(new Point(96, 72), 12, 12));
        PathConfig config = new PathConfig().robotWidth(0).robotHeight(0);

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, obstacles, config);

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, obstacles, config, 200));
    }

    @Test
    public void avoid_compositeObstacleOnPath_returnsObstacleClearPath() {
        // Arrange
        PathRoute route = straightRoute(new Point(72, 72), new Point(120, 72));
        List<Zone> obstacles = Collections.singletonList(new CompositeZone(
                new CircleZone(new Point(92, 72), 4),
                new PolygonZone(new Point(100, 72), 8, 8)));
        PathConfig config = new PathConfig().robotWidth(0).robotHeight(0);

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, obstacles, config);

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, obstacles, config, 200));
    }

    @Test
    public void avoid_multipleObstacles_returnsPathClearOfAll() {
        // Arrange
        PathRoute route = straightRoute(new Point(72, 72), new Point(120, 72));
        List<Zone> obstacles = Arrays.asList(
                new CircleZone(new Point(88, 72), 4),
                new CircleZone(new Point(104, 72), 4));
        PathConfig config = new PathConfig().robotWidth(0).robotHeight(0);

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, obstacles, config);

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual, obstacles, config, 300));
    }

    @Test
    public void avoid_obstacleOnPath_preservesRouteEndpoints() {
        // Arrange
        PathRoute route = straightRoute(new Point(10, 10), new Point(30, 10));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(20, 10), 3));

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, obstacles, new PathConfig());

        // Assert
        assertEquals(10.0, actual.get(0.0).getX(), EPS);
        assertEquals(10.0, actual.get(0.0).getY(), EPS);
        assertEquals(30.0, actual.get(1.0).getX(), EPS);
        assertEquals(10.0, actual.get(1.0).getY(), EPS);
    }

    @Test
    public void avoid_endpointInsideObstacle_preservesEndpoint() {
        // Arrange
        PathRoute route = straightRoute(new Point(10, 10), new Point(20, 10));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(20, 10), 5));

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, obstacles, new PathConfig());

        // Assert
        assertEquals(20.0, actual.get(1.0).getX(), EPS);
        assertEquals(10.0, actual.get(1.0).getY(), EPS);
    }

    @Test
    public void avoid_twoSegmentsObstacleOnSecondSegment_returnsTwoClearSegments() {
        // Arrange
        PathRoute route = new PathRoute(Arrays.asList(
                new PathCurve(Arrays.asList(new Point(72, 72), new Point(96, 72))),
                new PathCurve(Arrays.asList(new Point(96, 72), new Point(120, 72)))));
        List<Zone> obstacles = Collections.singletonList(new CircleZone(new Point(112, 72), 3));
        PathConfig config = new PathConfig().robotWidth(2).robotHeight(2).clearance(1);

        // Act
        PathRoute actual = ObstacleAvoider.avoid(route, obstacles, config);

        // Assert
        assertEquals(2, actual.getSegments().size());
        assertTrue(WeaverGenerator.isPathClear(actual, obstacles, config, 200));
    }

    // --- smooth ---

    @Test
    public void smooth_straightCurve_preservesEndpointsAndHeading() {
        // Arrange
        PathRoute route = straightRoute(new Point(0, 0), new Point(10, 0));

        // Act
        PathRoute actual = ObstacleAvoider.smooth(route);

        // Assert
        assertEquals(0.0, actual.get(0.0).getX(), EPS);
        assertEquals(0.0, actual.get(0.0).getY(), EPS);
        assertEquals(10.0, actual.get(1.0).getX(), EPS);
        assertEquals(0.0, actual.get(1.0).getY(), EPS);
        assertEquals(0.0, actual.getHeading(0.0), EPS);
        assertEquals(0.0, actual.getHeading(1.0), EPS);
    }

    @Test
    public void smooth_compositeCurve_passesThroughKeypoints() {
        // Arrange
        PathCurve composite = new PathCurve(Arrays.asList(
                new Point(0, 0),
                new Point(3, 3),
                new Point(7, 7),
                new Point(10, 10),
                new Point(13, 7),
                new Point(17, 3),
                new Point(20, 0)));
        PathRoute route = new PathRoute(Collections.singletonList(composite));

        // Act
        PathRoute actual = ObstacleAvoider.smooth(route);

        // Assert
        assertEquals(0.0, actual.get(0.0).getX(), EPS);
        assertEquals(0.0, actual.get(0.0).getY(), EPS);
        assertEquals(10.0, actual.get(0.5).getX(), EPS);
        assertEquals(10.0, actual.get(0.5).getY(), EPS);
        assertEquals(20.0, actual.get(1.0).getX(), EPS);
        assertEquals(0.0, actual.get(1.0).getY(), EPS);
    }

    @Test
    public void smooth_multipleSegments_returnsSameSegmentCount() {
        // Arrange
        PathRoute route = new PathRoute(Arrays.asList(
                new PathCurve(Arrays.asList(new Point(0, 0), new Point(10, 0))),
                new PathCurve(Arrays.asList(new Point(10, 0), new Point(20, 0)))));

        // Act
        PathRoute actual = ObstacleAvoider.smooth(route);

        // Assert
        assertEquals(2, actual.getSegments().size());
        assertEquals(20.0, actual.get(2.0).getX(), EPS);
    }

    private static PathRoute straightRoute(Point from, Point to) {
        return new PathRoute(Collections.singletonList(new PathCurve(Arrays.asList(from, to))));
    }
}
