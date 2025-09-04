/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class EquipmentTimeSeriesMapperObserver extends DefaultEquipmentTimeSeriesMapperObserver {
    public static final String EQUIPMENT = "equipment";
    public static final String VARIABLE = "variable";

    protected int currentVersion;
    protected int currentPointInChunk;
    protected int currentChunk;
    protected final int chunkSize;
    protected final Range<Integer> pointRange;
    protected final Set<MappingKey> equipmentTsKeys;
    protected final Set<MappingKey> equipmentNotMappedTsKeys;
    protected final Map<MappingKey, double[]> constantValues = new LinkedHashMap<>();
    protected final Map<MappingKey, double[]> values = new LinkedHashMap<>();

    private final MappingKeyNetworkValue mappingKeyNetworkValue;

    public EquipmentTimeSeriesMapperObserver(Network network, TimeSeriesMappingConfig mappingConfig, int chunkSize, Range<Integer> pointRange) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(mappingConfig);
        Objects.requireNonNull(pointRange);
        this.chunkSize = chunkSize;
        this.pointRange = pointRange;
        this.mappingKeyNetworkValue = new MappingKeyNetworkValue(network);
        TimeSeriesMappingConfigChecker configChecker = new TimeSeriesMappingConfigChecker(mappingConfig);
        this.equipmentTsKeys = configChecker.getEquipmentTimeSeriesKeys();
        this.equipmentNotMappedTsKeys = configChecker.getNotMappedEquipmentTimeSeriesKeys();
    }

    @Override
    public void versionStart(int version) {
        currentVersion = version;
        currentChunk = -1;
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        if (point != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            if (point % chunkSize == 0) {
                // start of chunk
                equipmentTsKeys.forEach(key -> values.put(key, new double[Math.min(chunkSize, pointRange.upperEndpoint() - point + 1)]));
                currentPointInChunk = 0;
                currentChunk++;
            } else {
                currentPointInChunk++;
            }
        }
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        MappingKey key = new MappingKey(variable, identifiable.getId());
        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID && equipmentTsKeys.contains(key)) {
            constantValues.put(key, new double[]{equipmentValue});
        } else if (values.containsKey(key)) {
            values.get(key)[currentPointInChunk] = equipmentValue;
        }
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            // end of constant variant
            constantValues.forEach((key, doubles) -> addTimeSeries(idAndVariableToTsName(key), currentVersion, pointRange, doubles, computeTags(key), index));
            equipmentTsKeys.removeAll(constantValues.keySet());
            constantValues.clear();
            // add constant time series for not mapped equipments
            equipmentNotMappedTsKeys.forEach(key -> addTimeSeries(idAndVariableToTsName(key), currentVersion, pointRange, computeValue(key), computeTags(key), index));
        } else {
            if (currentPointInChunk == chunkSize - 1 || point == pointRange.upperEndpoint()) {
                // end of chunk
                Range<Integer> chunkRange = Range.closed(currentChunk * chunkSize, Math.min((currentChunk + 1) * chunkSize - 1, pointRange.upperEndpoint()));
                values.forEach((key, doubles) -> addTimeSeries(idAndVariableToTsName(key), currentVersion, chunkRange, doubles, computeTags(key), index));
                values.clear();
            }
        }
    }

    @Override
    public void versionEnd(int version) {
        values.clear();
    }

    @Override
    public void end() {
        equipmentTsKeys.clear();
    }

    protected static String idAndVariableToTsName(MappingKey key) {
        // MappingKey(variable, id), entry of mappingConfig equipmentToTimeSeriesMapping -> equipment timeSeriesName <id>_<variable>
        return key.id() + "_" + key.mappingVariable().getVariableName();
    }

    protected static Map<String, String> computeTags(MappingKey key) {
        return ImmutableMap.of(EQUIPMENT, key.id(), VARIABLE, key.mappingVariable().getVariableName());
    }

    protected double[] computeValue(MappingKey key) {
        return new double[]{mappingKeyNetworkValue.getValue(key)};
    }
}
