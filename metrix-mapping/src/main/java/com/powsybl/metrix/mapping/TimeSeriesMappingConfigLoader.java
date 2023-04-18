/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.timeseries.ast.FloatNodeCalc;

import java.util.*;

public class TimeSeriesMappingConfigLoader {

    private final TimeSeriesMappingConfig config;
    private final Set<String> existingTimeSeriesNames;

    public TimeSeriesMappingConfigLoader(TimeSeriesMappingConfig config, Set<String> existingTimeSeriesNames) {
        this.config = config;
        this.existingTimeSeriesNames = existingTimeSeriesNames;
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
            case activePowerSetpoint:
                config.unmappedHvdcLines.remove(equipmentId);
                break;
            case minP:
                config.unmappedMinPHvdcLines.remove(equipmentId);
                break;
            case maxP:
                config.unmappedMaxPHvdcLines.remove(equipmentId);
                break;
            default:
                break;
        }
    }

    private void addLoadMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, EquipmentVariable variable) {
        addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                config.timeSeriesToLoadsMapping, config.loadToTimeSeriesMapping);
        switch (variable) {
            case p0:
                config.unmappedLoads.remove(equipmentId);
                config.unmappedFixedActivePowerLoads.remove(equipmentId);
                config.unmappedVariableActivePowerLoads.remove(equipmentId);
                break;
            case fixedActivePower:
                config.unmappedLoads.remove(equipmentId);
                config.unmappedFixedActivePowerLoads.remove(equipmentId);
                break;
            case variableActivePower:
                config.unmappedLoads.remove(equipmentId);
                config.unmappedVariableActivePowerLoads.remove(equipmentId);
                break;
            default:
                break;
        }
    }

    private void addGeneratorMapping(String timeSeriesName, String equipmentId, DistributionKey distributionKey, EquipmentVariable variable) {
        addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                config.timeSeriesToGeneratorsMapping, config.generatorToTimeSeriesMapping);
        switch (variable) {
            case targetP:
                config.unmappedGenerators.remove(equipmentId);
                break;
            case minP:
                config.unmappedMinPGenerators.remove(equipmentId);
                break;
            case maxP:
                config.unmappedMaxPGenerators.remove(equipmentId);
                break;
            default:
                break;
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
            case GENERATOR:
                addGeneratorMapping(timeSeriesName, equipmentId, distributionKey, variable);
                break;
            case LOAD:
                addLoadMapping(timeSeriesName, equipmentId, distributionKey, variable);
                break;
            case BOUNDARY_LINE:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToDanglingLinesMapping, config.danglingLineToTimeSeriesMapping);
                if (variable == EquipmentVariable.p0) {
                    config.unmappedDanglingLines.remove(equipmentId);
                }
                break;
            case HVDC_LINE:
                addHvdcLineMapping(timeSeriesName, equipmentId, distributionKey, variable);
                break;
            case SWITCH:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToBreakersMapping, config.breakerToTimeSeriesMapping);
                break;
            case PHASE_TAP_CHANGER:
            case PST:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToPhaseTapChangersMapping, config.phaseTapChangerToTimeSeriesMapping);
                if (variable == EquipmentVariable.phaseTapPosition) {
                    config.unmappedPhaseTapChangers.remove(equipmentId);
                }
                break;
            case TRANSFORMER:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToTransformersMapping, config.transformerToTimeSeriesMapping);
                break;
            case LINE:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToLinesMapping, config.lineToTimeSeriesMapping);
                break;
            case RATIO_TAP_CHANGER:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToRatioTapChangersMapping, config.ratioTapChangerToTimeSeriesMapping);
                break;
            case LCC_CONVERTER_STATION:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToLccConverterStationsMapping, config.lccConverterStationToTimeSeriesMapping);
                break;
            case VSC_CONVERTER_STATION:
                addMapping(timeSeriesName, equipmentId, distributionKey, variable,
                        config.timeSeriesToVscConverterStationsMapping, config.vscConverterStationToTimeSeriesMapping);
                break;
            default:
                throw new AssertionError();
        }
    }

    protected void addUnmappedEquipment(MappableEquipmentType equipmentType, String equipmentId) {
        switch (equipmentType) {
            case GENERATOR:
                config.ignoredUnmappedGenerators.add(equipmentId);
                break;
            case LOAD:
                config.ignoredUnmappedLoads.add(equipmentId);
                break;
            case BOUNDARY_LINE:
                config.ignoredUnmappedDanglingLines.add(equipmentId);
                break;
            case HVDC_LINE:
                config.ignoredUnmappedHvdcLines.add(equipmentId);
                break;
            case PHASE_TAP_CHANGER:
                config.ignoredUnmappedPhaseTapChangers.add(equipmentId);
                break;
            default:
                throw new AssertionError();
        }
    }

    protected void addEquipmentTimeSeries(MappableEquipmentType equipmentType, String equipmentId, Set<EquipmentVariable> equipmentVariables) {
        for (EquipmentVariable equipmentVariable : equipmentVariables) {
            MappingKey mappingKey = new MappingKey(equipmentVariable, equipmentId);
            switch (equipmentType) {
                case GENERATOR:
                    config.generatorTimeSeries.add(mappingKey);
                    break;
                case LOAD:
                    config.loadTimeSeries.add(mappingKey);
                    break;
                case BOUNDARY_LINE:
                    config.danglingLineTimeSeries.add(mappingKey);
                    break;
                case HVDC_LINE:
                    config.hvdcLineTimeSeries.add(mappingKey);
                    break;
                case SWITCH:
                    config.breakerTimeSeries.add(mappingKey);
                    break;
                case TRANSFORMER:
                    config.transformerTimeSeries.add(mappingKey);
                    break;
                case LINE:
                    config.lineTimeSeries.add(mappingKey);
                    break;
                case PHASE_TAP_CHANGER:
                    config.phaseTapChangerTimeSeries.add(mappingKey);
                    break;
                case RATIO_TAP_CHANGER:
                    config.ratioTapChangerTimeSeries.add(mappingKey);
                    break;
                case LCC_CONVERTER_STATION:
                    config.lccConverterStationTimeSeries.add(mappingKey);
                    break;
                case VSC_CONVERTER_STATION:
                    config.vscConverterStationTimeSeries.add(mappingKey);
                    break;
                default:
                    throw new AssertionError();
            }
        }
    }

    protected void addIgnoreLimits(String timeSeriesName) {
        config.ignoreLimitsTimeSeriesNames.add(timeSeriesName);
    }

    protected void addPlannedOutages(String timeSeriesName, Set<String> disconnectedIds) {
        config.timeSeriesToPlannedOutagesMapping.put(timeSeriesName, disconnectedIds);
    }

    public void addEquipmentTimeSeries(Object timeSeriesName, MappingVariable variable, String equipmentId) {
        if (timeSeriesName instanceof Number) {
            addNumberTimeSeries(timeSeriesName.toString(), ((Number) timeSeriesName).floatValue());
        } else {
            timeSeriesExists(timeSeriesName.toString());
        }
        config.addEquipmentTimeSeries(timeSeriesName.toString(), variable, equipmentId);
    }
}
