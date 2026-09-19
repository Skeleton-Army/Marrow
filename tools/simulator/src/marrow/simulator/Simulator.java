package marrow.simulator;

import com.skeletonarmy.marrow.weaver.PathConfig;
import com.skeletonarmy.marrow.weaver.PathPose;
import com.skeletonarmy.marrow.weaver.PathResult;
import com.skeletonarmy.marrow.weaver.PathRoute;
import com.skeletonarmy.marrow.weaver.Weaver;
import com.skeletonarmy.marrow.zones.CircleZone;
import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class Simulator extends JPanel {

    private static final double FIELD = 144.0;
    private static final double ROBOT_SIZE = 12.0;
    private static final double CLEARANCE = 4.0;

    private enum Mode { TARGET, OBSTACLE, START }

    private final List<Point> targets = new ArrayList<>();
    private final List<CircleZone> obstacles = new ArrayList<>();

    private Point start = new Point(24, 24);
    private double obstacleRadius = 6.0;
    private boolean reorder = false;
    private Mode mode = Mode.TARGET;

    private PathRoute path;
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

    private double scale = 1.0;
    private double ox, oy;

    public Simulator() {
        setPreferredSize(new Dimension(820, 820));
        setFocusable(true);

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
                    Point f = new Point(fx(e.getX()), fy(e.getY()));
                    switch (mode) {
                        case TARGET:
                            targets.add(f);
                            break;
                        case OBSTACLE:
                            obstacles.add(new CircleZone(f, obstacleRadius));
                            break;
                        case START:
                            start = f;
                            break;
                    }
                    rebuildPath();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_T: mode = Mode.TARGET; break;
                    case KeyEvent.VK_O: mode = Mode.OBSTACLE; break;
                    case KeyEvent.VK_S: mode = Mode.START; break;
                    case KeyEvent.VK_R:
                        reorder = !reorder;
                        rebuildPath();
                        break;
                    case KeyEvent.VK_C:
                        targets.clear();
                        obstacles.clear();
                        rebuildPath();
                        break;
                    case KeyEvent.VK_EQUALS:
                    case KeyEvent.VK_ADD:
                        obstacleRadius = Math.min(obstacleRadius + 1, 30);
                        break;
                    case KeyEvent.VK_MINUS:
                    case KeyEvent.VK_SUBTRACT:
                        obstacleRadius = Math.max(obstacleRadius - 1, 1);
                        break;
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
        statusMessage = "";

        if (targets.isEmpty()) {
            repaint();
            return;
        }

        try {
            Weaver.setConfig(new PathConfig()
                    .reach(0)
                    .width(0)
                    .robotSize(ROBOT_SIZE)
                    .clearance(CLEARANCE));

            Weaver.Builder b = Weaver.builder()
                    .start(new PathPose(start.getX(), start.getY(), 0));
            for (Point t : targets) {
                b.addTarget(new PathPose(t.getX(), t.getY()));
            }
            for (CircleZone o : obstacles) {
                b.addObstacle(o);
            }
            if (!reorder) {
                b.ordered();
            }

            PathResult result = b.generate();
            path = result.getPath();
            buildSamples();
            robotArc = 0;
            updateRobot();
        } catch (Exception ex) {
            path = null;
            statusMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
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

        if (targetIdx >= 0) targets.remove(targetIdx);
        else if (obstacleIdx >= 0) obstacles.remove(obstacleIdx);
        else return;

        rebuildPath();
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
        for (CircleZone o : obstacles) {
            Point c = o.getPosition();
            int r = (int) Math.max(2, o.getRadius() * scale);
            int px = (int) sx(c.getX());
            int py = (int) sy(c.getY());
            g.setColor(new Color(230, 60, 60, 90));
            g.fillOval(px - r, py - r, r * 2, r * 2);
            g.setColor(new Color(180, 0, 0));
            g.drawOval(px - r, py - r, r * 2, r * 2);
        }
    }

    private void drawTargets(Graphics2D g) {
        for (Point t : targets) {
            int s = 8;
            int px = (int) sx(t.getX());
            int py = (int) sy(t.getY());
            g.setColor(new Color(0, 170, 60));
            g.fillRoundRect(px - s / 2, py - s / 2, s, s, 3, 3);
            g.setColor(new Color(0, 90, 30));
            g.drawRoundRect(px - s / 2, py - s / 2, s, s, 3, 3);
        }
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
    }

    private void drawRobot(Graphics2D g) {
        double half = ROBOT_SIZE / 2.0;
        double cos = Math.cos(robotHeading);
        double sin = Math.sin(robotHeading);
        double[][] local = {
                {-half, -half}, {half, -half}, {half, half}, {-half, half}
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

        double nx = robotPos.getX() + half * cos;
        double ny = robotPos.getY() + half * sin;
        g.setColor(Color.BLACK);
        g.drawLine((int) sx(robotPos.getX()), (int) sy(robotPos.getY()), (int) sx(nx), (int) sy(ny));
    }

    private void drawHud(Graphics2D g) {
        String[] lines = {
                "Left-click: add      Right-click: remove",
                "T target | O obstacle | S start | R reorder | C clear | +/- obstacle size",
                "Mode: " + mode + "   Reorder: " + (reorder ? "ON" : "OFF")
                        + "   Targets: " + targets.size() + "   Obstacles: " + obstacles.size(),
                "Obstacle radius: " + (int) obstacleRadius + " in",
        };
        if (!statusMessage.isEmpty()) {
            lines = java.util.Arrays.copyOf(lines, lines.length + 1);
            lines[lines.length - 1] = "Error: " + statusMessage;
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

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("selftest")) {
            runSelfTest();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Marrow Weaver Simulator");
            Simulator sim = new Simulator();
            frame.setContentPane(sim);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            sim.requestFocusInWindow();
        });
    }

    private static void runSelfTest() {
        Weaver.setConfig(new PathConfig().reach(0).width(0).robotSize(ROBOT_SIZE).clearance(CLEARANCE));

        PathResult result = Weaver.builder()
                .start(new PathPose(24, 24, 0))
                .addTarget(new PathPose(72, 96))
                .addTarget(new PathPose(120, 48))
                .addObstacle(new CircleZone(new Point(72, 60), 6))
                .generate();

        PathRoute route = result.getPath();
        int segCount = route.getSegmentCount();
        for (int i = 0; i <= 10; i++) {
            double g = (double) i / 10 * segCount;
            Point p = route.get(g);
            System.out.printf("t=%.2f pos=(%.2f, %.2f) heading=%.3f%n",
                    (double) i / 10, p.getX(), p.getY(), route.getHeading(g));
        }

        List<Zone> obs = new ArrayList<>();
        obs.add(new CircleZone(new Point(72, 60), 6));
        boolean clear = Weaver.isPathClear(route, obs, CLEARANCE, ROBOT_SIZE, 300);
        System.out.println("path clear of obstacle: " + clear);
        System.out.println("SELFTEST OK");
    }
}
