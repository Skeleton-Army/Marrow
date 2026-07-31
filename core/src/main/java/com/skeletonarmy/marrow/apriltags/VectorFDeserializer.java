package com.skeletonarmy.marrow.apriltags;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.firstinspires.ftc.robotcore.external.matrices.VectorF;

import java.io.IOException;

public class VectorFDeserializer extends JsonDeserializer<VectorF> {
    @Override
    public VectorF deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        double x = Double.NaN, y = Double.NaN, z = Double.NaN;

        if (node == null || node.isNull() || node.size() != 3) {
            return null;
        }

        if (node.isArray()) {
            x = node.get(0).asDouble();
            y = node.get(1).asDouble();
            z = node.get(2).asDouble();
        }

        if (isJsonObjVector(node)) {
            x = node.get("x").asDouble();
            y = node.get("y").asDouble();
            z = node.get("z").asDouble();
        }

        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return null;
        return new VectorF((float) x, (float) y, (float) z);
    }

    private boolean isJsonObjVector(JsonNode node) {
        if (node == null || node.isNull() || !node.isObject()) return false;

        boolean structure = node.has("x") &&
                            node.has("y") &&
                            node.has("z");

        boolean types;
        if (structure){
            types = node.get("x").isNumber() &&
                    node.get("y").isNumber() &&
                    node.get("z").isNumber();
        } else {
           types = false;
        }

        return structure && types;
    }
}
