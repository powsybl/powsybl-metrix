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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.CURATIVE_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.PREVENTIVE_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.checkAllConfigured;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.createPostProcessingCostTimeSeries;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.findIdsToProcess;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.LOAD_CUR_PREFIX;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.LOAD_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.forEachContingencyTimeSeries;

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

    public static final LoadPostProcessingPrefixContainer PREVENTIVE_PREFIX_CONTAINER = new LoadPostProcessingPrefixContainer(
            MetrixVariable.PREVENTIVE_DOCTRINE_COST_DOWN,
            LOAD_PREFIX,
            PRE_SHEDDING_PREFIX,
            PRE_SHEDDING_COST_PREFIX);
    public static final LoadPostProcessingPrefixContainer CURATIVE_PREFIX_CONTAINER = new LoadPostProcessingPrefixContainer(
            MetrixVariable.CURATIVE_DOCTRINE_COST_DOWN,
            LOAD_CUR_PREFIX,
            CUR_SHEDDING_PREFIX,
            CUR_SHEDDING_COST_PREFIX);

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
        List<String> preventiveLoadIds = findIdsToProcess(metrixDslData.getPreventiveLoadsList(), allTimeSeriesNames, PREVENTIVE_PREFIX_CONTAINER.metrixResultPrefix());
        createLoadSheddingPostProcessingTimeSeries(PREVENTIVE_PREFIX_CONTAINER, preventiveLoadIds, Collections.emptySet());

        // Curative load shedding
        Set<String> contingencyIds = contingencies.stream().map(Contingency::getId).collect(Collectors.toSet());
        List<String> curativeLoadIds = findIdsToProcess(metrixDslData.getCurativeLoadsList(), allTimeSeriesNames, CURATIVE_PREFIX_CONTAINER.metrixResultPrefix(), contingencyIds);
        createLoadSheddingPostProcessingTimeSeries(CURATIVE_PREFIX_CONTAINER, curativeLoadIds, contingencyIds);
        return postProcessingTimeSeries;
    }

    /**
     * Check if doctrine costs are properly defined, i.e. all loads are configured
     * If not, no costs time series will be created
     * For each load having load shedding result (load shedding time series) (MW)
     * - create load shedding time series (MW)
     * - create load shedding cost time series
     *
     * @param prefixContainer    prefix of time series to create (preventive or curative)
     * @param loadIds            list of load ids having load shedding results
     * @param contingencyIds     list of contingency ids (empty for preventive)
     */
    private void createLoadSheddingPostProcessingTimeSeries(LoadPostProcessingPrefixContainer prefixContainer, List<String> loadIds, Set<String> contingencyIds) {
        // Check if doctrine costs are configured for all loads
        boolean allCostsConfigured = checkAllConfigured(loadIds, List.of(prefixContainer.doctrineCostVariable()), mappingConfig.getEquipmentToTimeSeries());
        if (!allCostsConfigured) {
            LOGGER.warn(DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED, "Load shedding");
        }

        // Compute load shedding cost time series
        loadIds.forEach(loadId -> createLoadSheddingPostProcessingTimeSeries(prefixContainer, loadId, contingencyIds, allCostsConfigured));
    }

    /**
     * For loadId each load shedding time series name result
     * - retrieve contingency name and contingency probability from time series name result
     * - create all load shedding postprocessing time series of loadId
     *
     * @param prefixContainer    prefix of time series to create (preventive or curative)
     * @param loadId             load id
     * @param contingencyIds     list of contingency ids (empty for preventive)
     * @param allCostsConfigured indicates if costs time series can be created
     */
    private void createLoadSheddingPostProcessingTimeSeries(LoadPostProcessingPrefixContainer prefixContainer,
                                                            String loadId,
                                                            Set<String> contingencyIds,
                                                            boolean allCostsConfigured) {
        forEachContingencyTimeSeries(prefixContainer.metrixResultPrefix(), loadId, contingencyIds, allTimeSeriesNames, contingencies, calculatedTimeSeries,
                (id, contingencyId, probabilityNodeCalc, loadTimeSeries) -> createLoadSheddingPostProcessingTimeSeries(id, contingencyId, probabilityNodeCalc, loadTimeSeries, prefixContainer, allCostsConfigured)
        );
    }

    /**
     * Create load shedding calculated time series
     *    load shedding = loadTimeSeries
     * Create cost load shedding calculated time series
     *    load shedding cost = load shedding * load shedding doctrine cost time series
     * @param loadId              load id
     * @param contingencyId       contingency id (empty for preventive)
     * @param probabilityNodeCalc contingency probability (ONE for preventive)
     * @param loadTimeSeries      metrix load shedding result time series
     * @param prefixContainer     prefix of time series to create (preventive or curative)
     * @param allCostsConfigured  indicates if costs time series can be created
     */
    private void createLoadSheddingPostProcessingTimeSeries(String loadId,
                                                            String contingencyId,
                                                            NodeCalc probabilityNodeCalc,
                                                            NodeCalc loadTimeSeries,
                                                            LoadPostProcessingPrefixContainer prefixContainer,
                                                            boolean allCostsConfigured) {
        LOGGER.debug("Creating load shedding postprocessing time-series for {} {}", loadId, contingencyId);
        String postfix = contingencyId.isEmpty() ? "" : ("_" + contingencyId);

        // Load shedding
        String loadSheddingTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.loadSheddingPrefix() + "_" + loadId, nullableSchemaName);
        postProcessingTimeSeries.put(loadSheddingTimeSeriesName + postfix, loadTimeSeries);

        // Load shedding cost
        if (allCostsConfigured) {
            String costTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(prefixContainer.doctrineCostVariable(), loadId));
            NodeCalc loadSheddingCostsTimeSeries = calculatedTimeSeries.computeIfAbsent(costTimeSeriesName, TimeSeriesNameNodeCalc::new);

            NodeCalc loadCostTimeSeries = BinaryOperation.multiply(BinaryOperation.multiply(loadTimeSeries, loadSheddingCostsTimeSeries), probabilityNodeCalc);
            createPostProcessingCostTimeSeries(postProcessingTimeSeries, loadCostTimeSeries, prefixContainer.loadSheddingCostPrefix(), loadId, postfix, nullableSchemaName);
        }
    }
}
