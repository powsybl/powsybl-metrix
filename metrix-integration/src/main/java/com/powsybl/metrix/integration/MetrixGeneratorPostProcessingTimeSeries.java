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
import com.powsybl.timeseries.ast.IntegerNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import com.powsybl.timeseries.ast.UnaryOperation;
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
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.GEN_CUR_PREFIX;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.GEN_PREFIX;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixGeneratorPostProcessingTimeSeries {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixGeneratorPostProcessingTimeSeries.class);

    public static final String PREVENTIVE = "preventive";
    public static final String CURATIVE = "curative";

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
            PREVENTIVE,
            PRE_REDISPATCHING_UP_PREFIX, PRE_REDISPATCHING_UP_COST_PREFIX,
            PRE_REDISPATCHING_DOWN_PREFIX, PRE_REDISPATCHING_DOWN_COST_PREFIX,
            PRE_REDISPATCHING_PREFIX, PRE_REDISPATCHING_COST_PREFIX);
    public static final GeneratorPostProcessingPrefixContainer CURATIVE_PREFIX_CONTAINER = new GeneratorPostProcessingPrefixContainer(
            CURATIVE,
            CUR_REDISPATCHING_UP_PREFIX, CUR_REDISPATCHING_UP_COST_PREFIX,
            CUR_REDISPATCHING_DOWN_PREFIX, CUR_REDISPATCHING_DOWN_COST_PREFIX,
            CUR_REDISPATCHING_PREFIX, CUR_REDISPATCHING_COST_PREFIX);

    private final MetrixDslData metrixDslData;
    private final TimeSeriesMappingConfig mappingConfig;
    private final Set<String> allTimeSeriesNames;
    private final String nullableSchemaName;
    Map<String, NodeCalc> calculatedTimeSeries;

    private final Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();

    public MetrixGeneratorPostProcessingTimeSeries(MetrixDslData metrixDslData,
                                                   TimeSeriesMappingConfig mappingConfig,
                                                   Set<String> allTimeSeriesNames,
                                                   String nullableSchemaName) {
        this.metrixDslData = metrixDslData;
        this.mappingConfig = mappingConfig;
        this.allTimeSeriesNames = allTimeSeriesNames;
        this.nullableSchemaName = nullableSchemaName;
        this.calculatedTimeSeries = new HashMap<>(mappingConfig.getTimeSeriesNodes());
    }

    /**
     * Create postprocessing calculated time series for preventive and curative redispatching
     */
    public Map<String, NodeCalc> createPostProcessingTimeSeries() {
        // Preventive
        createRedispatchingPostProcessingTimeSeries(PREVENTIVE_PREFIX_CONTAINER);

        // Curative
        createRedispatchingPostProcessingTimeSeries(CURATIVE_PREFIX_CONTAINER);

        return postProcessingTimeSeries;
    }

    /**
     * For each generator having GEN_generatorId result (generator redispatching time series) (MW)
     * - create up and down redispatching time series (MW)
     * - create up and down costs time series
     * - create global cost time series
     * @param prefixContainer prefix of time series to create (preventive or curative)
     */
    private void createRedispatchingPostProcessingTimeSeries(GeneratorPostProcessingPrefixContainer prefixContainer) {
        String prefix = prefixContainer.postProcessingType().equals(PREVENTIVE) ? GEN_PREFIX : GEN_CUR_PREFIX;
        List<String> generatorIds = findIdsToProcess(metrixDslData.getGeneratorsForRedispatching(), allTimeSeriesNames, prefix);

        // Retrieve doctrine costs time series
        List<String> upCostsTimeSeriesNames = generatorIds.stream().map(id -> mappingConfig.getTimeSeriesName(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, id))).toList();
        List<String> downCostsTimeSeriesNames = generatorIds.stream().map(id -> mappingConfig.getTimeSeriesName(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN, id))).toList();
        if (upCostsTimeSeriesNames.stream().anyMatch(Objects::isNull) || downCostsTimeSeriesNames.stream().anyMatch(Objects::isNull)) {
            LOGGER.warn(DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED, "Redispatching");
            return;
        }

        int size = generatorIds.size();
        for (int i = 0; i < size; i++) {
            String generatorId = generatorIds.get(i);
            String upCostsTimeSeriesName = upCostsTimeSeriesNames.get(i);
            String downCostsTimeSeriesName = downCostsTimeSeriesNames.get(i);
            NodeCalc upCostsTimeSeries = calculatedTimeSeries.computeIfAbsent(upCostsTimeSeriesName, TimeSeriesNameNodeCalc::new);
            NodeCalc downCostsTimeSeries = calculatedTimeSeries.computeIfAbsent(downCostsTimeSeriesName, TimeSeriesNameNodeCalc::new);

            // Reference to Metrix redispatching time series result
            NodeCalc genTimeSeries = new TimeSeriesNameNodeCalc(prefix + generatorId);

            // Compute redispatching up, down, cost time series
            createRedispatchingPostProcessingTimeSeries(generatorId, genTimeSeries, upCostsTimeSeries, downCostsTimeSeries, prefixContainer);
        }
    }

    /**
     * Create up and down redispatching calculated time series by decomposing GEN_generatorId
     *    prefix_redispatchingUp_generatorId = GEN_generatorId keeping values >0 and putting 0 otherwise
     *    prefix_redispatchingDown_generatorId = GEN_generatorId keeping values <0 and putting 0 otherwise
     * Create up and down costs calculated time series
     *    prefix_redispatchingUpCost_generatorId = pre_redispatchingUp_generatorId * redispatching up doctrine cost time series
     *    prefix_redispatchingDownCost_generatorId = pre_redispatchingDown_generatorId * redispatching down doctrine cost time series
     * Create global costs calculated time series
     *    prefix_redispatchingCost_generatorId = pre_redispatchingUpCost_generatorId + pre_redispatchingDownCost_generatorId
     * @param generatorId         generator id
     * @param genTimeSeries       GEN_generatorId time series
     * @param upCostsTimeSeries   redispatching up doctrine cost time series
     * @param downCostsTimeSeries redispatching down doctrine cost time series
     * @param prefixContainer     prefix of time series to create (preventive or curative)
     */
    private void createRedispatchingPostProcessingTimeSeries(String generatorId,
                                                             NodeCalc genTimeSeries,
                                                             NodeCalc upCostsTimeSeries,
                                                             NodeCalc downCostsTimeSeries,
                                                             GeneratorPostProcessingPrefixContainer prefixContainer) {
        LOGGER.debug("Creating redispatching postprocessing time-series for {}", generatorId);
        NodeCalc zero = new IntegerNodeCalc(0);

        // Generator up and down redispatching
        NodeCalc genUpPositiveConditionTimeSeries = BinaryOperation.greaterThan(genTimeSeries, zero);
        NodeCalc genDownNegativeConditionTimeSeries = BinaryOperation.lessThan(genTimeSeries, zero);
        NodeCalc genUpTimeSeries = BinaryOperation.multiply(genTimeSeries, genUpPositiveConditionTimeSeries);
        NodeCalc genDownTimeSeries = BinaryOperation.multiply(genTimeSeries, genDownNegativeConditionTimeSeries);
        String genUpTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingUpPrefix() + "_" + generatorId, nullableSchemaName);
        String genDownTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingDownPrefix() + "_" + generatorId, nullableSchemaName);
        postProcessingTimeSeries.put(genUpTimeSeriesName, genUpTimeSeries);
        postProcessingTimeSeries.put(genDownTimeSeriesName, genDownTimeSeries);

        // Generator up and down redispatching cost
        NodeCalc genUpCostTimeSeries = UnaryOperation.abs(BinaryOperation.multiply(genUpTimeSeries, upCostsTimeSeries));
        NodeCalc genDownCostTimeSeries = UnaryOperation.abs(BinaryOperation.multiply(genDownTimeSeries, downCostsTimeSeries));
        String genUpCostTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingUpCostPrefix() + "_" + generatorId, nullableSchemaName);
        String genDownCostTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingDownCostPrefix() + "_" + generatorId, nullableSchemaName);
        postProcessingTimeSeries.put(genUpCostTimeSeriesName, genUpCostTimeSeries);
        postProcessingTimeSeries.put(genDownCostTimeSeriesName, genDownCostTimeSeries);

        // Generator global redispatching cost = up cost + down cost
        NodeCalc genCostTimeSeries = BinaryOperation.plus(genUpCostTimeSeries, genDownCostTimeSeries);
        String genCostTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingCostPrefix() + "_" + generatorId, nullableSchemaName);
        postProcessingTimeSeries.put(genCostTimeSeriesName, genCostTimeSeries);
    }
}
