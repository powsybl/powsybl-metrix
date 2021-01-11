/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration.io;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixDie {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixDie.class);

    private static final int ATTRIBUTE_NAME_LENGTH = 8;

    private String integerFileName = "fort.44_BIN";
    private String floatFileName = "fort.45_BIN";
    private String doubleFileName = "fort.46_BIN";
    private String stringFileName = "fort.47_BIN";
    private String booleanFileName = "fort.48_BIN";

    private final Map<String, IntAttribute> intAttributes = new LinkedHashMap<>();
    private final Map<String, FloatAttribute> floatAttributes = new LinkedHashMap<>();
    private final Map<String, DoubleAttribute> doubleAttributes = new LinkedHashMap<>();
    private final Map<String, StringAttribute> stringAttributes = new LinkedHashMap<>();
    private final Map<String, BooleanAttribute> booleanAttributes = new LinkedHashMap<>();

    private interface AttributeFactory<T extends Attribute> {

        T create(String name, int n1, int n2);
    }

    private static Path fort2path(Path dir) {
        return dir.resolve("fort.2");
    }

    private void loadFort2(Path dir) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(fort2path(dir), StandardCharsets.US_ASCII)) {
            integerFileName = reader.readLine();
            floatFileName = reader.readLine();
            doubleFileName = reader.readLine();
            stringFileName = reader.readLine();
            booleanFileName = reader.readLine();
        }
    }

    private static int getRecordMaxValueCount(Map<String, ? extends Attribute> attributes) {
        return attributes.values().stream().map(Attribute::getValueCount).max(Integer::compare).orElse(0);
    }

    private static int getRecordMaxSize(Map<String, ? extends Attribute> attributes) {
        return attributes.values().stream().map(Attribute::getSize).max(Integer::compare).orElse(0);
    }

    private void saveFort2(Path dir) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(fort2path(dir), StandardCharsets.US_ASCII)) {
            writer.write(integerFileName);
            writer.newLine();
            writer.write(floatFileName);
            writer.newLine();
            writer.write(doubleFileName);
            writer.newLine();
            writer.write(stringFileName);
            writer.newLine();
            writer.write(booleanFileName);
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxValueCount(intAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxValueCount(floatAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxValueCount(doubleAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxValueCount(stringAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxValueCount(booleanAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxSize(intAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxSize(floatAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxSize(doubleAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxSize(stringAttributes)));
            writer.newLine();
            writer.write(Integer.toString(getRecordMaxSize(booleanAttributes)));
            writer.newLine();
            writer.write(Integer.toString(10));
            writer.newLine();
            writer.write(Integer.toString(15));
            writer.newLine();
            writer.write(Integer.toString(15));
            writer.newLine();
            writer.write(Integer.toString(8));
            writer.newLine();
            writer.write(Integer.toString(60));
            writer.newLine();
            writer.write(Integer.toString(1));
            writer.newLine();
            writer.write(Integer.toString(0));
            writer.newLine();
        }
    }

    private static <T extends Attribute, U extends AttributeFactory<T>> void loadFort4X(Path file, U attributeFactory, Map<String, T> attributes) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        try (LittleEndianDataInputStream is = new LittleEndianDataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            while (is.available() > 0) {
                byte[] buffer = new byte[ATTRIBUTE_NAME_LENGTH];
                is.readFully(buffer);
                String attributeName = new String(buffer, StandardCharsets.US_ASCII);
                int attributeType = is.readInt();
                int valueCount = is.readInt();
                int firstIndexMaxValue = is.readInt();
                int secondIndexMaxValue = is.readInt();
                int firstValueIndex = is.readInt();
                int lastValueIndex = is.readInt();

                LOGGER.trace("attributeName={}, attributeType={}, valueCount={}, firstIndexMaxValue={}, secondIndexMaxValue={}, firstValueIndex={}, lastValueIndex={}",
                        attributeName, attributeType, valueCount, firstIndexMaxValue, secondIndexMaxValue, firstValueIndex, lastValueIndex);

                T attribute = attributes.get(attributeName);
                if (attribute == null) {
                    attribute = attributeFactory.create(attributeName, firstIndexMaxValue, secondIndexMaxValue);
                    attributes.put(attributeName, attribute);
                }

                for (int index = firstValueIndex; index <= lastValueIndex; index++) {
                    int i = (index - 1) / firstIndexMaxValue;
                    int j = (index - 1) % firstIndexMaxValue;
                    attribute.read(is, i, j);
                }
            }
        }
    }

    private static <T extends Attribute> void saveFort4x(Path file, Map<String, T> attributes) throws IOException {
        try (LittleEndianDataOutputStream os = new LittleEndianDataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            for (T attribute : attributes.values()) {
                os.write(attribute.getName().getBytes(StandardCharsets.US_ASCII));
                os.writeInt(attribute.getType().getValue());
                os.writeInt(attribute.getValueCount());
                os.writeInt(attribute.getFirstIndexMaxValue());
                os.writeInt(attribute.getSecondIndexMaxValue());
                os.writeInt(1);
                os.writeInt(attribute.getValueCount());
                attribute.write(os);
            }
        }
    }

    public void load(Path dir) throws IOException {
        long start = System.currentTimeMillis();
        loadFort2(dir);
        loadFort4X(dir.resolve(integerFileName), (name, n1, n2) -> new IntAttribute(name, n1), intAttributes);
        loadFort4X(dir.resolve(floatFileName), (name, n1, n2) -> new FloatAttribute(name, n1), floatAttributes);
        loadFort4X(dir.resolve(doubleFileName), (name, n1, n2) -> new DoubleAttribute(name, n1), doubleAttributes);
        loadFort4X(dir.resolve(stringFileName), StringAttribute::new, stringAttributes);
        loadFort4X(dir.resolve(booleanFileName), (name, n1, n2) -> new BooleanAttribute(name, n1), booleanAttributes);
        LOGGER.info("fort.* loaded in {} ms", System.currentTimeMillis() - start);
    }

    public void save(Path dir) throws IOException {
        long start = System.currentTimeMillis();
        saveFort2(dir);
        saveFort4x(dir.resolve(integerFileName), intAttributes);
        saveFort4x(dir.resolve(floatFileName), floatAttributes);
        saveFort4x(dir.resolve(doubleFileName), doubleAttributes);
        saveFort4x(dir.resolve(stringFileName), stringAttributes);
        saveFort4x(dir.resolve(booleanFileName), booleanAttributes);
        LOGGER.info("fort.* saved in {} ms", System.currentTimeMillis() - start);
    }

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
            saveFort4xToJson(generator, integerFileName, intAttributes);
            saveFort4xToJson(generator, floatFileName, floatAttributes);
            saveFort4xToJson(generator, doubleFileName, doubleAttributes);
            saveFort4xToJson(generator, stringFileName, stringAttributes);
            saveFort4xToJson(generator, booleanFileName, booleanAttributes);
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
                if (integerFileName.equals(name)) {
                    loadFromJson(attributes, intAttributes, IntAttribute.class);
                } else if (floatFileName.equals(name)) {
                    loadFromJson(attributes, floatAttributes, FloatAttribute.class);
                } else if (doubleFileName.equals(name)) {
                    loadFromJson(attributes, doubleAttributes, DoubleAttribute.class);
                } else if (stringFileName.equals(name)) {
                    loadFromJson(attributes, stringAttributes, StringAttribute.class);
                } else if (booleanFileName.equals(name)) {
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
            throw new MetrixDieException("Int attribute " + name + " not found");
        }
        return attribute.getValues();
    }

    public int getInt(String name) {
        int[] array = getIntArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("Int attribute " + name + " is not a scalar");
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
            throw new MetrixDieException("Float attribute " + name + " not found");
        }
        return attribute.getValues();
    }

    public float getFloat(String name) {
        float[] array = getFloatArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("Float attribute " + name + " is not a scalar");
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
            throw new MetrixDieException("Double attribute " + name + " not found");
        }
        return attribute.getValues();
    }

    public double getDouble(String name) {
        double[] array = getDoubleArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("Double attribute " + name + " is not a scalar");
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
            throw new MetrixDieException("String attribute " + name + " not found");
        }
        return attribute.getValues();
    }

    public String getString(String name) {
        String[] array = getStringArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("String attribute " + name + " is not a scalar");
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
            throw new MetrixDieException("Boolean attribute " + name + " not found");
        }
        return attribute.getValues();
    }

    public boolean getBoolean(String name) {
        boolean[] array = getBooleanArray(name);
        if (array.length != 1) {
            throw new MetrixDieException("Boolean attribute " + name + " is not a scalar");
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
