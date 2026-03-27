/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.MetrixVariable;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.references.MappingKey;
import com.powsybl.timeseries.ast.BinaryOperation;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.LOAD_CUR_PREFIX;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.LOAD_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.CURATIVE_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.PREVENTIVE_PREFIX;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixLoadPostProcessingTimeSeries extends AbstractMetrixEquipmentPostProcessing {

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

    public MetrixLoadPostProcessingTimeSeries(MetrixDslData dslData,
                                              TimeSeriesMappingConfig mappingConfig,
                                              Map<String, NodeCalc> contingencyProbabilityById,
                                              Set<String> allTimeSeriesNames,
                                              String nullableSchemaName) {
        super(dslData, mappingConfig, contingencyProbabilityById, allTimeSeriesNames, nullableSchemaName, PostProcessingEquipmentType.LOAD);
    }

    @Override
    protected Set<String> getPreventiveIds() {
        return dslData.getPreventiveLoadsList();
    }

    @Override
    protected Set<String> getCurativeIds() {
        return dslData.getCurativeLoadsList();
    }

    @Override
    protected List<MetrixVariable> getRequiredVariables(PostProcessingPrefixContainer prefixContainer) {
        return List.of(((LoadPostProcessingPrefixContainer) prefixContainer).doctrineCostVariable());
    }

    /**
     * Create load shedding calculated time series
     * <ul>
     *     <li>load shedding = loadTimeSeries</li>
     * </ul>
     * Create cost load shedding calculated time series
     * <ul>
     *     <li>load shedding cost = load shedding * load shedding doctrine cost time series</li>
     * </ul>
     * @param loadId              load id
     * @param contingencyContext  contingency context
     * @param loadTimeSeries      metrix load shedding result time series
     * @param prefixContainer     prefix of time series to create (preventive or curative)
     * @param allCostsConfigured  indicates if costs time series can be created
     */
    @Override
    protected void compute(String loadId,
                           ContingencyContext contingencyContext,
                           NodeCalc loadTimeSeries,
                           PostProcessingPrefixContainer prefixContainer,
                           boolean allCostsConfigured) {

        LOGGER.debug("Creating load shedding postprocessing time-series for {}", loadId);

        LoadPostProcessingPrefixContainer loadPrefixContainer = (LoadPostProcessingPrefixContainer) prefixContainer;

        // Load shedding
        postProcessingTimeSeries.put(buildName(contingencyContext, loadPrefixContainer.loadSheddingPrefix(), loadId), loadTimeSeries);

        if (!allCostsConfigured) {
            return;
        }

        // Load shedding cost
        NodeCalc loadCostTimeSeries = BinaryOperation.multiply(BinaryOperation.multiply(loadTimeSeries, getCost(loadId, loadPrefixContainer.doctrineCostVariable())), contingencyContext.probability());
        postProcessingTimeSeries.put(buildName(contingencyContext, loadPrefixContainer.loadSheddingCostPrefix(), loadId), loadCostTimeSeries);
    }

    private NodeCalc getCost(String loadId, MetrixVariable variable) {
        String tsName = mappingConfig.getTimeSeriesName(new MappingKey(variable, loadId));
        return calculatedTimeSeries.computeIfAbsent(tsName, TimeSeriesNameNodeCalc::new);
    }
}
