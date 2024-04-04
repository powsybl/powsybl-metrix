/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import com.google.common.primitives.Floats;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(using = FloatAttribute.JsonDeserializer.class)
class FloatAttribute implements Attribute {

    static final AttributeType TYPE = AttributeType.FLOAT;

    private final String name;

    private final float[] values;

    FloatAttribute(String name, float[] values) {
        this.name = Objects.requireNonNull(name);
        this.values = Objects.requireNonNull(values).clone();
    }

    FloatAttribute(String name, int value) {
        this.name = Objects.requireNonNull(name);
        values = new float[value];
        Arrays.fill(values, Float.NaN);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AttributeType getType() {
        return TYPE;
    }

    @Override
    public void read(LittleEndianDataInputStream is, int i, int j) throws IOException {
        values[j] = is.readFloat();
    }

    @Override
    public void write(LittleEndianDataOutputStream os) throws IOException {
        for (float value : values) {
            os.writeFloat(value);
        }
    }

    @Override
    public int getFirstIndexMaxValue() {
        return values.length;
    }

    @Override
    public int getSecondIndexMaxValue() {
        return 1;
    }

    @Override
    public int getValueCount() {
        return values.length;
    }

    @Override
    public int getSize() {
        return values.length * Float.BYTES;
    }

    public float[] getValues() {
        return values.clone();
    }

    @Override
    public void print(PrintStream out) {
        for (int i = 0; i < values.length; i++) {
            out.print(values[i]);
            if (i < values.length - 1) {
                out.print(", ");
            }
        }
    }

    @Override
    public void writeJson(JsonGenerator generator) throws IOException {
        for (float value : values) {
            generator.writeNumber(value);
        }
    }

    public static class JsonDeserializer extends StdDeserializer<FloatAttribute> {

        public JsonDeserializer() {
            this(null);
        }

        public JsonDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public FloatAttribute deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            String name = node.get("name").asText();
            List<Float> values = new ArrayList<>();
            node.get("values").elements().forEachRemaining(i -> values.add(i.floatValue()));
            return new FloatAttribute(name, Floats.toArray(values));
        }
    }
}
