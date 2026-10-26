package marrow.simulator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skeletonarmy.marrow.weaver.PathConfig;
import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads and writes the JSON text used by the simulator's scenario clipboard.
 * <p>
 * A scenario stores the start pose, targets, obstacles and the subset of
 * {@link PathConfig} the simulator exposes. Unknown JSON keys are ignored so a
 * newer scenario can still be opened by an older simulator.
 */
final class ScenarioFile {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FORMAT = "marrow-scenario";
    private static final int VERSION = 1;

    private ScenarioFile() {}

    /** A complete simulator setup. */
    static final class Scenario {
        String name = "";
        String description = "";
        Point start = new Point(24, 24, 0);
        final List<Point> targets = new ArrayList<>();
        final List<Zone> obstacles = new ArrayList<>();
        final PathConfig config = new PathConfig();
        boolean reorder = true;
        int maxTargets = 0;
    }

    /**
     * Parses a scenario from JSON text.
     *
     * @param json scenario JSON
     * @return the parsed scenario
     * @throws IOException if the text is not a valid scenario
     */
    static Scenario parse(String json) throws IOException {
        if (json == null || json.trim().isEmpty()) {
            throw new IOException("Scenario text is empty");
        }

        JsonNode root = MAPPER.readTree(json);

        if (root == null || !root.isObject()) {
            throw new IOException("Scenario text must contain a JSON object");
        }

        JsonNode format = root.get("format");
        if (format != null && !format.isNull() && !FORMAT.equals(format.asText())) {
            throw new IOException("Unsupported scenario format: " + format.asText());
        }

        int version = integer(root, "version", VERSION, "scenario");
        if (version > VERSION) {
            throw new IOException("Scenario version " + version + " is newer than supported version " + VERSION);
        }

        Scenario scenario = new Scenario();
        scenario.name = text(root, "name", "");
        scenario.description = text(root, "description", "");

        JsonNode start = root.get("start");
        if (start != null && !start.isNull()) {
            requireObject(start, "start");
            double x = requiredNumber(start, "x", "start");
            double y = requiredNumber(start, "y", "start");
            scenario.start = new Point(x, y, heading(start, 0.0, "start"));
        }

        JsonNode targets = root.get("targets");
        if (targets != null && !targets.isNull()) {
            if (!targets.isArray()) {
                throw new IOException("targets must be an array");
            }

            for (int i = 0; i < targets.size(); i++) {
                scenario.targets.add(readTarget(targets.get(i), i));
            }
        }

        JsonNode obstacles = root.get("obstacles");
        if (obstacles != null && !obstacles.isNull()) {
            if (!obstacles.isArray()) {
                throw new IOException("obstacles must be an array");
            }

            for (int i = 0; i < obstacles.size(); i++) {
                scenario.obstacles.add(readObstacle(obstacles.get(i), i));
            }
        }

        JsonNode config = root.get("config");
        if (config != null && !config.isNull()) {
            readConfig(config, scenario);
        }

        return scenario;
    }

    /**
     * Serializes {@code scenario} to pretty-printed JSON.
     *
     * @param scenario scenario to write
     * @return the scenario as JSON text
     * @throws IOException if the scenario cannot be serialized
     */
    static String toJson(Scenario scenario) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("format", FORMAT);
        root.put("version", VERSION);

        if (!scenario.name.isEmpty()) {
            root.put("name", scenario.name);
        }

        if (!scenario.description.isEmpty()) {
            root.put("description", scenario.description);
        }

        ObjectNode start = root.putObject("start");
        start.put("x", scenario.start.getX());
        start.put("y", scenario.start.getY());
        start.put("headingDeg", Math.toDegrees(scenario.start.hasHeading() ? scenario.start.getHeadingRad() : 0.0));

        ArrayNode targets = root.putArray("targets");
        for (Point target : scenario.targets) {
            ObjectNode node = targets.addObject();
            node.put("x", target.getX());
            node.put("y", target.getY());

            if (target.hasHeading()) {
                node.put("headingDeg", Math.toDegrees(target.getHeadingRad()));
            }
        }

        ArrayNode obstacles = root.putArray("obstacles");
        for (Zone obstacle : scenario.obstacles) {
            writeObstacle(obstacles, obstacle);
        }

        ObjectNode config = root.putObject("config");
        config.put("intakeWidth", scenario.config.getIntakeWidth());
        config.put("robotWidth", scenario.config.getRobotWidth());
        config.put("robotHeight", scenario.config.getRobotHeight());
        config.put("clearance", scenario.config.getClearance());
        config.put("excludeBlockedTargets", scenario.config.isExcludeBlockedTargets());
        config.put("smoothing", scenario.config.isSmoothing());
        config.put("turnCostWeight", scenario.config.getTurnCostWeight());
        config.put("bruteForceOrderLimit", scenario.config.getBruteForceOrderLimit());
        config.put("reorder", scenario.reorder);
        config.put("maxTargets", scenario.maxTargets);

        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private static Point readTarget(JsonNode node, int index) throws IOException {
        String context = "targets[" + index + "]";
        requireObject(node, context);

        double x = requiredNumber(node, "x", context);
        double y = requiredNumber(node, "y", context);
        boolean hasHeading = node.hasNonNull("headingDeg") || node.hasNonNull("headingRad");
        boolean locked = bool(node, "locked", hasHeading, context);

        if (locked && !hasHeading) {
            throw new IOException(context + " is locked but has no headingDeg or headingRad");
        }

        return new Point(x, y, locked ? heading(node, 0.0, context) : Double.NaN);
    }

    private static Zone readObstacle(JsonNode node, int index) throws IOException {
        String context = "obstacles[" + index + "]";
        requireObject(node, context);

        String defaultType = node.has("points") || node.has("corners") ? "polygon" : "circle";
        String type = text(node, "type", defaultType).toLowerCase(Locale.ROOT);

        if ("circle".equals(type)) {
            double x = requiredNumber(node, "x", context);
            double y = requiredNumber(node, "y", context);
            double radius = requiredNumber(node, "radius", context);

            if (radius <= 0) {
                throw new IOException(context + ".radius must be greater than zero");
            }

            return new CircleZone(new Point(x, y), radius);
        }

        if ("polygon".equals(type)) {
            JsonNode points = node.has("points") ? node.get("points") : node.get("corners");

            if (points == null || !points.isArray() || points.size() < 3) {
                throw new IOException(context + " polygon needs at least three points");
            }

            Point[] corners = new Point[points.size()];

            for (int i = 0; i < points.size(); i++) {
                JsonNode point = points.get(i);
                String pointContext = context + ".points[" + i + "]";
                requireObject(point, pointContext);
                corners[i] = new Point(
                        requiredNumber(point, "x", pointContext), requiredNumber(point, "y", pointContext));
            }

            return new PolygonZone(corners);
        }

        throw new IOException(context + " has unsupported type: " + type);
    }

    private static void readConfig(JsonNode node, Scenario scenario) throws IOException {
        requireObject(node, "config");
        PathConfig config = scenario.config;

        config.intakeWidth(nonNegative(node, "intakeWidth", config.getIntakeWidth(), "config"));
        config.robotWidth(nonNegative(node, "robotWidth", config.getRobotWidth(), "config"));
        config.robotHeight(nonNegative(node, "robotHeight", config.getRobotHeight(), "config"));
        config.clearance(nonNegative(node, "clearance", config.getClearance(), "config"));
        config.excludeBlockedTargets(bool(node, "excludeBlockedTargets", config.isExcludeBlockedTargets(), "config"));
        config.smoothing(bool(node, "smoothing", config.isSmoothing(), "config"));
        config.turnCostWeight(nonNegative(node, "turnCostWeight", config.getTurnCostWeight(), "config"));
        config.bruteForceOrderLimit(
                Math.max(1, integer(node, "bruteForceOrderLimit", config.getBruteForceOrderLimit(), "config")));

        scenario.reorder = bool(node, "reorder", scenario.reorder, "config");
        scenario.maxTargets = Math.max(0, integer(node, "maxTargets", scenario.maxTargets, "config"));
    }

    private static void writeObstacle(ArrayNode obstacles, Zone obstacle) throws IOException {
        if (obstacle instanceof CircleZone) {
            CircleZone circle = (CircleZone) obstacle;
            ObjectNode node = obstacles.addObject();
            node.put("type", "circle");
            node.put("x", circle.getPosition().getX());
            node.put("y", circle.getPosition().getY());
            node.put("radius", circle.getRadius());
            return;
        }

        if (obstacle instanceof PolygonZone) {
            ObjectNode node = obstacles.addObject();
            node.put("type", "polygon");
            ArrayNode points = node.putArray("points");

            for (Point corner : ((PolygonZone) obstacle).getCorners()) {
                ObjectNode point = points.addObject();
                point.put("x", corner.getX());
                point.put("y", corner.getY());
            }

            return;
        }

        throw new IOException("Cannot export obstacle type: " + obstacle.getClass().getName());
    }

    private static void requireObject(JsonNode node, String context) throws IOException {
        if (node == null || !node.isObject()) {
            throw new IOException(context + " must be a JSON object");
        }
    }

    private static double requiredNumber(JsonNode node, String field, String context) throws IOException {
        JsonNode value = node.get(field);

        if (value == null || !value.isNumber() || !Double.isFinite(value.asDouble())) {
            throw new IOException(context + "." + field + " must be a finite number");
        }

        return value.asDouble();
    }

    private static double nonNegative(JsonNode node, String field, double fallback, String context) throws IOException {
        double value = number(node, field, fallback, context);

        if (value < 0) {
            throw new IOException(context + "." + field + " must not be negative");
        }

        return value;
    }

    private static double number(JsonNode node, String field, double fallback, String context) throws IOException {
        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return fallback;
        }

        if (!value.isNumber() || !Double.isFinite(value.asDouble())) {
            throw new IOException(context + "." + field + " must be a finite number");
        }

        return value.asDouble();
    }

    private static int integer(JsonNode node, String field, int fallback, String context) throws IOException {
        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return fallback;
        }

        if (!value.isIntegralNumber()) {
            throw new IOException(context + "." + field + " must be an integer");
        }

        return value.asInt();
    }

    private static boolean bool(JsonNode node, String field, boolean fallback, String context) throws IOException {
        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return fallback;
        }

        if (!value.isBoolean()) {
            throw new IOException(context + "." + field + " must be true or false");
        }

        return value.asBoolean();
    }

    private static String text(JsonNode node, String field, String fallback) throws IOException {
        JsonNode value = node.get(field);

        if (value == null || value.isNull()) {
            return fallback;
        }

        if (!value.isTextual()) {
            throw new IOException("scenario." + field + " must be a string");
        }

        return value.asText();
    }

    private static double heading(JsonNode node, double fallback, String context) throws IOException {
        if (node.hasNonNull("headingDeg")) {
            return Math.toRadians(number(node, "headingDeg", fallback, context));
        }

        if (node.hasNonNull("headingRad")) {
            return number(node, "headingRad", fallback, context);
        }

        return fallback;
    }
}
