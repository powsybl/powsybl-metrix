/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.mapping.DataTableStore;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.ast.BinaryOperation;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.metrix.integration.MetrixLossesPostProcessingTimeSeries.LOSSES_COST_PREFIX;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.LOSSES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixLossesPostProcessingTimeSeriesTest {

    private Network network;

    private final MetrixParameters parameters = new MetrixParameters();

    private final String metrixConfigurationScript = String.join(System.lineSeparator(),
            "losses() {",
            "    costs 'tsCost'",
            "}"
    );

    Map<String, NodeCalc> postProcessingTimeSeries;

    @BeforeEach
    void setUp() {
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @Test
    void postProcessingTimeSeriesTest() {
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore metrixResultTimeSeries = mock(ReadOnlyTimeSeriesStore.class);
        when(metrixResultTimeSeries.getTimeSeriesNames(Mockito.any())).thenReturn(Set.of(LOSSES));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("tsCost", index, 1000d, 1000d)
        );
        DataTableStore dataTableStore = new DataTableStore();

        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig();
        MetrixDslDataLoader metrixDslDataLoader = new MetrixDslDataLoader(metrixConfigurationScript);
        metrixDslDataLoader.load(network, parameters, store, dataTableStore, mappingConfig, null);

        MetrixLossesPostProcessingTimeSeries lossesProcessing = new MetrixLossesPostProcessingTimeSeries(mappingConfig, null);
        postProcessingTimeSeries = lossesProcessing.createPostProcessingTimeSeries();
        assertEquals(1, postProcessingTimeSeries.size());

        NodeCalc tsCost = new TimeSeriesNameNodeCalc("tsCost");
        NodeCalc metrixOutputNode = new TimeSeriesNameNodeCalc(LOSSES);
        NodeCalc expectedLossesCost = BinaryOperation.multiply(metrixOutputNode, tsCost);
        assertEquals(expectedLossesCost, postProcessingTimeSeries.get(LOSSES_COST_PREFIX));
    }
}
