package com.skeletonarmy.marrow.bezier;

import com.skeletonarmy.marrow.zones.Point;
import com.skeletonarmy.marrow.zones.Zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollisionAvoider {
    private CollisionAvoider() {}

    public static BezierPath avoid(BezierPath path, List<Zone> obstacles, BezierConfig config) {
        if (obstacles == null || obstacles.isEmpty()) {
            return path;
        }

        boolean single = path.getSegments().size() == 1;
        List<BezierCurve> segments = new ArrayList<>();
        for (BezierCurve curve : path.getSegments()) segments.addAll(curve.toCubicSegments());
        double targetClearance = config.getClearance() * 1.05 + 0.05;

        List<BezierCurve> best = new ArrayList<>(segments);
        double bestViolation = Double.MAX_VALUE;

        for (int iter = 0; iter < config.getMaxIterations(); iter++) {
            boolean anyViolation = false;
            double worstViolation = 0;

            List<BezierCurve> snapshot = new ArrayList<>(segments);

            for (int s = 0; s < segments.size(); s++) {
                BezierCurve curve = segments.get(s);
                List<Point> cps = new ArrayList<>(curve.getControlPoints());
                int cpCount = cps.size();
                double[] dx = new double[cpCount], dy = new double[cpCount], weightSum = new double[cpCount];

                List<Point> samples = curve.sample(Math.max(config.getSamplesPerCheck(), 2));
                double maxStep = Math.max(config.getClearance() + config.getRobotSize() / 2.0, 1.0) * 2.0;

                for (int i = 0; i < samples.size(); i++) {
                    Point sample = samples.get(i);
                    double t = (double) i / (samples.size() - 1);
                    Point tangent = curve.derivative(t);
                    double tLen = Math.hypot(tangent.getX(), tangent.getY());
                    double nx = tLen > 1e-9 ? -tangent.getY() / tLen : 0, ny = tLen > 1e-9 ? tangent.getX() / tLen : 0;

                    List<Point> footprint = RobotFootprint.samplePoints(sample, tLen > 1e-9 ? Math.atan2(tangent.getY(), tangent.getX()) : 0, config.getRobotSize());
                    for (Point fp : footprint) {
                        for (Zone zone : obstacles) {
                            double dist = zone.distanceToBoundary(fp);
                            if (zone.contains(fp) || dist < targetClearance) {
                                anyViolation = true;
                                worstViolation = Math.max(worstViolation, targetClearance - dist);

                                double vx = sample.getX() - zone.getPosition().getX(), vy = sample.getY() - zone.getPosition().getY();
                                double norm = Math.hypot(vx, vy);

                                if (norm < 1e-6) {
                                    vx = nx;
                                    vy = ny;
                                    norm = Math.hypot(vx, vy);
                                    if (norm < 1e-6) {
                                        vx = 1;
                                        vy = 0;
                                        norm = 1;
                                    }
                                } else {
                                    vx /= norm;
                                    vy /= norm;
                                    if (Math.abs(vx * (tLen > 1e-9 ? tangent.getX() / tLen : 0) + vy * (tLen > 1e-9 ? tangent.getY() / tLen : 0)) > 0.9 && tLen > 1e-9) {
                                        vx = 0.3 * vx + 0.7 * nx;
                                        vy = 0.3 * vy + 0.7 * ny;
                                        double blended = Math.hypot(vx, vy);
                                        if (blended > 1e-9) {
                                            vx /= blended;
                                            vy /= blended;
                                        }
                                    }
                                }

                                double push = (targetClearance - dist) * config.getStepSize();
                                for (int cpIdx = 1; cpIdx < cpCount - 1; cpIdx++) {
                                    double weight = Math.max(0, 1 - Math.abs((double) cpIdx / (cpCount - 1) - t) * cpCount);
                                    if (weight > 0) {
                                        dx[cpIdx] += vx * push * weight;
                                        dy[cpIdx] += vy * push * weight;
                                        weightSum[cpIdx] += weight;
                                    }
                                }
                            }
                        }
                    }
                }

                for (int cpIdx = 1; cpIdx < cpCount - 1; cpIdx++) {
                    if (weightSum[cpIdx] > 0) {
                        double mx = dx[cpIdx] / weightSum[cpIdx], my = dy[cpIdx] / weightSum[cpIdx], mLen = Math.hypot(mx, my);
                        if (mLen > maxStep) {
                            mx *= maxStep / mLen;
                            my *= maxStep / mLen;
                        }
                        cps.set(cpIdx, new Point(cps.get(cpIdx).getX() + mx, cps.get(cpIdx).getY() + my));
                    }
                }
                segments.set(s, new BezierCurve(cps));
            }

            if (worstViolation < bestViolation) {
                bestViolation = worstViolation;
                best = snapshot;
            }

            if (!anyViolation) {
                break;
            }
        }

        return single
                ? new BezierPath(Collections.singletonList(BezierCurve.fromCubicSegments(best)))
                : new BezierPath(best);
    }
}
