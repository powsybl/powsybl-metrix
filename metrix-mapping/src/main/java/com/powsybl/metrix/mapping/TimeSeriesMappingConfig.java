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
import com.powsybl.metrix.mapping.json.JsonFieldName;
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
import java.util.stream.Stream;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.CONNECTED_VALUE;
import static com.powsybl.metrix.mapping.TimeSeriesMapper.DISCONNECTED_VALUE;

public class TimeSeriesMappingConfig implements TimeSeriesConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMappingConfig.class);
    private static final long DESERIALIZATION_EXTENDED_MAX_STACK_SIZE = 4096000L;
    private static final int MIN_NUMBER_OF_POINTS = 50;

    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<MappingKey, DistributionKey> distributionKeys = new HashMap<>();

    private final Map<MappingKey, List<String>> timeSeriesToGeneratorsMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToLoadsMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToDanglingLinesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToHvdcLinesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToPhaseTapChangersMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToBreakersMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToTransformersMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToLinesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToRatioTapChangersMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToLccConverterStationsMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> timeSeriesToVscConverterStationsMapping = new LinkedHashMap<>();

    private final Map<MappingKey, List<String>> generatorToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> loadToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> danglingLineToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> hvdcLineToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> phaseTapChangerToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> breakerToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> transformerToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> lineToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> ratioTapChangerToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> lccConverterStationToTimeSeriesMapping = new LinkedHashMap<>();
    private final Map<MappingKey, List<String>> vscConverterStationToTimeSeriesMapping = new LinkedHashMap<>();

    private final Set<MappingKey> generatorTimeSeries = new HashSet<>();
    private final Set<MappingKey> loadTimeSeries = new HashSet<>();
    private final Set<MappingKey> danglingLineTimeSeries = new HashSet<>();
    private final Set<MappingKey> hvdcLineTimeSeries = new HashSet<>();
    private final Set<MappingKey> phaseTapChangerTimeSeries = new HashSet<>();
    private final Set<MappingKey> breakerTimeSeries = new HashSet<>();
    private final Set<MappingKey> transformerTimeSeries = new HashSet<>();
    private final Set<MappingKey> lineTimeSeries = new HashSet<>();
    private final Set<MappingKey> ratioTapChangerTimeSeries = new HashSet<>();
    private final Set<MappingKey> lccConverterStationTimeSeries = new HashSet<>();
    private final Set<MappingKey> vscConverterStationTimeSeries = new HashSet<>();

    private final Set<String> unmappedGenerators = new HashSet<>();
    private final Set<String> unmappedLoads = new HashSet<>();
    private final Set<String> unmappedFixedActivePowerLoads = new HashSet<>();
    private final Set<String> unmappedVariableActivePowerLoads = new HashSet<>();
    private final Set<String> unmappedDanglingLines = new HashSet<>();
    private final Set<String> unmappedHvdcLines = new HashSet<>();
    private final Set<String> unmappedPhaseTapChangers = new HashSet<>();

    private final Set<String> unmappedMinPGenerators = new HashSet<>();
    private final Set<String> unmappedMaxPGenerators = new HashSet<>();
    private final Set<String> unmappedMinPHvdcLines = new HashSet<>();
    private final Set<String> unmappedMaxPHvdcLines = new HashSet<>();

    private final Set<String> ignoredUnmappedGenerators = new HashSet<>();
    private final Set<String> ignoredUnmappedLoads = new HashSet<>();
    private final Set<String> ignoredUnmappedDanglingLines = new HashSet<>();
    private final Set<String> ignoredUnmappedHvdcLines = new HashSet<>();
    private final Set<String> ignoredUnmappedPhaseTapChangers = new HashSet<>();

    private final Set<String> disconnectedGenerators = new HashSet<>();
    private final Set<String> disconnectedLoads = new HashSet<>();
    private final Set<String> disconnectedDanglingLines = new HashSet<>();

    private final Set<String> outOfMainCcGenerators = new HashSet<>();
    private final Set<String> outOfMainCcLoads = new HashSet<>();
    private final Set<String> outOfMainCcDanglingLines = new HashSet<>();

    private final Map<String, Set<MappingKey>> timeSeriesToEquipmentMap = new HashMap<>();
    private final Map<MappingKey, String> equipmentToTimeSeriesMap = new HashMap<>();

    private final Map<String, Set<String>> timeSeriesToPlannedOutagesMapping = new LinkedHashMap<>();

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
                timeSeriesToPhaseTapChangersMapping,
                timeSeriesToBreakersMapping,
                timeSeriesToTransformersMapping,
                timeSeriesToLinesMapping,
                timeSeriesToRatioTapChangersMapping,
                timeSeriesToLccConverterStationsMapping,
                timeSeriesToVscConverterStationsMapping,
                generatorToTimeSeriesMapping,
                loadToTimeSeriesMapping,
                danglingLineToTimeSeriesMapping,
                hvdcLineToTimeSeriesMapping,
                phaseTapChangerToTimeSeriesMapping,
                breakerToTimeSeriesMapping,
                transformerToTimeSeriesMapping,
                lineToTimeSeriesMapping,
                ratioTapChangerToTimeSeriesMapping,
                lccConverterStationToTimeSeriesMapping,
                vscConverterStationToTimeSeriesMapping,
                generatorTimeSeries,
                loadTimeSeries,
                danglingLineTimeSeries,
                hvdcLineTimeSeries,
                phaseTapChangerTimeSeries,
                breakerTimeSeries,
                transformerTimeSeries,
                lineTimeSeries,
                ratioTapChangerTimeSeries,
                lccConverterStationTimeSeries,
                vscConverterStationTimeSeries,
                unmappedGenerators,
                unmappedLoads,
                unmappedFixedActivePowerLoads,
                unmappedVariableActivePowerLoads,
                unmappedDanglingLines,
                unmappedHvdcLines,
                unmappedPhaseTapChangers,
                unmappedMinPGenerators,
                unmappedMaxPGenerators,
                unmappedMinPHvdcLines,
                unmappedMaxPHvdcLines,
                ignoredUnmappedGenerators,
                ignoredUnmappedLoads,
                ignoredUnmappedDanglingLines,
                ignoredUnmappedHvdcLines,
                ignoredUnmappedPhaseTapChangers,
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
                ignoreLimitsTimeSeriesNames,
                timeSeriesToPlannedOutagesMapping
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
                    && timeSeriesToPhaseTapChangersMapping.equals(other.getTimeSeriesToPhaseTapChangersMapping())
                    && timeSeriesToBreakersMapping.equals(other.getTimeSeriesToBreakersMapping())
                    && timeSeriesToTransformersMapping.equals(other.getTimeSeriesToTransformersMapping())
                    && timeSeriesToLinesMapping.equals(other.getTimeSeriesToLinesMapping())
                    && timeSeriesToRatioTapChangersMapping.equals(other.getTimeSeriesToRatioTapChangersMapping())
                    && timeSeriesToLccConverterStationsMapping.equals(other.getTimeSeriesToLccConverterStationsMapping())
                    && timeSeriesToVscConverterStationsMapping.equals(other.getTimeSeriesToVscConverterStationsMapping())
                    && generatorToTimeSeriesMapping.equals(other.getGeneratorToTimeSeriesMapping())
                    && loadToTimeSeriesMapping.equals(other.getLoadToTimeSeriesMapping())
                    && danglingLineToTimeSeriesMapping.equals(other.getDanglingLineToTimeSeriesMapping())
                    && hvdcLineToTimeSeriesMapping.equals(other.getHvdcLineToTimeSeriesMapping())
                    && phaseTapChangerToTimeSeriesMapping.equals(other.getPhaseTapChangerToTimeSeriesMapping())
                    && breakerToTimeSeriesMapping.equals(other.getBreakerToTimeSeriesMapping())
                    && transformerToTimeSeriesMapping.equals(other.getTransformerToTimeSeriesMapping())
                    && lineToTimeSeriesMapping.equals(other.getLineToTimeSeriesMapping())
                    && ratioTapChangerToTimeSeriesMapping.equals(other.getRatioTapChangerToTimeSeriesMapping())
                    && lccConverterStationToTimeSeriesMapping.equals(other.getLccConverterStationToTimeSeriesMapping())
                    && vscConverterStationToTimeSeriesMapping.equals(other.getVscConverterStationToTimeSeriesMapping())
                    && generatorTimeSeries.equals(other.getGeneratorTimeSeries())
                    && loadTimeSeries.equals(other.getLoadTimeSeries())
                    && danglingLineTimeSeries.equals(other.getDanglingLineTimeSeries())
                    && hvdcLineTimeSeries.equals(other.getHvdcLineTimeSeries())
                    && phaseTapChangerTimeSeries.equals(other.getPhaseTapChangerTimeSeries())
                    && breakerTimeSeries.equals(other.getBreakerTimeSeries())
                    && transformerTimeSeries.equals(other.getTransformerTimeSeries())
                    && lineTimeSeries.equals(other.getLineTimeSeries())
                    && ratioTapChangerTimeSeries.equals(other.getRatioTapChangerTimeSeries())
                    && lccConverterStationTimeSeries.equals(other.getLccConverterStationTimeSeries())
                    && vscConverterStationTimeSeries.equals(other.getVscConverterStationTimeSeries())
                    && unmappedGenerators.equals(other.getUnmappedGenerators())
                    && unmappedLoads.equals(other.getUnmappedLoads())
                    && unmappedFixedActivePowerLoads.equals(other.getUnmappedFixedActivePowerLoads())
                    && unmappedVariableActivePowerLoads.equals(other.getUnmappedVariableActivePowerLoads())
                    && unmappedDanglingLines.equals(other.getUnmappedDanglingLines())
                    && unmappedHvdcLines.equals(other.getUnmappedHvdcLines())
                    && unmappedPhaseTapChangers.equals(other.getUnmappedPhaseTapChangers())
                    && unmappedMinPGenerators.equals(other.getUnmappedMinPGenerators())
                    && unmappedMaxPGenerators.equals(other.getUnmappedMaxPGenerators())
                    && unmappedMinPHvdcLines.equals(other.getUnmappedMinPHvdcLines())
                    && unmappedMaxPHvdcLines.equals(other.getUnmappedMaxPHvdcLines())
                    && ignoredUnmappedGenerators.equals(other.getIgnoredUnmappedGenerators())
                    && ignoredUnmappedLoads.equals(other.getIgnoredUnmappedLoads())
                    && ignoredUnmappedDanglingLines.equals(other.getIgnoredUnmappedDanglingLines())
                    && ignoredUnmappedHvdcLines.equals(other.getIgnoredUnmappedHvdcLines())
                    && ignoredUnmappedPhaseTapChangers.equals(other.getIgnoredUnmappedPhaseTapChangers())
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
                    && ignoreLimitsTimeSeriesNames.equals(other.getIgnoreLimitsTimeSeriesNames())
                    && timeSeriesToPlannedOutagesMapping.equals(other.getTimeSeriesToPlannedOutagesMapping());
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
            CalculatedTimeSeries calculatedTimeSeries = createCalculatedTimeSeries(nodeCalc, version, store);
            return Arrays.stream(calculatedTimeSeries.toArray()).skip(computationRange.getFirstVariant()).limit(computationRange.getVariantCount());
        }).toArray();
        return Arrays.stream(values).sorted().skip(new BigDecimal(values.length / 2).longValue()).limit(1).findFirst().orElse(Double.NaN);
    }

    public static double getTimeSeriesMedian(String timeSeriesName, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, store, computationRange).sum()).sum();
    }

    private static DoubleStream getTimeSeriesStream(NodeCalc nodeCalc, int version, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        CalculatedTimeSeries calculatedTimeSeries = createCalculatedTimeSeries(nodeCalc, version, store);
        return Arrays.stream(calculatedTimeSeries.toArray()).skip(computationRange.getFirstVariant()).limit(computationRange.getVariantCount());
    }

    private static CalculatedTimeSeries createCalculatedTimeSeries(NodeCalc nodeCalc, int version, ReadOnlyTimeSeriesStore store) {
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
            Map<MappingKey, String> map = new HashMap<>();
            String timeSeriesName = null;
            MappingKey mappingKey = null;
            JsonToken token;
            while ((token = parser.nextToken()) != null && token != JsonToken.END_ARRAY) {
                switch (token) {
                    case FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        switch (JsonFieldName.nameOf(fieldName)) {
                            case MAPPING_KEYS:
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

    public TimeSeriesMappingConfig() {
    }

    public TimeSeriesMappingConfig(Network network) {
        // init unmapped
        network.getGenerators().forEach(generator -> init(generator, unmappedGenerators, disconnectedGenerators, outOfMainCcGenerators));
        network.getLoads().forEach(load -> init(load, unmappedLoads, disconnectedLoads, outOfMainCcLoads));
        network.getDanglingLines().forEach(danglingLine -> init(danglingLine, unmappedDanglingLines, disconnectedDanglingLines, outOfMainCcDanglingLines));
        network.getHvdcLines().forEach(hvdcLine -> unmappedHvdcLines.add(hvdcLine.getId()));
        network.getTwoWindingsTransformers().forEach(transformer -> {
            if (transformer.hasPhaseTapChanger()) {
                unmappedPhaseTapChangers.add(transformer.getId());
            }
        });
        network.getLoads().forEach(load -> init(load, unmappedFixedActivePowerLoads, unmappedVariableActivePowerLoads));
        unmappedMinPGenerators.addAll(unmappedGenerators);
        unmappedMaxPGenerators.addAll(unmappedGenerators);
        unmappedMinPHvdcLines.addAll(unmappedHvdcLines);
        unmappedMaxPHvdcLines.addAll(unmappedHvdcLines);
    }

    public void setDistributionKeys(Map<MappingKey, DistributionKey> keys) {
        distributionKeys.putAll(keys);
    }

    public Map<MappingKey, DistributionKey> getDistributionKeys() {
        return distributionKeys;
    }

    public Map<String, Set<MappingKey>> getTimeSeriesToEquipment() {
        return timeSeriesToEquipmentMap;
    }

    public void setTimeSeriesToEquipment(Map<String, Set<MappingKey>> map) {
        timeSeriesToEquipmentMap.putAll(map);
    }

    public Map<MappingKey, String> getEquipmentToTimeSeries() {
        return equipmentToTimeSeriesMap;
    }

    public void setEquipmentToTimeSeries(Map<MappingKey, String> map) {
        equipmentToTimeSeriesMap.putAll(map);
    }

    public Map<String, NodeCalc> getTimeSeriesNodes() {
        return timeSeriesNodes;
    }

    public void setTimeSeriesNodes(Map<String, NodeCalc> nodes) {
        timeSeriesNodes.putAll(nodes);
    }

    public Set<String> getMappedTimeSeriesNames() {
        return Collections.unmodifiableSet(mappedTimeSeriesNames);
    }

    public Set<String> getIgnoreLimitsTimeSeriesNames() {
        return Collections.unmodifiableSet(ignoreLimitsTimeSeriesNames);
    }

    public void setMappedTimeSeriesNames(Set<String> timeSeriesNames) {
        mappedTimeSeriesNames.addAll(timeSeriesNames);
    }

    public void setIgnoreLimitsTimeSeriesNames(Set<String> timeSeriesNames) {
        ignoreLimitsTimeSeriesNames.addAll(timeSeriesNames);
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
                                    EquipmentVariable variable) {
        switch (equipmentType) {
            case GENERATOR:
                addGeneratorMapping(timeSeriesName, equipmentId, distributionKey, variable);
                break;
            case LOAD:
                addLoadMapping(timeSeriesName, equipmentId, distributionKey, variable);
                break;
            case BOUNDARY_LINE:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToDanglingLinesMapping, danglingLineToTimeSeriesMapping);
                if (variable == EquipmentVariable.p0) {
                    unmappedDanglingLines.remove(equipmentId);
                }
                break;
            case HVDC_LINE:
                addHvdcLineMapping(timeSeriesName, equipmentId, distributionKey, variable);
                break;
            case SWITCH:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                    timeSeriesToBreakersMapping, breakerToTimeSeriesMapping);
                break;
            case PHASE_TAP_CHANGER:
            case PST:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToPhaseTapChangersMapping, phaseTapChangerToTimeSeriesMapping);
                if (variable == EquipmentVariable.phaseTapPosition) {
                    unmappedPhaseTapChangers.remove(equipmentId);
                }
                break;
            case TRANSFORMER:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToTransformersMapping, transformerToTimeSeriesMapping);
                break;
            case LINE:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToLinesMapping, lineToTimeSeriesMapping);
                break;
            case RATIO_TAP_CHANGER:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToRatioTapChangersMapping, ratioTapChangerToTimeSeriesMapping);
                break;
            case LCC_CONVERTER_STATION:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToLccConverterStationsMapping, lccConverterStationToTimeSeriesMapping);
                break;
            case VSC_CONVERTER_STATION:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        timeSeriesToVscConverterStationsMapping, vscConverterStationToTimeSeriesMapping);
                break;
            default:
                throw new AssertionError();
        }
    }

    private void addHvdcLineMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, EquipmentVariable variable) {
        addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                timeSeriesToHvdcLinesMapping, hvdcLineToTimeSeriesMapping);
        switch (variable) {
            case activePowerSetpoint:
                unmappedHvdcLines.remove(equipmentId);
                break;
            case minP:
                unmappedMinPHvdcLines.remove(equipmentId);
                break;
            case maxP:
                unmappedMaxPHvdcLines.remove(equipmentId);
                break;
            default:
                break;
        }
    }

    private void addLoadMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, EquipmentVariable variable) {
        addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                timeSeriesToLoadsMapping, loadToTimeSeriesMapping);
        switch (variable) {
            case p0:
                unmappedLoads.remove(equipmentId);
                unmappedFixedActivePowerLoads.remove(equipmentId);
                unmappedVariableActivePowerLoads.remove(equipmentId);
                break;
            case fixedActivePower:
                unmappedLoads.remove(equipmentId);
                unmappedFixedActivePowerLoads.remove(equipmentId);
                break;
            case variableActivePower:
                unmappedLoads.remove(equipmentId);
                unmappedVariableActivePowerLoads.remove(equipmentId);
                break;
            default:
                break;
        }
    }

    private void addGeneratorMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, EquipmentVariable variable) {
        addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                timeSeriesToGeneratorsMapping, generatorToTimeSeriesMapping);
        switch (variable) {
            case targetP:
                unmappedGenerators.remove(equipmentId);
                break;
            case minP:
                unmappedMinPGenerators.remove(equipmentId);
                break;
            case maxP:
                unmappedMaxPGenerators.remove(equipmentId);
                break;
            default:
                break;
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
                break;
            case PHASE_TAP_CHANGER:
                ignoredUnmappedPhaseTapChangers.add(equipmentId);
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
                case HVDC_LINE:
                    hvdcLineTimeSeries.add(mappingKey);
                    break;
                case SWITCH:
                    breakerTimeSeries.add(mappingKey);
                    break;
                case TRANSFORMER:
                    transformerTimeSeries.add(mappingKey);
                    break;
                case LINE:
                    lineTimeSeries.add(mappingKey);
                    break;
                case PHASE_TAP_CHANGER:
                    phaseTapChangerTimeSeries.add(mappingKey);
                    break;
                case RATIO_TAP_CHANGER:
                    ratioTapChangerTimeSeries.add(mappingKey);
                    break;
                case LCC_CONVERTER_STATION:
                    lccConverterStationTimeSeries.add(mappingKey);
                    break;
                case VSC_CONVERTER_STATION:
                    vscConverterStationTimeSeries.add(mappingKey);
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }

    public void addIgnoreLimits(String timeSeriesName) {
        ignoreLimitsTimeSeriesNames.add(timeSeriesName);
    }

    public void setTimeSeriesToGeneratorsMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToGeneratorsMapping.putAll(map);
    }

    public void setTimeSeriesToLoadsMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToLoadsMapping.putAll(map);
    }

    public void setTimeSeriesToDanglingLinesMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToDanglingLinesMapping.putAll(map);
    }

    public void setTimeSeriesToHvdcLinesMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToHvdcLinesMapping.putAll(map);
    }

    public void setTimeSeriesToPhaseTapChangersMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToPhaseTapChangersMapping.putAll(map);
    }

    public void setTimeSeriesToBreakersMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToBreakersMapping.putAll(map);
    }

    public void setTimeSeriesToTransformersMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToTransformersMapping.putAll(map);
    }

    public void setTimeSeriesToLinesMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToLinesMapping.putAll(map);
    }

    public void setTimeSeriesToRatioTapChangersMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToRatioTapChangersMapping.putAll(map);
    }

    public void setTimeSeriesToLccConverterStationsMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToLccConverterStationsMapping.putAll(map);
    }

    public void setTimeSeriesToVscConverterStationsMapping(Map<MappingKey, List<String>> map) {
        timeSeriesToVscConverterStationsMapping.putAll(map);
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

    public Map<MappingKey, List<String>> getTimeSeriesToPhaseTapChangersMapping() {
        return Collections.unmodifiableMap(timeSeriesToPhaseTapChangersMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToBreakersMapping() {
        return Collections.unmodifiableMap(timeSeriesToBreakersMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToTransformersMapping() {
        return Collections.unmodifiableMap(timeSeriesToTransformersMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToLinesMapping() {
        return Collections.unmodifiableMap(timeSeriesToLinesMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToRatioTapChangersMapping() {
        return Collections.unmodifiableMap(timeSeriesToRatioTapChangersMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToLccConverterStationsMapping() {
        return Collections.unmodifiableMap(timeSeriesToLccConverterStationsMapping);
    }

    public Map<MappingKey, List<String>> getTimeSeriesToVscConverterStationsMapping() {
        return Collections.unmodifiableMap(timeSeriesToVscConverterStationsMapping);
    }

    public Map<String, Set<String>> getTimeSeriesToPlannedOutagesMapping() {
        return Collections.unmodifiableMap(timeSeriesToPlannedOutagesMapping);
    }

    public void setTimeSeriesToPlannedOutagesMapping(Map<String, Set<String>> map) {
        timeSeriesToPlannedOutagesMapping.putAll(map);
    }

    public void addPlannedOutages(String timeSeriesName, Set<String> disconnectedIds) {
        timeSeriesToPlannedOutagesMapping.put(timeSeriesName, disconnectedIds);
    }

    public void setGeneratorToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        generatorToTimeSeriesMapping.putAll(map);
    }

    public void setLoadToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        loadToTimeSeriesMapping.putAll(map);
    }

    public void setDanglingLineToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        danglingLineToTimeSeriesMapping.putAll(map);
    }

    public void setBreakerToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        breakerToTimeSeriesMapping.putAll(map);
    }

    public void setTransformerToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        transformerToTimeSeriesMapping.putAll(map);
    }

    public void setLineToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        lineToTimeSeriesMapping.putAll(map);
    }

    public void setRatioTapChangerToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        ratioTapChangerToTimeSeriesMapping.putAll(map);
    }

    public void setLccConverterStationToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        lccConverterStationToTimeSeriesMapping.putAll(map);
    }

    public void setVscConverterStationToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        vscConverterStationToTimeSeriesMapping.putAll(map);
    }

    public void setHvdcLineToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        hvdcLineToTimeSeriesMapping.putAll(map);
    }

    public void setPhaseTapChangerToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        phaseTapChangerToTimeSeriesMapping.putAll(map);
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

    public Map<MappingKey, List<String>> getTransformerToTimeSeriesMapping() {
        return Collections.unmodifiableMap(transformerToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getLineToTimeSeriesMapping() {
        return Collections.unmodifiableMap(lineToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getRatioTapChangerToTimeSeriesMapping() {
        return Collections.unmodifiableMap(ratioTapChangerToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getLccConverterStationToTimeSeriesMapping() {
        return Collections.unmodifiableMap(lccConverterStationToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getVscConverterStationToTimeSeriesMapping() {
        return Collections.unmodifiableMap(vscConverterStationToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getHvdcLineToTimeSeriesMapping() {
        return Collections.unmodifiableMap(hvdcLineToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getPhaseTapChangerToTimeSeriesMapping() {
        return Collections.unmodifiableMap(phaseTapChangerToTimeSeriesMapping);
    }

    public void setGeneratorTimeSeries(Set<MappingKey> set) {
        generatorTimeSeries.addAll(set);
    }

    public void setLoadTimeSeries(Set<MappingKey> set) {
        loadTimeSeries.addAll(set);
    }

    public void setDanglingLineTimeSeries(Set<MappingKey> set) {
        danglingLineTimeSeries.addAll(set);
    }

    public void setHvdcLineTimeSeries(Set<MappingKey> set) {
        hvdcLineTimeSeries.addAll(set);
    }

    public void setPhaseTapChangerTimeSeries(Set<MappingKey> set) {
        phaseTapChangerTimeSeries.addAll(set);
    }

    public void setBreakerTimeSeries(Set<MappingKey> set) {
        breakerTimeSeries.addAll(set);
    }

    public void setTransformerTimeSeries(Set<MappingKey> set) {
        transformerTimeSeries.addAll(set);
    }

    public void setLineTimeSeries(Set<MappingKey> set) {
        lineTimeSeries.addAll(set);
    }

    public void setRatioTapChangerTimeSeries(Set<MappingKey> set) {
        ratioTapChangerTimeSeries.addAll(set);
    }

    public void setLccConverterStationTimeSeries(Set<MappingKey> set) {
        lccConverterStationTimeSeries.addAll(set);
    }

    public void setVscConverterStationTimeSeries(Set<MappingKey> set) {
        vscConverterStationTimeSeries.addAll(set);
    }

    public void setUnmappedGenerators(Set<String> set) {
        unmappedGenerators.addAll(set);
    }

    public void setUnmappedLoads(Set<String> set) {
        unmappedLoads.addAll(set);
    }

    public void setUnmappedFixedActivePowerLoads(Set<String> set) {
        unmappedFixedActivePowerLoads.addAll(set);
    }

    public void setUnmappedVariableActivePowerLoads(Set<String> set) {
        unmappedVariableActivePowerLoads.addAll(set);
    }

    public void setUnmappedDanglingLines(Set<String> set) {
        unmappedDanglingLines.addAll(set);
    }

    public void setUnmappedHvdcLines(Set<String> set) {
        unmappedHvdcLines.addAll(set);
    }

    public void setUnmappedPhaseTapChangers(Set<String> set) {
        unmappedPhaseTapChangers.addAll(set);
    }

    public void setUnmappedMinPGenerators(Set<String> set) {
        unmappedMinPGenerators.addAll(set);
    }

    public void setUnmappedMaxPGenerators(Set<String> set) {
        unmappedMaxPGenerators.addAll(set);
    }

    public void setUnmappedMinPHvdcLines(Set<String> set) {
        unmappedMinPHvdcLines.addAll(set);
    }

    public void setUnmappedMaxPHvdcLines(Set<String> set) {
        unmappedMaxPHvdcLines.addAll(set);
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

    public Set<MappingKey> getPhaseTapChangerTimeSeries() {
        return Collections.unmodifiableSet(phaseTapChangerTimeSeries);
    }

    public Set<MappingKey> getBreakerTimeSeries() {
        return Collections.unmodifiableSet(breakerTimeSeries);
    }

    public Set<MappingKey> getTransformerTimeSeries() {
        return Collections.unmodifiableSet(transformerTimeSeries);
    }

    public Set<MappingKey> getLineTimeSeries() {
        return Collections.unmodifiableSet(lineTimeSeries);
    }

    public Set<MappingKey> getRatioTapChangerTimeSeries() {
        return Collections.unmodifiableSet(ratioTapChangerTimeSeries);
    }

    public Set<MappingKey> getLccConverterStationTimeSeries() {
        return Collections.unmodifiableSet(lccConverterStationTimeSeries);
    }

    public Set<MappingKey> getVscConverterStationTimeSeries() {
        return Collections.unmodifiableSet(vscConverterStationTimeSeries);
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

    public Set<String> getUnmappedPhaseTapChangers() {
        return Collections.unmodifiableSet(unmappedPhaseTapChangers);
    }

    public void setIgnoredUnmappedGenerators(Set<String> set) {
        ignoredUnmappedGenerators.addAll(set);
    }

    public void setIgnoredUnmappedLoads(Set<String> set) {
        ignoredUnmappedLoads.addAll(set);
    }

    public void setIgnoredUnmappedDanglingLines(Set<String> set) {
        ignoredUnmappedDanglingLines.addAll(set);
    }

    public void setIgnoredUnmappedHvdcLines(Set<String> set) {
        ignoredUnmappedHvdcLines.addAll(set);
    }

    public void setIgnoredUnmappedPhaseTapChangers(Set<String> set) {
        ignoredUnmappedPhaseTapChangers.addAll(set);
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

    public Set<String> getIgnoredUnmappedPhaseTapChangers() {
        return Collections.unmodifiableSet(ignoredUnmappedPhaseTapChangers);
    }

    public void setDisconnectedGenerators(Set<String> set) {
        disconnectedGenerators.addAll(set);
    }

    public void setDisconnectedLoads(Set<String> set) {
        disconnectedLoads.addAll(set);
    }

    public void setDisconnectedDanglingLines(Set<String> set) {
        disconnectedDanglingLines.addAll(set);
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

    public void setOutOfMainCcGenerators(Set<String> set) {
        outOfMainCcGenerators.addAll(set);
    }

    public void setOutOfMainCcLoads(Set<String> set) {
        outOfMainCcLoads.addAll(set);
    }

    public void setOutOfMainCcDanglingLines(Set<String> set) {
        outOfMainCcDanglingLines.addAll(set);
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
                + getNbUnmapped(unmappedPhaseTapChangers, ignoredUnmappedPhaseTapChangers) == 0;
    }

    public Iterable<String> findUsedTimeSeriesNames() {
        return Iterables.concat(mappedTimeSeriesNames,
                                timeSeriesToEquipmentMap.keySet(),
                                timeSeriesToPlannedOutagesMapping.keySet(),
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
        checkMappedAndUnmapped(phaseTapChangerToTimeSeriesMapping, unmappedPhaseTapChangers, ignoredUnmappedPhaseTapChangers);
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
        keys.addAll(getEquipmentTimeSeriesKeys(getPhaseTapChangerToTimeSeriesMapping().keySet(), getPhaseTapChangerTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getBreakerToTimeSeriesMapping().keySet(), getBreakerTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getTransformerToTimeSeriesMapping().keySet(), getTransformerTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getLineToTimeSeriesMapping().keySet(), getLineTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getRatioTapChangerToTimeSeriesMapping().keySet(), getRatioTapChangerTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getLccConverterStationToTimeSeriesMapping().keySet(), getLccConverterStationTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(getVscConverterStationToTimeSeriesMapping().keySet(), getVscConverterStationTimeSeries()));
        return keys;
    }

    public Set<MappingKey> checkEquipmentTimeSeries() {
        Set<MappingKey> keys = new HashSet<>();
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getGeneratorToTimeSeriesMapping().keySet(), getGeneratorTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getLoadToTimeSeriesMapping().keySet(), getLoadTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getDanglingLineToTimeSeriesMapping().keySet(), getDanglingLineTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getHvdcLineToTimeSeriesMapping().keySet(), getHvdcLineTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getPhaseTapChangerToTimeSeriesMapping().keySet(), getPhaseTapChangerTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getBreakerToTimeSeriesMapping().keySet(), getBreakerTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getTransformerToTimeSeriesMapping().keySet(), getTransformerTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getLineToTimeSeriesMapping().keySet(), getLineTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getRatioTapChangerToTimeSeriesMapping().keySet(), getRatioTapChangerTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getLccConverterStationToTimeSeriesMapping().keySet(), getLccConverterStationTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(getVscConverterStationToTimeSeriesMapping().keySet(), getVscConverterStationTimeSeries()));
        return keys;
    }

    public static ReadOnlyTimeSeriesStore buildPlannedOutagesTimeSeriesStore(ReadOnlyTimeSeriesStore store, int version, Map<String, Set<String>> timeSeriesToPlannedOutagesMapping) {
        List<DoubleTimeSeries> doubleTimeSeries = new ArrayList<>();

        // Check if store already contains equipment outages time series
        List<String> timeSeries = timeSeriesToPlannedOutagesMapping.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(store::timeSeriesExists)
                .collect(Collectors.toList());
        if (timeSeries.isEmpty()) {
            return store;
        }

        // Build equipment planned outages time series
        LOGGER.info("Building equipment planned outages time series");
        TimeSeriesIndex index = checkIndexUnicity(store, timeSeriesToPlannedOutagesMapping.keySet());
        int nbPoints = index.getPointCount();
        for (Map.Entry<String, Set<String>> entry : timeSeriesToPlannedOutagesMapping.entrySet()) {
            String timeSeriesName = entry.getKey();
            Set<String> disconnectedIds = entry.getValue();

            StringTimeSeries plannedOutagesTimeSeries = store.getStringTimeSeries(timeSeriesName, version).orElseThrow(() -> new TimeSeriesException("Invalid planned outages time series name " + timeSeriesName));
            String[] array = plannedOutagesTimeSeries.toArray();
            for (String id : disconnectedIds) {
                double[] values = new double[nbPoints];
                Arrays.fill(values, CONNECTED_VALUE);
                for (int i = 0; i < nbPoints; i++) {
                    String[] ids = array[i].split(",");
                    if (Stream.of(ids).anyMatch(e -> e.equals(id))) {
                        values[i] = DISCONNECTED_VALUE;
                    }
                }
                DoubleTimeSeries doubleTs = new StoredDoubleTimeSeries(
                        new TimeSeriesMetadata(timeSeriesName + "_" + id, TimeSeriesDataType.DOUBLE, index),
                        new UncompressedDoubleDataChunk(0, values).tryToCompress());
                doubleTimeSeries.add(doubleTs);
            }
        }
        return new ReadOnlyTimeSeriesStoreAggregator(new ReadOnlyTimeSeriesStoreCache(doubleTimeSeries), store);

    }
}

