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
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNames;
import com.powsybl.timeseries.json.TimeSeriesJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class TimeSeriesMappingConfig implements TimeSeriesConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMappingConfig.class);
    private static final long DESERIALIZATION_EXTENDED_MAX_STACK_SIZE = 4096000L;
    private static final int MIN_NUMBER_OF_POINTS = 50;

    private static final String MAPPINGKEY = "mappingKey";
    private static final String MAPPINGKEYS = "mappingKeys";
    private static final String DISTRIBUTIONKEY = "distributionKey";
    private static final String MAPPINGLIST = "mappingList";
    private static final String TSTOGENERATORS = "timeSeriesToGeneratorsMapping";
    private static final String TSTOLOADS = "timeSeriesToLoadsMapping";
    private static final String TSTODANGLINGLINES = "timeSeriesToDanglingLinesMapping";
    private static final String TSTOHVDCLINES = "timeSeriesToHvdcLinesMapping";
    private static final String TSTOPST = "timeSeriesToPstMapping";
    private static final String TSTOBREAKERS = "timeSeriesToBreakersMapping";
    private static final String GENERATORTOTS = "generatorToTimeSeriesMapping";
    private static final String LOADTOTS = "loadToTimeSeriesMapping";
    private static final String DANGLINGLINETOTS = "danglingLineToTimeSeriesMapping";
    private static final String HVDCLINETOTS = "hvdcLineToTimeSeriesMapping";
    private static final String PSTTOTS = "pstToTimeSeriesMapping";
    private static final String BREAKERTOTS = "breakerToTimeSeriesMapping";
    private static final String GENERATORTS = "generatorTimeSeries";
    private static final String LOADTS = "loadTimeSeries";
    private static final String DANGLINGLINETS = "danglingLineTimeSeries";
    private static final String HVDCLINETS = "hvdcLineTimeSeries";
    private static final String PSTTS = "pstTimeSeries";
    private static final String BREAKERTS = "breakerTimeSeries";
    private static final String UNMAPPEDGENERATORS = "unmappedGenerators";
    private static final String UNMAPPEDLOADS = "unmappedLoads";
    private static final String UNMAPPEDFIXEDACTIVEPOWERLOADS = "unmappedFixedActivePowerLoads";
    private static final String UNMAPPEDVARIABLEACTIVEPOWERLOADS = "unmappedVariableActivePowerLoads";
    private static final String UNMAPPEDDANGLINGLINES = "unmappedDanglingLines";
    private static final String UNMAPPEDHVDCLINES = "unmappedHvdcLines";
    private static final String UNMAPPEDPST = "unmappedPst";
    private static final String UNMAPPEDMINPGENERATORS = "unmappedMinPGenerators";
    private static final String UNMAPPEDMAXPGENERATORS = "unmappedMaxPGenerators";
    private static final String UNMAPPEDMINPHVDCLINES = "unmappedMinPHvdcLines";
    private static final String UNMAPPEDMAXPHVDCLINES = "unmappedMaxPHvdcLines";
    private static final String DISTRIBUTIONKEYS = "distributionKeys";
    private static final String IGNOREDUNMAPPEDGENERATORS = "ignoredUnmappedGenerators";
    private static final String IGNOREDUNMAPPEDLOADS = "ignoredUnmappedLoads";
    private static final String IGNOREDUNMAPPEDDANGLINGLINES = "ignoredUnmappedDanglingLines";
    private static final String IGNOREDUNMAPPEDHVDCLINES = "ignoredUnmappedHvdcLines";
    private static final String IGNOREDUNMAPPEDPST = "ignoredUnmappedPst";
    private static final String DISCONNECTEDGENERATORS = "disconnectedGenerators";
    private static final String DISCONNECTEDLOADS = "disconnectedLoads";
    private static final String DISCONNECTEDDANGLINGLINES = "disconnectedDanglingLines";
    private static final String OUTOFMAINCCGENERATORS = "outOfMainCcGenerators";
    private static final String OUTOFMAINCCLOADS = "outOfMainCcLoads";
    private static final String OUTOFMAINCCDANGLINGLINES = "outOfMainCcDanglingLines";
    private static final String TIMESERIESNODES = "timeSeriesNodes";
    private static final String TIMESERIESNAME = "timeSeriesName";
    private static final String TSTOEQUIPMENT = "timeSeriesToEquipment";
    private static final String EQUIPMENTTOTS = "equipmentToTimeSeries";
    private static final String MAPPEDTIMESERIESNAMES = "mappedTimeSeriesNames";
    private static final String IGNORELIMITSTIMESERIESNAMES = "ignoreLimitsTimeSeriesNames";

    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<MappingKey, DistributionKey> distributionKeys = new HashMap<>();

    private final Map<MappingKey, List<String>> timeSeriesToGeneratorsMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToLoadsMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToDanglingLinesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToHvdcLinesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToPstMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToBreakersMapping = new LinkedHashMap<>();

    private final Map<MappingKey, List<String>> generatorToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> loadToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> danglingLineToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> hvdcLineToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> pstToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> breakerToTimeSeriesMapping = new LinkedHashMap<>();

    private final Set<MappingKey> generatorTimeSeries = new HashSet<>();
    private final Set<MappingKey> loadTimeSeries = new HashSet<>();
    private final Set<MappingKey> danglingLineTimeSeries = new HashSet<>();
    private final Set<MappingKey> hvdcLineTimeSeries = new HashSet<>();
    private final Set<MappingKey> pstTimeSeries = new HashSet<>();
    private final Set<MappingKey> breakerTimeSeries = new HashSet<>();

    private final Set<String> unmappedGenerators = new HashSet<>();
    private final Set<String> unmappedLoads = new HashSet<>();
    private final Set<String> unmappedFixedActivePowerLoads = new HashSet<>();
    private final Set<String> unmappedVariableActivePowerLoads = new HashSet<>();
    private final Set<String> unmappedDanglingLines = new HashSet<>();
    private final Set<String> unmappedHvdcLines = new HashSet<>();
    private final Set<String> unmappedPst = new HashSet<>();

    private final Set<String> unmappedMinPGenerators = new HashSet<>();
    private final Set<String> unmappedMaxPGenerators = new HashSet<>();
    private final Set<String> unmappedMinPHvdcLines = new HashSet<>();
    private final Set<String> unmappedMaxPHvdcLines = new HashSet<>();

    private final Set<String> ignoredUnmappedGenerators = new HashSet<>();
    private final Set<String> ignoredUnmappedLoads = new HashSet<>();
    private final Set<String> ignoredUnmappedDanglingLines = new HashSet<>();
    private final Set<String> ignoredUnmappedHvdcLines = new HashSet<>();
    private final Set<String> ignoredUnmappedPst = new HashSet<>();

    private final Set<String> disconnectedGenerators = new HashSet<>();
    private final Set<String> disconnectedLoads = new HashSet<>();
    private final Set<String> disconnectedDanglingLines = new HashSet<>();

    private final Set<String> outOfMainCcGenerators = new HashSet<>();
    private final Set<String> outOfMainCcLoads = new HashSet<>();
    private final Set<String> outOfMainCcDanglingLines = new HashSet<>();

    private final Map<String, Set<MappingKey>> timeSeriesToEquipmentMap = new HashMap<>();
    private final Map<MappingKey, String> equipmentToTimeSeriesMap = new HashMap<>();

    private final Map<String, NodeCalc> timeSeriesNodes = new HashMap<>();

    // time series used in the mapping
    private final Set<String> mappedTimeSeriesNames = new HashSet<>();

    // time series to map with ignore limits option
    private final Set<String> ignoreLimitsTimeSeriesNames = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash(
                timeSeriesToGeneratorsMapping,
                timeSeriesToLoadsMapping,
                timeSeriesToDanglingLinesMapping,
                timeSeriesToHvdcLinesMapping,
                timeSeriesToPstMapping,
                timeSeriesToBreakersMapping,
                generatorToTimeSeriesMapping,
                loadToTimeSeriesMapping,
                danglingLineToTimeSeriesMapping,
                hvdcLineToTimeSeriesMapping,
                pstToTimeSeriesMapping,
                breakerToTimeSeriesMapping,
                generatorTimeSeries,
                loadTimeSeries,
                danglingLineTimeSeries,
                hvdcLineTimeSeries,
                pstTimeSeries,
                breakerTimeSeries,
                unmappedGenerators,
                unmappedLoads,
                unmappedFixedActivePowerLoads,
                unmappedVariableActivePowerLoads,
                unmappedDanglingLines,
                unmappedHvdcLines,
                unmappedPst,
                unmappedMinPGenerators,
                unmappedMaxPGenerators,
                unmappedMinPHvdcLines,
                unmappedMaxPHvdcLines,
                ignoredUnmappedGenerators,
                ignoredUnmappedLoads,
                ignoredUnmappedDanglingLines,
                ignoredUnmappedHvdcLines,
                ignoredUnmappedPst,
                disconnectedGenerators,
                disconnectedLoads,
                disconnectedDanglingLines,
                outOfMainCcGenerators,
                outOfMainCcLoads,
                outOfMainCcDanglingLines,
                timeSeriesNodes,
                timeSeriesToEquipmentMap,
                equipmentToTimeSeriesMap,
                mappedTimeSeriesNames,
                ignoreLimitsTimeSeriesNames
                );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TimeSeriesMappingConfig) {
            TimeSeriesMappingConfig other = (TimeSeriesMappingConfig) obj;
            return timeSeriesToGeneratorsMapping.equals(other.getTimeSeriesToGeneratorsMapping())
                    && timeSeriesToLoadsMapping.equals(other.getTimeSeriesToLoadsMapping())
                    && timeSeriesToDanglingLinesMapping.equals(other.getTimeSeriesToDanglingLinesMapping())
                    && timeSeriesToHvdcLinesMapping.equals(other.getTimeSeriesToHvdcLinesMapping())
                    && timeSeriesToPstMapping.equals(other.getTimeSeriesToPstMapping())
                    && timeSeriesToBreakersMapping.equals(other.getTimeSeriesToBreakersMapping())
                    && generatorToTimeSeriesMapping.equals(other.getGeneratorToTimeSeriesMapping())
                    && loadToTimeSeriesMapping.equals(other.getLoadToTimeSeriesMapping())
                    && danglingLineToTimeSeriesMapping.equals(other.getDanglingLineToTimeSeriesMapping())
                    && hvdcLineToTimeSeriesMapping.equals(other.getHvdcLineToTimeSeriesMapping())
                    && pstToTimeSeriesMapping.equals(other.getPstToTimeSeriesMapping())
                    && breakerToTimeSeriesMapping.equals(other.getBreakerToTimeSeriesMapping())
                    && generatorTimeSeries.equals(other.getGeneratorTimeSeries())
                    && loadTimeSeries.equals(other.getLoadTimeSeries())
                    && danglingLineTimeSeries.equals(other.getDanglingLineTimeSeries())
                    && hvdcLineTimeSeries.equals(other.getHvdcLineTimeSeries())
                    && pstTimeSeries.equals(other.getPstTimeSeries())
                    && breakerTimeSeries.equals(other.getBreakerTimeSeries())
                    && unmappedGenerators.equals(other.getUnmappedGenerators())
                    && unmappedLoads.equals(other.getUnmappedLoads())
                    && unmappedFixedActivePowerLoads.equals(other.getUnmappedFixedActivePowerLoads())
                    && unmappedVariableActivePowerLoads.equals(other.getUnmappedVariableActivePowerLoads())
                    && unmappedDanglingLines.equals(other.getUnmappedDanglingLines())
                    && unmappedHvdcLines.equals(other.getUnmappedHvdcLines())
                    && unmappedPst.equals(other.getUnmappedPst())
                    && unmappedMinPGenerators.equals(other.getUnmappedMinPGenerators())
                    && unmappedMaxPGenerators.equals(other.getUnmappedMaxPGenerators())
                    && unmappedMinPHvdcLines.equals(other.getUnmappedMinPHvdcLines())
                    && unmappedMaxPHvdcLines.equals(other.getUnmappedMaxPHvdcLines())
                    && ignoredUnmappedGenerators.equals(other.getIgnoredUnmappedGenerators())
                    && ignoredUnmappedLoads.equals(other.getIgnoredUnmappedLoads())
                    && ignoredUnmappedDanglingLines.equals(other.getIgnoredUnmappedDanglingLines())
                    && ignoredUnmappedHvdcLines.equals(other.getIgnoredUnmappedHvdcLines())
                    && ignoredUnmappedPst.equals(other.getIgnoredUnmappedPst())
                    && disconnectedGenerators.equals(other.getDisconnectedGenerators())
                    && disconnectedLoads.equals(other.getDisconnectedLoads())
                    && disconnectedDanglingLines.equals(other.getDisconnectedDanglingLines())
                    && outOfMainCcGenerators.equals(other.getOutOfMainCcGenerators())
                    && outOfMainCcLoads.equals(other.getOutOfMainCcLoads())
                    && outOfMainCcDanglingLines.equals(other.getOutOfMainCcDanglingLines())
                    && timeSeriesNodes.equals(other.getTimeSeriesNodes())
                    && timeSeriesToEquipmentMap.equals(other.getTimeSeriesToEquipment())
                    && equipmentToTimeSeriesMap.equals(other.getEquipmentToTimeSeries())
                    && mappedTimeSeriesNames.equals(other.getMappedTimeSeriesNames())
                    && ignoreLimitsTimeSeriesNames.equals(other.getIgnoreLimitsTimeSeriesNames());
        }
        return false;
    }

    public static double getTimeSeriesMin(NodeCalc nodeCalc, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).min().orElse(Double.NaN)).min().orElse(Double.NaN);
    }

    public static double getTimeSeriesMin(String timeSeriesName, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).min().orElse(Double.NaN)).min().orElse(Double.NaN);
    }

    public static double getTimeSeriesMax(NodeCalc nodeCalc, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).max().orElse(Double.NaN)).max().orElse(Double.NaN);
    }

    public static double getTimeSeriesMax(String timeSeriesName, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).max().orElse(Double.NaN)).max().orElse(Double.NaN);
    }

    public static double getTimeSeriesAvg(NodeCalc nodeCalc, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).average().orElse(Double.NaN)).average().orElse(Double.NaN);
    }

    public static double getTimeSeriesAvg(String timeSeriesName, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).average().orElse(Double.NaN)).average().orElse(Double.NaN);
    }

    public static double getTimeSeriesSum(NodeCalc nodeCalc, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).sum()).sum();
    }

    public static double getTimeSeriesSum(String timeSeriesName, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).sum()).sum();
    }

    public static double getTimeSeriesMedian(NodeCalc nodeCalc, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        double[] values = computationRange.getVersions().stream().flatMapToDouble(version -> {
            CalculatedTimeSeries calculatedTimeSeries = createCalculatedTimeSeries(nodeCalc, version, store, computationRange);
            return Arrays.stream(calculatedTimeSeries.toArray()).skip(computationRange.getFirstVariant()).limit(computationRange.getVariantCount());
        }).toArray();
        return Arrays.stream(values).sorted().skip(new BigDecimal(values.length / 2).longValue()).limit(1).findFirst().orElse(Double.NaN);
    }

    public static double getTimeSeriesMedian(String timeSeriesName, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).sum()).sum();
    }

    private static DoubleStream getTimeSeriesStream(NodeCalc nodeCalc, int version, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        CalculatedTimeSeries calculatedTimeSeries = createCalculatedTimeSeries(nodeCalc, version, store, computationRange);
        return Arrays.stream(calculatedTimeSeries.toArray()).skip(computationRange.getFirstVariant()).limit(computationRange.getVariantCount());
    }

    private static CalculatedTimeSeries createCalculatedTimeSeries(NodeCalc nodeCalc, int version, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        CalculatedTimeSeries calculatedTimeSeries = new CalculatedTimeSeries("", nodeCalc, new FromStoreTimeSeriesNameResolver(store, version));
        if (calculatedTimeSeries.getIndex() instanceof InfiniteTimeSeriesIndex) {
            Optional<TimeSeriesIndex> regularIndex = store
                    .getTimeSeriesMetadata(store.getTimeSeriesNames(null))
                    .stream()
                    .map(TimeSeriesMetadata::getIndex)
                    .filter(index -> !(index instanceof InfiniteTimeSeriesIndex))
                    .findFirst();
            regularIndex.ifPresent(calculatedTimeSeries::synchronize);
        }
        return calculatedTimeSeries;
    }

    public static int getNbUnmapped(Set<String> unmapped, Set<String> ignoredUnmapped) {
        Set<String> equipementSet = new HashSet<>(unmapped);
        equipementSet.removeAll(ignoredUnmapped);
        return equipementSet.size();
    }

    public static String toJson(TimeSeriesMappingConfig config) {
        Objects.requireNonNull(config);
        return JsonUtil.toJson(config::toJson);
    }

    public void toJson(JsonGenerator generator) {
        writeJson(generator, this);
    }

    public void writeJson(JsonGenerator generator, TimeSeriesMappingConfig config) {
        Objects.requireNonNull(generator);
        try {
            generator.writeStartObject();
            writeMappingKeyMap(generator, TSTOGENERATORS, config.getTimeSeriesToGeneratorsMapping());
            writeMappingKeyMap(generator, TSTOLOADS, config.getTimeSeriesToLoadsMapping());
            writeMappingKeyMap(generator, TSTODANGLINGLINES, config.getTimeSeriesToDanglingLinesMapping());
            writeMappingKeyMap(generator, TSTOHVDCLINES, config.getTimeSeriesToHvdcLinesMapping());
            writeMappingKeyMap(generator, TSTOPST, config.getTimeSeriesToPstMapping());
            writeMappingKeyMap(generator, TSTOBREAKERS, config.getTimeSeriesToBreakersMapping());
            writeMappingKeyMap(generator, GENERATORTOTS, config.getGeneratorToTimeSeriesMapping());
            writeMappingKeyMap(generator, LOADTOTS, config.getLoadToTimeSeriesMapping());
            writeMappingKeyMap(generator, DANGLINGLINETOTS, config.getDanglingLineToTimeSeriesMapping());
            writeMappingKeyMap(generator, HVDCLINETOTS, config.getHvdcLineToTimeSeriesMapping());
            writeMappingKeyMap(generator, PSTTOTS, config.getPstToTimeSeriesMapping());
            writeMappingKeyMap(generator, BREAKERTOTS, config.getBreakerToTimeSeriesMapping());
            writeMappingKeySet(generator, GENERATORTS, config.getGeneratorTimeSeries());
            writeMappingKeySet(generator, LOADTS, config.getLoadTimeSeries());
            writeMappingKeySet(generator, DANGLINGLINETS, config.getDanglingLineTimeSeries());
            writeMappingKeySet(generator, HVDCLINETS, config.getHvdcLineTimeSeries());
            writeMappingKeySet(generator, PSTTS, config.getPstTimeSeries());
            writeMappingKeySet(generator, BREAKERTS, config.getBreakerTimeSeries());
            writeMappingSet(generator, UNMAPPEDGENERATORS, config.getUnmappedGenerators());
            writeMappingSet(generator, UNMAPPEDLOADS, config.getUnmappedLoads());
            writeMappingSet(generator, UNMAPPEDFIXEDACTIVEPOWERLOADS, config.getUnmappedFixedActivePowerLoads());
            writeMappingSet(generator, UNMAPPEDVARIABLEACTIVEPOWERLOADS, config.getUnmappedVariableActivePowerLoads());
            writeMappingSet(generator, UNMAPPEDDANGLINGLINES, config.getUnmappedDanglingLines());
            writeMappingSet(generator, UNMAPPEDHVDCLINES, config.getUnmappedHvdcLines());
            writeMappingSet(generator, UNMAPPEDPST, config.getUnmappedPst());
            writeMappingSet(generator, UNMAPPEDMINPGENERATORS, config.getUnmappedMinPGenerators());
            writeMappingSet(generator, UNMAPPEDMAXPGENERATORS, config.getUnmappedMaxPGenerators());
            writeMappingSet(generator, UNMAPPEDMINPHVDCLINES, config.getUnmappedMinPHvdcLines());
            writeMappingSet(generator, UNMAPPEDMAXPHVDCLINES, config.getUnmappedMaxPHvdcLines());
            writeMappingSet(generator, IGNOREDUNMAPPEDGENERATORS, config.getIgnoredUnmappedGenerators());
            writeMappingSet(generator, IGNOREDUNMAPPEDLOADS, config.getIgnoredUnmappedLoads());
            writeMappingSet(generator, IGNOREDUNMAPPEDDANGLINGLINES, config.getIgnoredUnmappedDanglingLines());
            writeMappingSet(generator, IGNOREDUNMAPPEDHVDCLINES, config.getIgnoredUnmappedHvdcLines());
            writeMappingSet(generator, IGNOREDUNMAPPEDPST, config.getIgnoredUnmappedPst());
            writeMappingSet(generator, DISCONNECTEDGENERATORS, config.getDisconnectedGenerators());
            writeMappingSet(generator, DISCONNECTEDLOADS, config.getDisconnectedLoads());
            writeMappingSet(generator, DISCONNECTEDDANGLINGLINES, config.getDisconnectedDanglingLines());
            writeMappingSet(generator, OUTOFMAINCCGENERATORS, config.getOutOfMainCcGenerators());
            writeMappingSet(generator, OUTOFMAINCCLOADS, config.getOutOfMainCcLoads());
            writeMappingSet(generator, OUTOFMAINCCDANGLINGLINES, config.getOutOfMainCcDanglingLines());
            writeDistributionKeys(generator, DISTRIBUTIONKEYS, config.getDistributionKeys());
            writeTimeSeriesNodes(generator, TIMESERIESNODES, config.getTimeSeriesNodes());
            writeTimeSeriesToEquipmentMap(generator, TSTOEQUIPMENT, config.getTimeSeriesToEquipment());
            writeEquipmentToTimeSeriesMap(generator, EQUIPMENTTOTS, config.getEquipmentToTimeSeries());
            writeMappingSet(generator, MAPPEDTIMESERIESNAMES, config.getMappedTimeSeriesNames());
            writeMappingSet(generator, IGNORELIMITSTIMESERIESNAMES, config.getIgnoreLimitsTimeSeriesNames());
            generator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeTimeSeriesToEquipmentMap(JsonGenerator generator, String name, Map<String, Set<MappingKey>> equipmentMap) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(equipmentMap);
        try {
            generator.writeFieldName(name);
            generator.writeStartArray();
            for (Map.Entry<String, Set<MappingKey>> e : equipmentMap.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(TIMESERIESNAME);
                generator.writeString(e.getKey());
                generator.writeFieldName(MAPPINGKEYS);
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

    static void writeEquipmentToTimeSeriesMap(JsonGenerator generator, String name, Map<MappingKey, String> equipmentMap) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(equipmentMap);
        try {
            generator.writeFieldName(name);
            generator.writeStartArray();
            for (Map.Entry<MappingKey, String> e : equipmentMap.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(MAPPINGKEY);
                MappingKey.writeJson(generator, e.getKey());
                generator.writeFieldName(TIMESERIESNAME);
                generator.writeString(e.getValue());
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeMappingKeyMap(JsonGenerator generator, String name, Map<MappingKey, List<String>> mappingKeyMap) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(mappingKeyMap);
        try {
            generator.writeFieldName(name);
            generator.writeStartArray();
            for (Map.Entry<MappingKey, List<String>> e : mappingKeyMap.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(MAPPINGKEY);
                MappingKey.writeJson(generator, e.getKey());
                generator.writeFieldName(MAPPINGLIST);
                mapper.writeValue(generator, e.getValue());
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeMappingSet(JsonGenerator generator, String name, Set<String> mappingSet) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(mappingSet);
        try {
            generator.writeFieldName(name);
            mapper.writeValue(generator, mappingSet);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeMappingKeySet(JsonGenerator generator, String name, Set<MappingKey> mappingSet) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(mappingSet);
        try {
            generator.writeFieldName(name);
            generator.writeStartArray();
            for (MappingKey e : mappingSet) {
                generator.writeStartObject();
                generator.writeFieldName(MAPPINGKEY);
                MappingKey.writeJson(generator, e);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void writeDistributionKeys(JsonGenerator generator, String name, Map<MappingKey, DistributionKey> distributionKeys) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(distributionKeys);
        try {
            generator.writeFieldName(name);
            generator.writeStartArray();
            for (Map.Entry<MappingKey, DistributionKey> e : distributionKeys.entrySet()) {
                generator.writeStartObject();
                generator.writeFieldName(MAPPINGKEY);
                MappingKey.writeJson(generator, e.getKey());
                generator.writeFieldName(DISTRIBUTIONKEY);
                DistributionKey.writeJson(e.getValue(), generator);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void writeTimeSeriesNodes(JsonGenerator generator, String name, Map<String, NodeCalc> timeSeriesNodes) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(timeSeriesNodes);
        ObjectMapper mapper = JsonUtil.createObjectMapper()
                .registerModule(new TimeSeriesJsonModule());
        try {
            generator.writeFieldName(name);
            mapper.writeValue(generator, timeSeriesNodes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TimeSeriesMappingConfig parseJson(Reader reader) {
        return JsonUtil.parseJson(reader, TimeSeriesMappingConfig::parseJsonWithExtendedThreadStackSize);
    }

    public static TimeSeriesMappingConfig parseJson(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return TimeSeriesMappingConfig.parseJson(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TimeSeriesMappingConfig parseJsonWithExtendedThreadStackSize(JsonParser parser) {
        AtomicReference<TimeSeriesMappingConfig> result = new AtomicReference<>(null);
        Thread extendedStackSizeThread = new Thread(null, null, "MappingConfigDeserialization", DESERIALIZATION_EXTENDED_MAX_STACK_SIZE) {
            @Override
            public void run() {
                result.set(TimeSeriesMappingConfig.parseJson(parser));
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
        return JsonUtil.parseJson(json, TimeSeriesMappingConfig::parseJsonWithExtendedThreadStackSize);
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
                        if (fieldName.equals(MAPPINGKEY)) {
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
                        switch (fieldName) {
                            case MAPPINGKEY:
                                mappingKey = MappingKey.parseJson(parser);
                                break;
                            case MAPPINGLIST:
                                if ((token = parser.nextToken()) == JsonToken.START_ARRAY) {
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
                        switch (fieldName) {
                            case MAPPINGKEY:
                                mappingKey = MappingKey.parseJson(parser);
                                break;
                            case DISTRIBUTIONKEY:
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
            JsonToken token;
            ObjectMapper mapper = JsonUtil.createObjectMapper()
                    .registerModule(new TimeSeriesJsonModule());
            if ((token = parser.nextToken()) == JsonToken.START_OBJECT) {
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
            Map<String, Set<MappingKey>> map = new HashMap<>();
            String timeSeriesName = null;
            Set<MappingKey> mappingKeys = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case START_OBJECT:
                        mappingKeys = new HashSet<>();
                        break;
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        switch (fieldName) {
                            case TIMESERIESNAME:
                                if ((token = parser.nextToken()) == JsonToken.VALUE_STRING) {
                                    timeSeriesName = parser.getValueAsString();
                                }
                                break;
                            case MAPPINGKEYS:
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
            Map<MappingKey, String> map = new HashMap<>();
            String timeSeriesName = null;
            MappingKey mappingKey = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        switch (fieldName) {
                            case MAPPINGKEY:
                                mappingKey = MappingKey.parseJson(parser);
                                break;
                            case TIMESERIESNAME:
                                if ((token = parser.nextToken()) == JsonToken.VALUE_STRING) {
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

    public static TimeSeriesMappingConfig parseJson(JsonParser parser) {
        Objects.requireNonNull(parser);
        TimeSeriesMappingConfig config = new TimeSeriesMappingConfig();
        try {
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    switch (fieldName) {
                        case TSTOGENERATORS:
                            config.setTimeSeriesToGeneratorsMapping(parseMappingKeyMap(parser));
                            break;
                        case TSTOLOADS:
                            config.setTimeSeriesToLoadsMapping(parseMappingKeyMap(parser));
                            break;
                        case TSTODANGLINGLINES:
                            config.setTimeSeriesToDanglingLinesMapping(parseMappingKeyMap(parser));
                            break;
                        case TSTOHVDCLINES:
                            config.setTimeSeriesToHvdcLinesMapping(parseMappingKeyMap(parser));
                            break;
                        case TSTOPST:
                            config.setTimeSeriesToPstMapping(parseMappingKeyMap(parser));
                            break;
                        case TSTOBREAKERS:
                            config.setTimeSeriesToBreakersMapping(parseMappingKeyMap(parser));
                            break;
                        case GENERATORTOTS:
                            config.setGeneratorToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case LOADTOTS:
                            config.setLoadToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case DANGLINGLINETOTS:
                            config.setDanglingLineToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case HVDCLINETOTS:
                            config.setHvdcLineToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case PSTTOTS:
                            config.setPstToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case BREAKERTOTS:
                            config.setBreakerToTimeSeriesMapping(parseMappingKeyMap(parser));
                            break;
                        case GENERATORTS:
                            config.setGeneratorTimeSeries(parseMappingKeySet(parser));
                            break;
                        case LOADTS:
                            config.setLoadTimeSeries(parseMappingKeySet(parser));
                            break;
                        case DANGLINGLINETS:
                            config.setDanglingLineTimeSeries(parseMappingKeySet(parser));
                            break;
                        case HVDCLINETS:
                            config.setHvdcLineTimeSeries(parseMappingKeySet(parser));
                            break;
                        case PSTTS:
                            config.setPstTimeSeries(parseMappingKeySet(parser));
                            break;
                        case BREAKERTS:
                            config.setBreakerTimeSeries(parseMappingKeySet(parser));
                            break;
                        case UNMAPPEDGENERATORS:
                            config.setUnmappedGenerators(parseMappingSet(parser));
                            break;
                        case UNMAPPEDLOADS:
                            config.setUnmappedLoads(parseMappingSet(parser));
                            break;
                        case UNMAPPEDFIXEDACTIVEPOWERLOADS:
                            config.setUnmappedFixedActivePowerLoads(parseMappingSet(parser));
                            break;
                        case UNMAPPEDVARIABLEACTIVEPOWERLOADS:
                            config.setUnmappedVariableActivePowerLoads(parseMappingSet(parser));
                            break;
                        case UNMAPPEDDANGLINGLINES:
                            config.setUnmappedDanglingLines(parseMappingSet(parser));
                            break;
                        case UNMAPPEDHVDCLINES:
                            config.setUnmappedHvdcLines(parseMappingSet(parser));
                            break;
                        case UNMAPPEDPST:
                            config.setUnmappedPst(parseMappingSet(parser));
                            break;
                        case UNMAPPEDMINPGENERATORS:
                            config.setUnmappedMinPGenerators(parseMappingSet(parser));
                            break;
                        case UNMAPPEDMAXPGENERATORS:
                            config.setUnmappedMaxPGenerators(parseMappingSet(parser));
                            break;
                        case UNMAPPEDMINPHVDCLINES:
                            config.setUnmappedMinPHvdcLines(parseMappingSet(parser));
                            break;
                        case UNMAPPEDMAXPHVDCLINES:
                            config.setUnmappedMaxPHvdcLines(parseMappingSet(parser));
                            break;
                        case IGNOREDUNMAPPEDGENERATORS:
                            config.setIgnoredUnmappedGenerators(parseMappingSet(parser));
                            break;
                        case IGNOREDUNMAPPEDLOADS:
                            config.setIgnoredUnmappedLoads(parseMappingSet(parser));
                            break;
                        case IGNOREDUNMAPPEDDANGLINGLINES:
                            config.setIgnoredUnmappedDanglingLines(parseMappingSet(parser));
                            break;
                        case IGNOREDUNMAPPEDHVDCLINES:
                            config.setIgnoredUnmappedHvdcLines(parseMappingSet(parser));
                            break;
                        case IGNOREDUNMAPPEDPST:
                            config.setIgnoredUnmappedPst(parseMappingSet(parser));
                            break;
                        case DISCONNECTEDGENERATORS:
                            config.setDisconnectedGenerators(parseMappingSet(parser));
                            break;
                        case DISCONNECTEDLOADS:
                            config.setDisconnectedLoads(parseMappingSet(parser));
                            break;
                        case DISCONNECTEDDANGLINGLINES:
                            config.setDisconnectedDanglingLines(parseMappingSet(parser));
                            break;
                        case OUTOFMAINCCGENERATORS:
                            config.setOutOfMainCcGenerators(parseMappingSet(parser));
                            break;
                        case OUTOFMAINCCLOADS:
                            config.setOutOfMainCcLoads(parseMappingSet(parser));
                            break;
                        case OUTOFMAINCCDANGLINGLINES:
                            config.setOutOfMainCcDanglingLines(parseMappingSet(parser));
                            break;
                        case DISTRIBUTIONKEYS:
                            config.setDistributionKeys(parseDistributionKeys(parser));
                            break;
                        case TIMESERIESNODES:
                            config.setTimeSeriesNodes(parseTimeSeriesNodes(parser));
                            break;
                        case TSTOEQUIPMENT:
                            config.setTimeSeriesToEquipment(parseTimeSeriesToEquipment(parser));
                            break;
                        case EQUIPMENTTOTS:
                            config.setEquipmentToTimeSeries(parseEquipmentToTimeSeries(parser));
                            break;
                        case MAPPEDTIMESERIESNAMES:
                            config.setMappedTimeSeriesNames(parseMappingSet(parser));
                            break;
                        case IGNORELIMITSTIMESERIESNAMES:
                            config.setIgnoreLimitsTimeSeriesNames(parseMappingSet(parser));
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

    public TimeSeriesMappingConfig() {
    }

    public TimeSeriesMappingConfig(Network network) {
        // init unmapped
        network.getGenerators().forEach(generator -> init(generator, unmappedGenerators, disconnectedGenerators, outOfMainCcGenerators));
        network.getLoads().forEach(load -> init(load, unmappedLoads, disconnectedLoads, outOfMainCcLoads));
        network.getDanglingLines().forEach(danglingLine -> init(danglingLine, unmappedDanglingLines, disconnectedDanglingLines, outOfMainCcDanglingLines));
        network.getHvdcLines().forEach(hvdcLine -> unmappedHvdcLines.add(hvdcLine.getId()));
        network.getTwoWindingsTransformers().forEach(pst -> {
            if (pst.getPhaseTapChanger() != null) {
                unmappedPst.add(pst.getId());
            }
        });
        network.getLoads().forEach(load -> init(load, unmappedFixedActivePowerLoads, unmappedVariableActivePowerLoads));
        unmappedMinPGenerators.addAll(unmappedGenerators);
        unmappedMaxPGenerators.addAll(unmappedGenerators);
        unmappedMinPHvdcLines.addAll(unmappedHvdcLines);
        unmappedMaxPHvdcLines.addAll(unmappedHvdcLines);
    }

    public Map<MappingKey, DistributionKey> setDistributionKeys(Map<MappingKey, DistributionKey> keys) {
        distributionKeys.putAll(keys);
        return getDistributionKeys();
    }

    public Map<MappingKey, DistributionKey> getDistributionKeys() {
        return distributionKeys;
    }

    public Map<String, Set<MappingKey>> getTimeSeriesToEquipment() {
        return timeSeriesToEquipmentMap;
    }

    public Map<String, Set<MappingKey>> setTimeSeriesToEquipment(Map<String, Set<MappingKey>> map) {
        timeSeriesToEquipmentMap.putAll(map);
        return getTimeSeriesToEquipment();
    }

    public Map<MappingKey, String> getEquipmentToTimeSeries() {
        return equipmentToTimeSeriesMap;
    }

    public Map<MappingKey, String> setEquipmentToTimeSeries(Map<MappingKey, String> map) {
        equipmentToTimeSeriesMap.putAll(map);
        return getEquipmentToTimeSeries();
    }

    public Map<String, NodeCalc> getTimeSeriesNodes() {
        return timeSeriesNodes;
    }

    public Map<String, NodeCalc> setTimeSeriesNodes(Map<String, NodeCalc> nodes) {
        timeSeriesNodes.putAll(nodes);
        return getTimeSeriesNodes();
    }

    public Set<String> getMappedTimeSeriesNames() {
        return Collections.unmodifiableSet(mappedTimeSeriesNames);
    }

    public Set<String> getIgnoreLimitsTimeSeriesNames() {
        return Collections.unmodifiableSet(ignoreLimitsTimeSeriesNames);
    }

    public Set<String> setMappedTimeSeriesNames(Set<String> timeSeriesNames) {
        mappedTimeSeriesNames.addAll(timeSeriesNames);
        return getMappedTimeSeriesNames();
    }

    public Set<String> setIgnoreLimitsTimeSeriesNames(Set<String> timeSeriesNames) {
        ignoreLimitsTimeSeriesNames.addAll(timeSeriesNames);
        return getIgnoreLimitsTimeSeriesNames();
    }

    public Set<MappingKey> getEquipmentIds(String timeSeriesName) {
        return timeSeriesToEquipmentMap.get(timeSeriesName);
    }

    public String getTimeSeriesName(MappingKey key) {
        return equipmentToTimeSeriesMap.get(key);
    }

    public DistributionKey getDistributionKey(MappingKey equipmentId) {
        return distributionKeys.get(equipmentId);
    }

    public void addEquipmentTimeSeries(String timeSeriesName, MappingVariable variable, String id) {
        MappingKey mappingKey = new MappingKey(variable, id);
        equipmentToTimeSeriesMap.computeIfPresent(mappingKey, (k, v) -> {
            timeSeriesToEquipmentMap.get(v).remove(mappingKey);
            return v;
        });
        equipmentToTimeSeriesMap.put(mappingKey, timeSeriesName);
        timeSeriesToEquipmentMap.computeIfAbsent(timeSeriesName, k -> new LinkedHashSet<>()).add(mappingKey);
    }

    private static List<String> getMultimapValue(Map<MappingKey, List<String>> multimap, MappingKey key) {
        return multimap.computeIfAbsent(key, k -> new LinkedList<>());
    }

    private void addMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, MappingVariable variable,
                            Map<MappingKey, List<String>> timeSerieToEquipmentsMapping,
                            Map<MappingKey, List<String>> equipmentToTimeSeriesMapping) {

        MappingKey timeSerieToEquipmentsKey = new MappingKey(variable, timeSeriesName);
        if (equipmentId != null) {
            MappingKey equipmentToTimeSeriesKey = new MappingKey(variable, equipmentId);
            List<String> timeSeriesAlreadyMappedToThisEquipment = getMultimapValue(equipmentToTimeSeriesMapping, equipmentToTimeSeriesKey);
            if (!timeSeriesAlreadyMappedToThisEquipment.isEmpty()) {
                // remove old mapping
                String oldTimeSeriesName = timeSeriesAlreadyMappedToThisEquipment.get(0);
                MappingKey oldTimeSeriesKey = new MappingKey(variable, oldTimeSeriesName);
                List<String> equipmentsMappedToOldTimeSeries = getMultimapValue(timeSerieToEquipmentsMapping, oldTimeSeriesKey);
                equipmentsMappedToOldTimeSeries.remove(equipmentId);
                if (equipmentsMappedToOldTimeSeries.isEmpty()) {
                    timeSerieToEquipmentsMapping.remove(oldTimeSeriesKey);
                }
            }
            timeSeriesAlreadyMappedToThisEquipment.add(0, timeSeriesName);

            // add new mapping
            getMultimapValue(timeSerieToEquipmentsMapping, timeSerieToEquipmentsKey).add(equipmentId);

            distributionKeys.put(equipmentToTimeSeriesKey, distributionKey);
        } else {
            getMultimapValue(timeSerieToEquipmentsMapping, timeSerieToEquipmentsKey);
        }

        mappedTimeSeriesNames.add(timeSeriesName);

    }

    public void addEquipmentMapping(MappableEquipmentType equipmentType, String timeSeriesName, String equipmentId, DistributionKey distributionKey,
                                    MappingVariable variable) {
        switch (equipmentType) {
            case GENERATOR:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToGeneratorsMapping, generatorToTimeSeriesMapping);
                if (variable == EquipmentVariable.targetP) {
                    unmappedGenerators.remove(equipmentId);
                } else if (variable == EquipmentVariable.minP) {
                    unmappedMinPGenerators.remove(equipmentId);
                } else if (variable == EquipmentVariable.maxP) {
                    unmappedMaxPGenerators.remove(equipmentId);
                }
                break;
            case LOAD:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToLoadsMapping, loadToTimeSeriesMapping);
                if (variable == EquipmentVariable.p0) {
                    unmappedLoads.remove(equipmentId);
                    unmappedFixedActivePowerLoads.remove(equipmentId);
                    unmappedVariableActivePowerLoads.remove(equipmentId);
                } else if (variable == EquipmentVariable.fixedActivePower) {
                    unmappedLoads.remove(equipmentId);
                    unmappedFixedActivePowerLoads.remove(equipmentId);
                } else if (variable == EquipmentVariable.variableActivePower) {
                    unmappedLoads.remove(equipmentId);
                    unmappedVariableActivePowerLoads.remove(equipmentId);
                }
                break;
            case BOUNDARY_LINE:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToDanglingLinesMapping, danglingLineToTimeSeriesMapping);
                if (variable == EquipmentVariable.p0) {
                    unmappedDanglingLines.remove(equipmentId);
                }
                break;
            case HVDC_LINE:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToHvdcLinesMapping, hvdcLineToTimeSeriesMapping);
                if (variable == EquipmentVariable.activePowerSetpoint) {
                    unmappedHvdcLines.remove(equipmentId);
                } else if (variable == EquipmentVariable.minP) {
                    unmappedMinPHvdcLines.remove(equipmentId);
                } else if (variable == EquipmentVariable.maxP) {
                    unmappedMaxPHvdcLines.remove(equipmentId);
                }
                break;
            case SWITCH:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                    timeSeriesToBreakersMapping, breakerToTimeSeriesMapping);
                break;
            case PST:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToPstMapping, pstToTimeSeriesMapping);
                unmappedPst.remove(equipmentId);
                break;
            default:
                throw new AssertionError();
        }
    }

    public void addUnmappedEquipment(MappableEquipmentType equipmentType, String equipmentId) {
        switch (equipmentType) {
            case GENERATOR:
                ignoredUnmappedGenerators.add(equipmentId);
                break;
            case LOAD:
                ignoredUnmappedLoads.add(equipmentId);
                break;
            case BOUNDARY_LINE:
                ignoredUnmappedDanglingLines.add(equipmentId);
                break;
            case HVDC_LINE:
                ignoredUnmappedHvdcLines.add(equipmentId);
            case PST:
                ignoredUnmappedPst.add(equipmentId);
                break;
            default:
                throw new AssertionError();
        }
    }

    public void addEquipmentTimeSeries(MappableEquipmentType equipmentType, String equipmentId, Set<EquipmentVariable> equipmentVariables) {
        for (EquipmentVariable equipmentVariable : equipmentVariables) {
            MappingKey mappingKey = new MappingKey(equipmentVariable, equipmentId);
            switch (equipmentType) {
                case GENERATOR:
                    generatorTimeSeries.add(mappingKey);
                    break;
                case LOAD:
                    loadTimeSeries.add(mappingKey);
                    break;
                case BOUNDARY_LINE:
                    danglingLineTimeSeries.add(mappingKey);
                    break;
                case PST:
                    pstTimeSeries.add(mappingKey);
                    break;
                case HVDC_LINE:
                    hvdcLineTimeSeries.add(mappingKey);
                    break;
                case SWITCH:
                    breakerTimeSeries.add(mappingKey);
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }

    public void addIgnoreLimits(String timeSeriesName) {
        ignoreLimitsTimeSeriesNames.add(timeSeriesName);
    }

    public Map<MappingKey, List<String>> setTimeSeriesToGeneratorsMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToGeneratorsMapping.putAll(map);
        return getTimeSeriesToGeneratorsMapping();
    }

    public Map<MappingKey, List<String>> setTimeSeriesToLoadsMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToLoadsMapping.putAll(map);
        return getTimeSeriesToLoadsMapping();
    }

    public Map<MappingKey, List<String>> setTimeSeriesToDanglingLinesMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToDanglingLinesMapping.putAll(map);
        return getTimeSeriesToDanglingLinesMapping();
    }

    public Map<MappingKey, List<String>> setTimeSeriesToHvdcLinesMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToHvdcLinesMapping.putAll(map);
        return getTimeSeriesToHvdcLinesMapping();
    }

    public Map<MappingKey, List<String>> setTimeSeriesToPstMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToPstMapping.putAll(map);
        return getTimeSeriesToPstMapping();
    }

    public Map<MappingKey, List<String>> setTimeSeriesToBreakersMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToBreakersMapping.putAll(map);
        return getTimeSeriesToBreakersMapping();
    }

    public Map<MappingKey, List<String>> getTimeSeriesToGeneratorsMapping() {
        return Collections.unmodifiableMap(timeSeriesToGeneratorsMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToLoadsMapping() {
        return Collections.unmodifiableMap(timeSeriesToLoadsMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToDanglingLinesMapping() {
        return Collections.unmodifiableMap(timeSeriesToDanglingLinesMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToHvdcLinesMapping() {
        return Collections.unmodifiableMap(timeSeriesToHvdcLinesMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToPstMapping() {
        return Collections.unmodifiableMap(timeSeriesToPstMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToBreakersMapping() {
        return Collections.unmodifiableMap(timeSeriesToBreakersMapping);
    }

    public Map<MappingKey, List<String>> setGeneratorToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        generatorToTimeSeriesMapping.putAll(map);
        return getGeneratorToTimeSeriesMapping();
    }

    public Map<MappingKey, List<String>> setLoadToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        loadToTimeSeriesMapping.putAll(map);
        return getLoadToTimeSeriesMapping();
    }

    public Map<MappingKey, List<String>> setDanglingLineToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        danglingLineToTimeSeriesMapping.putAll(map);
        return getDanglingLineToTimeSeriesMapping();
    }

    public Map<MappingKey, List<String>> setBreakerToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        breakerToTimeSeriesMapping.putAll(map);
        return getBreakerToTimeSeriesMapping();
    }

    public Map<MappingKey, List<String>> setHvdcLineToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        hvdcLineToTimeSeriesMapping.putAll(map);
        return getHvdcLineToTimeSeriesMapping();
    }

    public Map<MappingKey, List<String>> setPstToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        pstToTimeSeriesMapping.putAll(map);
        return getPstToTimeSeriesMapping();
    }

    public Map<MappingKey, List<String>> getGeneratorToTimeSeriesMapping() {
        return Collections.unmodifiableMap(generatorToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getLoadToTimeSeriesMapping() {
        return Collections.unmodifiableMap(loadToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getDanglingLineToTimeSeriesMapping() {
        return Collections.unmodifiableMap(danglingLineToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getBreakerToTimeSeriesMapping() {
        return Collections.unmodifiableMap(breakerToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getHvdcLineToTimeSeriesMapping() {
        return Collections.unmodifiableMap(hvdcLineToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getPstToTimeSeriesMapping() {
        return Collections.unmodifiableMap(pstToTimeSeriesMapping);
    }

    public Set<MappingKey> setGeneratorTimeSeries(Set<MappingKey> set) {
        generatorTimeSeries.addAll(set);
        return getGeneratorTimeSeries();
    }

    public Set<MappingKey> setLoadTimeSeries(Set<MappingKey> set) {
        loadTimeSeries.addAll(set);
        return getLoadTimeSeries();
    }

    public Set<MappingKey> setDanglingLineTimeSeries(Set<MappingKey> set) {
        danglingLineTimeSeries.addAll(set);
        return getDanglingLineTimeSeries();
    }

    public Set<MappingKey> setHvdcLineTimeSeries(Set<MappingKey> set) {
        hvdcLineTimeSeries.addAll(set);
        return getHvdcLineTimeSeries();
    }

    public Set<MappingKey> setPstTimeSeries(Set<MappingKey> set) {
        pstTimeSeries.addAll(set);
        return getPstTimeSeries();
    }

    public Set<MappingKey> setBreakerTimeSeries(Set<MappingKey> set) {
        breakerTimeSeries.addAll(set);
        return getBreakerTimeSeries();
    }

    public Set<String> setUnmappedGenerators(Set<String> set) {
        unmappedGenerators.addAll(set);
        return getUnmappedGenerators();
    }

    public Set<String> setUnmappedLoads(Set<String> set) {
        unmappedLoads.addAll(set);
        return getUnmappedLoads();
    }

    public Set<String> setUnmappedFixedActivePowerLoads(Set<String> set) {
        unmappedFixedActivePowerLoads.addAll(set);
        return getUnmappedFixedActivePowerLoads();
    }

    public Set<String> setUnmappedVariableActivePowerLoads(Set<String> set) {
        unmappedVariableActivePowerLoads.addAll(set);
        return getUnmappedVariableActivePowerLoads();
    }

    public Set<String> setUnmappedDanglingLines(Set<String> set) {
        unmappedDanglingLines.addAll(set);
        return getUnmappedDanglingLines();
    }

    public Set<String> setUnmappedHvdcLines(Set<String> set) {
        unmappedHvdcLines.addAll(set);
        return getUnmappedHvdcLines();
    }

    public Set<String> setUnmappedPst(Set<String> set) {
        unmappedPst.addAll(set);
        return getUnmappedPst();
    }

    public Set<String> setUnmappedMinPGenerators(Set<String> set) {
        unmappedMinPGenerators.addAll(set);
        return getUnmappedMinPGenerators();
    }

    public Set<String> setUnmappedMaxPGenerators(Set<String> set) {
        unmappedMaxPGenerators.addAll(set);
        return getUnmappedMaxPGenerators();
    }

    public Set<String> setUnmappedMinPHvdcLines(Set<String> set) {
        unmappedMinPHvdcLines.addAll(set);
        return getUnmappedMinPHvdcLines();
    }

    public Set<String> setUnmappedMaxPHvdcLines(Set<String> set) {
        unmappedMaxPHvdcLines.addAll(set);
        return getUnmappedMaxPHvdcLines();
    }

    public Set<MappingKey> getGeneratorTimeSeries() {
        return Collections.unmodifiableSet(generatorTimeSeries);
    }

    public Set<MappingKey> getLoadTimeSeries() {
        return Collections.unmodifiableSet(loadTimeSeries);
    }

    public Set<MappingKey> getDanglingLineTimeSeries() {
        return Collections.unmodifiableSet(danglingLineTimeSeries);
    }

    public Set<MappingKey> getHvdcLineTimeSeries() {
        return Collections.unmodifiableSet(hvdcLineTimeSeries);
    }

    public Set<MappingKey> getPstTimeSeries() {
        return Collections.unmodifiableSet(pstTimeSeries);
    }

    public Set<MappingKey> getBreakerTimeSeries() {
        return Collections.unmodifiableSet(breakerTimeSeries);
    }

    public Set<String> getUnmappedGenerators() {
        return Collections.unmodifiableSet(unmappedGenerators);
    }

    public Set<String> getUnmappedLoads() {
        return Collections.unmodifiableSet(unmappedLoads);
    }

    public Set<String> getUnmappedFixedActivePowerLoads() {
        return Collections.unmodifiableSet(unmappedFixedActivePowerLoads);
    }

    public Set<String> getUnmappedVariableActivePowerLoads() {
        return Collections.unmodifiableSet(unmappedVariableActivePowerLoads);
    }

    public Set<String> getUnmappedMinPGenerators() {
        return Collections.unmodifiableSet(unmappedMinPGenerators);
    }

    public Set<String> getUnmappedMaxPGenerators() {
        return Collections.unmodifiableSet(unmappedMaxPGenerators);
    }

    public Set<String> getUnmappedMinPHvdcLines() {
        return Collections.unmodifiableSet(unmappedMinPHvdcLines);
    }

    public Set<String> getUnmappedMaxPHvdcLines() {
        return Collections.unmodifiableSet(unmappedMaxPHvdcLines);
    }

    public Set<String> getUnmappedDanglingLines() {
        return Collections.unmodifiableSet(unmappedDanglingLines);
    }

    public Set<String> getUnmappedHvdcLines() {
        return Collections.unmodifiableSet(unmappedHvdcLines);
    }

    public Set<String> getUnmappedPst() {
        return Collections.unmodifiableSet(unmappedPst);
    }

    public Set<String> setIgnoredUnmappedGenerators(Set<String> set) {
        ignoredUnmappedGenerators.addAll(set);
        return getIgnoredUnmappedGenerators();
    }

    public Set<String> setIgnoredUnmappedLoads(Set<String> set) {
        ignoredUnmappedLoads.addAll(set);
        return getIgnoredUnmappedLoads();
    }

    public Set<String> setIgnoredUnmappedDanglingLines(Set<String> set) {
        ignoredUnmappedDanglingLines.addAll(set);
        return getIgnoredUnmappedDanglingLines();
    }

    public Set<String> setIgnoredUnmappedHvdcLines(Set<String> set) {
        ignoredUnmappedHvdcLines.addAll(set);
        return getIgnoredUnmappedHvdcLines();
    }

    public Set<String> setIgnoredUnmappedPst(Set<String> set) {
        ignoredUnmappedPst.addAll(set);
        return getIgnoredUnmappedPst();
    }

    public Set<String> getIgnoredUnmappedGenerators() {
        return Collections.unmodifiableSet(ignoredUnmappedGenerators);
    }

    public Set<String> getIgnoredUnmappedLoads() {
        return Collections.unmodifiableSet(ignoredUnmappedLoads);
    }

    public Set<String> getIgnoredUnmappedDanglingLines() {
        return Collections.unmodifiableSet(ignoredUnmappedDanglingLines);
    }

    public Set<String> getIgnoredUnmappedHvdcLines() {
        return Collections.unmodifiableSet(ignoredUnmappedHvdcLines);
    }

    public Set<String> getIgnoredUnmappedPst() {
        return Collections.unmodifiableSet(ignoredUnmappedPst);
    }

    public Set<String> setDisconnectedGenerators(Set<String> set) {
        disconnectedGenerators.addAll(set);
        return getDisconnectedGenerators();
    }

    public Set<String> setDisconnectedLoads(Set<String> set) {
        disconnectedLoads.addAll(set);
        return getDisconnectedLoads();
    }

    public Set<String> setDisconnectedDanglingLines(Set<String> set) {
        disconnectedDanglingLines.addAll(set);
        return getDisconnectedDanglingLines();
    }

    public Set<String> getDisconnectedGenerators() {
        return Collections.unmodifiableSet(disconnectedGenerators);
    }

    public Set<String> getDisconnectedLoads() {
        return Collections.unmodifiableSet(disconnectedLoads);
    }

    public Set<String> getDisconnectedDanglingLines() {
        return Collections.unmodifiableSet(disconnectedDanglingLines);
    }

    public Set<String> setOutOfMainCcGenerators(Set<String> set) {
        outOfMainCcGenerators.addAll(set);
        return getOutOfMainCcGenerators();
    }

    public Set<String> setOutOfMainCcLoads(Set<String> set) {
        outOfMainCcLoads.addAll(set);
        return getOutOfMainCcLoads();
    }

    public Set<String> setOutOfMainCcDanglingLines(Set<String> set) {
        outOfMainCcDanglingLines.addAll(set);
        return getOutOfMainCcDanglingLines();
    }

    public Set<String> getOutOfMainCcGenerators() {
        return Collections.unmodifiableSet(outOfMainCcGenerators);
    }

    public Set<String> getOutOfMainCcLoads() {
        return Collections.unmodifiableSet(outOfMainCcLoads);
    }

    public Set<String> getOutOfMainCcDanglingLines() {
        return Collections.unmodifiableSet(outOfMainCcDanglingLines);
    }

    public boolean isStoreTimeSeriesEquipment(MappingVariable variable, Identifiable<?> identifiable) {
        MappingKey key = new MappingKey(variable, identifiable.getId());
        if (identifiable instanceof Generator) {
            return generatorTimeSeries.contains(key);
        } else if (identifiable instanceof Load) {
            return loadTimeSeries.contains(key);
        } else if (identifiable instanceof HvdcLine) {
            return hvdcLineTimeSeries.contains(key);
        } else if (identifiable instanceof TwoWindingsTransformer) {
            return pstTimeSeries.contains(key);
        } else if (identifiable instanceof DanglingLine) {
            return danglingLineTimeSeries.contains(key);
        } else if (identifiable instanceof Switch) {
            return breakerTimeSeries.contains(key);
        }
        return false;
    }

    private static void init(Injection<?> connectable, Set<String> unmappedEquipments, Set<String> disconnectedEquipments, Set<String> outOfMainCcEquipments) {
        Bus bus = connectable.getTerminal().getBusView().getBus();
        if (bus != null) {
            if (bus.isInMainConnectedComponent()) {
                unmappedEquipments.add(connectable.getId());
            } else {
                outOfMainCcEquipments.add(connectable.getId());
            }
        } else {
            disconnectedEquipments.add(connectable.getId());
        }
    }

    private static void init(Load load, Set<String> unmappedFixedActivePowerLoads, Set<String> unmappedVariableActivePowerLoads) {
        Bus bus = load.getTerminal().getBusView().getBus();
        if (bus != null && bus.isInMainConnectedComponent() && load.getExtension(LoadDetail.class) != null) {
            unmappedFixedActivePowerLoads.add(load.getId());
            unmappedVariableActivePowerLoads.add(load.getId());
        }
    }

    public boolean isMappingComplete() {
        return getNbUnmapped(unmappedGenerators, ignoredUnmappedGenerators)
                + getNbUnmapped(unmappedLoads, ignoredUnmappedLoads)
                + getNbUnmapped(unmappedDanglingLines, ignoredUnmappedDanglingLines)
                + getNbUnmapped(unmappedHvdcLines, ignoredUnmappedHvdcLines)
                + getNbUnmapped(unmappedPst, ignoredUnmappedPst) == 0;
    }

    public Iterable<String> findUsedTimeSeriesNames() {
        return Iterables.concat(mappedTimeSeriesNames,
                                timeSeriesToEquipmentMap.keySet(),
                                distributionKeys.values().stream()
                                        .filter(distributionKey -> distributionKey instanceof TimeSeriesDistributionKey)
                                        .map(distributionKey -> ((TimeSeriesDistributionKey) distributionKey).getTimeSeriesName())
                                        .collect(Collectors.toSet()));
    }

    private Set<String> findTimeSeriesNamesToLoad() {
        return findTimeSeriesNamesToLoad(findUsedTimeSeriesNames());
    }

    private Set<String> findTimeSeriesNamesToLoad(Iterable<String> usedTimeSeriesNames) {
        Set<String> timeSeriesNamesToLoad = new HashSet<>();

        // load data of each mapped time series and for each of the equipment time series
        for (String timeSeriesName : usedTimeSeriesNames) {
            NodeCalc nodeCalc = timeSeriesNodes.get(timeSeriesName);
            if (nodeCalc != null) {
                // it is a calculated time series
                // find stored time series used in this calculated time series
                timeSeriesNamesToLoad.addAll(TimeSeriesNames.list(nodeCalc));
            } else {
                // it is a stored time series
                timeSeriesNamesToLoad.add(timeSeriesName);
            }
        }

        return timeSeriesNamesToLoad;
    }

    public Map<MappingKey, Set<String>> findMappedTimeSeries(Map<MappingKey, List<String>> timeSeriesToEquipments) {
        Map<MappingKey, Set<String>> mappedTimeSeries = new LinkedHashMap<>();
        timeSeriesToEquipments.keySet().forEach(mappingKey -> {
            NodeCalc nodeCalc = timeSeriesNodes.get(mappingKey.getId());
            mappedTimeSeries.put(mappingKey, nodeCalc == null ? Collections.emptySet() : TimeSeriesNames.list(nodeCalc));
        });
        return mappedTimeSeries;
    }

    public Map<MappingKey, Set<String>> findDistributionKeyTimeSeries() {
        Map<MappingKey, Set<String>> mappedTimeSeries = new LinkedHashMap<>();
        distributionKeys.entrySet().stream()
            .filter(e -> e.getValue() instanceof TimeSeriesDistributionKey)
            .forEach(e -> {
                String timeSeriesName = ((TimeSeriesDistributionKey) e.getValue()).getTimeSeriesName();
                MappingVariable mappingVariable = e.getKey().getMappingVariable();
                NodeCalc nodeCalc = timeSeriesNodes.get(timeSeriesName);
                mappedTimeSeries.put(new MappingKey(mappingVariable, timeSeriesName), nodeCalc == null ? Collections.emptySet() : TimeSeriesNames.list(nodeCalc));
            });
        return mappedTimeSeries;
    }

    public TimeSeriesTable loadToTable(NavigableSet<Integer> versions, ReadOnlyTimeSeriesStore store, Range<Integer> pointRange) {
        return loadToTable(versions, store, pointRange, findUsedTimeSeriesNames());
    }

    public TimeSeriesTable loadToTable(NavigableSet<Integer> versions, ReadOnlyTimeSeriesStore store, Range<Integer> pointRange, Iterable<String> usedTimeSeriesNames) {
        Set<String> timeSeriesNamesToLoad = findTimeSeriesNamesToLoad(usedTimeSeriesNames);

        TimeSeriesIndex index = checkIndexUnicity(store, timeSeriesNamesToLoad);
        checkValues(store, versions, timeSeriesNamesToLoad);

        TimeSeriesTable table = new TimeSeriesTable(versions.first(), versions.last(), index);

        // load time series series
        for (int version : versions) {
            List<DoubleTimeSeries> loadedTimeSeries = Collections.emptyList();
            if (!timeSeriesNamesToLoad.isEmpty()) {
                List<DoubleTimeSeries> timeSeriesList = store.getDoubleTimeSeries(timeSeriesNamesToLoad, version);
                int nbPointsToCompute = pointRange.upperEndpoint() - pointRange.lowerEndpoint() + 1;
                if (index.getPointCount() != nbPointsToCompute) {
                    // to avoid loading all values
                    int nbPointsToLoad = Math.max(pointRange.upperEndpoint() + 1, Math.min(index.getPointCount(), MIN_NUMBER_OF_POINTS));
                    try {
                        List<List<DoubleTimeSeries>> split = TimeSeries.split(timeSeriesList, nbPointsToLoad);
                        loadedTimeSeries = split.get(0);
                    } catch (RuntimeException e) {
                        LOGGER.warn("Failed to split timeSeries with {} pointsToLoad and {} pointsToCompute (reason : {}). Will take the whole time series", nbPointsToLoad, nbPointsToCompute, e.getMessage());
                        loadedTimeSeries = store.getDoubleTimeSeries(timeSeriesNamesToLoad, version);
                    }
                } else {
                    loadedTimeSeries = store.getDoubleTimeSeries(timeSeriesNamesToLoad, version);
                }
            }
            List<DoubleTimeSeries> timeSeriesToAddToTable = new ArrayList<>(loadedTimeSeries);
            ReadOnlyTimeSeriesStore storeCache = new ReadOnlyTimeSeriesStoreCache(loadedTimeSeries);
            TimeSeriesNameResolver resolver = new FromStoreTimeSeriesNameResolver(storeCache, version);

            // add calculated time series
            for (String mappedTimeSeriesName : usedTimeSeriesNames) {
                NodeCalc nodeCalc = timeSeriesNodes.get(mappedTimeSeriesName);
                if (nodeCalc != null) {
                    CalculatedTimeSeries timeSeries = new CalculatedTimeSeries(mappedTimeSeriesName, nodeCalc);
                    timeSeries.setTimeSeriesNameResolver(resolver);
                    timeSeriesToAddToTable.add(timeSeries);
                }
            }

            table.load(version, timeSeriesToAddToTable);
        }

        return table;
    }

    public TimeSeriesIndex checkIndexUnicity(ReadOnlyTimeSeriesStore store) {
        return checkIndexUnicity(store, findTimeSeriesNamesToLoad());
    }

    public static TimeSeriesIndex checkIndexUnicity(ReadOnlyTimeSeriesStore store, Set<String> timeSeriesNamesToLoad) {
        Set<TimeSeriesIndex> indexes = timeSeriesNamesToLoad.isEmpty() ? Collections.emptySet()
                                                                       : store.getTimeSeriesMetadata(timeSeriesNamesToLoad)
                                                                              .stream()
                                                                              .map(TimeSeriesMetadata::getIndex)
                                                                              .filter(index -> !(index instanceof InfiniteTimeSeriesIndex))
                                                                              .collect(Collectors.toSet());

        if (indexes.isEmpty()) {
            return InfiniteTimeSeriesIndex.INSTANCE;
        } else if (indexes.size() > 1) {
            throw new TimeSeriesMappingException("Time series involved in the mapping must have the same index: "
                    + indexes);
        }
        return indexes.iterator().next();
    }

    public void checkValues(ReadOnlyTimeSeriesStore store, Set<Integer> versions) {
        checkValues(store, versions, findTimeSeriesNamesToLoad());
    }

    public static void checkValues(ReadOnlyTimeSeriesStore store, Set<Integer> versions, Set<String> timeSeriesNamesToLoad) {
        timeSeriesNamesToLoad.forEach(timeSeriesName -> {
            Set<Integer> existingVersions = store.getTimeSeriesDataVersions(timeSeriesName);
            if (!existingVersions.isEmpty() && !existingVersions.containsAll(versions)) {
                Set<Integer> undefinedVersions = new HashSet<>(versions);
                undefinedVersions.removeAll(existingVersions);
                throw new TimeSeriesMappingException("The time series store does not contain values for ts " + timeSeriesName + " and version(s) " + undefinedVersions);
            }
        });
    }

    public void checkMappedAndUnmapped(Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, Set<String> unmappedEquipments, Set<String> ignoredUnmappedEquipements) {
        for (Map.Entry<MappingKey, List<String>> e : equipmentToTimeSeriesMapping.entrySet()) {
            if (!unmappedEquipments.contains(e.getKey().getId()) && ignoredUnmappedEquipements.contains(e.getKey().getId())) {
                throw new TimeSeriesMappingException("Equipment '" + e.getKey().getId() + "' is declared unmapped but mapped on time series '" + e.getValue().get(0) + "'");
            }
        }
    }

    public void checkMappedVariables() {
        // check that mapping is consistent for each load
        Map<String, Set<MappingVariable>> mappedVariablesPerLoad = new HashMap<>();
        timeSeriesToLoadsMapping.forEach((mappingKey, ids) -> {
            for (String id : ids) {
                mappedVariablesPerLoad.computeIfAbsent(id, s -> new HashSet<>()).add(mappingKey.getMappingVariable());
            }
        });
        for (Map.Entry<String, Set<MappingVariable>> e : mappedVariablesPerLoad.entrySet()) {
            String id = e.getKey();
            Set<MappingVariable> variables = e.getValue();
            if (variables.contains(EquipmentVariable.p0)
                    && (variables.contains(EquipmentVariable.fixedActivePower) || variables.contains(EquipmentVariable.variableActivePower))) {
                throw new TimeSeriesMappingException("Load '" + id + "' is mapped on p0 and on one of the detailed variables (fixedActivePower/variableActivePower)");
            }
            if (variables.contains(EquipmentVariable.q0)
                    && (variables.contains(EquipmentVariable.fixedReactivePower) || variables.contains(EquipmentVariable.variableReactivePower))) {
                throw new TimeSeriesMappingException("Load '" + id + "' is mapped on q0 and on one of the detailed variables (fixedReactivePower/variableReactivePower)");
            }
        }

        checkMappedAndUnmapped(generatorToTimeSeriesMapping, unmappedGenerators, ignoredUnmappedGenerators);
        checkMappedAndUnmapped(loadToTimeSeriesMapping, unmappedLoads, ignoredUnmappedLoads);
        checkMappedAndUnmapped(danglingLineToTimeSeriesMapping, unmappedDanglingLines, ignoredUnmappedDanglingLines);
        checkMappedAndUnmapped(hvdcLineToTimeSeriesMapping, unmappedHvdcLines, ignoredUnmappedHvdcLines);
        checkMappedAndUnmapped(pstToTimeSeriesMapping, unmappedPst, ignoredUnmappedPst);
    }

    public Set<MappingKey> getEquipmentTimeSeriesKeys(Set<MappingKey> equipmentToTimeSeriesMapping, Set<MappingKey> equipmentTimeSeries) {
        Set<MappingKey> keySet = new HashSet<>(equipmentToTimeSeriesMapping);
        keySet.retainAll(equipmentTimeSeries);
        return keySet;
    }

    public Set<MappingKey> getNotMappedEquipmentTimeSeriesKeys(Set<MappingKey> equipmentToTimeSeriesMapping, Set<MappingKey> equipmentTimeSeries) {
        Set<MappingKey> keySet = new HashSet<>(equipmentTimeSeries);
        keySet.removeAll(equipmentToTimeSeriesMapping);
        return keySet;
    }

    public Set<MappingKey> getEquipmentTimeSeriesKeys() {
        Set<MappingKey> keys = new HashSet<>();
        keys.addAll(getEquipmentTimeSeriesKeys(getGeneratorToTimeSeriesMapping().keySet(), getGeneratorTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getLoadToTimeSeriesMapping().keySet(), getLoadTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getDanglingLineToTimeSeriesMapping().keySet(), getDanglingLineTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getHvdcLineToTimeSeriesMapping().keySet(), getHvdcLineTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getPstToTimeSeriesMapping().keySet(), getPstTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getBreakerToTimeSeriesMapping().keySet(), getBreakerTimeSeries()));
        return keys;
    }

    public Set<MappingKey> checkEquipmentTimeSeries() {
        Set<MappingKey> keys = new HashSet<>();
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getGeneratorToTimeSeriesMapping().keySet(), getGeneratorTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getLoadToTimeSeriesMapping().keySet(), getLoadTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getDanglingLineToTimeSeriesMapping().keySet(), getDanglingLineTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getHvdcLineToTimeSeriesMapping().keySet(), getHvdcLineTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getPstToTimeSeriesMapping().keySet(), getPstTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getBreakerToTimeSeriesMapping().keySet(), getBreakerTimeSeries()));
        return keys;
    }
}

