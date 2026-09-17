package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Generates .pp files for visual inspection in the Pedro Pathing visualizer.
 * Files are written to core/pp-exports/
 */
public class VisualExportTest {

    @Before
    public void setup() {
        BezierPathGenerator.resetToDefaults();
    }

    @Test
    public void exportDefaultIntakePath() throws IOException {
        BezierResult result = BezierPathGenerator.builder()
                .start(new Pose(72, 72, 0))
                .addWaypoint(new Waypoint(100, 80))
                .addWaypoint(new Waypoint(128, 60))
                .generate();

        export(new Pose(72, 72, 0), result, "default_intake");
    }

    @Test
    public void exportNoIntakePath() throws IOException {
        BezierPathGenerator.setConfig(new BezierConfig().reach(0).width(0));
        BezierResult result = BezierPathGenerator.builder()
                .start(new Pose(72, 72, 0))
                .addWaypoint(new Waypoint(100, 80))
                .addWaypoint(new Waypoint(128, 60))
                .generate();

        export(new Pose(72, 72, 0), result, "no_intake");
    }

    @Test
    public void exportPureAvoidancePath() throws IOException {
        BezierResult result = BezierPathGenerator.builder()
                .start(new Pose(72, 72))
                .to(new Pose(120, 72))
                .addObstacle(new CircleZone(new Point(96, 72), 6))
                .generate();

        export(new Pose(72, 72, 0), result, "pure_avoidance");
    }

    @Test
    public void exportIntakeWithAvoidance() throws IOException {
        BezierResult result = BezierPathGenerator.builder()
                .start(new Pose(20, 20, 0))
                .addWaypoint(new Waypoint(70, 70))
                .addWaypoint(new Waypoint(120, 20))
                .addObstacle(new CircleZone(new Point(43, 43), 5))
                .addObstacle(new CircleZone(new Point(91, 45), 5))
                .generate();

        export(new Pose(20, 20, 0), result, "intake_with_avoidance");
    }

    private void export(Pose start, BezierResult result, String name) throws IOException {
        String json = PedroPathingExport.toPpJson(start, result);
        PedroPathingExport.writePpFile(json, Paths.get("pp-exports"), name);
        System.out.println("Exported " + name + ".pp to core/pp-exports/");
    }
}
