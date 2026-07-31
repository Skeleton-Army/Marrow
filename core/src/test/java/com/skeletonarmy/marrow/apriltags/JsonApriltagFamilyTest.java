package com.skeletonarmy.marrow.apriltags;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class JsonApriltagFamilyTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String VALID_JSON_FULL = "[{\"id\":24,\"name\":\"RedTarget\",\"size\":6.5,\"position\":[-82.372696,79.6425,29.5],\"unit\":3,\"orientation\":[0.6725937,-0.6725937,-0.2182149,0.2182149,0]},{\"id\":21,\"name\":\"Obelisk_GPP\",\"size\":6.5,\"position\":null,\"unit\":3,\"orientation\":null},{\"id\":20,\"name\":\"BlueTarget\",\"size\":6.5,\"position\":{\"x\":-82.372696,\"y\":-78.6425,\"z\":29.5},\"unit\":\"inch\",\"orientation\":{\"w\":0.6725937,\"x\":-0.6725937,\"y\":-0.2182149,\"z\":0.2182149,\"acquisitionTime\":0}}]";
    private static final String VALID_JSON_MINIMAL = "[\n {\n \"id\": 22,\n \"name\": \"Obelisk_PGP\",\n \"size\": 6.5,\n \"unit\": 3\n }\n]";

    private void writeFile(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void loadsValidFullJsonIntoLibraryWithCorrectTags() throws IOException {
        File jsonFile = tempFolder.newFile("tags.json");
        writeFile(jsonFile, VALID_JSON_FULL);

        JsonApriltagFamily family = new JsonApriltagFamily(jsonFile.getAbsolutePath());
        AprilTagLibrary library = family.getFamily();

        AprilTagMetadata blueTarget = library.lookupTag(20);
        assertNotNull(blueTarget);
        assertEquals("BlueTarget", blueTarget.name);
        assertEquals(6.5, blueTarget.tagsize, 1e-9);
        assertEquals(DistanceUnit.INCH, blueTarget.distanceUnit);
        assertNotNull(blueTarget.fieldPosition);
        assertNotNull(blueTarget.fieldOrientation);

        AprilTagMetadata redTarget = library.lookupTag(24);
        assertNotNull(redTarget);
        assertEquals("RedTarget", redTarget.name);
        assertEquals(6.5, redTarget.tagsize, 1e-9);
        assertEquals(DistanceUnit.INCH, redTarget.distanceUnit);
        assertNotNull(redTarget.fieldPosition);
        assertNotNull(redTarget.fieldOrientation);

        AprilTagMetadata obelisk = library.lookupTag(21);
        assertNotNull(obelisk);
        assertEquals("Obelisk_GPP", obelisk.name);
    }

    @Test
    public void loadsValidMinimalJsonIntoLibraryWithCorrectTags() throws IOException {
        File jsonFile = tempFolder.newFile("tags.json");
        writeFile(jsonFile, VALID_JSON_MINIMAL);

        JsonApriltagFamily family = new JsonApriltagFamily(jsonFile.getAbsolutePath());
        AprilTagLibrary library = family.getFamily();

        AprilTagMetadata obelisk = library.lookupTag(22);
        assertNotNull(obelisk);
        assertEquals("Obelisk_PGP", obelisk.name);
        assertNotNull(obelisk.fieldPosition);
        assertNotNull(obelisk.fieldOrientation);
    }

    @Test
    public void returnsNullForUnknownTagId() throws IOException {
        File jsonFile = tempFolder.newFile("tags.json");
        writeFile(jsonFile, VALID_JSON_FULL);

        JsonApriltagFamily family = new JsonApriltagFamily(jsonFile.getAbsolutePath());
        AprilTagLibrary library = family.getFamily();

        assertNull(library.lookupTag(999));
    }

    @Test
    public void handlesMissingFileWithoutThrowing() {
        File missingFile = new File(tempFolder.getRoot(), "does-not-exist.json");

        JsonApriltagFamily family = new JsonApriltagFamily(missingFile.getAbsolutePath());

        // Should log a warning internally and produce an empty library rather than throw.
        // (An uncaught exception here would fail this test on its own, no wrapper needed.)
        AprilTagLibrary library = family.getFamily();
        assertNull(library.lookupTag(20));
    }

    @Test
    public void handlesMalformedJsonWithoutThrowing() throws IOException {
        File jsonFile = tempFolder.newFile("bad.json");
        writeFile(jsonFile, "{ this is not valid json ][");

        JsonApriltagFamily family = new JsonApriltagFamily(jsonFile.getAbsolutePath());

        AprilTagLibrary library = family.getFamily();
        assertNull(library.lookupTag(20));
    }

    @Test
    public void handlesEmptyArrayWithoutThrowing() throws IOException {
        File jsonFile = tempFolder.newFile("empty.json");
        writeFile(jsonFile, "[]");

        JsonApriltagFamily family = new JsonApriltagFamily(jsonFile.getAbsolutePath());
        AprilTagLibrary library = family.getFamily();

        assertNull(library.lookupTag(20));
    }

    @Test
    public void getFamilyIsIdempotentAcrossMultipleCalls() throws IOException {
        File jsonFile = tempFolder.newFile("tags.json");
        writeFile(jsonFile, VALID_JSON_FULL);

        JsonApriltagFamily family = new JsonApriltagFamily(jsonFile.getAbsolutePath());

        AprilTagLibrary first = family.getFamily();
        AprilTagLibrary second = family.getFamily();

        assertNotNull(first.lookupTag(20));
        assertNotNull(second.lookupTag(20));
    }
}
