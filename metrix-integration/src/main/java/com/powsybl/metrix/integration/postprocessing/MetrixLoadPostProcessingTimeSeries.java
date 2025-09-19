/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.contingency.Contingency;
import com.powsybl.metrix.integration.MetrixDataName;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.MetrixVariable;
import com.powsybl.metrix.mapping.references.MappingKey;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
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
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.CURATIVE_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.PREVENTIVE_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.findIdsToProcess;
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
    private final List<Contingency> contingencies;
    private final Set<String> allTimeSeriesNames;
    private final String nullableSchemaName;
    Map<String, NodeCalc> calculatedTimeSeries;

    private final Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();

    public MetrixLoadPostProcessingTimeSeries(MetrixDslData metrixDslData,
                                              TimeSeriesMappingConfig mappingConfig,
                                              List<Contingency> contingencies,
                                              Set<String> allTimeSeriesNames,
                                              String nullableSchemaName) {
        this.metrixDslData = metrixDslData;
        this.mappingConfig = mappingConfig;
        this.contingencies = contingencies;
        this.allTimeSeriesNames = allTimeSeriesNames;
        this.nullableSchemaName = nullableSchemaName;
        this.calculatedTimeSeries = new HashMap<>(mappingConfig.getTimeSeriesNodes());
    }

    /**
     * Create postprocessing calculated time series for preventive and curative load shedding
     */
    public Map<String, NodeCalc> createPostProcessingTimeSeries() {
        // Preventive load shedding
        List<String> preventiveLoadIds = findIdsToProcess(metrixDslData.getPreventiveLoadsList(), allTimeSeriesNames, LOAD_PREFIX);
        createLoadSheddingPostProcessingTimeSeries(LOAD_PREFIX, preventiveLoadIds, MetrixVariable.PREVENTIVE_DOCTRINE_COST_DOWN, PRE_SHEDDING_PREFIX, PRE_SHEDDING_COST_PREFIX);

        // Curative load shedding
        Set<String> contingencyIds = contingencies.stream().map(Contingency::getId).collect(Collectors.toSet());
        List<String> curativeLoadIds = findIdsToProcess(metrixDslData.getCurativeLoadsList(), allTimeSeriesNames, LOAD_CUR_PREFIX, contingencyIds);
        createLoadSheddingPostProcessingTimeSeries(LOAD_CUR_PREFIX, curativeLoadIds, MetrixVariable.CURATIVE_DOCTRINE_COST_DOWN, CUR_SHEDDING_PREFIX, CUR_SHEDDING_COST_PREFIX);
        return postProcessingTimeSeries;
    }

    /**
     * For each load having load shedding result (load shedding time series) (MW)
     * - create load shedding time series (MW)
     * - create load shedding cost time series
     * @param prefix             prefix of metrix results time series to process
     * @param loadIds            list of load ids having load shedding results
     * @param variable           doctrine cost configuration variable
     * @param sheddingPrefix     prefix of shedding time series to create (preventive or curative)
     * @param sheddingCostPrefix prefix of shedding cost time series to create (preventive or curative)
     */
    private void createLoadSheddingPostProcessingTimeSeries(String prefix, List<String> loadIds, MetrixVariable variable, String sheddingPrefix, String sheddingCostPrefix) {
        // Retrieve doctrine costs time series
        List<String> doctrineCostsTimeSeriesNames = loadIds.stream().map(id -> mappingConfig.getTimeSeriesName(new MappingKey(variable, id))).toList();
        if (doctrineCostsTimeSeriesNames.stream().anyMatch(Objects::isNull)) {
            LOGGER.warn(DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED, "Load shedding");
            return;
        }

        int size = loadIds.size();
        for (int i = 0; i < size; i++) {
            String loadId = loadIds.get(i);
            String timeSeriesName = doctrineCostsTimeSeriesNames.get(i);
            NodeCalc loadSheddingDoctrineCostsTimeSeries = calculatedTimeSeries.computeIfAbsent(timeSeriesName, TimeSeriesNameNodeCalc::new);

            // Reference to Metrix load shedding time series result
            NodeCalc loadTimeSeries = new TimeSeriesNameNodeCalc(prefix + loadId);

            // Compute load shedding cost time series
            createLoadSheddingPostProcessingTimeSeries(loadId, loadTimeSeries, loadSheddingDoctrineCostsTimeSeries, sheddingPrefix, sheddingCostPrefix);
        }
    }

    /**
     * Create load shedding calculated time series
     *    prefix_shedding_loadId = LOAD_loadId
     * Create cost load shedding calculated time series
     *    prefix_sheddingCost_loadId = prefix_shedding_loadId * load shedding doctrine cost time series
     * @param loadId                              load id
     * @param loadTimeSeries                      metrix load shedding result time series
     * @param loadSheddingDoctrineCostsTimeSeries load shedding doctrine cost time series
     * @param sheddingPrefix                      prefix of shedding time series to create (preventive or curative)
     * @param sheddingCostPrefix                  prefix of shedding cost time series to create (preventive or curative)
     */
    private void createLoadSheddingPostProcessingTimeSeries(String loadId,
                                                            NodeCalc loadTimeSeries,
                                                            NodeCalc loadSheddingDoctrineCostsTimeSeries,
                                                            String sheddingPrefix,
                                                            String sheddingCostPrefix) {
        LOGGER.debug("Creating load shedding postprocessing time-series for {}", loadId);

        // Load shedding
        String loadSheddingTimeSeriesName = MetrixDataName.getNameWithSchema(sheddingPrefix + "_" + loadId, nullableSchemaName);
        postProcessingTimeSeries.put(loadSheddingTimeSeriesName, loadTimeSeries);

        // Load shedding cost
        NodeCalc loadCostTimeSeries = BinaryOperation.multiply(loadTimeSeries, loadSheddingDoctrineCostsTimeSeries);
        String loadSheddingCostTimeSeriesName = MetrixDataName.getNameWithSchema(sheddingCostPrefix + "_" + loadId, nullableSchemaName);
        postProcessingTimeSeries.put(loadSheddingCostTimeSeriesName, loadCostTimeSeries);
    }
}
