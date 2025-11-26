/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.config;

import com.powsybl.metrix.commons.MappingVariable;
import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.metrix.mapping.MappableEquipmentType;
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException;
import com.powsybl.metrix.mapping.references.MappingKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.metrix.mapping.EquipmentVariable.isVariableCompatible;
import static com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigCsvWriter.getNotSignificantValue;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class TimeSeriesMappingConfigChecker {

    protected final TimeSeriesMappingConfig config;

    public TimeSeriesMappingConfigChecker(TimeSeriesMappingConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    private void checkMappedAndUnmapped(Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, Set<String> unmappedEquipments, Set<String> ignoredUnmappedEquipments) {
        for (Map.Entry<MappingKey, List<String>> e : equipmentToTimeSeriesMapping.entrySet()) {
            if (!unmappedEquipments.contains(e.getKey().id()) && ignoredUnmappedEquipments.contains(e.getKey().id())) {
                throw new TimeSeriesMappingException("Equipment '" + e.getKey().id() + "' is declared unmapped but mapped on time series '" + e.getValue().getFirst() + "'");
            }
        }
    }

    public void checkMappedVariables() {
        // check that mapping is consistent for each load
        Map<String, Set<MappingVariable>> mappedVariablesPerLoad = new HashMap<>();
        config.timeSeriesToLoadsMapping.forEach((mappingKey, ids) -> {
            for (String id : ids) {
                mappedVariablesPerLoad.computeIfAbsent(id, s -> new HashSet<>()).add(mappingKey.mappingVariable());
            }
        });
        for (Map.Entry<String, Set<MappingVariable>> e : mappedVariablesPerLoad.entrySet()) {
            String id = e.getKey();
            Set<MappingVariable> variables = e.getValue();
            if (variables.contains(EquipmentVariable.P0)
                    && (variables.contains(EquipmentVariable.FIXED_ACTIVE_POWER) || variables.contains(EquipmentVariable.VARIABLE_ACTIVE_POWER))) {
                throw new TimeSeriesMappingException("Load '" + id + "' is mapped on p0 and on one of the detailed variables (fixedActivePower/variableActivePower)");
            }
            if (variables.contains(EquipmentVariable.Q0)
                    && (variables.contains(EquipmentVariable.FIXED_REACTIVE_POWER) || variables.contains(EquipmentVariable.VARIABLE_REACTIVE_POWER))) {
                throw new TimeSeriesMappingException("Load '" + id + "' is mapped on q0 and on one of the detailed variables (fixedReactivePower/variableReactivePower)");
            }
        }

        checkMappedAndUnmapped(config.generatorToTimeSeriesMapping, config.unmappedGenerators, config.ignoredUnmappedGenerators);
        checkMappedAndUnmapped(config.loadToTimeSeriesMapping, config.unmappedLoads, config.ignoredUnmappedLoads);
        checkMappedAndUnmapped(config.danglingLineToTimeSeriesMapping, config.unmappedDanglingLines, config.ignoredUnmappedDanglingLines);
        checkMappedAndUnmapped(config.hvdcLineToTimeSeriesMapping, config.unmappedHvdcLines, config.ignoredUnmappedHvdcLines);
        checkMappedAndUnmapped(config.phaseTapChangerToTimeSeriesMapping, config.unmappedPhaseTapChangers, config.ignoredUnmappedPhaseTapChangers);
    }

    public Set<MappingKey> getNotMappedEquipmentTimeSeriesKeys() {
        Set<MappingKey> keys = new HashSet<>();
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getGeneratorToTimeSeriesMapping().keySet(), config.getGeneratorTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getLoadToTimeSeriesMapping().keySet(), config.getLoadTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getDanglingLineToTimeSeriesMapping().keySet(), config.getDanglingLineTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getHvdcLineToTimeSeriesMapping().keySet(), config.getHvdcLineTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getPhaseTapChangerToTimeSeriesMapping().keySet(), config.getPhaseTapChangerTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getBreakerToTimeSeriesMapping().keySet(), config.getBreakerTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getTransformerToTimeSeriesMapping().keySet(), config.getTransformerTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getLineToTimeSeriesMapping().keySet(), config.getLineTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getRatioTapChangerToTimeSeriesMapping().keySet(), config.getRatioTapChangerTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getLccConverterStationToTimeSeriesMapping().keySet(), config.getLccConverterStationTimeSeries()));
        keys.addAll(getNotMappedEquipmentTimeSeriesKeys(config.getVscConverterStationToTimeSeriesMapping().keySet(), config.getVscConverterStationTimeSeries()));
        return keys;
    }

    private Set<MappingKey> getNotMappedEquipmentTimeSeriesKeys(Set<MappingKey> equipmentToTimeSeriesMapping, Set<MappingKey> equipmentTimeSeries) {
        Set<MappingKey> keySet = new HashSet<>(equipmentTimeSeries);
        keySet.removeAll(equipmentToTimeSeriesMapping);
        return keySet;
    }

    public static int getNbMapped(Map<MappingKey, List<String>> equipmentToTimeSeriesMapping) {
        return equipmentToTimeSeriesMapping.keySet().stream()
                .map(MappingKey::id)
                .collect(Collectors.toSet())
                .size();
    }

    public static String getNbMapped(MappableEquipmentType mappableEquipmentType, EquipmentVariable variable, Map<MappingKey, List<String>> equipmentToTimeSeriesMapping) {
        return isVariableCompatible(mappableEquipmentType, variable) ?
                Integer.toString(getNbMapped(equipmentToTimeSeriesMapping, variable)) :
                getNotSignificantValue();
    }

    public static int getNbMapped(Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, EquipmentVariable variable) {
        return (int) equipmentToTimeSeriesMapping.keySet().stream()
                .filter(key -> key.mappingVariable() == variable)
                .count();
    }

    public static int getNbMultiMapped(Map<MappingKey, List<String>> equipmentToTimeSeriesMapping) {
        return (int) equipmentToTimeSeriesMapping.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .count();
    }

    public static int getNbUnmapped(Set<String> unmapped, Set<String> ignoredUnmapped) {
        Set<String> equipmentSet = new HashSet<>(unmapped);
        equipmentSet.removeAll(ignoredUnmapped);
        return equipmentSet.size();
    }

    public boolean isMappingComplete() {
        return getNbUnmapped(config.unmappedGenerators, config.ignoredUnmappedGenerators)
                + getNbUnmapped(config.unmappedLoads, config.ignoredUnmappedLoads)
                + getNbUnmapped(config.unmappedDanglingLines, config.ignoredUnmappedDanglingLines)
                + getNbUnmapped(config.unmappedHvdcLines, config.ignoredUnmappedHvdcLines)
                + getNbUnmapped(config.unmappedPhaseTapChangers, config.ignoredUnmappedPhaseTapChangers) == 0;
    }

    public Set<MappingKey> getEquipmentTimeSeriesKeys(Set<MappingKey> equipmentToTimeSeriesMapping, Set<MappingKey> equipmentTimeSeries) {
        Set<MappingKey> keySet = new HashSet<>(equipmentToTimeSeriesMapping);
        keySet.retainAll(equipmentTimeSeries);
        return keySet;
    }

    public Set<MappingKey> getEquipmentTimeSeriesKeys() {
        Set<MappingKey> keys = new HashSet<>();
        keys.addAll(getEquipmentTimeSeriesKeys(config.getGeneratorToTimeSeriesMapping().keySet(), config.getGeneratorTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getLoadToTimeSeriesMapping().keySet(), config.getLoadTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getDanglingLineToTimeSeriesMapping().keySet(), config.getDanglingLineTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getHvdcLineToTimeSeriesMapping().keySet(), config.getHvdcLineTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getPhaseTapChangerToTimeSeriesMapping().keySet(), config.getPhaseTapChangerTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getBreakerToTimeSeriesMapping().keySet(), config.getBreakerTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getTransformerToTimeSeriesMapping().keySet(), config.getTransformerTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getLineToTimeSeriesMapping().keySet(), config.getLineTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getRatioTapChangerToTimeSeriesMapping().keySet(), config.getRatioTapChangerTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getLccConverterStationToTimeSeriesMapping().keySet(), config.getLccConverterStationTimeSeries()));
        keys.addAll(getEquipmentTimeSeriesKeys(config.getVscConverterStationToTimeSeriesMapping().keySet(), config.getVscConverterStationTimeSeries()));
        return keys;
    }
}
