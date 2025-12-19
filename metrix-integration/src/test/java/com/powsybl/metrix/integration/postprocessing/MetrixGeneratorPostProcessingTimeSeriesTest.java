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
import com.powsybl.metrix.integration.configuration.MetrixParameters;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.config.ScriptLogConfig;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.ast.BinaryOperation;
import com.powsybl.timeseries.ast.DoubleNodeCalc;
import com.powsybl.timeseries.ast.IntegerNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import com.powsybl.timeseries.ast.UnaryOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.metrix.integration.postprocessing.MetrixGeneratorPostProcessingTimeSeries.CURATIVE_PREFIX_CONTAINER;
import static com.powsybl.metrix.integration.postprocessing.MetrixGeneratorPostProcessingTimeSeries.PREVENTIVE_PREFIX_CONTAINER;
import static com.powsybl.timeseries.ast.DoubleNodeCalc.ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixGeneratorPostProcessingTimeSeriesTest {

    private final NodeCalc preventiveProbabilityNodeCalc = ONE;
    private final NodeCalc curativeProbabilityNodeCalc = new DoubleNodeCalc(0.001F);

    private Network network;

    private final MetrixParameters parameters = new MetrixParameters();

    private final String metrixConfigurationScript = String.join(System.lineSeparator(),
            "generator('FSSV.O11_G') {",
            "    redispatchingUpCosts 'tsRedispatchingUpCosts'",
            "    redispatchingUpDoctrineCosts 'tsRedispatchingUpDoctrineCosts'",
            "    redispatchingDownCosts 'tsRedispatchingDownCosts'",
            "    redispatchingDownDoctrineCosts 'tsRedispatchingDownDoctrineCosts'",
            "}",
            "generator('FSSV.O12_G') {",
            "    redispatchingUpCosts 100",
            "    redispatchingUpDoctrineCosts 450",
            "    redispatchingDownCosts 200",
            "    redispatchingDownDoctrineCosts 550",
            "}"
    );

    private final String metrixConfigurationWithoutCostsScript = String.join(System.lineSeparator(),
            "generator('FSSV.O11_G') {",
            "    redispatchingUpCosts 'tsRedispatchingUpCosts'",
            "    redispatchingDownCosts 'tsRedispatchingDownCosts'",
            "    redispatchingDownDoctrineCosts 'tsRedispatchingDownDoctrineCosts'",
            "}",
            "generator('FSSV.O12_G') {",
            "    redispatchingUpCosts 100",
            "    redispatchingUpDoctrineCosts 450",
            "    redispatchingDownCosts 200",
            "}"
    );

    Map<String, NodeCalc> postProcessingTimeSeries;

    @BeforeEach
    void setUp() {
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    private void verifyRedispatching(String generatorName, NodeCalc expectedRedispatching, String prefix) {
        assertEquals(expectedRedispatching, postProcessingTimeSeries.get(prefix + "_" + generatorName));
    }

    private void verifyRedispatchingCost(String generatorName, NodeCalc expectedRedispatchingCost, String prefix) {
        assertEquals(expectedRedispatchingCost, postProcessingTimeSeries.get(prefix + "_" + generatorName));
    }

    private void verifyGeneratorPostProcessing(String generatorName,
                                               GeneratorPostProcessingPrefixContainer postProcessingPrefixContainer,
                                               String suffix,
                                               NodeCalc probabilityNodeCalc,
                                               NodeCalc tsRedispatchingUpCosts,
                                               NodeCalc tsRedispatchingDownCosts,
                                               boolean withCostComputation) {
        NodeCalc metrixOutputNode = new TimeSeriesNameNodeCalc(postProcessingPrefixContainer.metrixResultPrefix() + generatorName + suffix);

        NodeCalc expectedRedispatchingUp = BinaryOperation.multiply(metrixOutputNode, BinaryOperation.greaterThan(metrixOutputNode, new IntegerNodeCalc(0)));
        verifyRedispatching(generatorName + suffix, expectedRedispatchingUp, postProcessingPrefixContainer.redispatchingUpPrefix());

        NodeCalc expectedRedispatchingDown = BinaryOperation.multiply(metrixOutputNode, BinaryOperation.lessThan(metrixOutputNode, new IntegerNodeCalc(0)));
        verifyRedispatching(generatorName + suffix, expectedRedispatchingDown, postProcessingPrefixContainer.redispatchingDownPrefix());

        if (withCostComputation) {
            NodeCalc expectedRedispatchingUpCost = BinaryOperation.multiply(BinaryOperation.multiply(expectedRedispatchingUp, tsRedispatchingUpCosts), probabilityNodeCalc);
            verifyRedispatchingCost(generatorName + suffix, expectedRedispatchingUpCost, postProcessingPrefixContainer.redispatchingUpCostPrefix());

            NodeCalc expectedRedispatchingDownCost = BinaryOperation.multiply(BinaryOperation.multiply(UnaryOperation.abs(expectedRedispatchingDown), tsRedispatchingDownCosts), probabilityNodeCalc);
            verifyRedispatchingCost(generatorName + suffix, expectedRedispatchingDownCost, postProcessingPrefixContainer.redispatchingDownCostPrefix());

            NodeCalc expectedRedispatchingCost = BinaryOperation.plus(expectedRedispatchingUpCost, expectedRedispatchingDownCost);
            assertEquals(expectedRedispatchingCost, postProcessingTimeSeries.get(postProcessingPrefixContainer.redispatchingCostPrefix() + "_" + generatorName + suffix));
        }
    }

    private Map<String, NodeCalc> launchPostProcessingTimeSeries(String configurationScript) {
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        Set<String> generatorNames = Sets.newHashSet("FSSV.O11_G", "FSSV.O12_G");

        Set<String> resultTimeSeriesNames = new HashSet<>();
        generatorNames.forEach(generatorName -> resultTimeSeriesNames.add(MetrixOutputData.GEN_PREFIX + generatorName));
        generatorNames.forEach(generatorName -> resultTimeSeriesNames.add(MetrixOutputData.GEN_CUR_PREFIX + generatorName + "_cty"));
        ReadOnlyTimeSeriesStore metrixResultTimeSeries = mock(ReadOnlyTimeSeriesStore.class);
        when(metrixResultTimeSeries.getTimeSeriesNames(any())).thenReturn(resultTimeSeriesNames);

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("tsRedispatchingUpCosts", index, 1500d, 1500d),
                TimeSeries.createDouble("tsRedispatchingUpDoctrineCosts", index, 2500d, 2500d),
                TimeSeries.createDouble("tsRedispatchingDownCosts", index, 3500d, 3500d),
                TimeSeries.createDouble("tsRedispatchingDownDoctrineCosts", index, 4500d, 4500d)
        );

        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig();
        MetrixDslDataLoader metrixDslDataLoader = new MetrixDslDataLoader(configurationScript);
        MetrixDslData dslData = metrixDslDataLoader.load(network, parameters, store, new DataTableStore(), mappingConfig, new ScriptLogConfig());
        Contingency contingency = new Contingency("cty", List.of(new BranchContingency("FP.AND1  FVERGE1  1")));

        MetrixGeneratorPostProcessingTimeSeries generatorProcessing = new MetrixGeneratorPostProcessingTimeSeries(dslData, mappingConfig, List.of(contingency), metrixResultTimeSeries.getTimeSeriesNames(null), null);
        postProcessingTimeSeries = generatorProcessing.createPostProcessingTimeSeries();
        return mappingConfig.getTimeSeriesNodes();
    }

    @Test
    void postProcessingTimeSeriesTest() {
        Map<String, NodeCalc> timeSeriesNodes = launchPostProcessingTimeSeries(metrixConfigurationScript);

        assertEquals(20, postProcessingTimeSeries.size());

        NodeCalc tsRedispatchingUpCosts = new TimeSeriesNameNodeCalc("tsRedispatchingUpDoctrineCosts");
        NodeCalc tsRedispatchingDownCosts = new TimeSeriesNameNodeCalc("tsRedispatchingDownDoctrineCosts");

        verifyGeneratorPostProcessing("FSSV.O11_G", PREVENTIVE_PREFIX_CONTAINER, "", preventiveProbabilityNodeCalc, tsRedispatchingUpCosts, tsRedispatchingDownCosts, true);
        verifyGeneratorPostProcessing("FSSV.O11_G", CURATIVE_PREFIX_CONTAINER, "_cty", curativeProbabilityNodeCalc, tsRedispatchingUpCosts, tsRedispatchingDownCosts, true);
        verifyGeneratorPostProcessing("FSSV.O12_G", PREVENTIVE_PREFIX_CONTAINER, "", preventiveProbabilityNodeCalc, timeSeriesNodes.get("450"), timeSeriesNodes.get("550"), true);
        verifyGeneratorPostProcessing("FSSV.O12_G", CURATIVE_PREFIX_CONTAINER, "_cty", curativeProbabilityNodeCalc, timeSeriesNodes.get("450"), timeSeriesNodes.get("550"), true);
    }

    @Test
    void postProcessingTimeSeriesWithoutCostsTest() {
        Map<String, NodeCalc> timeSeriesNodes = launchPostProcessingTimeSeries(metrixConfigurationWithoutCostsScript);

        assertEquals(8, postProcessingTimeSeries.size());

        NodeCalc tsRedispatchingUpCosts = new TimeSeriesNameNodeCalc("tsRedispatchingUpDoctrineCosts");
        NodeCalc tsRedispatchingDownCosts = new TimeSeriesNameNodeCalc("tsRedispatchingDownDoctrineCosts");
        verifyGeneratorPostProcessing("FSSV.O11_G", PREVENTIVE_PREFIX_CONTAINER, "", preventiveProbabilityNodeCalc, tsRedispatchingUpCosts, tsRedispatchingDownCosts, false);
        verifyGeneratorPostProcessing("FSSV.O11_G", CURATIVE_PREFIX_CONTAINER, "_cty", curativeProbabilityNodeCalc, tsRedispatchingUpCosts, tsRedispatchingDownCosts, false);
        verifyGeneratorPostProcessing("FSSV.O12_G", PREVENTIVE_PREFIX_CONTAINER, "", preventiveProbabilityNodeCalc, timeSeriesNodes.get("450"), timeSeriesNodes.get("550"), false);
        verifyGeneratorPostProcessing("FSSV.O12_G", CURATIVE_PREFIX_CONTAINER, "_cty", curativeProbabilityNodeCalc, timeSeriesNodes.get("450"), timeSeriesNodes.get("550"), false);
    }
}
