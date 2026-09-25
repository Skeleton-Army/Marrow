package com.skeletonarmy.marrow.weaver;

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
        Weaver.resetToDefaults();
    }

    @Test
    public void exportDefaultIntakePath() throws IOException {
        PathResult result = Weaver.builder()
                .start(new PathPose(72, 72, 0))
                .addTarget(new PathPose(100, 80))
                .addTarget(new PathPose(128, 60))
                .generate();

        export(new PathPose(72, 72, 0), result, "default_intake");
    }

    @Test
    public void exportNoIntakePath() throws IOException {
        Weaver.setConfig(new PathConfig().width(0));
        PathResult result = Weaver.builder()
                .start(new PathPose(72, 72, 0))
                .addTarget(new PathPose(100, 80))
                .addTarget(new PathPose(128, 60))
                .generate();

        export(new PathPose(72, 72, 0), result, "no_intake");
    }

    @Test
    public void exportPureAvoidancePath() throws IOException {
        PathResult result = Weaver.builder()
                .start(new PathPose(72, 72))
                .end(new PathPose(120, 72))
                .addObstacle(new CircleZone(new Point(96, 72), 6))
                .generate();

        export(new PathPose(72, 72, 0), result, "pure_avoidance");
    }

    @Test
    public void exportIntakeWithAvoidance() throws IOException {
        PathResult result = Weaver.builder()
                .start(new PathPose(20, 20, 0))
                .addTarget(new PathPose(70, 70))
                .addTarget(new PathPose(120, 20))
                .addObstacle(new CircleZone(new Point(43, 43), 5))
                .addObstacle(new CircleZone(new Point(91, 45), 5))
                .generate();

        export(new PathPose(20, 20, 0), result, "intake_with_avoidance");
    }

    @Test
    public void exportManyPoses() throws IOException {
        PathPose start = new PathPose(24, 24, 0);
        PathResult result = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(36, 48))
                .addTarget(new PathPose(60, 36))
                .addTarget(new PathPose(72, 72))
                .addTarget(new PathPose(96, 60))
                .addTarget(new PathPose(108, 96))
                .addTarget(new PathPose(132, 72))
                .generate();

        export(start, result, "many_poses");
    }

    @Test
    public void exportManyPosesWithObstacles() throws IOException {
        PathPose start = new PathPose(24, 24, 0);
        PathResult result = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(40, 50))
                .addTarget(new PathPose(70, 40))
                .addTarget(new PathPose(90, 80))
                .addTarget(new PathPose(120, 60))
                .addObstacle(new CircleZone(new Point(56, 64), 6))
                .addObstacle(new CircleZone(new Point(84, 36), 6))
                .addObstacle(new CircleZone(new Point(108, 84), 6))
                .generate();

        export(start, result, "many_poses_with_obstacles");
    }

    @Test
    public void exportPosesWithHeadings() throws IOException {
        PathPose start = new PathPose(24, 24, 0);
        PathResult result = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(60, 24, 0))
                .addTarget(new PathPose(84, 84, Math.PI / 2))
                .addTarget(new PathPose(120, 36, 0))
                .generate();

        export(start, result, "poses_with_headings");
    }

    @Test
    public void exportScatteredReorderedAndOrdered() throws IOException {
        PathPose start = new PathPose(24, 24, 0);

        PathResult reordered = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(120, 60))
                .addTarget(new PathPose(40, 96))
                .addTarget(new PathPose(96, 24))
                .addTarget(new PathPose(60, 48))
                .generate();
        export(start, reordered, "scatter_reordered");

        PathResult ordered = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(120, 60))
                .addTarget(new PathPose(40, 96))
                .addTarget(new PathPose(96, 24))
                .addTarget(new PathPose(60, 48))
                .ordered()
                .generate();
        export(start, ordered, "scatter_ordered");
    }

    @Test
    public void exportWideIntake() throws IOException {
        Weaver.setConfig(new PathConfig().width(18));
        PathPose start = new PathPose(24, 24, 0);
        PathResult result = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(60, 30))
                .addTarget(new PathPose(96, 24))
                .addTarget(new PathPose(132, 30))
                .generate();

        export(start, result, "wide_intake");
    }

    @Test
    public void exportSharpZigzag() throws IOException {
        Weaver.setConfig(new PathConfig().width(0));
        PathPose start = new PathPose(24, 72, 0);
        PathResult result = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(48, 24))
                .addTarget(new PathPose(72, 96))
                .addTarget(new PathPose(96, 24))
                .addTarget(new PathPose(120, 96))
                .generate();

        export(start, result, "sharp_zigzag");
    }

    @Test
    public void exportUnevenSpacing() throws IOException {
        Weaver.setConfig(new PathConfig().width(0));
        PathPose start = new PathPose(24, 24, 0);
        PathResult result = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(28, 28))
                .addTarget(new PathPose(120, 40))
                .addTarget(new PathPose(124, 108))
                .addTarget(new PathPose(60, 84))
                .generate();

        export(start, result, "uneven_spacing");
    }

    @Test
    public void exportSinglePose() throws IOException {
        PathPose start = new PathPose(24, 24, 0);
        PathResult result = Weaver.builder()
                .start(start)
                .addTarget(new PathPose(100, 84, Math.PI / 2))
                .generate();

        export(start, result, "single_pose");
    }

    private void export(PathPose start, PathResult result, String name) throws IOException {
        String json = PedroPathingExport.toPpJson(start, result);
        PedroPathingExport.writePpFile(json, Paths.get("pp-exports"), name);
        System.out.println("Exported " + name + ".pp to core/pp-exports/");
    }
}
