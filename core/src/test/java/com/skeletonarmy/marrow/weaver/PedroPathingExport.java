package com.skeletonarmy.marrow.weaver;

import com.skeletonarmy.marrow.zones.Point;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PedroPathingExport {

    private PedroPathingExport() {
    }

    public static String toPpJson(PathPose start, PathRoute path) {
        return toPpJson(start, path, null, null, null);
    }

    public static String toPpJson(PathPose start, PathResult result) {
        return toPpJson(start, result.getPath(), null, null, result.getSegmentEndHeadingsRad());
    }

    public static String toPpJson(PathPose start, PathRoute path, List<String> names, List<String> modes, List<Double> endHeadings) {
        List<PathCurve> segments = new ArrayList<>();
        for (PathCurve curve : path.getSegments()) segments.addAll(curve.toCubicSegments());

        StringBuilder json = new StringBuilder("{\n");
        appendPointBlock(json, "  ", "startPoint", new Point(start.getX(), start.getY()), "startDeg", Math.toDegrees(start.getHeadingRad()), null);
        json.append(",\n  \"lines\": [\n");

        Double finalHeading = (endHeadings != null && !endHeadings.isEmpty()) ? endHeadings.get(endHeadings.size() - 1) : null;

        for (int i = 0; i < segments.size(); i++) {
            PathCurve seg = segments.get(i);
            List<Point> cps = seg.getControlPoints();
            double endRad = (finalHeading != null && i == segments.size() - 1) ? finalHeading : seg.getHeading(1.0);

            json.append("    {\n      \"name\": \"").append(escape(nameAt(names, i))).append("\",\n");
            appendPointBlock(json, "      ", "endPoint", cps.get(cps.size() - 1), "degrees", Math.toDegrees(endRad), modeAt(modes, i));
            json.append(",\n      \"controlPoints\": [\n");

            for (int c = 1; c < cps.size() - 1; c++) {
                json.append("        { \"x\": ").append(num(cps.get(c).getX())).append(", \"y\": ").append(num(cps.get(c).getY())).append(" }")
                        .append(c < cps.size() - 2 ? ",\n" : "\n");
            }
            json.append("      ]\n    }").append(i < segments.size() - 1 ? ",\n" : "\n");
        }
        return json.append("  ]\n}\n").toString();
    }

    public static String toPpJson(PathCurve curve, String name) {
        Point start = curve.get(0);
        return toPpJson(new PathPose(start.getX(), start.getY(), curve.getHeading(0)), new PathRoute(Collections.singletonList(curve)),
                Collections.singletonList(name != null ? name : "Segment_1"), null, null);
    }

    public static Path writePpFile(String json, Path dir, String name) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve(name.endsWith(".pp") ? name : name + ".pp");
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static void appendPointBlock(StringBuilder json, String indent, String key, Point p, String angleKey, double angle, String mode) {
        json.append(indent).append("\"").append(key).append("\": {\n")
                .append(indent).append("  \"x\": ").append(num(p.getX())).append(",\n")
                .append(indent).append("  \"y\": ").append(num(p.getY())).append(",\n")
                .append(indent).append("  \"").append(angleKey).append("\": ").append(num(angle));

        if (mode != null) {
            json.append(",\n").append(indent).append("  \"heading\": \"").append(escape(mode)).append("\"");
        }
        json.append("\n").append(indent).append("}");
    }

    private static String nameAt(List<String> names, int i) {
        return (names != null && i < names.size() && names.get(i) != null) ? names.get(i) : "Segment_" + (i + 1);
    }

    private static String modeAt(List<String> modes, int i) {
        return (modes != null && i < modes.size() && modes.get(i) != null) ? modes.get(i) : "tangential";
    }

    private static String num(double v) {
        return (Double.isFinite(v) && v == Math.rint(v)) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
