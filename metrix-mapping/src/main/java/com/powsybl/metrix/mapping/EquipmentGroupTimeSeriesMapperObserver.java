/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.util.*;

import static com.powsybl.metrix.mapping.EquipmentTimeSeriesMapperObserver.EQUIPMENT;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class EquipmentGroupTimeSeriesMapperObserver extends DefaultEquipmentTimeSeriesMapperObserver {
    public static final String GROUP = "group";

    private final int chunkSize;
    private final Range<Integer> pointRange;
    private final TimeSeriesMappingConfig mappingConfig;

    private int currentVersion;
    private int currentPointInChunk;
    private int currentChunk;

    // To retrieve base case values
    private final MappingKeyNetworkValue mappingKeyNetworkValue;

    // List of ids involved in group computation
    private final Set<String> generatorIds;
    private final Set<String> loadIds;

    // For each id, list of time series names to which mapped or base case value must be added
    private final Map<String, Set<String>> generatorGroupTimeSeries;
    private final Map<String, Set<String>> loadGroupVariableActivePowerTimeSeries;
    private final Map<String, Set<String>> loadGroupFixedActivePowerTimeSeries;

    // For each time series name, values for the current chunk
    private final Map<String, double[]> values = new LinkedHashMap<>();

    public EquipmentGroupTimeSeriesMapperObserver(Network network, TimeSeriesMappingConfig mappingConfig, int chunkSize, Range<Integer> pointRange) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(mappingConfig);
        Objects.requireNonNull(pointRange);
        this.chunkSize = chunkSize;
        this.pointRange = pointRange;
        this.mappingConfig = mappingConfig;
        this.mappingKeyNetworkValue = new MappingKeyNetworkValue(network);
        Map<String, Set<String>> generatorGroupTs = mappingConfig.getGeneratorGroupTimeSeries();
        this.generatorGroupTimeSeries = generatorGroupTs;
        this.generatorIds = generatorGroupTs.keySet();
        Map<String, Set<String>> loadGroupTs = mappingConfig.getLoadGroupTimeSeries();
        this.loadGroupVariableActivePowerTimeSeries = loadGroupTs;
        this.loadGroupFixedActivePowerTimeSeries = loadGroupTs;
        this.loadIds = loadGroupTs.keySet();
    }

    private boolean isStartOfChunk(int point) {
        return point % chunkSize == 0;
    }

    private boolean isEndOfChunk(int point) {
        return currentPointInChunk == chunkSize - 1 || point == pointRange.upperEndpoint();
    }

    private Range<Integer> computeChunkRange() {
        return Range.closed(currentChunk * chunkSize, Math.min((currentChunk + 1) * chunkSize - 1, pointRange.upperEndpoint()));
    }

    private String computeName(String name, String suffix) {
        return name + "_" + suffix;
    }

    private void initValues(int point, Set<String> names, String suffix) {
        names.forEach(name -> values.put(computeName(name, suffix), new double[Math.min(chunkSize, pointRange.upperEndpoint() - point + 1)]));
    }

    private void initValues(int point) {
        generatorGroupTimeSeries.forEach((key, names) -> initValues(point, names, EquipmentVariable.TARGET_P.getVariableName()));
        loadGroupVariableActivePowerTimeSeries.forEach((key, names) -> initValues(point, names, EquipmentVariable.VARIABLE_ACTIVE_POWER.getVariableName()));
        loadGroupFixedActivePowerTimeSeries.forEach((key, names) -> initValues(point, names, EquipmentVariable.FIXED_ACTIVE_POWER.getVariableName()));
    }

    private void addTimeSeries(TimeSeriesIndex index) {
        values.forEach((name, doubles) -> addTimeSeries(name, currentVersion, computeChunkRange(), doubles, computeTags(), index));
        values.clear();
    }

    private void addValue(String name, double value) {
        values.get(name)[currentPointInChunk] += value;
    }

    private void addGenerator(String id, MappingVariable variable, double value) {
        generatorGroupTimeSeries.get(id).forEach(name -> addValue(computeName(name, variable.getVariableName()), value));
    }

    private void mapToGenerator(Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        String id = identifiable.getId();
        if (!generatorIds.contains(id)) {
            return;
        }
        if (variable != EquipmentVariable.TARGET_P) {
            return;
        }
        addGenerator(id, variable, equipmentValue);
    }

    private void addLoad(String id, MappingVariable variable, double value) {
        if (variable == EquipmentVariable.VARIABLE_ACTIVE_POWER) {
            loadGroupVariableActivePowerTimeSeries.get(id).forEach(name -> addValue(computeName(name, variable.getVariableName()), value));
        } else if (variable == EquipmentVariable.FIXED_ACTIVE_POWER) {
            loadGroupFixedActivePowerTimeSeries.get(id).forEach(name -> addValue(computeName(name, variable.getVariableName()), value));
        } else if (variable == EquipmentVariable.P_0) {
            // for p0 mapping, power is added in variableActivePower and fixedActivePower is 0
            loadGroupVariableActivePowerTimeSeries.get(id).forEach(name -> addValue(computeName(name, EquipmentVariable.VARIABLE_ACTIVE_POWER.getVariableName()), value));
        }
    }

    private void mapToLoad(Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        String id = identifiable.getId();
        if (!loadIds.contains(id)) {
            return;
        }
        if (variable != EquipmentVariable.VARIABLE_ACTIVE_POWER && variable != EquipmentVariable.FIXED_ACTIVE_POWER && variable != EquipmentVariable.P_0) {
            return;
        }
        addLoad(id, variable, equipmentValue);
    }

    private void computeUnmappedValues(Set<String> unmappedEquipments, Map<String, Set<String>> groupTimeSeries, EquipmentVariable variable) {
        unmappedEquipments.forEach(id -> {
            MappingKey key = new MappingKey(variable, id);
            double value = mappingKeyNetworkValue.getValue(key);
            groupTimeSeries.get(id).forEach(name -> addValue(computeName(name, variable.getVariableName()), value));
        });
    }

    private void computeUnmappedValues() {
        // Unmapped generators
        final Set<String> unmappedGenerators = new HashSet<>(mappingConfig.getUnmappedGenerators());
        unmappedGenerators.retainAll(generatorIds);
        computeUnmappedValues(unmappedGenerators, generatorGroupTimeSeries, EquipmentVariable.TARGET_P);

        // Unmapped loads
        final Set<String> unmappedLoads = new HashSet<>(mappingConfig.getUnmappedLoads());
        unmappedLoads.retainAll(loadIds);
        computeUnmappedValues(unmappedLoads, loadGroupVariableActivePowerTimeSeries, EquipmentVariable.VARIABLE_ACTIVE_POWER);
        computeUnmappedValues(unmappedLoads, loadGroupFixedActivePowerTimeSeries, EquipmentVariable.FIXED_ACTIVE_POWER);

        final Set<String> unmappedVariableActivePowerLoads = new HashSet<>(mappingConfig.getUnmappedVariableActivePowerLoads());
        unmappedVariableActivePowerLoads.retainAll(loadIds);
        unmappedVariableActivePowerLoads.removeAll(unmappedLoads);
        computeUnmappedValues(unmappedVariableActivePowerLoads, loadGroupVariableActivePowerTimeSeries, EquipmentVariable.VARIABLE_ACTIVE_POWER);

        final Set<String> unmappedFixedActivePowerLoads = new HashSet<>(mappingConfig.getUnmappedFixedActivePowerLoads());
        unmappedFixedActivePowerLoads.retainAll(loadIds);
        unmappedFixedActivePowerLoads.removeAll(unmappedLoads);
        computeUnmappedValues(unmappedFixedActivePowerLoads, loadGroupFixedActivePowerTimeSeries, EquipmentVariable.FIXED_ACTIVE_POWER);
    }

    @Override
    public void versionStart(int version) {
        currentVersion = version;
        currentChunk = -1;
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            return;
        }
        if (isStartOfChunk(point)) {
            initValues(point);
            currentChunk++;
            currentPointInChunk = 0;
        } else {
            currentPointInChunk++;
        }
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            return;
        }
        if (identifiable instanceof Generator) {
            mapToGenerator(identifiable, variable, equipmentValue);
        } else if (identifiable instanceof Load) {
            mapToLoad(identifiable, variable, equipmentValue);
        }
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            return;
        }
        computeUnmappedValues();
        if (isEndOfChunk(point)) {
            addTimeSeries(index);
        }
    }

    @Override
    public void versionEnd(int version) {
        values.clear();
    }

    private static Map<String, String> computeTags() {
        return Map.of(EQUIPMENT, GROUP);
    }
}
