package marrow.simulator;

import com.skeletonarmy.marrow.weaver.HeadingPolicy;
import com.skeletonarmy.marrow.weaver.PathConfig;
import com.skeletonarmy.marrow.weaver.PathResult;
import com.skeletonarmy.marrow.weaver.PathRoute;
import com.skeletonarmy.marrow.weaver.Weaver;
import com.skeletonarmy.marrow.weaver.WeaverBuilder;
import com.skeletonarmy.marrow.weaver.WeaverGenerator;
import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.PolygonZone;
import com.skeletonarmy.marrow.zones.Zone;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Simulator extends JPanel {

    private static final double FIELD = 144.0;
    private static final PathConfig DEFAULT_CONFIG = new PathConfig();

    public enum Mode { TARGET, OBSTACLE, START }
    private enum DragMode { MOVE, RESIZE, ROTATE }

    private enum ObstacleShape {
        CIRCLE, POLYGON;

        ObstacleShape next() {
            return this == CIRCLE ? POLYGON : CIRCLE;
        }
    }

    private final List<Point> targets = new ArrayList<>();
    private final List<Zone> obstacles = new ArrayList<>();

    private Point start = new Point(24, 24);
    private double startHeading = 0.0;
    private double obstacleRadius = 6.0;
    private double width = DEFAULT_CONFIG.getIntakeWidth();
    private double robotWidth = DEFAULT_CONFIG.getRobotWidth();
    private double robotHeight = DEFAULT_CONFIG.getRobotHeight();
    private double clearance = DEFAULT_CONFIG.getClearance();
    private boolean excludeBlockedTargets = DEFAULT_CONFIG.isExcludeBlockedTargets();
    private double turnCostWeight = DEFAULT_CONFIG.getTurnCostWeight();
    private int bruteForceOrderLimit = DEFAULT_CONFIG.getBruteForceOrderLimit();
    private ObstacleShape obstacleShape = ObstacleShape.CIRCLE;
    private int polygonSides = 4;
    private boolean reorder = true;
    private boolean smoothing = true;
    private int maxTargets = 0;
    private Mode mode = Mode.TARGET;

    private String scenarioName = "";
    private String scenarioDescription = "";
    private String infoMessage = "";

    private static final double HEADING_HANDLE_PX = 34;

    private double defaultTargetHeading = Double.NaN;
    private int selectedTarget = -1;
    private boolean draggingHeading = false;

    private int dragIndex = -1;
    private DragMode dragMode = DragMode.MOVE;
    private Point moveStart;
    private Point grabField;
    private Point[] resizeOriginal;
    private Point resizeCenter;
    private double resizeAnchorDist = 1.0;
    private Point[] rotateOriginal;
    private Point rotateCenter;
    private double rotateStartAngle = 0.0;

    private PathRoute path;
    private List<Point> visitedTargets = new ArrayList<>();
    private double[] cumArc;
    private Point[] samplePos;
    private double[] sampleHeading;
    private double totalLength;

    private double robotArc;
    private Point robotPos = new Point(24, 24);
    private double robotHeading = 0.0;
    private final double robotSpeed = 40.0;

    private long lastTime = System.nanoTime();
    private String statusMessage = "";

    private SimulatorControls controls;

    private double scale = 1.0;
    private double ox, oy;

    public Simulator() {
        setPreferredSize(new Dimension(820, 820));
        setFocusable(true);
        setLayout(null);

        targets.add(new Point(72, 96));
        targets.add(new Point(120, 48));
        obstacles.add(new CircleZone(new Point(72, 60), 6));

        rebuildPath();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (SwingUtilities.isRightMouseButton(e)) {
                    removeNearest(e);
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    updateTransform();
                    Point f = new Point(fx(e.getX()), fy(e.getY()));

                    if (headingHandleAt(f)) {
                        beginHeadingDrag(f);
                        return;
                    }
                    int targetIndex = targetAt(f);
                    if (targetIndex >= 0) {
                        selectedTarget = targetIndex;
                        refreshControls();
                        repaint();
                        return;
                    }

                    int corner = obstacleCorner(f);
                    if (corner >= 0) {
                        beginRotate(corner, f);
                        return;
                    }
                    int near = obstacleNearBoundary(f);
                    if (near >= 0) {
                        beginResize(near, f);
                        return;
                    }
                    int inside = obstacleInside(f);
                    if (inside >= 0) {
                        beginMove(inside, f);
                        return;
                    }

                    switch (mode) {
                        case TARGET:
                            targets.add(new Point(f.getX(), f.getY(), defaultTargetHeading));
                            selectedTarget = targets.size() - 1;
                            break;
                        case OBSTACLE:
                            obstacles.add(newObstacle(f));
                            break;
                        case START:
                            start = f;
                            break;
                    }
                    rebuildPath();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingHeading) {
                    draggingHeading = false;
                    rebuildPath();
                    return;
                }
                if (dragIndex >= 0) {
                    dragIndex = -1;
                    rebuildPath();
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                updateTransform();
                Point f = new Point(fx(e.getX()), fy(e.getY()));
                if (draggingHeading) {
                    updateHeadingDrag(f);
                    return;
                }
                if (dragIndex < 0) return;
                switch (dragMode) {
                    case RESIZE: resizeDragged(f); break;
                    case ROTATE: rotateDragged(f); break;
                    case MOVE:
                    default: moveDragged(f); break;
                }
                repaint();
            }
        });

        Timer timer = new Timer(16, e -> tick());
        timer.start();
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastTime) / 1e9;
        lastTime = now;
        if (dt > 0.1) dt = 0.1;

        if (path != null && totalLength > 1e-6) {
            robotArc += robotSpeed * dt;
            if (robotArc >= totalLength) robotArc %= totalLength;
            updateRobot();
        }
        repaint();
    }

    private void rebuildPath() {
        path = null;
        totalLength = 0;
        cumArc = null;
        samplePos = null;
        sampleHeading = null;
        visitedTargets = new ArrayList<>();
        statusMessage = "";

        if (targets.isEmpty()) {
            refreshControls();
            repaint();
            return;
        }

        try {
            Weaver.setConfig(new PathConfig()
                    .intakeWidth(width)
                    .robotWidth(robotWidth)
                    .robotHeight(robotHeight)
                    .clearance(clearance)
                    .excludeBlockedTargets(excludeBlockedTargets)
                    .smoothing(smoothing)
                    .turnCostWeight(turnCostWeight)
                    .bruteForceOrderLimit(bruteForceOrderLimit));

            WeaverBuilder b = Weaver.builder()
                    .start(new Point(start.getX(), start.getY(), startHeading))
                    .maxTargets(maxTargets);
            for (Point t : targets) {
                b.addTarget(
                        new Point(t.getX(), t.getY(), t.getHeadingRad()),
                        t.hasHeading() ? HeadingPolicy.LOCKED : HeadingPolicy.FREE);
            }
            for (Zone o : obstacles) {
                b.addObstacle(o);
            }
            if (!reorder) {
                b.ordered();
            }

            PathResult result = b.generate();
            path = result.getPath();
            visitedTargets = result.getVisitedTargets();
            buildSamples();
            robotArc = 0;
            updateRobot();
        } catch (Exception ex) {
            path = null;
            statusMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        refreshControls();
        repaint();
    }

    private void buildSamples() {
        int n = 400;
        int segCount = path.getSegmentCount();
        cumArc = new double[n];
        samplePos = new Point[n];
        sampleHeading = new double[n];
        for (int i = 0; i < n; i++) {
            double g = (double) i / (n - 1) * segCount;
            samplePos[i] = path.get(g);
            sampleHeading[i] = path.getHeading(g);
            if (i > 0) {
                cumArc[i] = cumArc[i - 1] + samplePos[i].distanceTo(samplePos[i - 1]);
            }
        }
        totalLength = cumArc[n - 1];
    }

    private void updateRobot() {
        if (samplePos == null || cumArc == null || totalLength <= 1e-6) {
            return;
        }
        double s = Math.max(0, Math.min(robotArc, totalLength));
        int lo = 0, hi = cumArc.length - 1;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cumArc[mid] < s) lo = mid + 1; else hi = mid;
        }
        int i = Math.max(0, lo - 1);
        int j = Math.min(lo, cumArc.length - 1);
        double span = cumArc[j] - cumArc[i];
        double f = span > 1e-9 ? (s - cumArc[i]) / span : 0;

        double x = samplePos[i].getX() + (samplePos[j].getX() - samplePos[i].getX()) * f;
        double y = samplePos[i].getY() + (samplePos[j].getY() - samplePos[i].getY()) * f;
        robotPos = new Point(x, y);
        robotHeading = angleLerp(sampleHeading[i], sampleHeading[j], f);
    }

    private static double angleLerp(double a, double b, double f) {
        double d = b - a;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return a + d * f;
    }

    private Zone newObstacle(Point p) {
        if (obstacleShape == ObstacleShape.CIRCLE) {
            return new CircleZone(p, obstacleRadius);
        }
        return regularPolygon(p, obstacleRadius, polygonSides);
    }

    private static PolygonZone regularPolygon(Point center, double radius, int sides) {
        Point[] points = new Point[sides];
        for (int i = 0; i < sides; i++) {
            double angle = -Math.PI / 2.0 + (2.0 * Math.PI * i) / sides;
            points[i] = new Point(
                    center.getX() + radius * Math.cos(angle),
                    center.getY() + radius * Math.sin(angle));
        }
        return new PolygonZone(points);
    }

    private double handleThresholdField() {
        return Math.max(2.0, 10.0 / scale);
    }

    private int obstacleNearBoundary(Point f) {
        double thresh = handleThresholdField();
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Zone o = obstacles.get(i);
            if (o instanceof CircleZone) {
                CircleZone c = (CircleZone) o;
                if (Math.abs(c.getPosition().distanceTo(f) - c.getRadius()) <= thresh) return i;
            } else if (o instanceof PolygonZone) {
                if (((PolygonZone) o).distanceToBoundary(f) <= thresh) return i;
            }
        }
        return -1;
    }

    private int obstacleInside(Point f) {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            if (obstacles.get(i).contains(f)) return i;
        }
        return -1;
    }

    private int obstacleCorner(Point f) {
        double thresh = handleThresholdField();
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Zone o = obstacles.get(i);
            if (o instanceof PolygonZone) {
                for (Point corner : ((PolygonZone) o).getCorners()) {
                    if (corner.distanceTo(f) <= thresh) return i;
                }
            }
        }
        return -1;
    }

    private void beginResize(int idx, Point f) {
        dragIndex = idx;
        dragMode = DragMode.RESIZE;
        Zone o = obstacles.get(idx);
        resizeAnchorDist = Math.max(1e-6, o.getPosition().distanceTo(f));
        resizeCenter = o.getPosition();
        if (o instanceof PolygonZone) {
            resizeOriginal = ((PolygonZone) o).getCorners();
        } else {
            resizeOriginal = null;
        }
    }

    private void beginMove(int idx, Point f) {
        dragIndex = idx;
        dragMode = DragMode.MOVE;
        moveStart = obstacles.get(idx).getPosition();
        grabField = f;
    }

    private void beginRotate(int idx, Point f) {
        dragIndex = idx;
        dragMode = DragMode.ROTATE;
        PolygonZone p = (PolygonZone) obstacles.get(idx);
        rotateOriginal = p.getCorners();
        rotateCenter = p.getPosition();
        rotateStartAngle = Math.atan2(f.getY() - rotateCenter.getY(), f.getX() - rotateCenter.getX());
    }

    private void rotateDragged(Point f) {
        double angle = Math.atan2(f.getY() - rotateCenter.getY(), f.getX() - rotateCenter.getX());
        double delta = normalizeAngle(angle - rotateStartAngle);
        Point[] rotated = new Point[rotateOriginal.length];
        for (int i = 0; i < rotateOriginal.length; i++) {
            rotated[i] = rotatePoint(rotateOriginal[i], rotateCenter, delta);
        }
        obstacles.set(dragIndex, new PolygonZone(rotated));
    }

    private static double normalizeAngle(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    private static Point rotatePoint(Point p, Point center, double angle) {
        double dx = p.getX() - center.getX();
        double dy = p.getY() - center.getY();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Point(center.getX() + dx * cos - dy * sin,
                center.getY() + dx * sin + dy * cos);
    }

    private void resizeDragged(Point f) {
        Zone o = obstacles.get(dragIndex);
        if (o instanceof CircleZone) {
            double r = Math.max(1.0, resizeCenter.distanceTo(f));
            obstacles.set(dragIndex, new CircleZone(resizeCenter, r));
        } else if (o instanceof PolygonZone) {
            double dist = resizeCenter.distanceTo(f);
            double factor = Math.max(0.05, dist / resizeAnchorDist);
            Point[] scaled = new Point[resizeOriginal.length];
            for (int i = 0; i < resizeOriginal.length; i++) {
                scaled[i] = new Point(
                        resizeCenter.getX() + (resizeOriginal[i].getX() - resizeCenter.getX()) * factor,
                        resizeCenter.getY() + (resizeOriginal[i].getY() - resizeCenter.getY()) * factor);
            }
            obstacles.set(dragIndex, new PolygonZone(scaled));
        }
    }

    private void moveDragged(Point f) {
        Zone o = obstacles.get(dragIndex);
        o.setPosition(moveStart.getX() + (f.getX() - grabField.getX()),
                moveStart.getY() + (f.getY() - grabField.getY()));
    }

    private void removeNearest(MouseEvent e) {
        updateTransform();
        double best = 18 * 18;
        int targetIdx = -1;
        int obstacleIdx = -1;

        for (int i = 0; i < targets.size(); i++) {
            double d = distPx(e, targets.get(i));
            if (d < best) {
                best = d;
                targetIdx = i;
                obstacleIdx = -1;
            }
        }
        for (int i = 0; i < obstacles.size(); i++) {
            double d = distPx(e, obstacles.get(i).getPosition());
            if (d < best) {
                best = d;
                targetIdx = -1;
                obstacleIdx = i;
            }
        }

        if (targetIdx >= 0) {
            targets.remove(targetIdx);
            if (selectedTarget == targetIdx) selectedTarget = -1;
            else if (selectedTarget > targetIdx) selectedTarget--;
        } else if (obstacleIdx >= 0) {
            obstacles.remove(obstacleIdx);
        } else {
            return;
        }

        rebuildPath();
    }

    private int targetAt(Point f) {
        double threshold = Math.max(3.0, 9.0 / scale);
        for (int i = targets.size() - 1; i >= 0; i--) {
            if (targets.get(i).distanceTo(f) <= threshold) return i;
        }
        return -1;
    }

    private double headingHandleLength() {
        return HEADING_HANDLE_PX / scale;
    }

    private Point headingHandle(Point target, double angle) {
        double length = headingHandleLength();
        return new Point(
                target.getX() + Math.cos(angle) * length,
                target.getY() + Math.sin(angle) * length);
    }

    private boolean headingHandleAt(Point f) {
        if (selectedTarget < 0 || selectedTarget >= targets.size()) return false;
        Point target = targets.get(selectedTarget);
        double angle = target.hasHeading() ? target.getHeadingRad() : 0.0;
        return headingHandle(target, angle).distanceTo(f) <= Math.max(3.0, 10.0 / scale);
    }

    private void beginHeadingDrag(Point f) {
        draggingHeading = true;
        updateHeadingDrag(f);
    }

    private void updateHeadingDrag(Point f) {
        if (selectedTarget < 0 || selectedTarget >= targets.size()) return;
        Point target = targets.get(selectedTarget);
        double heading = Math.atan2(f.getY() - target.getY(), f.getX() - target.getX());
        targets.set(selectedTarget, new Point(target.getX(), target.getY(), heading));
        repaint();
    }

    private void setTargetHeading(int index, double headingRad) {
        Point target = targets.get(index);
        targets.set(index, new Point(target.getX(), target.getY(), normalizeAngle(headingRad)));
    }

    private double distPx(MouseEvent e, Point p) {
        double dx = e.getX() - sx(p.getX());
        double dy = e.getY() - sy(p.getY());
        return dx * dx + dy * dy;
    }

    private void updateTransform() {
        double w = getWidth();
        double h = getHeight();
        scale = Math.min(w, h) / FIELD * 0.95;
        ox = (w - FIELD * scale) / 2;
        oy = (h - FIELD * scale) / 2;
    }

    private double sx(double x) { return ox + x * scale; }
    private double sy(double y) { return oy + (FIELD - y) * scale; }
    private double fx(double px) { return (px - ox) / scale; }
    private double fy(double py) { return FIELD - (py - oy) / scale; }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        updateTransform();

        drawGrid(g);
        drawPath(g);
        drawObstacles(g);
        drawTargets(g);
        drawStart(g);
        drawRobot(g);
        drawHud(g);
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 20));
        for (int v = 0; v <= 144; v += 24) {
            g.drawLine((int) sx(v), (int) sy(0), (int) sx(v), (int) sy(144));
            g.drawLine((int) sx(0), (int) sy(v), (int) sx(144), (int) sy(v));
        }
        g.setColor(Color.BLACK);
        g.drawRect((int) sx(0), (int) sy(144), (int) (144 * scale), (int) (144 * scale));
    }

    private void drawPath(Graphics2D g) {
        if (samplePos == null) return;
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < samplePos.length; i++) {
            double px = sx(samplePos[i].getX());
            double py = sy(samplePos[i].getY());
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        g.setColor(new Color(0, 110, 230));
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
    }

    private void drawObstacles(Graphics2D g) {
        for (int idx = 0; idx < obstacles.size(); idx++) {
            Zone o = obstacles.get(idx);
            boolean active = idx == dragIndex;
            if (o instanceof CircleZone) {
                CircleZone c = (CircleZone) o;
                Point p = c.getPosition();
                int r = (int) Math.max(2, c.getRadius() * scale);
                int px = (int) sx(p.getX());
                int py = (int) sy(p.getY());
                g.setColor(new Color(230, 60, 60, 90));
                g.fillOval(px - r, py - r, r * 2, r * 2);
                g.setColor(new Color(180, 0, 0));
                g.drawOval(px - r, py - r, r * 2, r * 2);
                drawEdgeHandle(g, p.getX() + c.getRadius(), p.getY(), active && dragMode == DragMode.RESIZE);
            } else if (o instanceof PolygonZone) {
                PolygonZone poly = (PolygonZone) o;
                Point[] corners = poly.getCorners();
                Path2D.Double shape = new Path2D.Double();
                for (int i = 0; i < corners.length; i++) {
                    double px = sx(corners[i].getX());
                    double py = sy(corners[i].getY());
                    if (i == 0) shape.moveTo(px, py); else shape.lineTo(px, py);
                }
                shape.closePath();
                g.setColor(new Color(230, 60, 60, 90));
                g.fill(shape);
                g.setColor(new Color(180, 0, 0));
                g.draw(shape);
                for (int i = 0; i < corners.length; i++) {
                    Point a = corners[i];
                    Point b = corners[(i + 1) % corners.length];
                    drawEdgeHandle(g, (a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0,
                            active && dragMode == DragMode.RESIZE);
                }
                for (Point corner : corners) {
                    drawCornerHandle(g, corner.getX(), corner.getY(), active && dragMode == DragMode.ROTATE);
                }
            }
        }
    }

    private void drawCornerHandle(Graphics2D g, double fx, double fy, boolean active) {
        int s = 8;
        int px = (int) sx(fx);
        int py = (int) sy(fy);
        g.setColor(active ? new Color(0, 120, 220) : new Color(110, 110, 110));
        g.fillRect(px - s / 2, py - s / 2, s, s);
        g.setColor(Color.BLACK);
        g.drawRect(px - s / 2, py - s / 2, s, s);
    }

    private void drawEdgeHandle(Graphics2D g, double fx, double fy, boolean active) {
        int r = 5;
        int px = (int) sx(fx);
        int py = (int) sy(fy);
        g.setColor(active ? new Color(0, 120, 220) : new Color(160, 160, 160));
        g.fillOval(px - r, py - r, r * 2, r * 2);
        g.setColor(Color.BLACK);
        g.drawOval(px - r, py - r, r * 2, r * 2);
    }

    private void drawTargets(Graphics2D g) {
        for (int i = 0; i < targets.size(); i++) {
            Point t = targets.get(i);
            boolean visited = isVisited(t);
            boolean selected = i == selectedTarget;
            int s = 8;
            int px = (int) sx(t.getX());
            int py = (int) sy(t.getY());

            if (selected) {
                g.setColor(new Color(0, 120, 220, 70));
                g.fillOval(px - 9, py - 9, 18, 18);
            }
            if (visited) {
                g.setColor(new Color(0, 170, 60));
                g.fillRoundRect(px - s / 2, py - s / 2, s, s, 3, 3);
                g.setColor(new Color(0, 90, 30));
            } else {
                g.setColor(new Color(150, 150, 150));
            }
            g.drawRoundRect(px - s / 2, py - s / 2, s, s, 3, 3);

            if (t.hasHeading()) {
                drawHeading(g, t, visited, selected, true);
            } else if (selected) {
                drawHeading(g, t, false, selected, false);
            }
        }
    }

    private void drawHeading(Graphics2D g, Point target, boolean visited, boolean selected, boolean hasHeading) {
        double angle = hasHeading ? target.getHeadingRad() : 0.0;
        Point handle = headingHandle(target, angle);
        int ax = (int) sx(target.getX());
        int ay = (int) sy(target.getY());
        int bx = (int) sx(handle.getX());
        int by = (int) sy(handle.getY());

        Color color = selected
                ? new Color(0, 120, 220)
                : visited ? new Color(0, 90, 30) : new Color(110, 110, 110);

        g.setColor(color);
        g.setStroke(new BasicStroke(selected ? 2.2f : 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(ax, ay, bx, by);

        if (hasHeading) {
            double screenAngle = Math.atan2(by - ay, bx - ax);
            int headLength = 7;
            int x1 = (int) (bx - headLength * Math.cos(screenAngle - Math.PI / 7));
            int y1 = (int) (by - headLength * Math.sin(screenAngle - Math.PI / 7));
            int x2 = (int) (bx - headLength * Math.cos(screenAngle + Math.PI / 7));
            int y2 = (int) (by - headLength * Math.sin(screenAngle + Math.PI / 7));
            g.drawLine(bx, by, x1, y1);
            g.drawLine(bx, by, x2, y2);
        }

        int r = 4;
        if (hasHeading) {
            g.fillOval(bx - r, by - r, r * 2, r * 2);
            g.setColor(Color.WHITE);
            g.drawOval(bx - r, by - r, r * 2, r * 2);
        } else {
            g.drawOval(bx - r, by - r, r * 2, r * 2);
        }
    }

    private boolean isVisited(Point t) {
        for (Point v : visitedTargets) {
            if (v.getX() == t.getX() && v.getY() == t.getY()) {
                return true;
            }
        }
        return false;
    }

    private void drawStart(Graphics2D g) {
        int px = (int) sx(start.getX());
        int py = (int) sy(start.getY());
        int s = 9;
        g.setColor(new Color(120, 60, 200));
        g.fillPolygon(
                new int[]{px, px + s, px, px - s},
                new int[]{py - s, py, py + s, py},
                4);

        double length = headingHandleLength();
        int hx = (int) sx(start.getX() + Math.cos(startHeading) * length);
        int hy = (int) sy(start.getY() + Math.sin(startHeading) * length);
        g.setColor(new Color(120, 60, 200));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(px, py, hx, hy);

        double arrowAngle = Math.atan2(hy - py, hx - px);
        int headLength = 6;
        int x1 = (int) (hx - headLength * Math.cos(arrowAngle - Math.PI / 7));
        int y1 = (int) (hy - headLength * Math.sin(arrowAngle - Math.PI / 7));
        int x2 = (int) (hx - headLength * Math.cos(arrowAngle + Math.PI / 7));
        int y2 = (int) (hy - headLength * Math.sin(arrowAngle + Math.PI / 7));
        g.drawLine(hx, hy, x1, y1);
        g.drawLine(hx, hy, x2, y2);
    }

    private void drawRobot(Graphics2D g) {
        double halfW = robotWidth / 2.0;
        double halfH = robotHeight / 2.0;
        double cos = Math.cos(robotHeading);
        double sin = Math.sin(robotHeading);
        double[][] local = {
                {-halfW, -halfH}, {halfW, -halfH}, {halfW, halfH}, {-halfW, halfH}
        };

        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < local.length; i++) {
            double lx = local[i][0];
            double ly = local[i][1];
            double wx = robotPos.getX() + lx * cos - ly * sin;
            double wy = robotPos.getY() + lx * sin + ly * cos;
            if (i == 0) p.moveTo(sx(wx), sy(wy)); else p.lineTo(sx(wx), sy(wy));
        }
        p.closePath();
        g.setColor(new Color(255, 140, 0, 200));
        g.fill(p);
        g.setColor(new Color(160, 80, 0));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(p);

        double nx = robotPos.getX() + halfW * cos;
        double ny = robotPos.getY() + halfW * sin;
        g.setColor(Color.BLACK);
        g.drawLine((int) sx(robotPos.getX()), (int) sy(robotPos.getY()), (int) sx(nx), (int) sy(ny));

        if (width > 0) {
            double px = -sin, py = cos;
            double hw = width / 2.0;
            double ix = robotPos.getX() + halfW * cos;
            double iy = robotPos.getY() + halfW * sin;
            int ax = (int) sx(ix - hw * px);
            int ay = (int) sy(iy - hw * py);
            int bx = (int) sx(ix + hw * px);
            int by = (int) sy(iy + hw * py);
            g.setColor(new Color(0, 120, 220));
            g.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(ax, ay, bx, by);
        }
    }

    private void drawHud(Graphics2D g) {
        String selectedLabel;
        if (selectedTarget >= 0 && selectedTarget < targets.size()) {
            Point selected = targets.get(selectedTarget);
            selectedLabel = "Selected target: (" + fmt(selected.getX()) + ", " + fmt(selected.getY())
                    + ")   Locked heading: " + headingLabel(selected.getHeadingRad());
        } else {
            selectedLabel = "Selected target: none";
        }

        String[] lines = {
                "Left-click: add/select      Right-click: remove",
                "Drag body: move   Drag edge: resize   Drag corner: rotate",
                "Drag heading handle: rotate   Use the side panel for all options",
                "Mode: " + mode + "   Reorder: " + (reorder ? "ON" : "OFF")
                        + "   Smoothing: " + (smoothing ? "ON" : "OFF")
                        + "   Scenario: " + (scenarioName.isEmpty() ? "(unsaved)" : scenarioName),
                "Targets: " + targets.size() + "   Obstacles: " + obstacles.size()
                        + "   Width: " + (int) width + " in",
                "Robot: " + fmt(robotWidth) + " x " + fmt(robotHeight) + " in"
                        + "   Clearance: " + fmt(clearance) + " in",
                "Start: (" + fmt(start.getX()) + ", " + fmt(start.getY()) + ")"
                        + " heading " + headingLabel(startHeading),
                "Max targets: " + (maxTargets <= 0 ? "unlimited" : maxTargets)
                        + "   Visiting: " + visitedTargets.size(),
                selectedLabel + "   New locked heading: " + headingLabel(defaultTargetHeading),
                "Obstacle shape: " + obstacleShape
                        + (obstacleShape == ObstacleShape.POLYGON ? " (" + polygonSides + " sides)" : "")
                        + "   Size: " + (int) obstacleRadius + " in",
        };
        if (!statusMessage.isEmpty()) {
            lines = java.util.Arrays.copyOf(lines, lines.length + 1);
            lines[lines.length - 1] = "Error: " + statusMessage;
        } else if (!infoMessage.isEmpty()) {
            lines = java.util.Arrays.copyOf(lines, lines.length + 1);
            lines[lines.length - 1] = "Info: " + infoMessage;
        }

        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        Font old = g.getFont();
        int lineH = g.getFontMetrics().getHeight();
        int pad = 6;
        int width = 0;
        for (String l : lines) width = Math.max(width, g.getFontMetrics().stringWidth(l));
        int boxW = width + pad * 2;
        int boxH = lineH * lines.length + pad * 2;

        g.setColor(new Color(255, 255, 255, 190));
        g.fillRect(6, 6, boxW, boxH);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(6, 6, boxW, boxH);
        g.setColor(Color.BLACK);
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 6 + pad, 6 + pad + lineH * i + g.getFontMetrics().getAscent());
        }
        g.setFont(old);
    }

    public void generateCode() {
        if (targets.isEmpty()) {
            statusMessage = "Nothing to generate: add a target first";
            repaint();
            return;
        }

        String code = buildCode();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);

        JTextArea area = new JTextArea(code);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setEditable(false);
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(640, 480));
        JOptionPane.showMessageDialog(
                this, scroll, "Weaver code (copied to clipboard)", JOptionPane.INFORMATION_MESSAGE);
        requestFocusInWindow();
    }

    private String buildCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("Weaver.setConfig(new PathConfig().intakeWidth(")
                .append(fmt(width))
                .append(")");
        if (!smoothing) {
            sb.append(".smoothing(false)");
        }
        if (Double.compare(robotWidth, DEFAULT_CONFIG.getRobotWidth()) != 0) {
            sb.append(".robotWidth(").append(fmt(robotWidth)).append(")");
        }
        if (Double.compare(robotHeight, DEFAULT_CONFIG.getRobotHeight()) != 0) {
            sb.append(".robotHeight(").append(fmt(robotHeight)).append(")");
        }
        if (Double.compare(clearance, DEFAULT_CONFIG.getClearance()) != 0) {
            sb.append(".clearance(").append(fmt(clearance)).append(")");
        }
        if (excludeBlockedTargets != DEFAULT_CONFIG.isExcludeBlockedTargets()) {
            sb.append(".excludeBlockedTargets(").append(excludeBlockedTargets).append(")");
        }
        if (Double.compare(turnCostWeight, DEFAULT_CONFIG.getTurnCostWeight()) != 0) {
            sb.append(".turnCostWeight(").append(fmt(turnCostWeight)).append(")");
        }
        if (bruteForceOrderLimit != DEFAULT_CONFIG.getBruteForceOrderLimit()) {
            sb.append(".bruteForceOrderLimit(").append(bruteForceOrderLimit).append(")");
        }
        sb.append(");\n\n");

        sb.append("PathResult result = Weaver.builder()\n");
        sb.append("        .start(new Point(")
                .append(fmt(start.getX()))
                .append(", ")
                .append(fmt(start.getY()))
                .append(", ")
                .append(fmt(startHeading))
                .append("))\n");

        for (Point t : targets) {
            sb.append("        .addTarget(new Point(")
                    .append(fmt(t.getX()))
                    .append(", ")
                    .append(fmt(t.getY()));
            if (t.hasHeading()) {
                sb.append(", ").append(fmt(t.getHeadingRad()));
            }
            sb.append("), HeadingPolicy.")
                    .append(t.hasHeading() ? "LOCKED" : "FREE")
                    .append(")\n");
        }

        if (!obstacles.isEmpty()) {
            sb.append("        .obstacles(\n");

            for (int i = 0; i < obstacles.size(); i++) {
                sb.append("                ").append(obstacleCode(obstacles.get(i)))
                        .append(i < obstacles.size() - 1 ? "," : "")
                        .append("\n");
            }

            sb.append("        )\n");
        }

        if (maxTargets > 0) {
            sb.append("        .maxTargets(").append(maxTargets).append(")\n");
        }

        if (!reorder) {
            sb.append("        .ordered()\n");
        }

        sb.append("        .build();\n");
        return sb.toString();
    }

    private String obstacleCode(Zone o) {
        if (o instanceof CircleZone) {
            CircleZone c = (CircleZone) o;
            return "new CircleZone(new Point("
                    + fmt(c.getPosition().getX())
                    + ", "
                    + fmt(c.getPosition().getY())
                    + "), "
                    + fmt(c.getRadius())
                    + ")";
        }

        if (o instanceof PolygonZone) {
            StringBuilder sb = new StringBuilder("new PolygonZone(");
            Point[] corners = ((PolygonZone) o).getCorners();

            for (int i = 0; i < corners.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("new Point(")
                        .append(fmt(corners[i].getX()))
                        .append(", ")
                        .append(fmt(corners[i].getY()))
                        .append(")");
            }

            return sb.append(")").toString();
        }

        return o.toString();
    }

    private static String headingLabel(double headingRad) {
        if (Double.isNaN(headingRad)) return "none";
        return fmt(Math.toDegrees(normalizeAngle(headingRad))) + " deg";
    }

    private static String fmt(double value) {
        if (value == 0.0) {
            return "0";
        }

        String s = String.format(Locale.ROOT, "%.2f", value);

        while (s.contains(".") && s.endsWith("0")) {
            s = s.substring(0, s.length() - 1);
        }

        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }

        return s;
    }

    void setControls(SimulatorControls controls) {
        this.controls = controls;
        refreshControls();
    }

    private void refreshControls() {
        if (controls != null) {
            controls.refresh();
        }
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        if (mode != null) {
            this.mode = mode;
            refreshControls();
            repaint();
        }
    }

    public boolean isReorder() {
        return reorder;
    }

    public void setReorder(boolean reorder) {
        this.reorder = reorder;
        rebuildPath();
    }

    public boolean isSmoothing() {
        return smoothing;
    }

    public void setSmoothing(boolean smoothing) {
        this.smoothing = smoothing;
        rebuildPath();
    }

    public void clearAll() {
        targets.clear();
        obstacles.clear();
        selectedTarget = -1;
        defaultTargetHeading = Double.NaN;
        rebuildPath();
    }

    public boolean isPolygonObstacles() {
        return obstacleShape == ObstacleShape.POLYGON;
    }

    public void setPolygonObstacles(boolean polygon) {
        obstacleShape = polygon ? ObstacleShape.POLYGON : ObstacleShape.CIRCLE;
        refreshControls();
        repaint();
    }

    public int getPolygonSides() {
        return polygonSides;
    }

    public void setPolygonSides(int sides) {
        polygonSides = Math.max(3, Math.min(12, sides));
        refreshControls();
        repaint();
    }

    public double getObstacleRadius() {
        return obstacleRadius;
    }

    public void setObstacleRadius(double radius) {
        obstacleRadius = Math.max(1, Math.min(30, radius));
        refreshControls();
        repaint();
    }

    public double getIntakeWidth() {
        return width;
    }

    public void setIntakeWidth(double value) {
        width = Math.max(0, Math.min(60, value));
        rebuildPath();
    }

    public int getMaxTargets() {
        return maxTargets;
    }

    public void setMaxTargets(int max) {
        maxTargets = Math.max(0, Math.min(99, max));
        rebuildPath();
    }

    public int getTargetCount() {
        return targets.size();
    }

    public int getSelectedTargetIndex() {
        return selectedTarget;
    }

    public boolean hasSelectedTarget() {
        return selectedTarget >= 0 && selectedTarget < targets.size();
    }

    public boolean isSelectedHeadingLocked() {
        return hasSelectedTarget() && targets.get(selectedTarget).hasHeading();
    }

    public double getSelectedHeadingDegrees() {
        if (!hasSelectedTarget()) return 0.0;
        Point target = targets.get(selectedTarget);
        return toDisplayDegrees(target.hasHeading() ? target.getHeadingRad() : 0.0);
    }

    public void setSelectedHeadingLocked(boolean locked) {
        if (!hasSelectedTarget()) return;
        Point target = targets.get(selectedTarget);
        if (locked == target.hasHeading()) return;
        setTargetHeading(selectedTarget, locked
                ? (target.hasHeading() ? target.getHeadingRad() : 0.0)
                : Double.NaN);
        rebuildPath();
    }

    public void setSelectedHeadingDegrees(double degrees) {
        if (!hasSelectedTarget()) return;
        setTargetHeading(selectedTarget, Math.toRadians(degrees));
        rebuildPath();
    }

    public void nudgeSelectedHeading(double degrees) {
        if (!hasSelectedTarget()) return;
        Point target = targets.get(selectedTarget);
        double heading = target.hasHeading() ? target.getHeadingRad() : 0.0;
        setTargetHeading(selectedTarget, heading + Math.toRadians(degrees));
        rebuildPath();
    }

    public boolean isNewTargetLocked() {
        return !Double.isNaN(defaultTargetHeading);
    }

    public double getNewTargetHeadingDegrees() {
        return toDisplayDegrees(Double.isNaN(defaultTargetHeading) ? 0.0 : defaultTargetHeading);
    }

    public void setNewTargetLocked(boolean locked) {
        if (locked == isNewTargetLocked()) return;
        defaultTargetHeading = locked ? 0.0 : Double.NaN;
        refreshControls();
        repaint();
    }

    public void setNewTargetHeadingDegrees(double degrees) {
        defaultTargetHeading = normalizeAngle(Math.toRadians(degrees));
        refreshControls();
        repaint();
    }

    public double getStartHeadingDegrees() {
        return toDisplayDegrees(startHeading);
    }

    public void setStartHeadingDegrees(double degrees) {
        startHeading = normalizeAngle(Math.toRadians(degrees));
        rebuildPath();
    }

    public void deselect() {
        selectedTarget = -1;
        refreshControls();
        repaint();
    }

    private static double toDisplayDegrees(double headingRad) {
        double degrees = Math.toDegrees(headingRad) % 360.0;
        if (degrees < 0) degrees += 360.0;
        return degrees;
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu scenario = new JMenu("Scenario");
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        JMenuItem copyItem = new JMenuItem("Copy current scenario");
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask));
        copyItem.addActionListener(e -> copyScenario());
        scenario.add(copyItem);

        JMenuItem pasteItem = new JMenuItem("Paste scenario from clipboard");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));
        pasteItem.addActionListener(e -> pasteScenario());
        scenario.add(pasteItem);

        JMenu file = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        file.add(exitItem);

        bar.add(scenario);
        bar.add(file);
        return bar;
    }

    public void copyScenario() {
        try {
            String json = ScenarioFile.toJson(currentScenario());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);
            statusMessage = "";
            infoMessage = "Copied current scenario to clipboard";
        } catch (Exception ex) {
            showScenarioError("Could not copy scenario", ex);
        }

        repaint();
        requestFocusInWindow();
    }

    public void pasteScenario() {
        String text = readClipboardText();
        String failure = null;

        try {
            ScenarioFile.Scenario scenario = ScenarioFile.parse(text);
            applyScenario(scenario);
            statusMessage = "";
            infoMessage = "Pasted scenario" + (scenarioName.isEmpty() ? "" : ": " + scenarioName);
        } catch (Exception ex) {
            failure = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        }

        if (failure != null) {
            statusMessage = failure;
            infoMessage = "";
            JOptionPane.showMessageDialog(
                    this,
                    "Could not paste scenario:\n" + failure,
                    "Paste failed",
                    JOptionPane.ERROR_MESSAGE);
        }

        repaint();
        requestFocusInWindow();
    }

    private static String readClipboardText() {
        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            return data instanceof String ? (String) data : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void showScenarioError(String title, Exception ex) {
        statusMessage = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        infoMessage = "";
        JOptionPane.showMessageDialog(this, title + ":\n" + statusMessage, title, JOptionPane.ERROR_MESSAGE);
    }

    private ScenarioFile.Scenario currentScenario() {
        ScenarioFile.Scenario scenario = new ScenarioFile.Scenario();
        scenario.name = scenarioName;
        scenario.description = scenarioDescription;
        scenario.start = new Point(start.getX(), start.getY(), startHeading);

        for (Point target : targets) {
            scenario.targets.add(new Point(target.getX(), target.getY(), target.getHeadingRad()));
        }

        scenario.obstacles.addAll(obstacles);
        scenario.config
                .intakeWidth(width)
                .robotWidth(robotWidth)
                .robotHeight(robotHeight)
                .clearance(clearance)
                .excludeBlockedTargets(excludeBlockedTargets)
                .smoothing(smoothing)
                .turnCostWeight(turnCostWeight)
                .bruteForceOrderLimit(bruteForceOrderLimit);
        scenario.reorder = reorder;
        scenario.maxTargets = maxTargets;
        return scenario;
    }

    private void applyScenario(ScenarioFile.Scenario scenario) {
        targets.clear();
        for (Point target : scenario.targets) {
            targets.add(new Point(target.getX(), target.getY(), target.getHeadingRad()));
        }

        obstacles.clear();
        obstacles.addAll(scenario.obstacles);

        start = new Point(scenario.start.getX(), scenario.start.getY());
        startHeading = scenario.start.hasHeading() ? scenario.start.getHeadingRad() : 0.0;

        PathConfig config = scenario.config;
        width = Math.max(0, config.getIntakeWidth());
        robotWidth = Math.max(0, config.getRobotWidth());
        robotHeight = Math.max(0, config.getRobotHeight());
        clearance = Math.max(0, config.getClearance());
        excludeBlockedTargets = config.isExcludeBlockedTargets();
        smoothing = config.isSmoothing();
        turnCostWeight = Math.max(0, config.getTurnCostWeight());
        bruteForceOrderLimit = Math.max(1, config.getBruteForceOrderLimit());
        reorder = scenario.reorder;
        maxTargets = Math.max(0, scenario.maxTargets);

        scenarioName = scenario.name;
        scenarioDescription = scenario.description;
        selectedTarget = -1;
        defaultTargetHeading = Double.NaN;
        draggingHeading = false;
        dragIndex = -1;

        rebuildPath();
        robotArc = 0;
        updateRobot();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Marrow Weaver Simulator");
            Simulator sim = new Simulator();
            SimulatorControls controls = new SimulatorControls(sim);
            sim.setControls(controls);

            JPanel root = new JPanel(new BorderLayout());
            root.add(sim, BorderLayout.CENTER);

            JScrollPane controlsScroll = new JScrollPane(controls);
            controlsScroll.setPreferredSize(new Dimension(300, 820));
            controlsScroll.setBorder(null);
            controlsScroll.getVerticalScrollBar().setUnitIncrement(16);
            root.add(controlsScroll, BorderLayout.EAST);

            frame.setContentPane(root);
            frame.setJMenuBar(sim.createMenuBar());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            sim.requestFocusInWindow();
        });
    }
}
