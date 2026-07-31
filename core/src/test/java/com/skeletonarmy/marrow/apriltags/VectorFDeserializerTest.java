package com.skeletonarmy.marrow.apriltags;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class VectorFDeserializerTest {
    private static final String VALID_VECTOR_JSON = "{\"x\" : 1.5, \"y\" : -2.25, \"z\" : 3.0}";
    private static final String VALID_WRONG_ORDER_VECTOR_JSON = "{\"z\" : 1, \"y\" : 2, \"x\" : 3}";
    private static final String INVALID_TYPES_VECTOR_JSON = "{\"x\" : \"1.5\", \"y\" : -2.25, \"z\" : 3.0}";
    private static final String INVALID_KEYS_VECTOR_JSON = "{\"foo\" : 1, \"bar\" : 2, \"baz\" : 3, \"x\" : 1.5, \"y\" : -2.25, \"z\" : 3.0}";
    private static final String INVALID_SIZE_VECTOR_JSON = "{\"x\" : 1.5, \"y\" : -2.25}";

    private ObjectMapper mapper;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(VectorF.class, new VectorFDeserializer());
        mapper.registerModule(module);
    }

    @Test
    public void deserializesValidThreeElementArray() throws Exception {
        VectorF result = mapper.readValue("[1.5, -2.25, 3.0]", VectorF.class);

        assertNotNull(result);
        assertEquals(1.5f, result.get(0), 1e-6);
        assertEquals(-2.25f, result.get(1), 1e-6);
        assertEquals(3.0f, result.get(2), 1e-6);
    }

    @Test
    public void deserializesIntegerValuedArray() throws Exception {
        // Jackson should coerce ints to doubles fine via asDouble()
        VectorF result = mapper.readValue("[-82, 79, 29]", VectorF.class);

        assertNotNull(result);
        assertEquals(-82f, result.get(0), 1e-6);
        assertEquals(79f, result.get(1), 1e-6);
        assertEquals(29f, result.get(2), 1e-6);
    }

    @Test
    public void returnsNullForJsonNull() throws Exception {
        VectorF result = mapper.readValue("null", VectorF.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForTooFewElements() throws Exception {
        VectorF result = mapper.readValue("[1.0, 2.0]", VectorF.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForTooManyElements() throws Exception {
        VectorF result = mapper.readValue("[1.0, 2.0, 3.0, 4.0]", VectorF.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForEmptyArray() throws Exception {
        VectorF result = mapper.readValue("[]", VectorF.class);
        assertNull(result);
    }

    @Test
    public void deserializesValidJsonObject() throws Exception {
        VectorF result = mapper.readValue(VALID_VECTOR_JSON, VectorF.class);
        assertTrue(FtcEquals.equals(new VectorF(1.5f, -2.25f, 3.0f), result));
    }

    @Test
    public void deserializesWronglyOrderedValidJsonObject() throws Exception {
        VectorF result = mapper.readValue(VALID_WRONG_ORDER_VECTOR_JSON, VectorF.class);
        assertTrue(FtcEquals.equals(new VectorF(3, 2, 1), result));
    }

    @Test
    public void returnsNullForInvalidTypes() throws Exception {
        VectorF result = mapper.readValue(INVALID_TYPES_VECTOR_JSON, VectorF.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForInvalidKeys() throws Exception {
        VectorF result = mapper.readValue(INVALID_KEYS_VECTOR_JSON, VectorF.class);
        assertNull(result);
    }

    @Test
    public void returnsNullForInvalidSize() throws Exception {
        VectorF result = mapper.readValue(INVALID_SIZE_VECTOR_JSON, VectorF.class);
        assertNull(result);
    }
}
