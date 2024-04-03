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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(using = IntAttribute.JsonDeserializer.class)
class IntAttribute implements Attribute {

    static final AttributeType TYPE = AttributeType.INTEGER;

    private final String name;

    private final int[] values;

    IntAttribute(String name, int[] values) {
        this.name = Objects.requireNonNull(name);
        this.values = Objects.requireNonNull(values).clone();
    }

    IntAttribute(String name, int value) {
        this.name = Objects.requireNonNull(name);
        this.values = new int[value];
        Arrays.fill(values, -1);

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
        values[j] = is.readInt();
    }

    @Override
    public void write(LittleEndianDataOutputStream os) throws IOException {
        for (int value : values) {
            os.writeInt(value);
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
        return values.length * Integer.BYTES;
    }

    public int[] getValues() {
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
        for (int value : values) {
            generator.writeNumber(value);
        }
    }

    public static class JsonDeserializer extends StdDeserializer<IntAttribute> {

        public JsonDeserializer() {
            this(null);
        }

        public JsonDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public IntAttribute deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            String name = node.get("name").asText();
            List<Integer> values = new ArrayList<>();
            node.get("values").elements().forEachRemaining(i -> values.add(i.intValue()));
            return new IntAttribute(name, values.stream().mapToInt(i -> i).toArray());
        }
    }

}
