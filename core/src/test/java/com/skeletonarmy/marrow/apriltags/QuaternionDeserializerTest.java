package com.skeletonarmy.marrow.apriltags;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class QuaternionDeserializerTest {
    private static final String VALID_QUATERNION_JSON = "{\"w\" : 0.6725937, \"x\" : -0.6725937, \"y\" : -0.2182149, \"z\" : 0.2182149, \"acquisitionTime\" : 3}";
    private static final String VALID_WRONG_ORDER_QUATERNION_JSON = "{\"x\": 1, \"y\": 2, \"z\": 3, \"w\": 4, \"acquisitionTime\": 5}";
    private static final String INVALID_TYPES_QUATERNION_JSON = "{\"x\" : \"0.6725937\", \"y\" : -0.6725937, \"z\" : -0.2182149, \"w\" : 0.2182149, \"acquisitionTime\" : 3}";
    private static final String INVALID_KEYS_QUATERNION_JSON = "{\"foo\" : 1, \"bar\" : 2, \"baz\" : 3, \"w\" : 0.2182149, \"acquisitionTime\" : 3}";
    private static final String INVALID_SIZE_QUATERNION_JSON = "{\"x\" : 0.6725937, \"y\" : -0.6725937, \"z\" : -0.2182149, \"w\" : 0.2182149}";

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Quaternion.class, new QuaternionDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void deserializesValidFiveElementArray() throws Exception {
        Quaternion result = mapper.readValue(
                "[0.6725937, -0.6725937, -0.2182149, 0.2182149, 0]", Quaternion.class);

        assertNotNull(result);
        assertEquals(0.6725937f, result.w, 1e-6);
        assertEquals(-0.6725937f, result.x, 1e-6);
        assertEquals(-0.2182149f, result.y, 1e-6);
        assertEquals(0.2182149f, result.z, 1e-6);
        assertEquals(0L, result.acquisitionTime);
    }

    @Test
    public void deserializesValidJsonObject() throws Exception {
        Quaternion result = mapper.readValue(VALID_QUATERNION_JSON, Quaternion.class);

        assertNotNull(result);
        assertEquals(0.6725937f, result.w, 1e-6);
        assertEquals(-0.6725937f, result.x, 1e-6);
        assertEquals(-0.2182149f, result.y, 1e-6);
        assertEquals(0.2182149f, result.z, 1e-6);
        assertEquals(3L, result.acquisitionTime);
    }

    @Test
    public void deserializesWronglyOrderedValidJsonObject() throws Exception {
        Quaternion result = mapper.readValue(VALID_WRONG_ORDER_QUATERNION_JSON, Quaternion.class);

        assertNotNull(result);
        assertEquals(1, result.x, 1e-6);
        assertEquals(2, result.y, 1e-6);
        assertEquals(3, result.z, 1e-6);
        assertEquals(4, result.w, 1e-6);
        assertEquals(5, result.acquisitionTime);
    }

    @Test
    public void deserializesNonZeroAcquisitionTime() throws Exception {
        Quaternion result = mapper.readValue("[1, 0, 0, 0, 123456789]", Quaternion.class);

        assertNotNull(result);
        assertEquals(123456789L, result.acquisitionTime);
    }

    @Test
    public void returnsNullForJsonNull() throws Exception {
        Quaternion result = mapper.readValue("null", Quaternion.class);
        assertTrue(FtcEquals.equals(Quaternion.identityQuaternion(), result));
    }

    @Test
    public void returnsNullForFourElementArray() throws Exception {
        // Missing acquisitionTime - a plausible mistake if someone forgets the 5th element
        Quaternion result = mapper.readValue("[1, 0, 0, 0]", Quaternion.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForSixElementArray() throws Exception {
        Quaternion result = mapper.readValue("[1, 0, 0, 0, 0, 0]", Quaternion.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForInvalidTypes() throws Exception {
        Quaternion result = mapper.readValue(INVALID_TYPES_QUATERNION_JSON, Quaternion.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForInvalidKeys() throws Exception {
        Quaternion result = mapper.readValue(INVALID_KEYS_QUATERNION_JSON, Quaternion.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForInvalidSize() throws Exception {
        Quaternion result = mapper.readValue(INVALID_SIZE_QUATERNION_JSON, Quaternion.class);
        assertNull(result);
    }
}
