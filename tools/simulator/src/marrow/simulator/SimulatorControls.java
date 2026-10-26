package marrow.simulator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

final class SimulatorControls extends JPanel {

    private final Simulator sim;
    private boolean refreshing;

    private final JToggleButton targetButton = new JToggleButton("Target");
    private final JToggleButton obstacleButton = new JToggleButton("Obstacle");
    private final JToggleButton startButton = new JToggleButton("Start");
    private final JButton deselectButton = new JButton("Deselect");

    private final JCheckBox polygonCheck = new JCheckBox("Polygon");
    private final JSpinner sidesSpinner = new JSpinner(new SpinnerNumberModel(4, 3, 12, 1));
    private final JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(6.0, 1.0, 30.0, 1.0));

    private final JSpinner intakeSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 60.0, 1.0));
    private final JCheckBox reorderCheck = new JCheckBox("Reorder targets");
    private final JCheckBox smoothingCheck = new JCheckBox("Smooth path");
    private final JSpinner maxTargetsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));

    private final JLabel selectionLabel = new JLabel("No target selected");
    private final JCheckBox lockSelectedCheck = new JCheckBox("Lock heading");
    private final JSpinner selectedHeadingSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 360.0, 5.0));
    private final JButton turnLeftButton = new JButton("-15");
    private final JButton turnRightButton = new JButton("+15");

    private final JCheckBox newLockCheck = new JCheckBox("Lock new targets");
    private final JSpinner newHeadingSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 360.0, 5.0));

    private final JSpinner startHeadingSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 360.0, 5.0));

    private final JButton clearButton = new JButton("Clear all");
    private final JButton generateButton = new JButton("Generate code");
    private final JButton copyButton = new JButton("Copy scenario");
    private final JButton pasteButton = new JButton("Paste scenario");

    SimulatorControls(Simulator sim) {
        this.sim = sim;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(290, 780));

        addSection(toolSection());
        addSection(obstacleSection());
        addSection(plannerSection());
        addSection(selectedTargetSection());
        addSection(newTargetSection());
        addSection(startSection());
        addSection(actionsSection());

        wire();
        refresh();
    }

    private JPanel toolSection() {
        JPanel section = section("Tool");
        targetButton.setFocusable(false);
        obstacleButton.setFocusable(false);
        startButton.setFocusable(false);
        ButtonGroup group = new ButtonGroup();
        group.add(targetButton);
        group.add(obstacleButton);
        group.add(startButton);
        row(section, targetButton, obstacleButton, startButton);

        deselectButton.setFocusable(false);
        row(section, deselectButton);
        lockHeight(section);
        return section;
    }

    private JPanel obstacleSection() {
        JPanel section = section("Obstacle");
        polygonCheck.setFocusable(false);
        row(section, polygonCheck);
        fieldRow(section, "Sides", sidesSpinner);
        fieldRow(section, "Size (in)", sizeSpinner);
        lockHeight(section);
        return section;
    }

    private JPanel plannerSection() {
        JPanel section = section("Planner");
        fieldRow(section, "Intake width (in)", intakeSpinner);
        reorderCheck.setFocusable(false);
        smoothingCheck.setFocusable(false);
        row(section, reorderCheck);
        row(section, smoothingCheck);
        fieldRow(section, "Max targets (0 = all)", maxTargetsSpinner);
        lockHeight(section);
        return section;
    }

    private JPanel selectedTargetSection() {
        JPanel section = section("Selected target");
        row(section, selectionLabel);
        lockSelectedCheck.setFocusable(false);
        row(section, lockSelectedCheck);
        fieldRow(section, "Heading (deg)", selectedHeadingSpinner);
        turnLeftButton.setFocusable(false);
        turnRightButton.setFocusable(false);
        row(section, turnLeftButton, turnRightButton);
        lockHeight(section);
        return section;
    }

    private JPanel newTargetSection() {
        JPanel section = section("New targets");
        newLockCheck.setFocusable(false);
        row(section, newLockCheck);
        fieldRow(section, "Heading (deg)", newHeadingSpinner);
        lockHeight(section);
        return section;
    }

    private JPanel startSection() {
        JPanel section = section("Start pose");
        fieldRow(section, "Heading (deg)", startHeadingSpinner);
        lockHeight(section);
        return section;
    }

    private JPanel actionsSection() {
        JPanel section = section("Actions");
        clearButton.setFocusable(false);
        generateButton.setFocusable(false);
        copyButton.setFocusable(false);
        pasteButton.setFocusable(false);
        row(section, clearButton);
        row(section, generateButton);
        row(section, copyButton, pasteButton);
        lockHeight(section);
        return section;
    }

    private void wire() {
        targetButton.addActionListener(e -> modeSelected(Simulator.Mode.TARGET));
        obstacleButton.addActionListener(e -> modeSelected(Simulator.Mode.OBSTACLE));
        startButton.addActionListener(e -> modeSelected(Simulator.Mode.START));
        deselectButton.addActionListener(e -> sim.deselect());

        polygonCheck.addActionListener(e -> {
            if (!refreshing) sim.setPolygonObstacles(polygonCheck.isSelected());
        });
        sidesSpinner.addChangeListener(e -> {
            if (!refreshing) sim.setPolygonSides(intValue(sidesSpinner));
        });
        sizeSpinner.addChangeListener(e -> {
            if (!refreshing) sim.setObstacleRadius(doubleValue(sizeSpinner));
        });

        intakeSpinner.addChangeListener(e -> {
            if (!refreshing) sim.setIntakeWidth(doubleValue(intakeSpinner));
        });
        reorderCheck.addActionListener(e -> {
            if (!refreshing) sim.setReorder(reorderCheck.isSelected());
        });
        smoothingCheck.addActionListener(e -> {
            if (!refreshing) sim.setSmoothing(smoothingCheck.isSelected());
        });
        maxTargetsSpinner.addChangeListener(e -> {
            if (!refreshing) sim.setMaxTargets(intValue(maxTargetsSpinner));
        });

        lockSelectedCheck.addActionListener(e -> {
            if (!refreshing) sim.setSelectedHeadingLocked(lockSelectedCheck.isSelected());
        });
        selectedHeadingSpinner.addChangeListener(e -> {
            if (!refreshing && sim.hasSelectedTarget() && sim.isSelectedHeadingLocked()) {
                sim.setSelectedHeadingDegrees(doubleValue(selectedHeadingSpinner));
            }
        });
        turnLeftButton.addActionListener(e -> sim.nudgeSelectedHeading(-15));
        turnRightButton.addActionListener(e -> sim.nudgeSelectedHeading(15));

        newLockCheck.addActionListener(e -> {
            if (!refreshing) sim.setNewTargetLocked(newLockCheck.isSelected());
        });
        newHeadingSpinner.addChangeListener(e -> {
            if (!refreshing && sim.isNewTargetLocked()) {
                sim.setNewTargetHeadingDegrees(doubleValue(newHeadingSpinner));
            }
        });

        startHeadingSpinner.addChangeListener(e -> {
            if (!refreshing) sim.setStartHeadingDegrees(doubleValue(startHeadingSpinner));
        });

        clearButton.addActionListener(e -> sim.clearAll());
        generateButton.addActionListener(e -> sim.generateCode());
        copyButton.addActionListener(e -> sim.copyScenario());
        pasteButton.addActionListener(e -> sim.pasteScenario());
    }

    private void modeSelected(Simulator.Mode mode) {
        if (!refreshing) sim.setMode(mode);
    }

    void refresh() {
        refreshing = true;
        try {
            targetButton.setSelected(sim.getMode() == Simulator.Mode.TARGET);
            obstacleButton.setSelected(sim.getMode() == Simulator.Mode.OBSTACLE);
            startButton.setSelected(sim.getMode() == Simulator.Mode.START);

            polygonCheck.setSelected(sim.isPolygonObstacles());
            sidesSpinner.setValue(Integer.valueOf(sim.getPolygonSides()));
            sidesSpinner.setEnabled(sim.isPolygonObstacles());
            sizeSpinner.setValue(Double.valueOf(sim.getObstacleRadius()));

            intakeSpinner.setValue(Double.valueOf(sim.getIntakeWidth()));
            reorderCheck.setSelected(sim.isReorder());
            smoothingCheck.setSelected(sim.isSmoothing());
            maxTargetsSpinner.setValue(Integer.valueOf(sim.getMaxTargets()));

            boolean selected = sim.hasSelectedTarget();
            selectionLabel.setText(selected
                    ? "Target " + (sim.getSelectedTargetIndex() + 1) + " of " + sim.getTargetCount()
                    : "No target selected");
            boolean locked = selected && sim.isSelectedHeadingLocked();
            lockSelectedCheck.setEnabled(selected);
            lockSelectedCheck.setSelected(locked);
            selectedHeadingSpinner.setEnabled(locked);
            selectedHeadingSpinner.setValue(Double.valueOf(selected ? sim.getSelectedHeadingDegrees() : 0.0));
            turnLeftButton.setEnabled(selected);
            turnRightButton.setEnabled(selected);

            boolean newLocked = sim.isNewTargetLocked();
            newLockCheck.setSelected(newLocked);
            newHeadingSpinner.setEnabled(newLocked);
            newHeadingSpinner.setValue(Double.valueOf(sim.getNewTargetHeadingDegrees()));

            startHeadingSpinner.setValue(Double.valueOf(sim.getStartHeadingDegrees()));
        } finally {
            refreshing = false;
        }
    }

    private JPanel section(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(title));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    private void addSection(JPanel section) {
        add(section);
        add(Box.createVerticalStrut(4));
    }

    private void row(JPanel section, Component... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component component : components) {
            row.add(component);
        }
        section.add(row);
    }

    private void fieldRow(JPanel section, String label, JComponent field) {
        field.setPreferredSize(new Dimension(100, field.getPreferredSize().height));
        row(section, new JLabel(label), field);
    }

    private static void lockHeight(JPanel section) {
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, section.getPreferredSize().height));
    }

    private static double doubleValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    private static int intValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }
}
