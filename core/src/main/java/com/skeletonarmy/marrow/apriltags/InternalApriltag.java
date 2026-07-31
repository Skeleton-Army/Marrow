package com.skeletonarmy.marrow.apriltags;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;

//This class is almost identical to the desired AprilTagMetadata class, but needed for the custom deserializer.
class InternalApriltag {
    private int id;
    private String name;
    private double size;
    @JsonDeserialize(using = VectorFDeserializer.class)
    private VectorF position = null;
    @JsonDeserialize(using = DistanceUnitDeserializer.class)
    private DistanceUnit unit;
    @JsonDeserialize(using = QuaternionDeserializer.class)
    private Quaternion orientation = null;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getSize() { return size; }
    public void setSize(double size) { this.size = size; }
    public VectorF getPosition() { return position; }
    public void setPosition(VectorF position) { this.position = position; }
    public DistanceUnit getUnit() { return unit; }
    public void setUnit(DistanceUnit unit) { this.unit = unit; }
    public Quaternion getOrientation() { return orientation; }
    public void setOrientation(Quaternion orientation) { this.orientation = orientation; }

    public AprilTagMetadata toAprilTagMetadata() {
        if (position == null && orientation == null) {
            return new AprilTagMetadata(
                    id,
                    name,
                    size,
                    unit
            );
        }

        if (position == null) {
            return new AprilTagMetadata(
                    id,
                    name,
                    size,
                    new VectorF(0,0,0),
                    unit,
                    orientation
            );
        }

        if (orientation == null) {
            return new AprilTagMetadata(
                    id,
                    name,
                    size,
                    position,
                    unit,
                    Quaternion.identityQuaternion()
            );
        }


        return new AprilTagMetadata(
                id,
                name,
                size,
                position,
                unit,
                orientation
        );
    }
}
