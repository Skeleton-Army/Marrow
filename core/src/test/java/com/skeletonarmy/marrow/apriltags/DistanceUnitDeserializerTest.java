package com.skeletonarmy.marrow.apriltags;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DistanceUnitDeserializerTest {

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(DistanceUnit.class, new DistanceUnitDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void mapsOrdinalZeroToMeter() throws Exception {
        assertEquals(DistanceUnit.METER, mapper.readValue("0", DistanceUnit.class));
    }

    @Test
    public void mapsOrdinalOneToCm() throws Exception {
        assertEquals(DistanceUnit.CM, mapper.readValue("1", DistanceUnit.class));
    }

    @Test
    public void mapsOrdinalTwoToMm() throws Exception {
        assertEquals(DistanceUnit.MM, mapper.readValue("2", DistanceUnit.class));
    }

    @Test
    public void mapsOrdinalThreeToInch() throws Exception {
        assertEquals(DistanceUnit.INCH, mapper.readValue("3", DistanceUnit.class));
    }

    @Test
    public void mapsTextualValueToMeter() throws Exception {
        DistanceUnit resultLong = mapper.readValue("\"meter\"", DistanceUnit.class);
        DistanceUnit resultShort = mapper.readValue("\"m\"", DistanceUnit.class);
        assertEquals(DistanceUnit.METER, resultLong);
        assertEquals(DistanceUnit.METER, resultShort);
    }

    @Test
    public void mapsTextualValueToCm() throws Exception {
        DistanceUnit resultLong = mapper.readValue("\"centimeter\"", DistanceUnit.class);
        DistanceUnit resultShort = mapper.readValue("\"cm\"", DistanceUnit.class);
        assertEquals(DistanceUnit.CM, resultLong);
        assertEquals(DistanceUnit.CM, resultShort);
    }

    @Test
    public void mapsTextualValueToMm() throws Exception {
        DistanceUnit resultLong = mapper.readValue("\"millimeter\"", DistanceUnit.class);
        DistanceUnit resultShort = mapper.readValue("\"mm\"", DistanceUnit.class);
        assertEquals(DistanceUnit.MM, resultLong);
        assertEquals(DistanceUnit.MM, resultShort);
    }

    @Test
    public void mapsTextualValueToInch() throws Exception {
        DistanceUnit resultLong = mapper.readValue("\"inch\"", DistanceUnit.class);
        DistanceUnit resultShort = mapper.readValue("\"in\"", DistanceUnit.class);
        assertEquals(DistanceUnit.INCH, resultLong);
        assertEquals(DistanceUnit.INCH, resultShort);
    }

    @Test
    public void fallsBackToInchForOutOfRangePositiveOrdinal() throws Exception {
        DistanceUnit result = mapper.readValue("99", DistanceUnit.class);
        assertEquals(DistanceUnit.INCH, result);
    }

    @Test
    public void fallsBackToInchForNegativeOrdinal() throws Exception {
        DistanceUnit result = mapper.readValue("-1", DistanceUnit.class);
        assertEquals(DistanceUnit.INCH, result);
    }

    @Test
    public void fallsBackToInchForJsonNull() throws Exception {
        DistanceUnit result = mapper.readValue("null", DistanceUnit.class);
        assertEquals(DistanceUnit.INCH, result);
    }

    @Test
    public void fallsBackToInchForNonUnitsTextualValue() throws Exception {
        DistanceUnit result = mapper.readValue("\"foo\"", DistanceUnit.class);
        assertEquals(DistanceUnit.INCH, result);
    }

    @Test
    public void fallsBackToInchForArrayNode() throws Exception {
        DistanceUnit result = mapper.readValue("[1, 2, 3]", DistanceUnit.class);
        assertEquals(DistanceUnit.INCH, result);
    }

    @Test
    public void truncatesFractionalOrdinalRatherThanRounding() throws Exception {
        // node.asInt(3) truncates (1.9 -> 1), it does not round. This documents current
        // behavior, which could silently mis-map a value like 1.9 to CM instead of MM.
        DistanceUnit result = mapper.readValue("1.9", DistanceUnit.class);
        assertEquals(DistanceUnit.CM, result);
    }
}
