package com.skeletonarmy.marrow.apriltags;

import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;
import org.junit.Test;

import static org.junit.Assert.*;

public class InternalApriltagTest {

    @Test
    public void buildsFullMetadataWhenPositionAndOrientationPresent() {
        InternalApriltag tag = new InternalApriltag();
        tag.setId(20);
        tag.setName("BlueTarget");
        tag.setSize(6.5);
        tag.setUnit(DistanceUnit.INCH);
        tag.setPosition(new VectorF(-82.372696f, -78.6425f, 29.5f));
        tag.setOrientation(new Quaternion(
                0.6725937f, -0.6725937f, -0.2182149f, 0.2182149f, 0));

        AprilTagMetadata metadata = tag.toAprilTagMetadata();

        assertEquals(20, metadata.id);
        assertEquals("BlueTarget", metadata.name);
        assertEquals(6.5, metadata.tagsize, 1e-9);
        assertEquals(DistanceUnit.INCH, metadata.distanceUnit);
        assertNotNull(metadata.fieldPosition);
        assertNotNull(metadata.fieldOrientation);
        assertEquals(-82.372696f, metadata.fieldPosition.get(0), 1e-3);
    }

    @Test
    public void fallsBackToSimpleMetadataWhenPositionIsNull() {
        InternalApriltag tag = new InternalApriltag();
        tag.setId(23);
        tag.setName("Obelisk_PPG");
        tag.setSize(6.5);
        tag.setUnit(DistanceUnit.INCH);
        tag.setPosition(null);
        tag.setOrientation(null);

        AprilTagMetadata metadata = tag.toAprilTagMetadata();

        assertEquals(23, metadata.id);
        assertEquals("Obelisk_PPG", metadata.name);
        assertEquals(DistanceUnit.INCH, metadata.distanceUnit);
        assertTrue(FtcEquals.equals(new VectorF(0,0,0), metadata.fieldPosition));
        assertTrue(FtcEquals.equals(Quaternion.identityQuaternion(), metadata.fieldOrientation));
    }

    @Test
    public void fallsBackToSimpleMetadataWhenOnlyOrientationIsNull() {
        // Position present but orientation missing - current code treats this the
        // same as "both missing" since toAprilTagMetadata() checks with OR.
        VectorF tagPose = new VectorF(-82.372696f, 79.6425f, 29.5f);
        InternalApriltag tag = new InternalApriltag();
        tag.setId(24);
        tag.setName("RedTarget");
        tag.setSize(6.5);
        tag.setUnit(DistanceUnit.INCH);
        tag.setPosition(tagPose);
        tag.setOrientation(null);

        AprilTagMetadata metadata = tag.toAprilTagMetadata();

        assertTrue(FtcEquals.equals(tagPose, metadata.fieldPosition));
        assertTrue(FtcEquals.equals(Quaternion.identityQuaternion(), metadata.fieldOrientation));
    }

    @Test
    public void fallsBackToSimpleMetadataWhenOnlyPositionIsNull() {
        InternalApriltag tag = new InternalApriltag();
        tag.setId(25);
        tag.setName("Partial");
        tag.setSize(6.5);
        tag.setUnit(DistanceUnit.INCH);
        tag.setPosition(null);
        tag.setOrientation(Quaternion.identityQuaternion());

        AprilTagMetadata metadata = tag.toAprilTagMetadata();

        assertTrue(FtcEquals.equals(new VectorF(0,0,0), metadata.fieldPosition));
        assertTrue(FtcEquals.equals(Quaternion.identityQuaternion(), metadata.fieldOrientation));
    }
}
