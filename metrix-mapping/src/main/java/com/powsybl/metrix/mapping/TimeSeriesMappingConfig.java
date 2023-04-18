/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.timeseries.ast.NodeCalc;

import java.util.*;
import java.util.stream.Collectors;

public class TimeSeriesMappingConfig {

    // Distribution keys
    protected final Map<MappingKey, DistributionKey> distributionKeys = new HashMap<>();

    // Time series to equipments
    protected final Map<MappingKey, List<String>> timeSeriesToGeneratorsMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToLoadsMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToDanglingLinesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToHvdcLinesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToPhaseTapChangersMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToBreakersMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToTransformersMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToLinesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToRatioTapChangersMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToLccConverterStationsMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> timeSeriesToVscConverterStationsMapping = new LinkedHashMap<>();

    // Equipment to time series
    protected final Map<MappingKey, List<String>> generatorToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> loadToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> danglingLineToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> hvdcLineToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> phaseTapChangerToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> breakerToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> transformerToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> lineToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> ratioTapChangerToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> lccConverterStationToTimeSeriesMapping = new LinkedHashMap<>();

    protected final Map<MappingKey, List<String>> vscConverterStationToTimeSeriesMapping = new LinkedHashMap<>();

    //
    protected final Map<String, Set<MappingKey>> timeSeriesToEquipmentMap = new LinkedHashMap<>();

    protected final Map<MappingKey, String> equipmentToTimeSeriesMap = new LinkedHashMap<>();

    // Equipments for which time series must be provided
    protected final Set<MappingKey> generatorTimeSeries = new HashSet<>();

    protected final Set<MappingKey> loadTimeSeries = new HashSet<>();

    protected final Set<MappingKey> danglingLineTimeSeries = new HashSet<>();

    protected final Set<MappingKey> hvdcLineTimeSeries = new HashSet<>();

    protected final Set<MappingKey> phaseTapChangerTimeSeries = new HashSet<>();

    protected final Set<MappingKey> breakerTimeSeries = new HashSet<>();

    protected final Set<MappingKey> transformerTimeSeries = new HashSet<>();

    protected final Set<MappingKey> lineTimeSeries = new HashSet<>();

    protected final Set<MappingKey> ratioTapChangerTimeSeries = new HashSet<>();

    protected final Set<MappingKey> lccConverterStationTimeSeries = new HashSet<>();

    protected final Set<MappingKey> vscConverterStationTimeSeries = new HashSet<>();

    // Unmapped equipments
    protected final Set<String> unmappedGenerators = new HashSet<>();

    protected final Set<String> unmappedLoads = new HashSet<>();

    protected final Set<String> unmappedFixedActivePowerLoads = new HashSet<>();

    protected final Set<String> unmappedVariableActivePowerLoads = new HashSet<>();

    protected final Set<String> unmappedDanglingLines = new HashSet<>();

    protected final Set<String> unmappedHvdcLines = new HashSet<>();

    protected final Set<String> unmappedPhaseTapChangers = new HashSet<>();

    protected final Set<String> unmappedMinPGenerators = new HashSet<>();

    protected final Set<String> unmappedMaxPGenerators = new HashSet<>();

    protected final Set<String> unmappedMinPHvdcLines = new HashSet<>();

    protected final Set<String> unmappedMaxPHvdcLines = new HashSet<>();

    // Equipments to ignore concerning unmapped equipments
    protected final Set<String> ignoredUnmappedGenerators = new HashSet<>();

    protected final Set<String> ignoredUnmappedLoads = new HashSet<>();

    protected final Set<String> ignoredUnmappedDanglingLines = new HashSet<>();

    protected final Set<String> ignoredUnmappedHvdcLines = new HashSet<>();

    protected final Set<String> ignoredUnmappedPhaseTapChangers = new HashSet<>();

    // Disconnected equipments
    protected final Set<String> disconnectedGenerators = new HashSet<>();

    protected final Set<String> disconnectedLoads = new HashSet<>();

    protected final Set<String> disconnectedDanglingLines = new HashSet<>();

    // Out of main Cc equipments
    protected final Set<String> outOfMainCcGenerators = new HashSet<>();

    protected final Set<String> outOfMainCcLoads = new HashSet<>();

    protected final Set<String> outOfMainCcDanglingLines = new HashSet<>();

    // Planned outages
    protected final Map<String, Set<String>> timeSeriesToPlannedOutagesMapping = new LinkedHashMap<>();

    // Calculated time series
    protected final Map<String, NodeCalc> timeSeriesNodes = new HashMap<>();

    // Time series used in the mapping
    protected final Set<String> mappedTimeSeriesNames = new HashSet<>();

    // Time series to map with ignore limits option
    protected final Set<String> ignoreLimitsTimeSeriesNames = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash(
                distributionKeys,
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
            return distributionKeys.equals(other.getDistributionKeys())
                    && timeSeriesToGeneratorsMapping.equals(other.getTimeSeriesToGeneratorsMapping())
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

    private void init(Injection<?> connectable, Set<String> unmappedEquipments, Set<String> disconnectedEquipments, Set<String> outOfMainCcEquipments) {
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

    protected void init(Network network) {
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

    public TimeSeriesMappingConfig() {
    }

    public TimeSeriesMappingConfig(Network network) {
        init(network);
    }

    // Distribution keys
    public void setDistributionKeys(Map<MappingKey, DistributionKey> keys) {
        distributionKeys.putAll(keys);
    }

    public Map<MappingKey, DistributionKey> getDistributionKeys() {
        return distributionKeys;
    }

    public DistributionKey getDistributionKey(MappingKey equipmentId) {
        return distributionKeys.get(equipmentId);
    }

    // Time series to equipments
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

    // Equipment to time series
    public void setGeneratorToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        generatorToTimeSeriesMapping.putAll(map);
    }

    public void setLoadToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        loadToTimeSeriesMapping.putAll(map);
    }

    public void setDanglingLineToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        danglingLineToTimeSeriesMapping.putAll(map);
    }

    public void setHvdcLineToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        hvdcLineToTimeSeriesMapping.putAll(map);
    }

    public void setPhaseTapChangerToTimeSeriesMapping(Map<MappingKey, List<String>> map) {
        phaseTapChangerToTimeSeriesMapping.putAll(map);
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

    public Map<MappingKey, List<String>> getGeneratorToTimeSeriesMapping() {
        return Collections.unmodifiableMap(generatorToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getLoadToTimeSeriesMapping() {
        return Collections.unmodifiableMap(loadToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getDanglingLineToTimeSeriesMapping() {
        return Collections.unmodifiableMap(danglingLineToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getHvdcLineToTimeSeriesMapping() {
        return Collections.unmodifiableMap(hvdcLineToTimeSeriesMapping);
    }

    public Map<MappingKey, List<String>> getPhaseTapChangerToTimeSeriesMapping() {
        return Collections.unmodifiableMap(phaseTapChangerToTimeSeriesMapping);
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

    // Equipment time series (thresholds and costs)
    public void setTimeSeriesToEquipment(Map<String, Set<MappingKey>> map) {
        timeSeriesToEquipmentMap.putAll(map);
    }

    public Map<String, Set<MappingKey>> getTimeSeriesToEquipment() {
        return timeSeriesToEquipmentMap;
    }

    public Set<MappingKey> getEquipmentIds(String timeSeriesName) {
        return timeSeriesToEquipmentMap.get(timeSeriesName);
    }

    public void setEquipmentToTimeSeries(Map<MappingKey, String> map) {
        equipmentToTimeSeriesMap.putAll(map);
    }

    public Map<MappingKey, String> getEquipmentToTimeSeries() {
        return equipmentToTimeSeriesMap;
    }

    public String getTimeSeriesName(MappingKey key) {
        return equipmentToTimeSeriesMap.get(key);
    }

    public Set<String> getEquipmentIds() {
        return equipmentToTimeSeriesMap.keySet().stream().map(MappingKey::getId).collect(Collectors.toSet());
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

    // Equipments for which time series must be provided
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

    // Unmapped equipments
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

    public Set<String> getUnmappedDanglingLines() {
        return Collections.unmodifiableSet(unmappedDanglingLines);
    }

    public Set<String> getUnmappedHvdcLines() {
        return Collections.unmodifiableSet(unmappedHvdcLines);
    }

    public Set<String> getUnmappedPhaseTapChangers() {
        return Collections.unmodifiableSet(unmappedPhaseTapChangers);
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

    // Equipments to ignore concerning unmapped equipments
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

    // Disconnected equipments
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

    // Out of main Cc equipments
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

    // Planned outages
    public void setTimeSeriesToPlannedOutagesMapping(Map<String, Set<String>> map) {
        timeSeriesToPlannedOutagesMapping.putAll(map);
    }

    public Map<String, Set<String>> getTimeSeriesToPlannedOutagesMapping() {
        return Collections.unmodifiableMap(timeSeriesToPlannedOutagesMapping);
    }

    // Calculated time series
    public void setTimeSeriesNodes(Map<String, NodeCalc> nodes) {
        timeSeriesNodes.putAll(nodes);
    }

    public Map<String, NodeCalc> getTimeSeriesNodes() {
        return timeSeriesNodes;
    }

    public Set<String> getTimeSeriesNodesKeys() {
        return timeSeriesNodes.keySet();
    }

    // Time series used in the mapping
    public void setMappedTimeSeriesNames(Set<String> timeSeriesNames) {
        mappedTimeSeriesNames.addAll(timeSeriesNames);
    }

    public Set<String> getMappedTimeSeriesNames() {
        return Collections.unmodifiableSet(mappedTimeSeriesNames);
    }

    // Time series to map with ignore limits option
    public void setIgnoreLimitsTimeSeriesNames(Set<String> timeSeriesNames) {
        ignoreLimitsTimeSeriesNames.addAll(timeSeriesNames);
    }

    public Set<String> getIgnoreLimitsTimeSeriesNames() {
        return Collections.unmodifiableSet(ignoreLimitsTimeSeriesNames);
    }
}
