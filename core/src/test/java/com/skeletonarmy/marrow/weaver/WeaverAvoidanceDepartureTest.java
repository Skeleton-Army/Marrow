package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the direction an avoidance route sets off in.
 */
public class WeaverAvoidanceDepartureTest {

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
    public void generate_destinationBehindBar_leavesAwayFromTheBar() {
        // Arrange: the straight line from start to end crosses the bar, so the route
        // has to go round one end of it. It must not set off along that line.
        Weaver.setConfig(new PathConfig().robotWidth(18).robotHeight(18).clearance(2));
        WeaverBuilder builder = Weaver.builder()
                .start(new Point(-40, 0, 0))
                .end(new Point(0, 0))
                .obstacles(bar());

        // Act
        PathResult actual = builder.generate();

        // Assert
        assertTrue(WeaverGenerator.isPathClear(actual.getPath(), bar(), Weaver.getConfig()));
        assertTrue(Math.abs(actual.getPath().getHeading(0.0)) > Math.PI / 4);
    }
}
