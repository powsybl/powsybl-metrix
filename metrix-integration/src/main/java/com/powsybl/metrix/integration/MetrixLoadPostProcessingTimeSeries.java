/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.metrix.mapping.MappingKey;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ast.BinaryOperation;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.CURATIVE_PREFIX;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.PREVENTIVE_PREFIX;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.findIdsToProcess;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.LOAD_CUR_PREFIX;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.LOAD_PREFIX;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixLoadPostProcessingTimeSeries {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixLoadPostProcessingTimeSeries.class);

    public static final String SHEDDING = "shedding";
    public static final String SHEDDING_COST = SHEDDING + "Cost";

    public static final String PRE_SHEDDING_PREFIX = PREVENTIVE_PREFIX + SHEDDING;
    public static final String PRE_SHEDDING_COST_PREFIX = PREVENTIVE_PREFIX + SHEDDING_COST;
    public static final String CUR_SHEDDING_PREFIX = CURATIVE_PREFIX + SHEDDING;
    public static final String CUR_SHEDDING_COST_PREFIX = CURATIVE_PREFIX + SHEDDING_COST;

    private final MetrixDslData metrixDslData;
    private final TimeSeriesMappingConfig mappingConfig;
    private final Set<String> allTimeSeriesNames;
    private final String nullableSchemaName;
    Map<String, NodeCalc> calculatedTimeSeries;

    private final Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();

    public MetrixLoadPostProcessingTimeSeries(MetrixDslData metrixDslData,
                                              TimeSeriesMappingConfig mappingConfig,
                                              Set<String> allTimeSeriesNames,
                                              String nullableSchemaName) {
        this.metrixDslData = metrixDslData;
        this.mappingConfig = mappingConfig;
        this.allTimeSeriesNames = allTimeSeriesNames;
        this.nullableSchemaName = nullableSchemaName;
        this.calculatedTimeSeries = new HashMap<>(mappingConfig.getTimeSeriesNodes());
    }

    public Map<String, NodeCalc> createPostProcessingTimeSeries() {
        // Preventive load shedding
        createLoadSheddingPostProcessingTimeSeries(true);
        // Curative load shedding
        createLoadSheddingPostProcessingTimeSeries(false);
        return postProcessingTimeSeries;
    }

    private void createLoadSheddingPostProcessingTimeSeries(boolean isPreventive) {
        Set<String> allIds = isPreventive ? metrixDslData.getPreventiveLoadsList() : metrixDslData.getCurativeLoadsList();
        MetrixVariable variable = isPreventive ? MetrixVariable.PREVENTIVE_DOCTRINE_COST_DOWN : MetrixVariable.CURATIVE_DOCTRINE_COST_DOWN;

        // Retrieve doctrine costs time series
        List<String> doctrineCostsTimeSeriesNames = allIds.stream().map(id -> mappingConfig.getTimeSeriesName(new MappingKey(variable, id))).toList();
        if (doctrineCostsTimeSeriesNames.stream().anyMatch(Objects::isNull)) {
            LOGGER.warn(DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED, "Load shedding");
            return;
        }

        String prefix = isPreventive ? LOAD_PREFIX : LOAD_CUR_PREFIX;
        List<String> loadIds = findIdsToProcess(allIds, allTimeSeriesNames, prefix);
        int size = loadIds.size();
        for (int i = 0; i < size; i++) {
            String loadId = loadIds.get(i);
            String timeSeriesName = doctrineCostsTimeSeriesNames.get(i);
            NodeCalc loadSheddingDoctrineCostsTimeSeries = calculatedTimeSeries.computeIfAbsent(timeSeriesName, TimeSeriesNameNodeCalc::new);

            // Reference to Metrix load shedding time series result
            NodeCalc loadTimeSeries = new TimeSeriesNameNodeCalc(prefix + loadId);

            // Compute load shedding cost time series
            createLoadSheddingPostProcessingTimeSeries(loadId, loadTimeSeries, loadSheddingDoctrineCostsTimeSeries, isPreventive);
        }
    }

    private void createLoadSheddingPostProcessingTimeSeries(String loadId,
                                                            NodeCalc loadTimeSeries,
                                                            NodeCalc loadSheddingDoctrineCostsTimeSeries,
                                                            boolean isPreventive) {
        LOGGER.debug("Creating load shedding postprocessing time-series for {}", loadId);

        // Load shedding
        String loadSheddingTimeSeriesName = MetrixDataName.getNameWithSchema((isPreventive ? PRE_SHEDDING_PREFIX : CUR_SHEDDING_PREFIX) + "_" + loadId, nullableSchemaName);
        postProcessingTimeSeries.put(loadSheddingTimeSeriesName, loadTimeSeries);

        // Load shedding cost
        NodeCalc loadCostTimeSeries = BinaryOperation.multiply(loadTimeSeries, loadSheddingDoctrineCostsTimeSeries);
        String loadSheddingCostTimeSeriesName = MetrixDataName.getNameWithSchema((isPreventive ? PRE_SHEDDING_COST_PREFIX : CUR_SHEDDING_COST_PREFIX) + "_" + loadId, nullableSchemaName);
        postProcessingTimeSeries.put(loadSheddingCostTimeSeriesName, loadCostTimeSeries);
    }
}
