/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.metrix.commons.MappingVariable;
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException;
import com.powsybl.metrix.mapping.keys.DistributionKey;
import com.powsybl.metrix.mapping.utils.TimeSeriesUtils;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ast.FloatNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigEquipmentCsvWriter.getSubstation;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class TimeSeriesMappingConfigLoader implements DefaultGenericMetadata {

    private final TimeSeriesMappingConfig config;
    private final Set<String> existingTimeSeriesNames;

    public TimeSeriesMappingConfigLoader(TimeSeriesMappingConfig config, Set<String> existingTimeSeriesNames) {
        this.config = config;
        this.existingTimeSeriesNames = existingTimeSeriesNames;
    }

    private static List<String> getMultimapValue(Map<MappingKey, List<String>> multimap, MappingKey key) {
        return multimap.computeIfAbsent(key, k -> new LinkedList<>());
    }

    private static <K, V> Stream<K> keys(Map<K, V> map, V value) {
        return map.entrySet().stream()
                .filter(entry -> value == entry.getValue())
                .map(Map.Entry::getKey);
    }

    protected String computeGroupName(Injection<?> injection, EquipmentGroupType equipmentGroupType) {
        VoltageLevel voltageLevel = injection.getTerminal().getVoltageLevel();
        String name = StringUtils.EMPTY;
        if (!(equipmentGroupType instanceof SimpleEquipmentGroupType type)) {
            return name;
        }
        return switch (type) {
            case SUBSTATION -> getSubstation(voltageLevel);
            case VOLTAGE_LEVEL -> voltageLevel.getId();
        };
    }

    protected String computePowerTypeName(Generator ignored) {
        return StringUtils.EMPTY;
    }

    protected Map<String, String> getMetadataTags(NodeCalc nodeCalc, ReadOnlyTimeSeriesStore store) {
        Map<String, String> metadataTags = new HashMap<>();
        if (config.getTimeSeriesNodes().containsValue(nodeCalc)) {
            String tsName = keys(config.getTimeSeriesNodes(), nodeCalc).findFirst().orElseThrow();
            metadataTags.putAll(TimeSeriesUtils.getMetadataTags(tsName, config.getTimeSeriesNodeTags()));
        }
        if (nodeCalc instanceof TimeSeriesNameNodeCalc) {
            metadataTags.putAll(TimeSeriesUtils.getMetadataTags(nodeCalc, store));
        }
        return metadataTags;
    }

    protected void tag(NodeCalc nodeCalc, String tag, String parameter) {
        String tsName = keys(config.getTimeSeriesNodes(), nodeCalc).findFirst().orElseThrow();
        config.addTag(tsName, tag, parameter);
    }

    private static String nameWithDelimiter(String name) {
        return !name.isEmpty() ? "_" + name : StringUtils.EMPTY;
    }

    private static String computeSpecificName(String name) {
        if (name == null) {
            return StringUtils.EMPTY;
        }
        return name;
    }

    private static boolean isWithPowerType(Boolean withPowerType) {
        return withPowerType != null && withPowerType;
    }

    private String computeGeneratorTsName(Generator generator, EquipmentGroupType equipmentGroupType, Boolean withPowerType, String name) {
        String groupName = computeGroupName(generator, equipmentGroupType);
        String specificName = computeSpecificName(name);
        if (!isWithPowerType(withPowerType)) {
            return groupName + nameWithDelimiter(specificName);
        }
        String powerTypeName = computePowerTypeName(generator);
        return groupName + nameWithDelimiter(powerTypeName) + nameWithDelimiter(specificName);
    }

    private String computeLoadTsName(Load load, EquipmentGroupType equipmentGroupType, String name) {
        String groupName = computeGroupName(load, equipmentGroupType);
        String specificName = computeSpecificName(name);
        return groupName + nameWithDelimiter(specificName);
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
                String oldTimeSeriesName = timeSeriesAlreadyMappedToThisEquipment.getFirst();
                MappingKey oldTimeSeriesKey = new MappingKey(variable, oldTimeSeriesName);
                List<String> equipmentsMappedToOldTimeSeries = getMultimapValue(timeSerieToEquipmentsMapping, oldTimeSeriesKey);
                equipmentsMappedToOldTimeSeries.remove(equipmentId);
                if (equipmentsMappedToOldTimeSeries.isEmpty()) {
                    timeSerieToEquipmentsMapping.remove(oldTimeSeriesKey);
                }
            }
            timeSeriesAlreadyMappedToThisEquipment.addFirst(timeSeriesName);

            // add new mapping
            getMultimapValue(timeSerieToEquipmentsMapping, timeSerieToEquipmentsKey).add(equipmentId);

            config.distributionKeys.put(equipmentToTimeSeriesKey, distributionKey);
        } else {
            getMultimapValue(timeSerieToEquipmentsMapping, timeSerieToEquipmentsKey);
        }
        config.mappedTimeSeriesNames.add(timeSeriesName);
    }

    private void addHvdcLineMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, EquipmentVariable variable) {
        addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                config.timeSeriesToHvdcLinesMapping, config.hvdcLineToTimeSeriesMapping);
        switch (variable) {
            case ACTIVE_POWER_SETPOINT -> config.unmappedHvdcLines.remove(equipmentId);
            case MIN_P -> config.unmappedMinPHvdcLines.remove(equipmentId);
            case MAX_P -> config.unmappedMaxPHvdcLines.remove(equipmentId);
            default -> {
                // Do nothing
            }
        }
    }

    private void addLoadMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, EquipmentVariable variable) {
        addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                config.timeSeriesToLoadsMapping, config.loadToTimeSeriesMapping);
        switch (variable) {
            case P0 -> {
                config.unmappedLoads.remove(equipmentId);
                config.unmappedFixedActivePowerLoads.remove(equipmentId);
                config.unmappedVariableActivePowerLoads.remove(equipmentId);
            }
            case FIXED_ACTIVE_POWER -> {
                config.unmappedLoads.remove(equipmentId);
                config.unmappedFixedActivePowerLoads.remove(equipmentId);
            }
            case VARIABLE_ACTIVE_POWER -> {
                config.unmappedLoads.remove(equipmentId);
                config.unmappedVariableActivePowerLoads.remove(equipmentId);
            }
            default -> {
                // Do nothing
            }
        }
    }

    private void addGeneratorMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, EquipmentVariable variable) {
        addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                config.timeSeriesToGeneratorsMapping, config.generatorToTimeSeriesMapping);
        switch (variable) {
            case TARGET_P -> config.unmappedGenerators.remove(equipmentId);
            case MIN_P -> config.unmappedMinPGenerators.remove(equipmentId);
            case MAX_P -> config.unmappedMaxPGenerators.remove(equipmentId);
            default -> {
                // Do nothing
            }
        }
    }

    private void addNumberTimeSeries(String timeSeriesName, float value) {
        if (!config.getTimeSeriesNodesKeys().contains(timeSeriesName)) {
            config.timeSeriesNodes.put(timeSeriesName, new FloatNodeCalc(value));
        }
    }

    protected void timeSeriesExists(String timeSeriesName) {
        if (timeSeriesName == null) {
            throw new TimeSeriesMappingException("'timeSeriesName' is not set");
        }
        if (!existingTimeSeriesNames.contains(timeSeriesName) && !config.getTimeSeriesNodesKeys().contains(timeSeriesName)) {
            throw new TimeSeriesMappingException("Time Series '" + timeSeriesName + "' not found");
        }
    }

    protected void addEquipmentMapping(MappableEquipmentType equipmentType, String timeSeriesName, String equipmentId, DistributionKey distributionKey,
                                    EquipmentVariable variable) {
        switch (equipmentType) {
            case GENERATOR -> addGeneratorMapping(timeSeriesName, equipmentId, distributionKey, variable);
            case LOAD -> addLoadMapping(timeSeriesName, equipmentId, distributionKey, variable);
            case BOUNDARY_LINE -> {
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                    config.timeSeriesToDanglingLinesMapping, config.danglingLineToTimeSeriesMapping);
                if (variable == EquipmentVariable.P0) {
                    config.unmappedDanglingLines.remove(equipmentId);
                }
            }
            case HVDC_LINE -> addHvdcLineMapping(timeSeriesName, equipmentId, distributionKey, variable);
            case SWITCH -> addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                config.timeSeriesToBreakersMapping, config.breakerToTimeSeriesMapping);
            case PHASE_TAP_CHANGER -> {
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                    config.timeSeriesToPhaseTapChangersMapping, config.phaseTapChangerToTimeSeriesMapping);
                if (variable == EquipmentVariable.PHASE_TAP_POSITION) {
                    config.unmappedPhaseTapChangers.remove(equipmentId);
                }
            }
            case TRANSFORMER -> addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToTransformersMapping, config.transformerToTimeSeriesMapping);
            case LINE -> addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToLinesMapping, config.lineToTimeSeriesMapping);
            case RATIO_TAP_CHANGER -> addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToRatioTapChangersMapping, config.ratioTapChangerToTimeSeriesMapping);
            case LCC_CONVERTER_STATION -> addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToLccConverterStationsMapping, config.lccConverterStationToTimeSeriesMapping);
            case VSC_CONVERTER_STATION -> addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToVscConverterStationsMapping, config.vscConverterStationToTimeSeriesMapping);
            default -> throw new AssertionError();
        }
    }

    protected void addUnmappedEquipment(MappableEquipmentType equipmentType, String equipmentId) {
        switch (equipmentType) {
            case GENERATOR -> config.ignoredUnmappedGenerators.add(equipmentId);
            case LOAD -> config.ignoredUnmappedLoads.add(equipmentId);
            case BOUNDARY_LINE -> config.ignoredUnmappedDanglingLines.add(equipmentId);
            case HVDC_LINE -> config.ignoredUnmappedHvdcLines.add(equipmentId);
            case PHASE_TAP_CHANGER -> config.ignoredUnmappedPhaseTapChangers.add(equipmentId);
            default -> throw new AssertionError();
        }
    }

    protected void addEquipmentTimeSeries(MappableEquipmentType equipmentType, String equipmentId, Set<EquipmentVariable> equipmentVariables) {
        for (EquipmentVariable equipmentVariable : equipmentVariables) {
            MappingKey mappingKey = new MappingKey(equipmentVariable, equipmentId);
            switch (equipmentType) {
                case GENERATOR -> config.generatorTimeSeries.add(mappingKey);
                case LOAD -> config.loadTimeSeries.add(mappingKey);
                case BOUNDARY_LINE -> config.danglingLineTimeSeries.add(mappingKey);
                case HVDC_LINE -> config.hvdcLineTimeSeries.add(mappingKey);
                case SWITCH -> config.breakerTimeSeries.add(mappingKey);
                case TRANSFORMER -> config.transformerTimeSeries.add(mappingKey);
                case LINE -> config.lineTimeSeries.add(mappingKey);
                case PHASE_TAP_CHANGER -> config.phaseTapChangerTimeSeries.add(mappingKey);
                case RATIO_TAP_CHANGER -> config.ratioTapChangerTimeSeries.add(mappingKey);
                case LCC_CONVERTER_STATION -> config.lccConverterStationTimeSeries.add(mappingKey);
                case VSC_CONVERTER_STATION -> config.vscConverterStationTimeSeries.add(mappingKey);
                default -> throw new AssertionError();
            }
        }
    }

    protected void addGroupGeneratorTimeSeries(Generator generator, EquipmentGroupType equipmentGroupType, Boolean withPowerType, String name) {
        String timeSeriesName = computeGeneratorTsName(generator, equipmentGroupType, withPowerType, name);
        config.generatorGroupToTimeSeriesMapping.computeIfAbsent(generator.getId(), k -> new HashSet<>()).add(timeSeriesName);
    }

    protected void addGroupLoadTimeSeries(Load load, EquipmentGroupType equipmentGroupType, String name) {
        String timeSeriesName = computeLoadTsName(load, equipmentGroupType, name);
        config.loadGroupToTimeSeriesMapping.computeIfAbsent(load.getId(), k -> new HashSet<>()).add(timeSeriesName);
    }

    protected void addIgnoreLimits(String timeSeriesName) {
        config.ignoreLimitsTimeSeriesNames.add(timeSeriesName);
    }

    protected void addPlannedOutages(String timeSeriesName, Set<String> disconnectedIds) {
        config.timeSeriesToPlannedOutagesMapping.put(timeSeriesName, disconnectedIds);
    }

    public void addEquipmentTimeSeries(Object timeSeriesName, MappingVariable variable, String equipmentId) {
        if (timeSeriesName instanceof Number number) {
            addNumberTimeSeries(timeSeriesName.toString(), number.floatValue());
        } else {
            timeSeriesExists(timeSeriesName.toString());
        }
        config.addEquipmentTimeSeries(timeSeriesName.toString(), variable, equipmentId);
    }
}
