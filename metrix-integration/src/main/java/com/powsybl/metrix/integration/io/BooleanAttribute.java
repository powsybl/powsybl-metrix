/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.io;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import com.google.common.primitives.Booleans;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
@JsonDeserialize(using = BooleanAttribute.JsonDeserializer.class)
public class BooleanAttribute implements Attribute {

    static final AttributeType TYPE = AttributeType.BOOLEAN;

    private final String name;

    private final boolean[] values;

    BooleanAttribute(String name, boolean[] values) {
        this.name = Objects.requireNonNull(name);
        this.values = Objects.requireNonNull(values).clone();
    }

    BooleanAttribute(String name, int value) {
        this.name = Objects.requireNonNull(name);
        values = new boolean[value];
        Arrays.fill(values, false);
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
        values[j] = is.readInt() == 1;
    }

    @Override
    public void write(LittleEndianDataOutputStream os) throws IOException {
        for (boolean value : values) {
            os.writeInt(value ? 1 : 0);
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

    public boolean[] getValues() {
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
        for (boolean value : values) {
            generator.writeString(Boolean.toString(value));
        }
    }

    public static class JsonDeserializer extends StdDeserializer<BooleanAttribute> {

        public JsonDeserializer() {
            this(BooleanAttribute.class);
        }

        public JsonDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public BooleanAttribute deserialize(JsonParser jp, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(jp);
            String name = node.get("name").asText();
            List<Boolean> values = new ArrayList<>();
            node.get("values").iterator().forEachRemaining(i -> values.add(Boolean.parseBoolean(i.asText())));
            return new BooleanAttribute(name, Booleans.toArray(values));
        }
    }

}
