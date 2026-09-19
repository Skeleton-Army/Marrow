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
                .addTarget(new Pose(100, 80))
                .addTarget(new Pose(128, 60))
                .generate();

        export(new Pose(72, 72, 0), result, "default_intake");
    }

    @Test
    public void exportNoIntakePath() throws IOException {
        BezierPathGenerator.setConfig(new BezierConfig().reach(0).width(0));
        BezierResult result = BezierPathGenerator.builder()
                .start(new Pose(72, 72, 0))
                .addTarget(new Pose(100, 80))
                .addTarget(new Pose(128, 60))
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
                .addTarget(new Pose(70, 70))
                .addTarget(new Pose(120, 20))
                .addObstacle(new CircleZone(new Point(43, 43), 5))
                .addObstacle(new CircleZone(new Point(91, 45), 5))
                .generate();

        export(new Pose(20, 20, 0), result, "intake_with_avoidance");
    }

    @Test
    public void exportManyPoses() throws IOException {
        Pose start = new Pose(24, 24, 0);
        BezierResult result = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(36, 48))
                .addTarget(new Pose(60, 36))
                .addTarget(new Pose(72, 72))
                .addTarget(new Pose(96, 60))
                .addTarget(new Pose(108, 96))
                .addTarget(new Pose(132, 72))
                .generate();

        export(start, result, "many_poses");
    }

    @Test
    public void exportManyPosesWithObstacles() throws IOException {
        Pose start = new Pose(24, 24, 0);
        BezierResult result = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(40, 50))
                .addTarget(new Pose(70, 40))
                .addTarget(new Pose(90, 80))
                .addTarget(new Pose(120, 60))
                .addObstacle(new CircleZone(new Point(56, 64), 6))
                .addObstacle(new CircleZone(new Point(84, 36), 6))
                .addObstacle(new CircleZone(new Point(108, 84), 6))
                .generate();

        export(start, result, "many_poses_with_obstacles");
    }

    @Test
    public void exportPosesWithHeadings() throws IOException {
        Pose start = new Pose(24, 24, 0);
        BezierResult result = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(60, 24, 0))
                .addTarget(new Pose(84, 84, Math.PI / 2))
                .addTarget(new Pose(120, 36, 0))
                .generate();

        export(start, result, "poses_with_headings");
    }

    @Test
    public void exportScatteredReorderedAndOrdered() throws IOException {
        Pose start = new Pose(24, 24, 0);

        BezierResult reordered = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(120, 60))
                .addTarget(new Pose(40, 96))
                .addTarget(new Pose(96, 24))
                .addTarget(new Pose(60, 48))
                .generate();
        export(start, reordered, "scatter_reordered");

        BezierResult ordered = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(120, 60))
                .addTarget(new Pose(40, 96))
                .addTarget(new Pose(96, 24))
                .addTarget(new Pose(60, 48))
                .ordered()
                .generate();
        export(start, ordered, "scatter_ordered");
    }

    @Test
    public void exportWideIntake() throws IOException {
        BezierPathGenerator.setConfig(new BezierConfig().width(18).reach(0));
        Pose start = new Pose(24, 24, 0);
        BezierResult result = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(60, 30))
                .addTarget(new Pose(96, 24))
                .addTarget(new Pose(132, 30))
                .generate();

        export(start, result, "wide_intake");
    }

    @Test
    public void exportSharpZigzag() throws IOException {
        BezierPathGenerator.setConfig(new BezierConfig().reach(0).width(0));
        Pose start = new Pose(24, 72, 0);
        BezierResult result = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(48, 24))
                .addTarget(new Pose(72, 96))
                .addTarget(new Pose(96, 24))
                .addTarget(new Pose(120, 96))
                .generate();

        export(start, result, "sharp_zigzag");
    }

    @Test
    public void exportUnevenSpacing() throws IOException {
        BezierPathGenerator.setConfig(new BezierConfig().reach(0).width(0));
        Pose start = new Pose(24, 24, 0);
        BezierResult result = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(28, 28))
                .addTarget(new Pose(120, 40))
                .addTarget(new Pose(124, 108))
                .addTarget(new Pose(60, 84))
                .generate();

        export(start, result, "uneven_spacing");
    }

    @Test
    public void exportSinglePose() throws IOException {
        Pose start = new Pose(24, 24, 0);
        BezierResult result = BezierPathGenerator.builder()
                .start(start)
                .addTarget(new Pose(100, 84, Math.PI / 2))
                .generate();

        export(start, result, "single_pose");
    }

    private void export(Pose start, BezierResult result, String name) throws IOException {
        String json = PedroPathingExport.toPpJson(start, result);
        PedroPathingExport.writePpFile(json, Paths.get("pp-exports"), name);
        System.out.println("Exported " + name + ".pp to core/pp-exports/");
    }
}
