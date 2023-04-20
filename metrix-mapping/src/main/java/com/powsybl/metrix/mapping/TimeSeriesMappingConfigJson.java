/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.metrix.mapping.json.JsonFieldName;
import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.json.TimeSeriesJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class TimeSeriesMappingConfigJson {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMappingConfig.class);
    private static final long DESERIALIZATION_EXTENDED_MAX_STACK_SIZE = 4096000L;

    private final ObjectMapper mapper = new ObjectMapper();

    protected final TimeSeriesMappingConfig config;

    public TimeSeriesMappingConfigJson(TimeSeriesMappingConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public static String toJson(TimeSeriesMappingConfig config) {
        return JsonUtil.toJson(generator -> new TimeSeriesMappingConfigJson(config).toJson(generator));
    }

    public void toJson(JsonGenerator generator) {
        writeJson(generator, config);
    }

    public void writeJson(JsonGenerator generator, TimeSeriesMappingConfig config) {
        Objects.requireNonNull(generator);
        try {
            generator.writeStartObject();
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_GENERATORS, config.getTimeSeriesToGeneratorsMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_LOADS, config.getTimeSeriesToLoadsMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_DANGLING_LINES, config.getTimeSeriesToDanglingLinesMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_HVDC_LINES, config.getTimeSeriesToHvdcLinesMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_PHASE_TAP_CHANGERS, config.getTimeSeriesToPhaseTapChangersMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_BREAKERS, config.getTimeSeriesToBreakersMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_TRANSFORMERS, config.getTimeSeriesToTransformersMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_LINES, config.getTimeSeriesToLinesMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_RATIO_TAP_CHANGERS, config.getTimeSeriesToRatioTapChangersMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_LCC_CONVERTER_STATIONS, config.getTimeSeriesToLccConverterStationsMapping());
            writeMappingKeyMap(generator, JsonFieldName.TS_TO_VSC_CONVERTER_STATIONS, config.getTimeSeriesToVscConverterStationsMapping());
            writeMappingKeyMap(generator, JsonFieldName.GENERATOR_TO_TS, config.getGeneratorToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.LOAD_TO_TS, config.getLoadToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.DANGLING_LINE_TO_TS, config.getDanglingLineToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.HVDC_LINE_TO_TS, config.getHvdcLineToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.PHASE_TAP_CHANGER_TO_TS, config.getPhaseTapChangerToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.BREAKER_TO_TS, config.getBreakerToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.TRANSFORMER_TO_TS, config.getTransformerToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.LINE_TO_TS, config.getLineToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.RATIO_TAP_CHANGER_TO_TS, config.getRatioTapChangerToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.LCC_CONVERTER_STATION_TO_TS, config.getLccConverterStationToTimeSeriesMapping());
            writeMappingKeyMap(generator, JsonFieldName.VSC_CONVERTER_STATION_TO_TS, config.getVscConverterStationToTimeSeriesMapping());
            writeMappingKeySet(generator, JsonFieldName.GENERATOR_TS, config.getGeneratorTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.LOAD_TS, config.getLoadTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.DANGLING_LINE_TS, config.getDanglingLineTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.HVDC_LINE_TS, config.getHvdcLineTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.PHASE_TAP_CHANGER_TS, config.getPhaseTapChangerTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.BREAKER_TS, config.getBreakerTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.TRANSFORMER_TS, config.getTransformerTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.LINE_TS, config.getLineTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.RATIO_TAP_CHANGER_TS, config.getRatioTapChangerTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.LCC_CONVERTER_STATION_TS, config.getLccConverterStationTimeSeries());
            writeMappingKeySet(generator, JsonFieldName.VSC_CONVERTER_STATION_TS, config.getVscConverterStationTimeSeries());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_GENERATORS, config.getUnmappedGenerators());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_LOADS, config.getUnmappedLoads());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_FIXED_ACTIVE_POWER_LOADS, config.getUnmappedFixedActivePowerLoads());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_VARIABLE_ACTIVE_POWER_LOADS, config.getUnmappedVariableActivePowerLoads());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_DANGLING_LINES, config.getUnmappedDanglingLines());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_HVDC_LINES, config.getUnmappedHvdcLines());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_PHASE_TAP_CHANGERS, config.getUnmappedPhaseTapChangers());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_MIN_P_GENERATORS, config.getUnmappedMinPGenerators());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_MAX_P_GENERATORS, config.getUnmappedMaxPGenerators());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_MIN_P_HVDC_LINES, config.getUnmappedMinPHvdcLines());
            writeMappingSet(generator, JsonFieldName.UNMAPPED_MAX_P_HVDC_LINES, config.getUnmappedMaxPHvdcLines());
            writeMappingSet(generator, JsonFieldName.IGNORED_UNMAPPED_GENERATORS, config.getIgnoredUnmappedGenerators());
            writeMappingSet(generator, JsonFieldName.IGNORED_UNMAPPED_LOADS, config.getIgnoredUnmappedLoads());
            writeMappingSet(generator, JsonFieldName.IGNORED_UNMAPPED_DANGLING_LINES, config.getIgnoredUnmappedDanglingLines());
            writeMappingSet(generator, JsonFieldName.IGNORED_UNMAPPED_HVDC_LINES, config.getIgnoredUnmappedHvdcLines());
            writeMappingSet(generator, JsonFieldName.IGNORED_UNMAPPED_PHASE_TAP_CHANGERS, config.getIgnoredUnmappedPhaseTapChangers());
            writeMappingSet(generator, JsonFieldName.DISCONNECTED_GENERATORS, config.getDisconnectedGenerators());
            writeMappingSet(generator, JsonFieldName.DISCONNECTED_LOADS, config.getDisconnectedLoads());
            writeMappingSet(generator, JsonFieldName.DISCONNECTED_DANGLING_LINES, config.getDisconnectedDanglingLines());
            writeMappingSet(generator, JsonFieldName.OUT_OF_MAIN_CC_GENERATORS, config.getOutOfMainCcGenerators());
            writeMappingSet(generator, JsonFieldName.OUT_OF_MAIN_CC_LOADS, config.getOutOfMainCcLoads());
            writeMappingSet(generator, JsonFieldName.OUT_OF_MAIN_CC_DANGLING_LINES, config.getOutOfMainCcDanglingLines());
            writeDistributionKeys(generator, config.getDistributionKeys());
            writeTimeSeriesNodes(generator, config.getTimeSeriesNodes());
            writeTimeSeriesToEquipmentMap(generator, config.getTimeSeriesToEquipment());
            writeEquipmentToTimeSeriesMap(generator, config.getEquipmentToTimeSeries());
            writeMappingSet(generator, JsonFieldName.MAPPED_TIME_SERIES_NAMES, config.getMappedTimeSeriesNames());
            writeMappingSet(generator, JsonFieldName.IGNORE_LIMITS_TIME_SERIES_NAMES, config.getIgnoreLimitsTimeSeriesNames());
            writeTimeSeriesToPlannedOutagesMap(generator, config.getTimeSeriesToPlannedOutagesMapping());
            generator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeTimeSeriesToEquipmentMap(JsonGenerator generator, Map<String, Set<MappingKey>> equipmentMap) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(equipmentMap);
        try {
            generator.writeFieldName(JsonFieldName.TS_TO_EQUIPMENT.getFieldName());
            generator.writeStartArray();
            for (Map.Entry<String, Set<MappingKey>> e : equipmentMap.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(JsonFieldName.TIME_SERIES_NAME.getFieldName());
                generator.writeString(e.getKey());
                generator.writeFieldName(JsonFieldName.MAPPING_KEYS.getFieldName());
                generator.writeStartArray();
                for (MappingKey key : e.getValue()) {
                    MappingKey.writeJson(generator, key);
                }
                generator.writeEndArray();
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeTimeSeriesToPlannedOutagesMap(JsonGenerator generator, Map<String, Set<String>> plannedOutagesMap) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(plannedOutagesMap);
        try {
            generator.writeFieldName(JsonFieldName.TS_TO_PLANNED_OUTAGES.getFieldName());
            generator.writeStartArray();
            for (Map.Entry<String, Set<String>> e : plannedOutagesMap.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(JsonFieldName.TIME_SERIES_NAME.getFieldName());
                generator.writeString(e.getKey());
                generator.writeFieldName(JsonFieldName.OUTAGES.getFieldName());
                generator.writeStartArray();
                for (String id : e.getValue()) {
                    generator.writeString(id);
                }
                generator.writeEndArray();
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeEquipmentToTimeSeriesMap(JsonGenerator generator, Map<MappingKey, String> equipmentMap) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(equipmentMap);
        try {
            generator.writeFieldName(JsonFieldName.EQUIPMENT_TO_TS.getFieldName());
            generator.writeStartArray();
            for (Map.Entry<MappingKey, String> e : equipmentMap.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(JsonFieldName.MAPPING_KEY.getFieldName());
                MappingKey.writeJson(generator, e.getKey());
                generator.writeFieldName(JsonFieldName.TIME_SERIES_NAME.getFieldName());
                generator.writeString(e.getValue());
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeMappingKeyMap(JsonGenerator generator, JsonFieldName jsonFieldName, Map<MappingKey, List<String>> mappingKeyMap) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(mappingKeyMap);
        try {
            generator.writeFieldName(jsonFieldName.getFieldName());
            generator.writeStartArray();
            for (Map.Entry<MappingKey, List<String>> e : mappingKeyMap.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(JsonFieldName.MAPPING_KEY.getFieldName());
                MappingKey.writeJson(generator, e.getKey());
                generator.writeFieldName(JsonFieldName.MAPPING_LIST.getFieldName());
                mapper.writeValue(generator, e.getValue());
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeMappingSet(JsonGenerator generator, JsonFieldName fieldName, Set<String> mappingSet) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(mappingSet);
        try {
            generator.writeFieldName(fieldName.getFieldName());
            mapper.writeValue(generator, mappingSet);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeMappingKeySet(JsonGenerator generator, JsonFieldName jsonFieldName, Set<MappingKey> mappingSet) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(mappingSet);
        try {
            generator.writeFieldName(jsonFieldName.getFieldName());
            generator.writeStartArray();
            for (MappingKey e : mappingSet) {
                generator.writeStartObject();
                generator.writeFieldName(JsonFieldName.MAPPING_KEY.getFieldName());
                MappingKey.writeJson(generator, e);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeDistributionKeys(JsonGenerator generator, Map<MappingKey, DistributionKey> distributionKeys) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(distributionKeys);
        try {
            generator.writeFieldName(JsonFieldName.DISTRIBUTION_KEYS.getFieldName());
            generator.writeStartArray();
            for (Map.Entry<MappingKey, DistributionKey> e : distributionKeys.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(JsonFieldName.MAPPING_KEY.getFieldName());
                MappingKey.writeJson(generator, e.getKey());
                generator.writeFieldName(JsonFieldName.DISTRIBUTION.getFieldName());
                DistributionKey.writeJson(e.getValue(), generator);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeTimeSeriesNodes(JsonGenerator generator, Map<String, NodeCalc> timeSeriesNodes) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(timeSeriesNodes);
        ObjectMapper timeSeriesMapper = JsonUtil.createObjectMapper()
                .registerModule(new TimeSeriesJsonModule());
        try {
            generator.writeFieldName(JsonFieldName.TIME_SERIES_NODES.getFieldName());
            timeSeriesMapper.writeValue(generator, timeSeriesNodes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TimeSeriesMappingConfig parseJson(Reader reader) {
        return JsonUtil.parseJson(reader, TimeSeriesMappingConfigJson::parseJsonWithExtendedThreadStackSize);
    }

    public static TimeSeriesMappingConfig parseJson(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return TimeSeriesMappingConfigJson.parseJson(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TimeSeriesMappingConfig parseJsonWithExtendedThreadStackSize(JsonParser parser) {
        AtomicReference<TimeSeriesMappingConfig> result = new AtomicReference<>(null);
        Thread extendedStackSizeThread = new Thread(null, null, "MappingConfigDeserialization", DESERIALIZATION_EXTENDED_MAX_STACK_SIZE) {
            @Override
            public void run() {
                result.set(TimeSeriesMappingConfigJson.parseJson(parser));
            }
        };
        extendedStackSizeThread.start();
        try {
            extendedStackSizeThread.join();
        } catch (InterruptedException e) {
            LOGGER.error("Mapping deserialization interrupted", e);
            extendedStackSizeThread.interrupt();
            throw new RuntimeException(e);
        }
        return result.get();
    }

    public static TimeSeriesMappingConfig parseJson(String json) {
        return JsonUtil.parseJson(json, TimeSeriesMappingConfigJson::parseJsonWithExtendedThreadStackSize);
    }

    static Set<String> parseMappingSet(JsonParser parser) {
        Objects.requireNonNull(parser);
        try {
            JsonToken token;
            ObjectMapper mapper = new ObjectMapper();
            if ((token = parser.nextToken()) == JsonToken.START_ARRAY) {
                return mapper.readValue(parser, TypeFactory.defaultInstance().constructCollectionType(Set.class, String.class));
            }
            throw new TimeSeriesException("Unexpected JSON token: " + token);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Set<MappingKey> parseMappingKeySet(JsonParser parser) {
        Objects.requireNonNull(parser);
        try {
            Set<MappingKey> set = new LinkedHashSet<>();
            MappingKey mappingKey = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        if (fieldName.equals(JsonFieldName.MAPPING_KEY.getFieldName())) {
                            mappingKey = MappingKey.parseJson(parser);
                        } else {
                            throw new IllegalStateException("Unexpected field name " + fieldName);
                        }
                        break;
                    case END_OBJECT:
                        if (mappingKey == null) {
                            throw new TimeSeriesException("Invalid time series mapping config JSON");
                        }
                        set.add(mappingKey);
                        break;
                    default:
                        break;
                }
            }
            return set;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<MappingKey, List<String>> parseMappingKeyMap(JsonParser parser) {
        Objects.requireNonNull(parser);
        try {
            Map<MappingKey, List<String>> map = new LinkedHashMap<>();
            MappingKey mappingKey = null;
            List<String> mappingList = null;
            JsonToken token;
            ObjectMapper mapper = new ObjectMapper();
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case START_OBJECT:
                        mappingList = new LinkedList<>();
                        break;
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        switch (JsonFieldName.nameOf(fieldName)) {
                            case MAPPING_KEY:
                                mappingKey = MappingKey.parseJson(parser);
                                break;
                            case MAPPING_LIST:
                                if (parser.nextToken() == JsonToken.START_ARRAY) {
                                    assert mappingList != null;
                                    mappingList.addAll(mapper.readValue(parser, TypeFactory.defaultInstance().constructCollectionType(List.class, String.class)));
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unexpected field name " + fieldName);
                        }
                        break;
                    case END_OBJECT:
                        if (mappingKey == null || mappingList == null) {
                            throw new TimeSeriesException("Invalid time series mapping config JSON");
                        }
                        map.put(mappingKey, mappingList);
                        break;
                    default:
                        break;
                }
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<MappingKey, DistributionKey> parseDistributionKeys(JsonParser parser) {
        Objects.requireNonNull(parser);
        try {
            Map<MappingKey, DistributionKey> map = new HashMap<>();
            MappingKey mappingKey = null;
            DistributionKey distributionKey = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        switch (JsonFieldName.nameOf(fieldName)) {
                            case MAPPING_KEY:
                                mappingKey = MappingKey.parseJson(parser);
                                break;
                            case DISTRIBUTION:
                                distributionKey = DistributionKey.parseJson(parser);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected field name " + fieldName);
                        }
                        break;
                    case END_OBJECT:
                        if (mappingKey == null || distributionKey == null) {
                            throw new TimeSeriesException("Invalid time series mapping config JSON");
                        }
                        map.put(mappingKey, distributionKey);
                        break;
                    default:
                        break;
                }
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<String, NodeCalc> parseTimeSeriesNodes(JsonParser parser) {
        Objects.requireNonNull(parser);
        Map<String, NodeCalc> map = new HashMap<>();
        try {
            ObjectMapper mapper = JsonUtil.createObjectMapper()
                    .registerModule(new TimeSeriesJsonModule());
            if (parser.nextToken() == JsonToken.START_OBJECT) {
                map.putAll(mapper.readValue(parser, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, NodeCalc.class)));
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<String, Set<MappingKey>> parseTimeSeriesToEquipment(JsonParser parser) {
        Objects.requireNonNull(parser);
        try {
            Map<String, Set<MappingKey>> map = new LinkedHashMap<>();
            String timeSeriesName = null;
            Set<MappingKey> mappingKeys = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case START_OBJECT:
                        mappingKeys = new LinkedHashSet<>();
                        break;
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        switch (JsonFieldName.nameOf(fieldName)) {
                            case TIME_SERIES_NAME:
                                if (parser.nextToken() == JsonToken.VALUE_STRING) {
                                    timeSeriesName = parser.getValueAsString();
                                }
                                break;
                            case MAPPING_KEYS:
                                while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                                    if (token == JsonToken.START_OBJECT) {
                                        assert mappingKeys != null;
                                        mappingKeys.add(MappingKey.parseJson(parser));
                                    }
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unexpected field name " + fieldName);
                        }
                        break;
                    case END_OBJECT:
                        if (timeSeriesName == null || mappingKeys == null) {
                            throw new TimeSeriesException("Invalid time series mapping config JSON");
                        }
                        map.put(timeSeriesName, mappingKeys);
                        break;
                    default:
                        break;
                }
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<MappingKey, String> parseEquipmentToTimeSeries(JsonParser parser) {
        Objects.requireNonNull(parser);
        try {
            Map<MappingKey, String> map = new LinkedHashMap<>();
            String timeSeriesName = null;
            MappingKey mappingKey = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        switch (JsonFieldName.nameOf(fieldName)) {
                            case MAPPING_KEY:
                                mappingKey = MappingKey.parseJson(parser);
                                break;
                            case TIME_SERIES_NAME:
                                if (parser.nextToken() == JsonToken.VALUE_STRING) {
                                    timeSeriesName = parser.getValueAsString();
                                }
                                break;
                            default:
                                throw new IllegalStateException("Unexpected field name " + fieldName);
                        }
                        break;
                    case END_OBJECT:
                        if (mappingKey == null || timeSeriesName == null) {
                            throw new TimeSeriesException("Invalid time series mapping config JSON");
                        }
                        map.put(mappingKey, timeSeriesName);
                        break;
                    default:
                        break;
                }
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<String, Set<String>> parseTimeSeriesToPlannedOutages(JsonParser parser) {
        Objects.requireNonNull(parser);
        try {
            Map<String, Set<String>> map = new HashMap<>();
            String timeSeriesName = null;
            Set<String> ids = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case START_OBJECT:
                        ids = new HashSet<>();
                        break;
                    case FIELD_NAME:
                        String newTimeSeriesName = parseFieldName(parser, ids);
                        timeSeriesName = newTimeSeriesName != null ? newTimeSeriesName : timeSeriesName;
                        break;
                    case END_OBJECT:
                        if (timeSeriesName == null || ids == null) {
                            throw new TimeSeriesException("Invalid time series mapping config JSON");
                        }
                        map.put(timeSeriesName, ids);
                        break;
                    default:
                        break;
                }
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String parseFieldName(JsonParser parser, Set<String> ids) throws IOException {
        JsonToken token;
        String fieldName = parser.getCurrentName();
        String newTimeSeriesName = null;
        switch (JsonFieldName.nameOf(fieldName)) {
            case TIME_SERIES_NAME:
                if (parser.nextToken() == JsonToken.VALUE_STRING) {
                    newTimeSeriesName = parser.getValueAsString();
                }
                break;
            case OUTAGES:
                while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                    if (token == JsonToken.VALUE_STRING) {
                        assert ids != null;
                        ids.add(parser.getValueAsString());
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unexpected field name " + fieldName);
        }
        return newTimeSeriesName;
    }

    public static TimeSeriesMappingConfig parseJson(JsonParser parser) {
        Objects.requireNonNull(parser);
        TimeSeriesMappingConfig config = new TimeSeriesMappingConfig();
        try {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    switch (JsonFieldName.nameOf(fieldName)) {
                        case TS_TO_GENERATORS:
                            config.setTimeSeriesToGeneratorsMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_LOADS:
                            config.setTimeSeriesToLoadsMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_DANGLING_LINES:
                            config.setTimeSeriesToDanglingLinesMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_HVDC_LINES:
                            config.setTimeSeriesToHvdcLinesMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_PHASE_TAP_CHANGERS:
                            config.setTimeSeriesToPhaseTapChangersMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_BREAKERS:
                            config.setTimeSeriesToBreakersMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_TRANSFORMERS:
                            config.setTimeSeriesToTransformersMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_LINES:
                            config.setTimeSeriesToLinesMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_RATIO_TAP_CHANGERS:
                            config.setTimeSeriesToRatioTapChangersMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_LCC_CONVERTER_STATIONS:
                            config.setTimeSeriesToLccConverterStationsMapping(parseMappingKeyMap(parser));
                            break;
                        case TS_TO_VSC_CONVERTER_STATIONS:
                            config.setTimeSeriesToVscConverterStationsMapping(parseMappingKeyMap(parser));
                            break;
                        case GENERATOR_TO_TS:
                            config.setGeneratorToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case LOAD_TO_TS:
                            config.setLoadToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case DANGLING_LINE_TO_TS:
                            config.setDanglingLineToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case HVDC_LINE_TO_TS:
                            config.setHvdcLineToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case PHASE_TAP_CHANGER_TO_TS:
                            config.setPhaseTapChangerToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case BREAKER_TO_TS:
                            config.setBreakerToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case TRANSFORMER_TO_TS:
                            config.setTransformerToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case LINE_TO_TS:
                            config.setLineToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case RATIO_TAP_CHANGER_TO_TS:
                            config.setRatioTapChangerToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case LCC_CONVERTER_STATION_TO_TS:
                            config.setLccConverterStationToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case VSC_CONVERTER_STATION_TO_TS:
                            config.setVscConverterStationToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case GENERATOR_TS:
                            config.setGeneratorTimeSeries(parseMappingKeySet(parser));
                            break;
                        case LOAD_TS:
                            config.setLoadTimeSeries(parseMappingKeySet(parser));
                            break;
                        case DANGLING_LINE_TS:
                            config.setDanglingLineTimeSeries(parseMappingKeySet(parser));
                            break;
                        case HVDC_LINE_TS:
                            config.setHvdcLineTimeSeries(parseMappingKeySet(parser));
                            break;
                        case PHASE_TAP_CHANGER_TS:
                            config.setPhaseTapChangerTimeSeries(parseMappingKeySet(parser));
                            break;
                        case BREAKER_TS:
                            config.setBreakerTimeSeries(parseMappingKeySet(parser));
                            break;
                        case TRANSFORMER_TS:
                            config.setTransformerTimeSeries(parseMappingKeySet(parser));
                            break;
                        case LINE_TS:
                            config.setLineTimeSeries(parseMappingKeySet(parser));
                            break;
                        case RATIO_TAP_CHANGER_TS:
                            config.setRatioTapChangerTimeSeries(parseMappingKeySet(parser));
                            break;
                        case LCC_CONVERTER_STATION_TS:
                            config.setLccConverterStationTimeSeries(parseMappingKeySet(parser));
                            break;
                        case VSC_CONVERTER_STATION_TS:
                            config.setVscConverterStationTimeSeries(parseMappingKeySet(parser));
                            break;
                        case UNMAPPED_GENERATORS:
                            config.setUnmappedGenerators(parseMappingSet(parser));
                            break;
                        case UNMAPPED_LOADS:
                            config.setUnmappedLoads(parseMappingSet(parser));
                            break;
                        case UNMAPPED_FIXED_ACTIVE_POWER_LOADS:
                            config.setUnmappedFixedActivePowerLoads(parseMappingSet(parser));
                            break;
                        case UNMAPPED_VARIABLE_ACTIVE_POWER_LOADS:
                            config.setUnmappedVariableActivePowerLoads(parseMappingSet(parser));
                            break;
                        case UNMAPPED_DANGLING_LINES:
                            config.setUnmappedDanglingLines(parseMappingSet(parser));
                            break;
                        case UNMAPPED_HVDC_LINES:
                            config.setUnmappedHvdcLines(parseMappingSet(parser));
                            break;
                        case UNMAPPED_PHASE_TAP_CHANGERS:
                            config.setUnmappedPhaseTapChangers(parseMappingSet(parser));
                            break;
                        case UNMAPPED_MIN_P_GENERATORS:
                            config.setUnmappedMinPGenerators(parseMappingSet(parser));
                            break;
                        case UNMAPPED_MAX_P_GENERATORS:
                            config.setUnmappedMaxPGenerators(parseMappingSet(parser));
                            break;
                        case UNMAPPED_MIN_P_HVDC_LINES:
                            config.setUnmappedMinPHvdcLines(parseMappingSet(parser));
                            break;
                        case UNMAPPED_MAX_P_HVDC_LINES:
                            config.setUnmappedMaxPHvdcLines(parseMappingSet(parser));
                            break;
                        case IGNORED_UNMAPPED_GENERATORS:
                            config.setIgnoredUnmappedGenerators(parseMappingSet(parser));
                            break;
                        case IGNORED_UNMAPPED_LOADS:
                            config.setIgnoredUnmappedLoads(parseMappingSet(parser));
                            break;
                        case IGNORED_UNMAPPED_DANGLING_LINES:
                            config.setIgnoredUnmappedDanglingLines(parseMappingSet(parser));
                            break;
                        case IGNORED_UNMAPPED_HVDC_LINES:
                            config.setIgnoredUnmappedHvdcLines(parseMappingSet(parser));
                            break;
                        case IGNORED_UNMAPPED_PHASE_TAP_CHANGERS:
                            config.setIgnoredUnmappedPhaseTapChangers(parseMappingSet(parser));
                            break;
                        case DISCONNECTED_GENERATORS:
                            config.setDisconnectedGenerators(parseMappingSet(parser));
                            break;
                        case DISCONNECTED_LOADS:
                            config.setDisconnectedLoads(parseMappingSet(parser));
                            break;
                        case DISCONNECTED_DANGLING_LINES:
                            config.setDisconnectedDanglingLines(parseMappingSet(parser));
                            break;
                        case OUT_OF_MAIN_CC_GENERATORS:
                            config.setOutOfMainCcGenerators(parseMappingSet(parser));
                            break;
                        case OUT_OF_MAIN_CC_LOADS:
                            config.setOutOfMainCcLoads(parseMappingSet(parser));
                            break;
                        case OUT_OF_MAIN_CC_DANGLING_LINES:
                            config.setOutOfMainCcDanglingLines(parseMappingSet(parser));
                            break;
                        case DISTRIBUTION_KEYS:
                            config.setDistributionKeys(parseDistributionKeys(parser));
                            break;
                        case TIME_SERIES_NODES:
                            config.setTimeSeriesNodes(parseTimeSeriesNodes(parser));
                            break;
                        case TS_TO_EQUIPMENT:
                            config.setTimeSeriesToEquipment(parseTimeSeriesToEquipment(parser));
                            break;
                        case EQUIPMENT_TO_TS:
                            config.setEquipmentToTimeSeries(parseEquipmentToTimeSeries(parser));
                            break;
                        case MAPPED_TIME_SERIES_NAMES:
                            config.setMappedTimeSeriesNames(parseMappingSet(parser));
                            break;
                        case IGNORE_LIMITS_TIME_SERIES_NAMES:
                            config.setIgnoreLimitsTimeSeriesNames(parseMappingSet(parser));
                            break;
                        case TS_TO_PLANNED_OUTAGES:
                            config.setTimeSeriesToPlannedOutagesMapping(parseTimeSeriesToPlannedOutages(parser));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected field name " + fieldName);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return config;
    }
}

