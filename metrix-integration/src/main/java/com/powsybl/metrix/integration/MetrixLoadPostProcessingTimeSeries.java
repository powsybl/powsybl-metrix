/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.contingency.Contingency;
import com.powsybl.metrix.mapping.MappingKey;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ast.BinaryOperation;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.CURATIVE_PREFIX;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.PREVENTIVE_PREFIX;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.createPostProcessingCostTimeSeries;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.findIdsToProcess;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.forEachContingencyTimeSeries;
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
        createLoadSheddingPostProcessingTimeSeries(LOAD_PREFIX, preventiveLoadIds, MetrixVariable.PREVENTIVE_DOCTRINE_COST_DOWN, PRE_SHEDDING_PREFIX, PRE_SHEDDING_COST_PREFIX, Collections.emptySet());

        // Curative load shedding
        Set<String> contingencyIds = contingencies.stream().map(Contingency::getId).collect(Collectors.toSet());
        List<String> curativeLoadIds = findIdsToProcess(metrixDslData.getCurativeLoadsList(), allTimeSeriesNames, LOAD_CUR_PREFIX, contingencyIds);
        createLoadSheddingPostProcessingTimeSeries(LOAD_CUR_PREFIX, curativeLoadIds, MetrixVariable.CURATIVE_DOCTRINE_COST_DOWN, CUR_SHEDDING_PREFIX, CUR_SHEDDING_COST_PREFIX, contingencyIds);
        return postProcessingTimeSeries;
    }

    /**
     * For each load having load shedding result (load shedding time series) (MW)
     * - create load shedding time series (MW)
     * - create load shedding cost time series
     *
     * @param prefix             prefix of metrix results time series to process
     * @param loadIds            list of load ids having load shedding results
     * @param variable           doctrine cost configuration variable
     * @param sheddingPrefix     prefix of shedding time series to create (preventive or curative)
     * @param sheddingCostPrefix prefix of shedding cost time series to create (preventive or curative)
     * @param contingencyIds     list of contingency ids (empty for preventive)
     */
    private void createLoadSheddingPostProcessingTimeSeries(String prefix, List<String> loadIds, MetrixVariable variable, String sheddingPrefix, String sheddingCostPrefix, Set<String> contingencyIds) {
        // Retrieve doctrine costs time series
        List<String> doctrineCostsTimeSeriesNames = loadIds.stream().map(id -> mappingConfig.getTimeSeriesName(new MappingKey(variable, id))).toList();
        if (doctrineCostsTimeSeriesNames.stream().anyMatch(Objects::isNull)) {
            LOGGER.warn(DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED, "Load shedding");
            return;
        }

        int size = loadIds.size();
        for (int i = 0; i < size; i++) {
            String loadId = loadIds.get(i);
            String costTimeSeriesName = doctrineCostsTimeSeriesNames.get(i);
            NodeCalc loadSheddingCostsTimeSeries = calculatedTimeSeries.computeIfAbsent(costTimeSeriesName, TimeSeriesNameNodeCalc::new);

            // Compute load shedding cost time series
            createLoadSheddingPostProcessingTimeSeries(prefix, loadId, contingencyIds, loadSheddingCostsTimeSeries, sheddingPrefix, sheddingCostPrefix);
        }
    }

    /**
     * For loadId each load shedding time series name result
     * - retrieve contingency name and contingency probability from time series name result
     * - create all load shedding postprocessing time series of loadId
     *
     * @param prefix                      prefix of metrix results time series to process
     * @param loadId                      load id
     * @param contingencyIds              list of contingency ids (empty for preventive)
     * @param loadSheddingCostsTimeSeries load shedding doctrine cost time series
     * @param sheddingPrefix              prefix of load shedding time series to create
     * @param sheddingCostPrefix          prefix of load shedding cost time series to create
     */
    private void createLoadSheddingPostProcessingTimeSeries(String prefix,
                                                            String loadId,
                                                            Set<String> contingencyIds,
                                                            NodeCalc loadSheddingCostsTimeSeries,
                                                            String sheddingPrefix,
                                                            String sheddingCostPrefix) {
        forEachContingencyTimeSeries(prefix, loadId, contingencyIds, allTimeSeriesNames, contingencies, calculatedTimeSeries,
                (id, contingencyId, probabilityNodeCalc, loadTimeSeries) -> createLoadSheddingPostProcessingTimeSeries(id, contingencyId, probabilityNodeCalc, loadTimeSeries, loadSheddingCostsTimeSeries, sheddingPrefix, sheddingCostPrefix)
        );
    }

    /**
     * Create load shedding calculated time series
     *    load shedding = loadTimeSeries
     * Create cost load shedding calculated time series
     *    load shedding cost = load shedding * load shedding doctrine cost time series
     * @param loadId                      load id
     * @param contingencyId               contingency id (empty for preventive)
     * @param probabilityNodeCalc         contingency probability (ONE for preventive)
     * @param loadTimeSeries              metrix load shedding result time series
     * @param loadSheddingCostsTimeSeries load shedding doctrine cost time series
     * @param sheddingPrefix              prefix of load shedding time series to create
     * @param sheddingCostPrefix          prefix of load shedding cost time series to create
     */
    private void createLoadSheddingPostProcessingTimeSeries(String loadId,
                                                            String contingencyId,
                                                            NodeCalc probabilityNodeCalc,
                                                            NodeCalc loadTimeSeries,
                                                            NodeCalc loadSheddingCostsTimeSeries,
                                                            String sheddingPrefix,
                                                            String sheddingCostPrefix) {
        LOGGER.debug("Creating load shedding postprocessing time-series for {} {}", loadId, contingencyId);
        String postfix = contingencyId.isEmpty() ? "" : ("_" + contingencyId);

        // Load shedding
        String loadSheddingTimeSeriesName = MetrixDataName.getNameWithSchema(sheddingPrefix + "_" + loadId, nullableSchemaName);
        postProcessingTimeSeries.put(loadSheddingTimeSeriesName + postfix, loadTimeSeries);

        // Load shedding cost
        NodeCalc loadCostTimeSeries = BinaryOperation.multiply(BinaryOperation.multiply(loadTimeSeries, loadSheddingCostsTimeSeries), probabilityNodeCalc);
        createPostProcessingCostTimeSeries(postProcessingTimeSeries, loadCostTimeSeries, sheddingCostPrefix, loadId, postfix, nullableSchemaName);
    }
}
