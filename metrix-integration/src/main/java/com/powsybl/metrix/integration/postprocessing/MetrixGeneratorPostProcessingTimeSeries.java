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
import com.powsybl.timeseries.ast.IntegerNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import com.powsybl.timeseries.ast.UnaryOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.GEN_CUR_PREFIX;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.GEN_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.CURATIVE_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.PREVENTIVE_PREFIX;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixGeneratorPostProcessingTimeSeries extends AbstractMetrixEquipmentPostProcessing {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixGeneratorPostProcessingTimeSeries.class);
    private static final NodeCalc ZERO = new IntegerNodeCalc(0);

    public static final String REDISPATCHING = "redispatching";
    public static final String REDISPATCHING_UP = REDISPATCHING + "Up";
    public static final String REDISPATCHING_UP_COST = REDISPATCHING + "UpCost";
    public static final String REDISPATCHING_DOWN = REDISPATCHING + "Down";
    public static final String REDISPATCHING_DOWN_COST = REDISPATCHING + "DownCost";
    public static final String REDISPATCHING_COST = REDISPATCHING + "Cost";

    public static final String PRE_REDISPATCHING_UP_PREFIX = PREVENTIVE_PREFIX + REDISPATCHING_UP;
    public static final String PRE_REDISPATCHING_UP_COST_PREFIX = PREVENTIVE_PREFIX + REDISPATCHING_UP_COST;
    public static final String PRE_REDISPATCHING_DOWN_PREFIX = PREVENTIVE_PREFIX + REDISPATCHING_DOWN;
    public static final String PRE_REDISPATCHING_DOWN_COST_PREFIX = PREVENTIVE_PREFIX + REDISPATCHING_DOWN_COST;
    public static final String PRE_REDISPATCHING_PREFIX = PREVENTIVE_PREFIX + REDISPATCHING;
    public static final String PRE_REDISPATCHING_COST_PREFIX = PREVENTIVE_PREFIX + REDISPATCHING_COST;

    public static final String CUR_REDISPATCHING_UP_PREFIX = CURATIVE_PREFIX + REDISPATCHING_UP;
    public static final String CUR_REDISPATCHING_UP_COST_PREFIX = CURATIVE_PREFIX + REDISPATCHING_UP_COST;
    public static final String CUR_REDISPATCHING_DOWN_PREFIX = CURATIVE_PREFIX + REDISPATCHING_DOWN;
    public static final String CUR_REDISPATCHING_DOWN_COST_PREFIX = CURATIVE_PREFIX + REDISPATCHING_DOWN_COST;
    public static final String CUR_REDISPATCHING_PREFIX = CURATIVE_PREFIX + REDISPATCHING;
    public static final String CUR_REDISPATCHING_COST_PREFIX = CURATIVE_PREFIX + REDISPATCHING_COST;

    public static final GeneratorPostProcessingPrefixContainer PREVENTIVE_PREFIX_CONTAINER = new GeneratorPostProcessingPrefixContainer(
            GEN_PREFIX,
            PRE_REDISPATCHING_UP_PREFIX, PRE_REDISPATCHING_UP_COST_PREFIX,
            PRE_REDISPATCHING_DOWN_PREFIX, PRE_REDISPATCHING_DOWN_COST_PREFIX,
            PRE_REDISPATCHING_PREFIX, PRE_REDISPATCHING_COST_PREFIX);
    public static final GeneratorPostProcessingPrefixContainer CURATIVE_PREFIX_CONTAINER = new GeneratorPostProcessingPrefixContainer(
            GEN_CUR_PREFIX,
            CUR_REDISPATCHING_UP_PREFIX, CUR_REDISPATCHING_UP_COST_PREFIX,
            CUR_REDISPATCHING_DOWN_PREFIX, CUR_REDISPATCHING_DOWN_COST_PREFIX,
            CUR_REDISPATCHING_PREFIX, CUR_REDISPATCHING_COST_PREFIX);

    public MetrixGeneratorPostProcessingTimeSeries(MetrixDslData metrixDslData,
                                                   TimeSeriesMappingConfig mappingConfig,
                                                   Map<String, NodeCalc> contingencyProbabilityById,
                                                   Set<String> allTimeSeriesNames,
                                                   String nullableSchemaName) {
        super(metrixDslData, mappingConfig, contingencyProbabilityById, allTimeSeriesNames, nullableSchemaName, PostProcessingEquipmentType.GENERATOR);
    }

    @Override
    protected List<MetrixVariable> getRequiredVariables(PostProcessingPrefixContainer prefixContainer) {
        return List.of(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN);
    }

    /**
     * Create up and down redispatching calculated time series by decomposing genTimeSeries
     * <ul>
     *     <li>generator redispatching up = genTimeSeries keeping values >0 and putting 0 otherwise</li>
     *     <li>generator redispatching down = genTimeSeries keeping values <0 and putting 0 otherwise</li>
     * </ul>
     * Create up and down costs calculated time series
     * <ul>
     *    <li>generator redispatching up cost = generator redispatching up * redispatching up doctrine cost time series * contingency probability</li>
     *    <li>generator redispatching down cost = |generator redispatching down| * redispatching down doctrine cost time series * contingency probability</li>
     * </ul>
     * Create global costs calculated time series
     * <ul>
     *    <li>generator redispatching cost = generator redispatching up cost + generator redispatching down cost</li>
     * </ul>
     * @param generatorId         generator id
     * @param contingencyContext  contingency context
     * @param genTimeSeries       metrix redispatching result time series
     * @param prefixContainer     prefix of time series to create (preventive or curative)
     * @param allCostsConfigured  indicates if costs time series can be created
     */
    @Override
    protected void compute(String generatorId,
                           ContingencyContext contingencyContext,
                           NodeCalc genTimeSeries,
                           PostProcessingPrefixContainer prefixContainer,
                           boolean allCostsConfigured) {

        LOGGER.debug("Creating redispatching postprocessing time-series for {}", generatorId);

        GeneratorPostProcessingPrefixContainer generatorPrefixContainer = (GeneratorPostProcessingPrefixContainer) prefixContainer;

        // Generator up and down redispatching
        NodeCalc genUpPositiveConditionTimeSeries = BinaryOperation.greaterThan(genTimeSeries, ZERO);
        NodeCalc genDownNegativeConditionTimeSeries = BinaryOperation.lessThan(genTimeSeries, ZERO);
        NodeCalc genUpTimeSeries = BinaryOperation.multiply(genTimeSeries, genUpPositiveConditionTimeSeries);
        NodeCalc genDownTimeSeries = BinaryOperation.multiply(genTimeSeries, genDownNegativeConditionTimeSeries);
        postProcessingTimeSeries.put(buildName(contingencyContext, generatorPrefixContainer.redispatchingUpPrefix(), generatorId), genUpTimeSeries);
        postProcessingTimeSeries.put(buildName(contingencyContext, generatorPrefixContainer.redispatchingDownPrefix(), generatorId), genDownTimeSeries);

        if (!allCostsConfigured) {
            return;
        }

        // Generator up and down redispatching cost
        NodeCalc genUpCostTimeSeries = BinaryOperation.multiply(BinaryOperation.multiply(genUpTimeSeries, getUpCost(generatorId)), contingencyContext.probability());
        NodeCalc genDownCostTimeSeries = BinaryOperation.multiply(BinaryOperation.multiply(UnaryOperation.abs(genDownTimeSeries), getDownCost(generatorId)), contingencyContext.probability());
        postProcessingTimeSeries.put(buildName(contingencyContext, generatorPrefixContainer.redispatchingUpCostPrefix(), generatorId), genUpCostTimeSeries);
        postProcessingTimeSeries.put(buildName(contingencyContext, generatorPrefixContainer.redispatchingDownCostPrefix(), generatorId), genDownCostTimeSeries);

        // Generator global redispatching cost = up cost + down cost
        NodeCalc genCostTimeSeries = BinaryOperation.plus(genUpCostTimeSeries, genDownCostTimeSeries);
        postProcessingTimeSeries.put(buildName(contingencyContext, generatorPrefixContainer.redispatchingCostPrefix(), generatorId), genCostTimeSeries);
    }

    @Override
    protected Set<String> getPreventiveIds() {
        return dslData.getGeneratorsForRedispatching();
    }

    @Override
    protected Set<String> getCurativeIds() {
        return dslData.getGeneratorsForRedispatching();
    }

    private NodeCalc getUpCost(String generatorId) {
        String tsName = mappingConfig.getTimeSeriesName(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, generatorId));
        return calculatedTimeSeries.computeIfAbsent(tsName, TimeSeriesNameNodeCalc::new);
    }

    private NodeCalc getDownCost(String generatorId) {
        String tsName = mappingConfig.getTimeSeriesName(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN, generatorId));
        return calculatedTimeSeries.computeIfAbsent(tsName, TimeSeriesNameNodeCalc::new);
    }
}
