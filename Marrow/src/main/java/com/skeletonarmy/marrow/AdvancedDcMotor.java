package com.skeletonarmy.marrow;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.frozenmilk.dairy.cachinghardware.CachingDcMotorEx;

/**
 * An advanced motor wrapper that enhances {@link DcMotorEx} by adding several useful features:
 * <ul>
 *     <li><b>Caching:</b> Improves performance by reducing repeated hardware calls through caching via {@link CachingDcMotorEx}.</li>
 *     <li><b>Custom PID Control:</b> Supports optional {@link PIDFController}-based custom control for precise positioning.</li>
 *     <li><b>Linked Motors:</b> Synchronizes one or more additional {@link DcMotorEx} instances with the primary motor.</li>
 *     <li><b>Ticks-to-Units Conversion:</b> Converts between encoder ticks and user-defined physical units like inches or degrees.</li>
 * </ul>
 */
public class AdvancedDcMotor extends CachingDcMotorEx {
    private static final List<AdvancedDcMotor> registeredMotors = new ArrayList<>(); // Static registry of all AdvancedDcMotors

    private final List<DcMotorEx> linkedMotors = new ArrayList<>();

    private PIDFController controller;
    private CustomPIDFController customPIDFController = null;
    private boolean useCustomPIDF;
    private boolean runningCustomPIDF = false;

    private double unitsPerTick = 0.0;

    private boolean autoUpdate = true; // Controls whether this motor auto-updates PID

    /**
     * Constructs an {@code AdvancedDcMotor} using the primary motor and optional linked motors.
     * The linked motors will mirror the behavior of the primary motor.
     *
     * @param primaryMotor the main motor
     * @param linkedMotors motors to mirror behavior of the primary motor (optional)
     */
    public AdvancedDcMotor(DcMotorEx primaryMotor, DcMotorEx... linkedMotors) {
        super(primaryMotor);
        if (linkedMotors != null) this.linkedMotors.addAll(Arrays.asList(linkedMotors));
        initialize();
        registeredMotors.add(this); // Register this instance
    }

    private void initialize() {
        setMode(RunMode.RUN_WITHOUT_ENCODER);
    }

    @Override
    public void setMode(RunMode mode) {
        super.setMode(mode);

        for (DcMotorEx motor : linkedMotors) {
            motor.setMode(mode);
        }

        runningCustomPIDF = false;
    }

    @Override
    public void setPower(double power) {
        super.setPower(power);

        for (DcMotorEx motor : linkedMotors) {
            motor.setPower(power);
        }
    }

    /**
     * Sets the motors to mirror the primary motor.
     *
     * @param motors the motors to be linked
     */
    public void setLinkedMotors(DcMotorEx... motors) {
        linkedMotors.clear();
        if (motors != null) linkedMotors.addAll(Arrays.asList(motors));
    }

    /**
     * Returns whether custom PIDF control is enabled.
     *
     * @return {@code true} if custom PIDF is used, {@code false} otherwise
     */
    public boolean getUseCustomPIDF() {
        return useCustomPIDF;
    }

    /**
     * Enables or disables custom PIDF control.
     *
     * @param enable {@code true} to enable, {@code false} to disable
     */
    public void setUseCustomPIDF(boolean enable) {
        useCustomPIDF = enable;
    }

    /**
     * Returns the internal {@link PIDFController} instance.
     *
     * @return the PIDF controller used for custom control
     */
    public PIDFController getPIDFController() {
        return controller;
    }

    /**
     * Sets the PIDF coefficients of the controller.
     *
     * @param kp proportional coefficient
     * @param ki integral coefficient
     * @param kd derivative coefficient
     * @param kf feedforward coefficient
     */
    public void setCustomPIDFCoefficients(double kp, double ki, double kd, double kf) {
        controller = new PIDFController(kp, ki, kd, kf);
    }

    /**
     * Sets the PIDF coefficients of the controller (no feedforward).
     *
     * @param kp proportional coefficient
     * @param ki integral coefficient
     * @param kd derivative coefficient
     */
    public void setCustomPIDCoefficients(double kp, double ki, double kd) {
        setCustomPIDFCoefficients(kp, ki, kd, 0);
    }

    /**
     * Sets the custom feedforward callback.
     *
     * @param controller the custom feedforward implementation
     */
    public void setCustomPIDFController(CustomPIDFController controller) {
        this.customPIDFController = controller;
    }

    /**
     * Updates the PIDF controller and sets motor power based on the target position.
     * Requires RUN_TO_POSITION and custom PIDF to be enabled.
     * Throws {@link RuntimeException} if coefficients are not initialized.
     */
    public void update() {
        fakeRunToPosition();

        if (runningCustomPIDF) {
            throw new RuntimeException("PID coefasdasdasdasdasdficients not set on AdvancedDcMotor. Please set them using setCustomPIDCoefficients() or setCustomPIDFCoefficients().");

//            if (controller == null) {
//                throw new RuntimeException("PID coefficients not set on AdvancedDcMotor. Please set them using setCustomPIDCoefficients() or setCustomPIDFCoefficients().");
//            }
//
//            int pos = getCurrentPosition();
//            int target = getTargetPosition();
//
//            double power;
//
//            if (customPIDFController != null) {
//                // Custom specified PIDF calculation
//                power = customPIDFController.calculate(this, target);
//            } else {
//                // Default PIDF calculation
//                power = controller.calculate(pos, target);
//            }
//
//            setPower(power);
        }
    }

    /**
     * When custom PIDF is enabled and the motor is set to RUN_TO_POSITION, this method silently switches the mode to RUN_WITHOUT_ENCODER
     * so we can manually control the motor and fake as if we are using RUN_TO_POSITION.
     * <p>
     * Reverts back to RUN_TO_POSITION if custom PIDF is disabled.
     */
    private void fakeRunToPosition() {
        if (runningCustomPIDF && !useCustomPIDF) {
            setMode(RunMode.RUN_TO_POSITION);
            runningCustomPIDF = false;
        }

        if (useCustomPIDF && getMode() == RunMode.RUN_TO_POSITION) {
            runningCustomPIDF = true;
            setMode(RunMode.RUN_WITHOUT_ENCODER);
        }
    }

    // === Ticks-to-units conversion ===

    /**
     * Sets the conversion factor from encoder ticks to user-defined units.
     *
     * @param unitsPerTick conversion factor
     */
    public void setUnitsPerTick(double unitsPerTick) {
        this.unitsPerTick = unitsPerTick;
    }

    /**
     * Sets the conversion factor from user-defined units to encoder ticks.
     *
     * @param ticksPerUnit conversion factor (will be inverted)
     */
    public void setTicksPerUnit(double ticksPerUnit) {
        this.unitsPerTick = ticksPerUnit == 0 ? 0 : 1.0 / ticksPerUnit;
    }

    /**
     * Returns the number of user-defined units per encoder tick.
     *
     * @return units per tick
     */
    public double getUnitsPerTick() {
        return unitsPerTick;
    }

    /**
     * Returns the number of encoder ticks per user-defined unit.
     *
     * @return ticks per unit
     */
    public double getTicksPerUnit() {
        return unitsPerTick == 0 ? 0 : 1.0 / unitsPerTick;
    }

    /**
     * Converts encoder ticks to user-defined units.
     *
     * @param ticks the number of encoder ticks
     * @return equivalent distance in units
     */
    public double ticksToUnits(int ticks) {
        return ticks * unitsPerTick;
    }

    /**
     * Converts user-defined units to encoder ticks.
     *
     * @param units the distance in units
     * @return equivalent number of encoder ticks
     */
    public int unitsToTicks(double units) {
        return unitsPerTick == 0 ? 0 : (int) Math.round(units / unitsPerTick);
    }

    /**
     * Controls whether this motor updates its PID controller automatically
     * when {@link #updateAll()} is called.
     *
     * @param autoUpdate true to enable automatic update, false to disable
     */
    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    /**
     * Stops auto updating and removes this motor from the global registry.
     * Call this if you want to clean up.
     */
    public void unregister() {
        registeredMotors.remove(this);
    }

    /**
     * Updates the PID controller of all registered motors that have auto-update enabled.
     * Call this once per loop to update all motors automatically.
     */
    public static void updateAll() {
        for (AdvancedDcMotor motor : registeredMotors) {
            if (motor.autoUpdate) {
                motor.update();
            }
        }
    }

    @FunctionalInterface
    public interface CustomPIDFController {
        /**
         * Calculate motor power based on the motor and target position.
         * @param motor the AdvancedDcMotor instance
         * @param target the target position in ticks
         * @return the power output (PID + feedforward)
         */
        double calculate(AdvancedDcMotor motor, int target);
    }
}
