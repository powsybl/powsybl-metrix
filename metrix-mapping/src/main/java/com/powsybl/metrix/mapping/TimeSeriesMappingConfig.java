/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.CONNECTED_VALUE;
import static com.powsybl.metrix.mapping.TimeSeriesMapper.DISCONNECTED_VALUE;

public class TimeSeriesMappingConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMappingConfig.class);
    private static final int MIN_NUMBER_OF_POINTS = 50;

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
            getRegularIndex(store).ifPresent(calculatedTimeSeries::synchronize);
        }
        return calculatedTimeSeries;
    }

    public static Optional<TimeSeriesIndex> getRegularIndex(ReadOnlyTimeSeriesStore store) {
        return store
                .getTimeSeriesMetadata(store.getTimeSeriesNames(null))
                .stream()
                .map(TimeSeriesMetadata::getIndex)
                .filter(index -> !(index instanceof InfiniteTimeSeriesIndex))
                .findFirst();
    }

    public static int getNbUnmapped(Set<String> unmapped, Set<String> ignoredUnmapped) {
        Set<String> equipementSet = new HashSet<>(unmapped);
        equipementSet.removeAll(ignoredUnmapped);
        return equipementSet.size();
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

    public Set<String> getTimeSeriesNodesKeys() {
        return timeSeriesNodes.keySet();
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

    public Set<String> getEquipmentIds() {
        return getEquipmentToTimeSeries().keySet().stream().map(MappingKey::getId).collect(Collectors.toSet());
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

    public void removeEquipmentTimeSeries(MappingVariable variable, String id) {
        MappingKey mappingKey = new MappingKey(variable, id);
        String timeSeriesName = equipmentToTimeSeriesMap.get(mappingKey);
        equipmentToTimeSeriesMap.remove(mappingKey);
        Set<MappingKey> keys = timeSeriesToEquipmentMap.get(timeSeriesName);
        keys.remove(mappingKey);
        if (keys.isEmpty()) {
            timeSeriesToEquipmentMap.remove(timeSeriesName);
        }
    }

    public boolean isEquipmentThresholdDefined(MappingVariable variable, String id) {
        return equipmentToTimeSeriesMap.containsKey(new MappingKey(variable, id));
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

