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
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.CURATIVE_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.PREVENTIVE_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.checkAllConfigured;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.createPostProcessingCostTimeSeries;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.findIdsToProcess;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.GEN_CUR_PREFIX;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.GEN_PREFIX;
import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.forEachContingencyTimeSeries;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixGeneratorPostProcessingTimeSeries {

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
        List<String> preventiveGeneratorIds = findIdsToProcess(metrixDslData.getGeneratorsForRedispatching(), allTimeSeriesNames, PREVENTIVE_PREFIX_CONTAINER.metrixResultPrefix());
        createRedispatchingPostProcessingTimeSeries(PREVENTIVE_PREFIX_CONTAINER, preventiveGeneratorIds, Collections.emptySet());

        // Curative
        Set<String> contingencyIds = contingencies.stream().map(Contingency::getId).collect(Collectors.toSet());
        List<String> curativeGeneratorIds = findIdsToProcess(metrixDslData.getGeneratorsForRedispatching(), allTimeSeriesNames, CURATIVE_PREFIX_CONTAINER.metrixResultPrefix(), contingencyIds);
        createRedispatchingPostProcessingTimeSeries(CURATIVE_PREFIX_CONTAINER, curativeGeneratorIds, contingencyIds);

        return postProcessingTimeSeries;
    }

    /**
     * Check if doctrine costs are properly defined, i.e., all generators are configured for up and down costs.
     * If not, no costs time series will be created.
     * For each generator having redispatching result (generator redispatching time series) (MW)
     * - create up and down redispatching time series (MW)
     * - create up and down costs time series
     * - create global cost time series
     *
     * @param prefixContainer prefix of time series to create (preventive or curative)
     * @param generatorIds    list of generator ids having redispatching results
     * @param contingencyIds  list of contingency ids (empty for preventive)
     */
    private void createRedispatchingPostProcessingTimeSeries(GeneratorPostProcessingPrefixContainer prefixContainer, List<String> generatorIds, Set<String> contingencyIds) {
        // Check if up and down doctrine costs are configured for all generators
        boolean allCostsConfigured = checkAllConfigured(generatorIds, List.of(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN), mappingConfig.getEquipmentToTimeSeries());
        if (!allCostsConfigured) {
            LOGGER.warn(DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED, "Redispatching");
        }

        // Compute redispatching up, down, cost time series
        generatorIds.forEach(generatorId -> createRedispatchingPostProcessingTimeSeries(generatorId, contingencyIds, prefixContainer, allCostsConfigured));
    }

    /**
     * For generatorId each redispatching time series name result:
     * <ul>
     *     <li>retrieve contingency name and contingency probability from time series name result</li>
     *     <li>create all redispatching postprocessing time series of generatorId</li>
     * </ul>
     *
     * @param generatorId        generator id
     * @param contingencyIds     list of contingency ids (empty for preventive)
     * @param prefixContainer    prefix of time series to create (preventive or curative)
     * @param allCostsConfigured indicates if costs time series can be created
     */
    private void createRedispatchingPostProcessingTimeSeries(String generatorId,
                                                             Set<String> contingencyIds,
                                                             GeneratorPostProcessingPrefixContainer prefixContainer,
                                                             boolean allCostsConfigured) {

        forEachContingencyTimeSeries(prefixContainer.metrixResultPrefix(), generatorId, contingencyIds, allTimeSeriesNames, contingencies, calculatedTimeSeries,
                (id, contingencyId, probabilityNodeCalc, genTimeSeries) -> createRedispatchingPostProcessingTimeSeries(id, contingencyId, probabilityNodeCalc, genTimeSeries, prefixContainer, allCostsConfigured)
        );
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
     * Create global costs calculated time series</li>
     * <ul>
     *    <li>generator redispatching cost = generator redispatching up cost + generator redispatching down cost
     * </ul>
     * @param generatorId         generator id
     * @param contingencyId       contingency id (empty for preventive)
     * @param probabilityNodeCalc contingency probability (ONE for preventive)
     * @param genTimeSeries       metrix redispatching result time series
     * @param prefixContainer     prefix of time series to create (preventive or curative)
     * @param allCostsConfigured  indicates if costs time series can be created
     */
    private void createRedispatchingPostProcessingTimeSeries(String generatorId,
                                                             String contingencyId,
                                                             NodeCalc probabilityNodeCalc,
                                                             NodeCalc genTimeSeries,
                                                             GeneratorPostProcessingPrefixContainer prefixContainer,
                                                             boolean allCostsConfigured) {
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

        if (allCostsConfigured) {
            // Generator up and down redispatching cost
            String upCostsTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, generatorId));
            String downCostsTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN, generatorId));
            NodeCalc upCostsTimeSeries = calculatedTimeSeries.computeIfAbsent(upCostsTimeSeriesName, TimeSeriesNameNodeCalc::new);
            NodeCalc downCostsTimeSeries = calculatedTimeSeries.computeIfAbsent(downCostsTimeSeriesName, TimeSeriesNameNodeCalc::new);
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
}
