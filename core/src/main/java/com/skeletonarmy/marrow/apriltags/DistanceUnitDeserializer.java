package com.skeletonarmy.marrow.apriltags;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import java.io.IOException;
import java.util.Locale;

public class DistanceUnitDeserializer extends JsonDeserializer<DistanceUnit> {
    @Override
    public DistanceUnit deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);

        if (node == null || node.isNull() || !(node.isNumber() || node.isTextual())) {
            return DistanceUnit.INCH; //To my knowledge, most teams use inches. So I'm going with inches on error.
        }

        if (node.isNumber()) {
            switch (node.asInt()) {
                case 0:
                    return DistanceUnit.METER;
                case 1:
                    return DistanceUnit.CM;
                case 2:
                    return DistanceUnit.MM;
                default:
                    return DistanceUnit.INCH;
            }
        }

        //To help make JSON clearer to read
        switch (node.asText().toLowerCase()) {
            case "m":
            case "meter":
                return DistanceUnit.METER;
            case "cm":
            case "centimeter":
                return DistanceUnit.CM;
            case "mm":
            case "millimeter":
                return DistanceUnit.MM;
            default:
                return DistanceUnit.INCH;
        }

    }

    @Override
    public DistanceUnit getNullValue(DeserializationContext ctxt) {
        return DistanceUnit.INCH;
    }
}
