/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.observer;

import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.mapping.writers.EquipmentTimeSeriesWriter;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.nio.file.Path;
import java.util.Map;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class EquipmentGroupTimeSeriesWriterObserver extends EquipmentGroupTimeSeriesMapperObserver {

    private static final String OUTPUT_FILE_NAME = "group_version_";

    private final EquipmentTimeSeriesWriter equipmentTimeSeriesWriter;

    public EquipmentGroupTimeSeriesWriterObserver(Network network, TimeSeriesMappingConfig mappingConfig, int chunkSize, Range<Integer> pointRange, Path dir) {
        super(network, mappingConfig, chunkSize, pointRange);
        this.equipmentTimeSeriesWriter = new EquipmentTimeSeriesWriter(dir, OUTPUT_FILE_NAME);
    }

    @Override
    public void addTimeSeries(String timeSeriesName, int version, Range<Integer> pointRange, double[] values, Map<String, String> tags, TimeSeriesIndex index) {
        equipmentTimeSeriesWriter.addTimeSeries(timeSeriesName, pointRange, values, index);
    }

    @Override
    public void versionEnd(int version) {
        equipmentTimeSeriesWriter.versionEnd(version);
    }
}
