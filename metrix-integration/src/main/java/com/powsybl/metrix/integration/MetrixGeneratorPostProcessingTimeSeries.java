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
import com.powsybl.timeseries.ast.IntegerNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import com.powsybl.timeseries.ast.UnaryOperation;
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
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.GEN_CUR_PREFIX;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.GEN_PREFIX;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixGeneratorPostProcessingTimeSeries {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixGeneratorPostProcessingTimeSeries.class);
    private static final NodeCalc ZERO = new IntegerNodeCalc(0);

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
    private final List<Contingency> contingencies;
    private final Set<String> allTimeSeriesNames;
    private final String nullableSchemaName;
    Map<String, NodeCalc> calculatedTimeSeries;

    private final Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();

    public MetrixGeneratorPostProcessingTimeSeries(MetrixDslData metrixDslData,
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
     * Create postprocessing calculated time series for preventive and curative redispatching
     */
    public Map<String, NodeCalc> createPostProcessingTimeSeries() {
        // Preventive
        List<String> preventiveGeneratorIds = findIdsToProcess(metrixDslData.getGeneratorsForRedispatching(), allTimeSeriesNames, GEN_PREFIX);
        createRedispatchingPostProcessingTimeSeries(PREVENTIVE_PREFIX_CONTAINER, GEN_PREFIX, preventiveGeneratorIds, Collections.emptySet());

        // Curative
        Set<String> contingencyIds = contingencies.stream().map(Contingency::getId).collect(Collectors.toSet());
        List<String> curativeGeneratorIds = findIdsToProcess(metrixDslData.getGeneratorsForRedispatching(), allTimeSeriesNames, GEN_CUR_PREFIX, contingencyIds);
        createRedispatchingPostProcessingTimeSeries(CURATIVE_PREFIX_CONTAINER, GEN_CUR_PREFIX, curativeGeneratorIds, contingencyIds);

        return postProcessingTimeSeries;
    }

    /**
     * For each generator having redispatching result (generator redispatching time series) (MW)
     * - create up and down redispatching time series (MW)
     * - create up and down costs time series
     * - create global cost time series
     *
     * @param prefixContainer prefix of time series to create (preventive or curative)
     * @param prefix          prefix of metrix results time series to process
     * @param generatorIds    list of generator ids having redispatching results
     * @param contingencyIds      list of contingency ids (empty for preventive)
     */
    private void createRedispatchingPostProcessingTimeSeries(GeneratorPostProcessingPrefixContainer prefixContainer, String prefix, List<String> generatorIds, Set<String> contingencyIds) {
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

            // Compute redispatching up, down, cost time series
            createRedispatchingPostProcessingTimeSeries(prefix, generatorId, contingencyIds, upCostsTimeSeries, downCostsTimeSeries, prefixContainer);
        }
    }

    /**
     * For generatorId each redispatching time series name result:
     * <ul>
     *     <li>retrieve contingency name and contingency probability from time series name result</li>
     *     <li>create all redispatching postprocessing time series of generatorId</li>
     * </ul>
     *
     * @param prefix              prefix of metrix results time series to process
     * @param generatorId         generator id
     * @param contingencyIds      list of contingency ids (empty for preventive)
     * @param upCostsTimeSeries   redispatching up doctrine cost time series
     * @param downCostsTimeSeries redispatching down doctrine cost time series
     * @param prefixContainer     prefix of time series to create (preventive or curative)
     */
    private void createRedispatchingPostProcessingTimeSeries(String prefix,
                                                             String generatorId,
                                                             Set<String> contingencyIds,
                                                             NodeCalc upCostsTimeSeries,
                                                             NodeCalc downCostsTimeSeries,
                                                             GeneratorPostProcessingPrefixContainer prefixContainer) {

        forEachContingencyTimeSeries(prefix, generatorId, contingencyIds, allTimeSeriesNames, contingencies, calculatedTimeSeries,
                (id, contingencyId, probabilityNodeCalc, genTimeSeries) -> createRedispatchingPostProcessingTimeSeries(id, contingencyId, probabilityNodeCalc, genTimeSeries, upCostsTimeSeries, downCostsTimeSeries, prefixContainer)
        );
    }

    /**
     * Create up and down redispatching calculated time series by decomposing genTimeSeries
     * <ul>
     *     <li>generator redispatching up = genTimeSeries keeping values >0 and putting 0 otherwise</li>
     *     <li>generator redispatching down = genTimeSeries keeping values <0 and putting 0 otherwise</li>
     * </ul>
     * Create up and down costs calculated time series
     *    generator redispatching up cost = generator redispatching up * redispatching up doctrine cost time series * contingency probability
     *    generator redispatching down cost = |generator redispatching down| * redispatching down doctrine cost time series * contingency probability
     * Create global costs calculated time series
     *    generator redispatching cost = generator redispatching up cost + generator redispatching down cost
     * @param generatorId         generator id
     * @param contingencyId       contingency id (empty for preventive)
     * @param probabilityNodeCalc contingency probability (ONE for preventive)
     * @param genTimeSeries       metrix redispatching result time series
     * @param upCostsTimeSeries   redispatching up doctrine cost time series
     * @param downCostsTimeSeries redispatching down doctrine cost time series
     * @param prefixContainer     prefix of time series to create (preventive or curative)
     */
    private void createRedispatchingPostProcessingTimeSeries(String generatorId,
                                                             String contingencyId,
                                                             NodeCalc probabilityNodeCalc,
                                                             NodeCalc genTimeSeries,
                                                             NodeCalc upCostsTimeSeries,
                                                             NodeCalc downCostsTimeSeries,
                                                             GeneratorPostProcessingPrefixContainer prefixContainer) {
        LOGGER.debug("Creating redispatching postprocessing time-series for {} {}", generatorId, contingencyId);
        String postfix = contingencyId.isEmpty() ? "" : ("_" + contingencyId);

        // Generator up and down redispatching
        NodeCalc genUpPositiveConditionTimeSeries = BinaryOperation.greaterThan(genTimeSeries, ZERO);
        NodeCalc genDownNegativeConditionTimeSeries = BinaryOperation.lessThan(genTimeSeries, ZERO);
        NodeCalc genUpTimeSeries = BinaryOperation.multiply(genTimeSeries, genUpPositiveConditionTimeSeries);
        NodeCalc genDownTimeSeries = BinaryOperation.multiply(genTimeSeries, genDownNegativeConditionTimeSeries);
        String genUpTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingUpPrefix() + "_" + generatorId, nullableSchemaName);
        String genDownTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingDownPrefix() + "_" + generatorId, nullableSchemaName);
        postProcessingTimeSeries.put(genUpTimeSeriesName + postfix, genUpTimeSeries);
        postProcessingTimeSeries.put(genDownTimeSeriesName + postfix, genDownTimeSeries);

        // Generator up and down redispatching cost
        NodeCalc genUpCostTimeSeries = BinaryOperation.multiply(BinaryOperation.multiply(genUpTimeSeries, upCostsTimeSeries), probabilityNodeCalc);
        NodeCalc genDownCostTimeSeries = BinaryOperation.multiply(BinaryOperation.multiply(UnaryOperation.abs(genDownTimeSeries), downCostsTimeSeries), probabilityNodeCalc);
        String genUpCostTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingUpCostPrefix() + "_" + generatorId, nullableSchemaName);
        String genDownCostTimeSeriesName = MetrixDataName.getNameWithSchema(prefixContainer.redispatchingDownCostPrefix() + "_" + generatorId, nullableSchemaName);
        postProcessingTimeSeries.put(genUpCostTimeSeriesName + postfix, genUpCostTimeSeries);
        postProcessingTimeSeries.put(genDownCostTimeSeriesName + postfix, genDownCostTimeSeries);

        // Generator global redispatching cost = up cost + down cost
        NodeCalc genCostTimeSeries = BinaryOperation.plus(genUpCostTimeSeries, genDownCostTimeSeries);
        createPostProcessingCostTimeSeries(postProcessingTimeSeries, genCostTimeSeries, prefixContainer.redispatchingCostPrefix(), generatorId, postfix, nullableSchemaName);
    }
}
