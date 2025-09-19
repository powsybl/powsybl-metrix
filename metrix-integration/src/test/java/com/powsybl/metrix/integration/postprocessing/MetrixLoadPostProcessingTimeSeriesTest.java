/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

import com.google.common.collect.Sets;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.MetrixDslDataLoader;
import com.powsybl.metrix.integration.MetrixParameters;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
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
import org.threeten.extra.Interval;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.metrix.integration.postprocessing.MetrixLoadPostProcessingTimeSeries.CUR_SHEDDING_COST_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixLoadPostProcessingTimeSeries.CUR_SHEDDING_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixLoadPostProcessingTimeSeries.PRE_SHEDDING_COST_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixLoadPostProcessingTimeSeries.PRE_SHEDDING_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixLoadPostProcessingTimeSeriesTest {

    private Network network;

    private final MetrixParameters parameters = new MetrixParameters();

    private final String metrixConfigurationScript = String.join(System.lineSeparator(),
            "load('FSSV.O11_L') {",
            "    preventiveSheddingPercentage 100",
            "    preventiveSheddingDoctrineCost 'tsPreventiveSheddingDoctrineCost'",
            "    curativeSheddingPercentage 100",
            "    curativeSheddingCost 'tsCurativeSheddingCost'",
            "    curativeSheddingDoctrineCost 'tsCurativeSheddingDoctrineCost'",
            "    onContingencies 'cty'",
            "}",
            "load('FVALDI11_L') {",
            "    preventiveSheddingPercentage 100",
            "    preventiveSheddingDoctrineCost 450",
            "    curativeSheddingPercentage 100",
            "    curativeSheddingCost 'tsCurativeSheddingCost'",
            "    curativeSheddingDoctrineCost 550",
            "    onContingencies 'cty'",
            "}"
    );

    Map<String, NodeCalc> postProcessingTimeSeries;

    @BeforeEach
    void setUp() {
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    private void verifyLoadPostProcessing(String loadName,
                                          String postProcessingLoadSheddingPrefix,
                                          String postProcessingLoadSheddingCostPrefix,
                                          String metrixOutputPrefix,
                                          NodeCalc tsSheddingCost) {
        NodeCalc metrixOutputNode = new TimeSeriesNameNodeCalc(metrixOutputPrefix + loadName);

        assertEquals(metrixOutputNode, postProcessingTimeSeries.get(postProcessingLoadSheddingPrefix + "_" + loadName));

        NodeCalc expectedLoadSheddingCost = BinaryOperation.multiply(metrixOutputNode, tsSheddingCost);
        assertEquals(expectedLoadSheddingCost, postProcessingTimeSeries.get(postProcessingLoadSheddingCostPrefix + "_" + loadName));
    }

    @Test
    void postProcessingTimeSeriesTest() {
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        Set<String> loadNames = Sets.newHashSet("FSSV.O11_L", "FVALDI11_L");

        Set<String> resultTimeSeriesNames = new HashSet<>();
        loadNames.forEach(loadName -> resultTimeSeriesNames.add(MetrixOutputData.LOAD_PREFIX + loadName));
        loadNames.forEach(loadName -> resultTimeSeriesNames.add(MetrixOutputData.LOAD_CUR_PREFIX + loadName + "_cty"));
        ReadOnlyTimeSeriesStore metrixResultTimeSeries = mock(ReadOnlyTimeSeriesStore.class);
        when(metrixResultTimeSeries.getTimeSeriesNames(any())).thenReturn(resultTimeSeriesNames);

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("tsCurativeSheddingCost", index, 1500d, 1500d),
                TimeSeries.createDouble("tsPreventiveSheddingDoctrineCost", index, 2500d, 2500d),
                TimeSeries.createDouble("tsCurativeSheddingDoctrineCost", index, 3500d, 3500d)
        );

        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig();
        MetrixDslDataLoader metrixDslDataLoader = new MetrixDslDataLoader(metrixConfigurationScript);
        MetrixDslData dslData = metrixDslDataLoader.load(network, parameters, store, new DataTableStore(), mappingConfig, null);
        Contingency contingency = new Contingency("cty", List.of(new BranchContingency("FP.AND1  FVERGE1  1")));

        MetrixLoadPostProcessingTimeSeries loadProcessing = new MetrixLoadPostProcessingTimeSeries(dslData, mappingConfig, List.of(contingency), metrixResultTimeSeries.getTimeSeriesNames(null), null);
        postProcessingTimeSeries = loadProcessing.createPostProcessingTimeSeries();
        assertEquals(8, postProcessingTimeSeries.size());

        NodeCalc tsPreventiveSheddingDoctrineCost = new TimeSeriesNameNodeCalc("tsPreventiveSheddingDoctrineCost");
        NodeCalc tsCurativeSheddingDoctrineCost = new TimeSeriesNameNodeCalc("tsCurativeSheddingDoctrineCost");
        verifyLoadPostProcessing("FSSV.O11_L", PRE_SHEDDING_PREFIX, PRE_SHEDDING_COST_PREFIX, MetrixOutputData.LOAD_PREFIX, tsPreventiveSheddingDoctrineCost);
        verifyLoadPostProcessing("FSSV.O11_L", CUR_SHEDDING_PREFIX, CUR_SHEDDING_COST_PREFIX, MetrixOutputData.LOAD_CUR_PREFIX, tsCurativeSheddingDoctrineCost);
        verifyLoadPostProcessing("FVALDI11_L", PRE_SHEDDING_PREFIX, PRE_SHEDDING_COST_PREFIX, MetrixOutputData.LOAD_PREFIX, mappingConfig.getTimeSeriesNodes().get("450"));
        verifyLoadPostProcessing("FVALDI11_L", CUR_SHEDDING_PREFIX, CUR_SHEDDING_COST_PREFIX, MetrixOutputData.LOAD_CUR_PREFIX, mappingConfig.getTimeSeriesNodes().get("550"));
    }
}
