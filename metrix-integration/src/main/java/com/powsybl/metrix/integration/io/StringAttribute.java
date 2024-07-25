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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
@JsonDeserialize(using = StringAttribute.JsonDeserializer.class)
class StringAttribute implements Attribute {

    static final AttributeType TYPE = AttributeType.STRING;

    private static final byte BLANK_BYTE = Character.toString(' ').getBytes(StandardCharsets.UTF_8)[0];

    private final String name;

    private final byte[][] data;

    private String[] values;

    private static byte[][] newByteArray(int val1, int val2) {
        byte[][] data = new byte[val2][val1];
        for (byte[] datum : data) {
            Arrays.fill(datum, BLANK_BYTE);
        }
        return data;
    }

    private static String[] toStringArray(byte[][] data) {
        String[] values = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            values[i] = new String(data[i], StandardCharsets.UTF_8).trim();
        }
        return values;
    }

    private static byte[][] toByteArray(String[] values) {
        int n1 = getSecondIndexMaxValue(values);
        int n2 = getFirstIndexMaxValue(values);
        byte[][] data = new byte[n1][n2];
        for (int i = 0; i < n1; i++) {
            byte[] bytes = values[i].getBytes(StandardCharsets.UTF_8);
            System.arraycopy(bytes, 0, data[i], 0, bytes.length);
            Arrays.fill(data[i], bytes.length, n2, BLANK_BYTE);
        }
        return data;
    }

    StringAttribute(String name, int val1, int val2) {
        this.name = Objects.requireNonNull(name);
        data = newByteArray(val1, val2);
    }

    StringAttribute(String name, String[] values) {
        this.name = Objects.requireNonNull(name);
        data = toByteArray(values);
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
        data[i][j] = is.readByte();
    }

    @Override
    public void write(LittleEndianDataOutputStream os) throws IOException {
        for (byte[] datum : data) {
            os.write(datum);
        }
    }

    private static int getFirstIndexMaxValue(String[] values) {
        return Arrays.stream(values).map(s -> s.getBytes(StandardCharsets.UTF_8).length).max(Integer::compare).orElse(0);
    }

    @Override
    public int getFirstIndexMaxValue() {
        return getFirstIndexMaxValue(getValues());
    }

    private static int getSecondIndexMaxValue(String[] values) {
        return values.length;
    }

    @Override
    public int getSecondIndexMaxValue() {
        return getSecondIndexMaxValue(getValues());
    }

    @Override
    public int getValueCount() {
        return getFirstIndexMaxValue() * getSecondIndexMaxValue();
    }

    @Override
    public int getSize() {
        return getValueCount();
    }

    public String[] getValues() {
        if (values == null) {
            values = toStringArray(data);
        }
        return values.clone();
    }

    @Override
    public void print(PrintStream out) {
        for (int i = 0; i < getValues().length; i++) {
            out.print(getValues()[i]);
            if (i < values.length - 1) {
                out.print(", ");
            }
        }
    }

    @Override
    public void writeJson(JsonGenerator generator) throws IOException {
        for (int i = 0; i < getValues().length; i++) {
            generator.writeString(getValues()[i]);
        }
    }

    public static class JsonDeserializer extends StdDeserializer<StringAttribute> {

        public JsonDeserializer() {
            this(null);
        }

        public JsonDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public StringAttribute deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            String name = node.get("name").asText();
            List<String> values = new ArrayList<>();
            node.get("values").elements().forEachRemaining(i -> values.add(i.asText()));
            return new StringAttribute(name, values.toArray(new String[0]));
        }
    }

}
