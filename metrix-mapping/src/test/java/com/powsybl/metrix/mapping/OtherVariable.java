package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Created by marifunf on 26/02/19.
 */
public enum OtherVariable implements MappingVariable {
    otherVariable1("otherVariable1"),
    otherVariable2("otherVariable2");

    private static final String NAME = "other";

    static String getName() {
        return NAME;
    }

    @Override
    public String getFieldName() {
        return getName();
    }

    static void writeJson(OtherVariable variable, JsonGenerator generator) throws IOException {
        Objects.requireNonNull(generator);
        try {
            generator.writeStartObject();
            generator.writeFieldName(variable.getFieldName());
            generator.writeString(variable.getVariableName());
            generator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static MappingVariable parseJson(JsonParser parser) throws IOException {
        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            if (token == JsonToken.VALUE_STRING) {
                return OtherVariable.valueOf(parser.getValueAsString());
            } else {
                throw new IllegalStateException("Unexpected JSON token: " + token);
            }
        }
        throw new IllegalStateException("Invalid OtherVariable JSON");
    }

    private final String variable;

    OtherVariable(String variable) {
        this.variable = variable;
    }

    @Override
    public String getVariableName() {
        return variable;
    }
}
