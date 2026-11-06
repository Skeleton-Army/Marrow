package com.skeletonarmy.marrow.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.skeletonarmy.marrow.zones.Point;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * Tests that target selection asks the reachability check about each pair only once.
 */
public class TargetOrdererReachabilityCacheTest {

    @Test
    public void select_exhaustiveSearch_checksEachPairOnce() {
        // Arrange
        List<Point> targets = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            targets.add(new Point(10 * i, (i * 7) % 5 * 10));
        }

        int[] calls = {0};

        // Act
        List<Point> actual = TargetOrderer.select(new Point(0, 0), 0, targets, 7, new PathConfig(), (from, target) -> {
            calls[0]++;
            return true;
        });

        // Assert: at most one call per (from, target) pair.
        assertEquals(7, actual.size());
        assertTrue("calls=" + calls[0], calls[0] <= 8 * 7);
    }

    @Test
    public void select_blockedTargets_areStillLeftOut() {
        // Arrange
        Point blocked = new Point(50, 50);
        List<Point> targets = new ArrayList<>();
        targets.add(new Point(10, 0));
        targets.add(blocked);
        targets.add(new Point(20, 0));

        // Act
        List<Point> actual = TargetOrderer.select(
                new Point(0, 0), 0, targets, 3, new PathConfig(), (from, target) -> target != blocked);

        // Assert
        assertEquals(2, actual.size());
        assertTrue(!actual.contains(blocked));
    }
}
