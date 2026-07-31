package com.skeletonarmy.marrow.apriltags;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;

import java.io.IOException;

public class QuaternionDeserializer extends JsonDeserializer<Quaternion> {
    @Override
    public Quaternion deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        float w = Float.NaN, x = Float.NaN, y = Float.NaN, z = Float.NaN;
        long acquisitionTime = Long.MIN_VALUE; //Assumes acquisition time won't be -29 years

        if (node == null || node.isNull() || node.size() != 5) return null;

        if (node.isArray()) {
            w = (float) node.get(0).asDouble();
            x = (float) node.get(1).asDouble();
            y = (float) node.get(2).asDouble();
            z = (float) node.get(3).asDouble();
            acquisitionTime = node.get(4).asLong();
        }

        if (isJsonObjQuaternion(node)) {
            w = (float) node.get("w").asDouble();
            x = (float) node.get("x").asDouble();
            y = (float) node.get("y").asDouble();
            z = (float) node.get("z").asDouble();
            acquisitionTime = node.get("acquisitionTime").asLong();
        }

        if (Double.isNaN(w) || Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || acquisitionTime == Long.MIN_VALUE) {
            return null;
        }

        return new Quaternion(w, x, y, z, acquisitionTime);
    }

    public Quaternion getNullValue(DeserializationContext ctxt) {
        return Quaternion.identityQuaternion();
    }

    private boolean isJsonObjQuaternion(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) return false;
        boolean structure = node.has("w") &&
                            node.has("x") &&
                            node.has("y") &&
                            node.has("z") &&
                            node.has("acquisitionTime");

        boolean types;
        if (structure) {
            types = node.get("w").isNumber() &&
                    node.get("x").isNumber() &&
                    node.get("y").isNumber() &&
                    node.get("z").isNumber();
        } else {
            types = false;
        }

        return structure && types;
    }
}
