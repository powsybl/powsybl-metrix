/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.io;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixDie {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixDie.class);

    private static final int ATTRIBUTE_NAME_LENGTH = 8;

    private static final String INTEGER_FILE_NAME = "IntegerFile";
    private static final String FLOAT_FILE_NAME = "FloatFile";
    private static final String DOUBLE_FILE_NAME = "DoubleFile";
    private static final String STRING_FILE_NAME = "StringFile";
    private static final String BOOLEAN_FILE_NAME = "BooleanFile";
    private static final String NOT_FOUND = " not found";
    private static final String IS_NOT_A_SCALAR = " is not a scalar";

    private final Map<String, IntAttribute> intAttributes = new LinkedHashMap<>();
    private final Map<String, FloatAttribute> floatAttributes = new LinkedHashMap<>();
    private final Map<String, DoubleAttribute> doubleAttributes = new LinkedHashMap<>();
    private final Map<String, StringAttribute> stringAttributes = new LinkedHashMap<>();
    private final Map<String, BooleanAttribute> booleanAttributes = new LinkedHashMap<>();

    private static <T extends Attribute> void saveFort4xToJson(JsonGenerator generator, String fileName, Map<String, T> attributes) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("name", fileName);
        generator.writeFieldName("attributes");
        generator.writeStartArray();
        for (T attribute : attributes.values()) {
            generator.writeStartObject();
            generator.writeStringField("name", attribute.getName());
            generator.writeStringField("type", attribute.getType().name());
            generator.writeNumberField("valueCount", attribute.getValueCount());
            generator.writeNumberField("firstIndexMaxValue", attribute.getFirstIndexMaxValue());
            generator.writeNumberField("secondIndexMaxValue", attribute.getSecondIndexMaxValue());
            generator.writeNumberField("firstValueIndex", 1);
            generator.writeNumberField("lastValueIndex", attribute.getValueCount());
            generator.writeFieldName("values");
            generator.writeStartArray();
            attribute.writeJson(generator);
            generator.writeEndArray();
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }

    public void saveToJson(BufferedWriter writer) throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonGenerator generator = factory.createGenerator(writer)) {
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeFieldName("files");
            generator.writeStartArray();
            saveFort4xToJson(generator, INTEGER_FILE_NAME, intAttributes);
            saveFort4xToJson(generator, FLOAT_FILE_NAME, floatAttributes);
            saveFort4xToJson(generator, DOUBLE_FILE_NAME, doubleAttributes);
            saveFort4xToJson(generator, STRING_FILE_NAME, stringAttributes);
            saveFort4xToJson(generator, BOOLEAN_FILE_NAME, booleanAttributes);
            generator.writeEndArray();
            generator.writeEndObject();
        }
    }

    public void saveToJson(Path file) throws IOException {
        BufferedWriter bufferedWriter = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        saveToJson(bufferedWriter);
    }

    private static <T extends Attribute> void loadFromJson(JsonNode nodes, Map<String, T> attributes, Class<T> attributeClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        for (Iterator<JsonNode> nodeAttributes = nodes.elements(); nodeAttributes.hasNext(); ) {
            JsonNode attr = nodeAttributes.next();
            T attribute = mapper.readValue(attr.toString(), attributeClass);
            attributes.put(attribute.getName(), attribute);
        }
    }

    public void loadFromJson(Path jsonFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(jsonFile)) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(reader).path("files");
            for (Iterator<JsonNode> files = rootNode.elements(); files.hasNext(); ) {
                JsonNode dieFile = files.next();
                String name = dieFile.path("name").textValue();
                JsonNode attributes = dieFile.path("attributes");
                if (INTEGER_FILE_NAME.equals(name)) {
                    loadFromJson(attributes, intAttributes, IntAttribute.class);
                } else if (FLOAT_FILE_NAME.equals(name)) {
                    loadFromJson(attributes, floatAttributes, FloatAttribute.class);
                } else if (DOUBLE_FILE_NAME.equals(name)) {
                    loadFromJson(attributes, doubleAttributes, DoubleAttribute.class);
                } else if (STRING_FILE_NAME.equals(name)) {
                    loadFromJson(attributes, stringAttributes, StringAttribute.class);
                } else if (BOOLEAN_FILE_NAME.equals(name)) {
                    loadFromJson(attributes, booleanAttributes, BooleanAttribute.class);
                }
            }
        }
    }

    private static void checkAttributeNameLength(String name) {
        if (name.length() != ATTRIBUTE_NAME_LENGTH) {
            throw new MetrixDieException("Incorrect attribute name length: "
                    + name + " (should be " + ATTRIBUTE_NAME_LENGTH + ")");
        }
    }

    public int[] getIntArray(String name) {
        IntAttribute attribute = intAttributes.get(name);
        if (attribute == null) {
            throw new MetrixDieException("Int attribute " + name + NOT_FOUND);
        }
        return attribute.getValues();
    }

    public int getInt(String name) {
        int[] array = getIntArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("Int attribute " + name + IS_NOT_A_SCALAR);
        }
        return array[0];
    }

    public void setIntArray(String name, int[] values) {
        checkAttributeNameLength(name);
        intAttributes.put(name, new IntAttribute(name, values));
    }

    public void setInt(String name, int value) {
        checkAttributeNameLength(name);
        intAttributes.put(name, new IntAttribute(name, new int[]{value}));
    }

    public float[] getFloatArray(String name) {
        FloatAttribute attribute = floatAttributes.get(name);
        if (attribute == null) {
            throw new MetrixDieException("Float attribute " + name + NOT_FOUND);
        }
        return attribute.getValues();
    }

    public float getFloat(String name) {
        float[] array = getFloatArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("Float attribute " + name + IS_NOT_A_SCALAR);
        }
        return array[0];
    }

    public void setFloatArray(String name, float[] values) {
        checkAttributeNameLength(name);
        floatAttributes.put(name, new FloatAttribute(name, values));
    }

    public void setFloat(String name, float value) {
        checkAttributeNameLength(name);
        floatAttributes.put(name, new FloatAttribute(name, new float[]{value}));
    }

    public double[] getDoubleArray(String name) {
        DoubleAttribute attribute = doubleAttributes.get(name);
        if (attribute == null) {
            throw new MetrixDieException("Double attribute " + name + NOT_FOUND);
        }
        return attribute.getValues();
    }

    public double getDouble(String name) {
        double[] array = getDoubleArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("Double attribute " + name + IS_NOT_A_SCALAR);
        }
        return array[0];
    }

    public void setDoubleArray(String name, double[] values) {
        checkAttributeNameLength(name);
        doubleAttributes.put(name, new DoubleAttribute(name, values));
    }

    public void setDouble(String name, double value) {
        checkAttributeNameLength(name);
        doubleAttributes.put(name, new DoubleAttribute(name, new double[]{value}));
    }

    public String[] getStringArray(String name) {
        StringAttribute attribute = stringAttributes.get(name);
        if (attribute == null) {
            throw new MetrixDieException("String attribute " + name + NOT_FOUND);
        }
        return attribute.getValues();
    }

    public String getString(String name) {
        String[] array = getStringArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("String attribute " + name + IS_NOT_A_SCALAR);
        }
        return array[0];
    }

    public void setStringArray(String name, String[] values) {
        checkAttributeNameLength(name);
        stringAttributes.put(name, new StringAttribute(name, values));
    }

    public void setString(String name, String value) {
        checkAttributeNameLength(name);
        stringAttributes.put(name, new StringAttribute(name, new String[]{value}));
    }

    public boolean[] getBooleanArray(String name) {
        BooleanAttribute attribute = booleanAttributes.get(name);
        if (attribute == null) {
            throw new MetrixDieException("Boolean attribute " + name + NOT_FOUND);
        }
        return attribute.getValues();
    }

    public boolean getBoolean(String name) {
        boolean[] array = getBooleanArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("Boolean attribute " + name + IS_NOT_A_SCALAR);
        }
        return array[0];
    }

    public void setBooleanArray(String name, boolean[] values) {
        checkAttributeNameLength(name);
        booleanAttributes.put(name, new BooleanAttribute(name, values));
    }

    public void setBoolean(String name, boolean value) {
        checkAttributeNameLength(name);
        booleanAttributes.put(name, new BooleanAttribute(name, new boolean[]{value}));
    }

    public Set<String> getAttributeNames() {
        Set<String> names = new HashSet<>();
        names.addAll(intAttributes.keySet());
        names.addAll(floatAttributes.keySet());
        names.addAll(doubleAttributes.keySet());
        names.addAll(stringAttributes.keySet());
        names.addAll(booleanAttributes.keySet());
        return names;
    }

    public void print(PrintStream out) {
        for (Attribute attribute : Iterables.concat(intAttributes.values(),
                floatAttributes.values(),
                doubleAttributes.values(),
                stringAttributes.values(),
                booleanAttributes.values())) {
            out.print(attribute.getName());
            out.print(": [");
            attribute.print(out);
            out.println("]");
        }
    }
}
