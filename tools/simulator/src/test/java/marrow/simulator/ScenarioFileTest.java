package marrow.simulator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;
import org.junit.Test;

public class ScenarioFileTest {

    private static final double EPS = 1e-9;

    private static final String FACING_AWAY = "{"
            + "\"format\":\"marrow-scenario\",\"version\":1,"
            + "\"name\":\"Facing-away cluster\","
            + "\"start\":{\"x\":24,\"y\":70,\"headingDeg\":90},"
            + "\"targets\":["
            + "{\"x\":24,\"y\":62},"
            + "{\"x\":24.4,\"y\":61.7},"
            + "{\"x\":23.7,\"y\":61.9},"
            + "{\"x\":24.1,\"y\":62.3}],"
            + "\"obstacles\":[],"
            + "\"config\":{\"intakeWidth\":13,\"robotWidth\":17,\"robotHeight\":16,"
            + "\"clearance\":4,\"smoothing\":true,\"reorder\":true,\"maxTargets\":3}"
            + "}";

    @Test
    public void parse_facingAwayCluster_readsPoseTargetsAndConfig() throws Exception {
        ScenarioFile.Scenario scenario = ScenarioFile.parse(FACING_AWAY);

        assertEquals("Facing-away cluster", scenario.name);
        assertEquals(24.0, scenario.start.getX(), EPS);
        assertEquals(70.0, scenario.start.getY(), EPS);
        assertEquals(Math.PI / 2.0, scenario.start.getHeadingRad(), EPS);
        assertEquals(4, scenario.targets.size());
        assertTrue(scenario.targets.get(0).distanceTo(new Point(24, 62)) < EPS);
        assertTrue(!scenario.targets.get(0).hasHeading());
        assertTrue(scenario.obstacles.isEmpty());
        assertEquals(13.0, scenario.config.getIntakeWidth(), EPS);
        assertEquals(17.0, scenario.config.getRobotWidth(), EPS);
        assertEquals(16.0, scenario.config.getRobotHeight(), EPS);
        assertEquals(3, scenario.maxTargets);
    }

    @Test
    public void parse_lockedHeadingReversal_readsLockedTarget() throws Exception {
        ScenarioFile.Scenario scenario = ScenarioFile.parse("{"
                + "\"format\":\"marrow-scenario\",\"version\":1,"
                + "\"targets\":[{\"x\":30,\"y\":0,\"headingDeg\":180}]"
                + "}");

        assertEquals(1, scenario.targets.size());
        Point target = scenario.targets.get(0);
        assertTrue(target.hasHeading());
        assertEquals(Math.PI, Math.abs(target.getHeadingRad()), EPS);
    }

    @Test
    public void toJsonThenParse_roundTripsEverything() throws Exception {
        ScenarioFile.Scenario original = new ScenarioFile.Scenario();
        original.name = "Round trip";
        original.description = "Everything";
        original.start = new Point(7, 8, Math.toRadians(45));
        original.targets.add(new Point(10, 20, Math.toRadians(90)));
        original.targets.add(new Point(30, 40));
        original.obstacles.add(new CircleZone(new Point(50, 60), 5));
        original.obstacles.add(new PolygonZone(new Point[] {
            new Point(70, 80), new Point(90, 80), new Point(90, 100), new Point(70, 100)
        }));
        original.config.intakeWidth(12).robotWidth(17).robotHeight(16).clearance(2).smoothing(false);
        original.config.excludeBlockedTargets(false).turnCostWeight(3).bruteForceOrderLimit(4);
        original.reorder = false;
        original.maxTargets = 2;

        ScenarioFile.Scenario actual = ScenarioFile.parse(ScenarioFile.toJson(original));

        assertEquals(original.name, actual.name);
        assertEquals(original.description, actual.description);
        assertEquals(original.start.getX(), actual.start.getX(), EPS);
        assertEquals(original.start.getY(), actual.start.getY(), EPS);
        assertEquals(original.start.getHeadingRad(), actual.start.getHeadingRad(), EPS);
        assertEquals(2, actual.targets.size());
        assertEquals(Math.toRadians(90), actual.targets.get(0).getHeadingRad(), EPS);
        assertTrue(!actual.targets.get(1).hasHeading());
        assertEquals(2, actual.obstacles.size());
        assertTrue(actual.obstacles.get(0) instanceof CircleZone);
        assertTrue(actual.obstacles.get(1) instanceof PolygonZone);
        assertEquals(4, ((PolygonZone) actual.obstacles.get(1)).getCorners().length);
        assertEquals(12.0, actual.config.getIntakeWidth(), EPS);
        assertEquals(17.0, actual.config.getRobotWidth(), EPS);
        assertEquals(16.0, actual.config.getRobotHeight(), EPS);
        assertEquals(2.0, actual.config.getClearance(), EPS);
        assertTrue(!actual.config.isSmoothing());
        assertTrue(!actual.config.isExcludeBlockedTargets());
        assertEquals(3.0, actual.config.getTurnCostWeight(), EPS);
        assertEquals(4, actual.config.getBruteForceOrderLimit());
        assertTrue(!actual.reorder);
        assertEquals(2, actual.maxTargets);
    }

    @Test
    public void parse_unknownKeys_areIgnored() throws Exception {
        ScenarioFile.Scenario scenario = ScenarioFile.parse("{"
                + "\"format\":\"marrow-scenario\",\"version\":1,\"futureField\":true,"
                + "\"start\":{\"x\":1,\"y\":2},\"targets\":[],"
                + "\"config\":{\"futureConfig\":42}"
                + "}");

        assertEquals(1.0, scenario.start.getX(), EPS);
        assertEquals(2.0, scenario.start.getY(), EPS);
        assertEquals(0.0, scenario.start.getHeadingRad(), EPS);
    }

    @Test
    public void parse_polygonWithoutType_isReadAsPolygon() throws Exception {
        ScenarioFile.Scenario scenario = ScenarioFile.parse("{"
                + "\"format\":\"marrow-scenario\",\"version\":1,\"obstacles\":["
                + "{\"points\":[{\"x\":0,\"y\":0},{\"x\":5,\"y\":0},{\"x\":0,\"y\":5}]}]"
                + "}");

        assertEquals(1, scenario.obstacles.size());
        Zone obstacle = scenario.obstacles.get(0);
        assertTrue(obstacle instanceof PolygonZone);
    }
}
