/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.mapping.MappingKey;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.metrix.integration.AbstractMetrix.MAX_THREAT_PREFIX;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixPostProcessingTimeSeries {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixPostProcessingTimeSeries.class);

    public static final String BASECASE_LOAD_PREFIX = "basecaseLoad_";
    public static final String BASECASE_OVERLOAD_PREFIX = "basecaseOverload_";
    public static final PostProcessingPrefixContainer OUTAGE_PREFIX_CONTAINER = new PostProcessingPrefixContainer("Outage", "outageLoad_", "outageOverload_", "overallOverload_", MAX_THREAT_PREFIX);
    public static final PostProcessingPrefixContainer ITAM_PREFIX_CONTAINER = new PostProcessingPrefixContainer("ITAM", "itamLoad_", "itamOverload_", "overallItamOverload_", MetrixOutputData.MAX_TMP_THREAT_FLOW);

    private MetrixPostProcessingTimeSeries() {
    }

    public static Map<String, NodeCalc> getPostProcessingTimeSeries(MetrixDslData dslData,
                                                                    TimeSeriesMappingConfig mappingConfig,
                                                                    ReadOnlyTimeSeriesStore store,
                                                                    String nullableSchemaName) {
        Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();
        if (dslData != null && mappingConfig != null) {
            Map<String, NodeCalc> calculatedTimeSeries = new HashMap<>(mappingConfig.getTimeSeriesNodes());
            createBasecasePostprocessingTimeSeries(dslData, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store, nullableSchemaName);
            createOutagePostprocessingTimeSeries(dslData, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store, nullableSchemaName);
            createItamPostprocessingTimeSeries(dslData, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store, nullableSchemaName);
        }
        return postProcessingTimeSeries;
    }

    private static NodeCalc createLoadTimeSeries(NodeCalc flowTimeSeries, NodeCalc ratingTimeSeries) {
        return BinaryOperation.multiply(BinaryOperation.div(flowTimeSeries, ratingTimeSeries), new FloatNodeCalc(100));
    }

    private static NodeCalc createLoadTimeSeries(NodeCalc flowTimeSeries, NodeCalc ratingTimeSeriesOrEx, NodeCalc ratingTimeSeriesExOr) {
        if (ratingTimeSeriesOrEx == ratingTimeSeriesExOr) {
            return createLoadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx);
        } else {
            NodeCalc zero = new IntegerNodeCalc(0);
            NodeCalc ratingTimeSeries = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(flowTimeSeries, zero), ratingTimeSeriesOrEx),
                    BinaryOperation.multiply(BinaryOperation.lessThan(flowTimeSeries, zero), ratingTimeSeriesExOr));
            return createLoadTimeSeries(flowTimeSeries, ratingTimeSeries);
        }
    }

    private static NodeCalc createOverloadTimeSeries(NodeCalc flowTimeSeries, NodeCalc ratingTimeSeriesOrEx, NodeCalc ratingTimeSeriesExOr) {
        NodeCalc positiveOverloadTimeSeries = BinaryOperation.minus(flowTimeSeries, ratingTimeSeriesOrEx);
        NodeCalc negativeRatingTimeSeries = UnaryOperation.negative(ratingTimeSeriesExOr);
        NodeCalc negativeOverloadTimeSeries = BinaryOperation.minus(flowTimeSeries, negativeRatingTimeSeries);
        return BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(flowTimeSeries, ratingTimeSeriesOrEx), positiveOverloadTimeSeries),
                BinaryOperation.multiply(BinaryOperation.lessThan(flowTimeSeries, negativeRatingTimeSeries), negativeOverloadTimeSeries));
    }

    private static NodeCalc createOverallOverloadTimeSeries(NodeCalc basecaseOverloadTimeSeries, NodeCalc otherOverloadTimeSeries) {
        return BinaryOperation.plus(UnaryOperation.abs(basecaseOverloadTimeSeries), UnaryOperation.abs(otherOverloadTimeSeries));
    }

    private static void createBasecasePostprocessingTimeSeries(MetrixDslData metrixDslData,
                                                               TimeSeriesMappingConfig mappingConfig,
                                                               Map<String, NodeCalc> postProcessingTimeSeries,
                                                               Map<String, NodeCalc> calculatedTimeSeries,
                                                               ReadOnlyTimeSeriesStore store,
                                                               String nullableSchemaName) {

        for (String branch : metrixDslData.getBranchMonitoringNList()) {
            MetrixVariable threshold = metrixDslData.getBranchMonitoringStatisticsThresholdN(branch);
            MetrixVariable thresholdEndOr = threshold == MetrixVariable.THRESHOLD_N ? MetrixVariable.THRESHOLD_N_END_OR : MetrixVariable.ANALYSIS_THRESHOLD_N_END_OR;
            if (mappingConfig.getTimeSeriesName(new MappingKey(threshold, branch)) != null) {
                createBasecasePostprocessingTimeSeries(branch, threshold, thresholdEndOr, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store, nullableSchemaName);
            }
        }
    }

    private static void createBasecasePostprocessingTimeSeries(String branch,
                                                               MetrixVariable thresholdN,
                                                               MetrixVariable thresholdNEndOr,
                                                               TimeSeriesMappingConfig mappingConfig,
                                                               Map<String, NodeCalc> postProcessingTimeSeries,
                                                               Map<String, NodeCalc> calculatedTimeSeries,
                                                               ReadOnlyTimeSeriesStore store,
                                                               String nullableSchemaName) {
        try {
            if (!store.timeSeriesExists(MetrixOutputData.FLOW_NAME + branch)) {
                LOGGER.debug("FLOW time-series not found for {}", branch);
                return;
            }
            LOGGER.debug("Creating basecase postprocessing time-series for {}", branch);
            NodeCalc flowTimeSeries = new TimeSeriesNameNodeCalc(MetrixDataName.getNameWithSchema(MetrixOutputData.FLOW_NAME + branch, nullableSchemaName));
            String ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(thresholdN, branch));
            NodeCalc ratingTimeSeriesOrEx = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);

            NodeCalc ratingTimeSeriesExOr = ratingTimeSeriesOrEx;
            if (thresholdNEndOr != null) {
                ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(thresholdNEndOr, branch));
                if (ratingTimeSeriesName != null) {
                    ratingTimeSeriesExOr = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);
                }
            }

            // Basecase load
            postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(BASECASE_LOAD_PREFIX + branch, nullableSchemaName), createLoadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx, ratingTimeSeriesExOr));
            // Basecase overload
            postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(BASECASE_OVERLOAD_PREFIX + branch, nullableSchemaName), createOverloadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx, ratingTimeSeriesExOr));
        } catch (IllegalStateException ise) {
            LOGGER.debug("Monitored branch {} not found in network", branch);
        }
    }

    private static void createOutagePostprocessingTimeSeries(MetrixDslData metrixDslData,
                                                             TimeSeriesMappingConfig mappingConfig,
                                                             Map<String, NodeCalc> postProcessingTimeSeries,
                                                             Map<String, NodeCalc> calculatedTimeSeries,
                                                             ReadOnlyTimeSeriesStore store,
                                                             String nullableSchemaName) {

        for (String branch : metrixDslData.getBranchMonitoringNkList()) {
            MetrixVariable threshold = metrixDslData.getBranchMonitoringStatisticsThresholdNk(branch);
            MetrixVariable thresholdEndOr = threshold == MetrixVariable.THRESHOLD_N1 ? MetrixVariable.THRESHOLD_N1_END_OR : MetrixVariable.ANALYSIS_THRESHOLD_NK_END_OR;
            if (mappingConfig.getTimeSeriesName(new MappingKey(threshold, branch)) != null) {
                createPostprocessingTimeSeries(branch, threshold, thresholdEndOr, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store, nullableSchemaName, OUTAGE_PREFIX_CONTAINER);
            }
        }
    }

    private static void createItamPostprocessingTimeSeries(MetrixDslData metrixDslData,
                                                           TimeSeriesMappingConfig mappingConfig,
                                                           Map<String, NodeCalc> postProcessingTimeSeries,
                                                           Map<String, NodeCalc> calculatedTimeSeries,
                                                           ReadOnlyTimeSeriesStore store,
                                                           String nullableSchemaName) {
        for (String branch : metrixDslData.getBranchMonitoringNkList()) {
            MetrixVariable threshold = MetrixVariable.THRESHOLD_ITAM;
            MetrixVariable thresholdEndOr = MetrixVariable.THRESHOLD_ITAM_END_OR;
            if (mappingConfig.getTimeSeriesName(new MappingKey(threshold, branch)) != null) {
                createPostprocessingTimeSeries(branch, threshold, thresholdEndOr, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store, nullableSchemaName, ITAM_PREFIX_CONTAINER);
            }
        }
    }

    private static void createPostprocessingTimeSeries(String branch,
                                                       MetrixVariable threshold,
                                                       MetrixVariable thresholdEndOr,
                                                       TimeSeriesMappingConfig mappingConfig,
                                                       Map<String, NodeCalc> postProcessingTimeSeries,
                                                       Map<String, NodeCalc> calculatedTimeSeries,
                                                       ReadOnlyTimeSeriesStore store,
                                                       String nullableSchemaName,
                                                       PostProcessingPrefixContainer postProcessingPrefixContainer) {
        try {
            if (!store.timeSeriesExists(postProcessingPrefixContainer.maxThreatPrefix + branch)) {
                LOGGER.debug("{}{} time-series not found", postProcessingPrefixContainer.maxThreatPrefix, branch);
                return;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating outage postprocessing time-series for {}", branch);
                LOGGER.debug("Creating {} postprocessing time-series for {}", postProcessingPrefixContainer.postProcessingType, branch);
            }
            NodeCalc flowTimeSeries = new TimeSeriesNameNodeCalc(MetrixDataName.getNameWithSchema(postProcessingPrefixContainer.maxThreatPrefix + branch, nullableSchemaName));
            String ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(threshold, branch));
            NodeCalc ratingTimeSeriesOrEx = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);

            NodeCalc ratingTimeSeriesExOr = ratingTimeSeriesOrEx;
            if (thresholdEndOr != null) {
                ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(thresholdEndOr, branch));
                if (ratingTimeSeriesName != null) {
                    ratingTimeSeriesExOr = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);

                }
            }

            // load
            postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(postProcessingPrefixContainer.loadPrefix + branch, nullableSchemaName), createLoadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx, ratingTimeSeriesExOr));
            // overload
            NodeCalc overloadTimeSeries = createOverloadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx, ratingTimeSeriesExOr);
            postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(postProcessingPrefixContainer.overloadPrefix + branch, nullableSchemaName), overloadTimeSeries);
            NodeCalc basecaseOverLoadTimeSeries = postProcessingTimeSeries.get(BASECASE_OVERLOAD_PREFIX + branch);
            if (!Objects.isNull(basecaseOverLoadTimeSeries)) {
                postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(postProcessingPrefixContainer.overallOverloadPrefix + branch, nullableSchemaName), createOverallOverloadTimeSeries(basecaseOverLoadTimeSeries, overloadTimeSeries));
            }
        } catch (IllegalStateException ise) {
            LOGGER.debug("Monitored branch {} not found in network", branch);
        }
    }
}
