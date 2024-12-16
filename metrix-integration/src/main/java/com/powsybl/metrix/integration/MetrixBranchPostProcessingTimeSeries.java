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
import com.powsybl.timeseries.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.powsybl.metrix.integration.AbstractMetrix.MAX_THREAT_PREFIX;
import static com.powsybl.metrix.integration.MetrixPostProcessingTimeSeries.findIdsToProcess;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixBranchPostProcessingTimeSeries {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixBranchPostProcessingTimeSeries.class);

    public static final String BASECASE_LOAD_PREFIX = "basecaseLoad_";
    public static final String BASECASE_OVERLOAD_PREFIX = "basecaseOverload_";
    public static final BranchPostProcessingPrefixContainer OUTAGE_PREFIX_CONTAINER = new BranchPostProcessingPrefixContainer("Outage", "outageLoad_", "outageOverload_", "overallOverload_", MAX_THREAT_PREFIX);
    public static final BranchPostProcessingPrefixContainer ITAM_PREFIX_CONTAINER = new BranchPostProcessingPrefixContainer("ITAM", "itamLoad_", "itamOverload_", "overallItamOverload_", MetrixOutputData.MAX_TMP_THREAT_FLOW);

    private final MetrixDslData metrixDslData;
    private final TimeSeriesMappingConfig mappingConfig;
    private final Set<String> allTimeSeriesNames;
    private final String nullableSchemaName;
    Map<String, NodeCalc> calculatedTimeSeries;

    private final Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();

    public MetrixBranchPostProcessingTimeSeries(MetrixDslData metrixDslData,
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
        createBaseCasePostProcessingTimeSeries();
        createOutagePostProcessingTimeSeries();
        createItamPostProcessingTimeSeries();
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

    private void createBaseCasePostProcessingTimeSeries() {
        List<String> branchIds = findIdsToProcess(metrixDslData.getBranchMonitoringNList(), allTimeSeriesNames, MetrixOutputData.FLOW_NAME);
        for (String branch : branchIds) {
            MetrixVariable threshold = metrixDslData.getBranchMonitoringStatisticsThresholdN(branch);
            if (mappingConfig.getTimeSeriesName(new MappingKey(threshold, branch)) != null) {
                MetrixVariable thresholdEndOr = threshold == MetrixVariable.THRESHOLD_N ? MetrixVariable.THRESHOLD_N_END_OR : MetrixVariable.ANALYSIS_THRESHOLD_N_END_OR;
                createBaseCasePostProcessingTimeSeries(branch, threshold, thresholdEndOr);
            }
        }
    }

    private void createBaseCasePostProcessingTimeSeries(String branch,
                                                        MetrixVariable thresholdN,
                                                        MetrixVariable thresholdNEndOr) {
        LOGGER.debug("Creating basecase postprocessing time-series for {}", branch);
        NodeCalc flowTimeSeries = new TimeSeriesNameNodeCalc(MetrixDataName.getNameWithSchema(MetrixOutputData.FLOW_NAME + branch, nullableSchemaName));
        RatingTimeSeriesData ratingTimeSeriesData = new RatingTimeSeriesData(branch, thresholdN, thresholdNEndOr);

        // Basecase load
        postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(BASECASE_LOAD_PREFIX + branch, nullableSchemaName), createLoadTimeSeries(flowTimeSeries, ratingTimeSeriesData.ratingTimeSeriesOrEx, ratingTimeSeriesData.ratingTimeSeriesExOr));
        // Basecase overload
        postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(BASECASE_OVERLOAD_PREFIX + branch, nullableSchemaName), createOverloadTimeSeries(flowTimeSeries, ratingTimeSeriesData.ratingTimeSeriesOrEx, ratingTimeSeriesData.ratingTimeSeriesExOr));
    }

    private void createOutagePostProcessingTimeSeries() {
        List<String> branchIds = findIdsToProcess(metrixDslData.getBranchMonitoringNList(), allTimeSeriesNames, OUTAGE_PREFIX_CONTAINER.maxThreatPrefix());
        for (String branch : branchIds) {
            MetrixVariable threshold = metrixDslData.getBranchMonitoringStatisticsThresholdNk(branch);
            if (mappingConfig.getTimeSeriesName(new MappingKey(threshold, branch)) != null) {
                MetrixVariable thresholdEndOr = threshold == MetrixVariable.THRESHOLD_N1 ? MetrixVariable.THRESHOLD_N1_END_OR : MetrixVariable.ANALYSIS_THRESHOLD_NK_END_OR;
                createPostProcessingTimeSeries(branch, threshold, thresholdEndOr, OUTAGE_PREFIX_CONTAINER);
            }
        }
    }

    private void createItamPostProcessingTimeSeries() {
        List<String> branchIds = findIdsToProcess(metrixDslData.getBranchMonitoringNList(), allTimeSeriesNames, ITAM_PREFIX_CONTAINER.maxThreatPrefix());
        for (String branch : branchIds) {
            MetrixVariable threshold = MetrixVariable.THRESHOLD_ITAM;
            if (mappingConfig.getTimeSeriesName(new MappingKey(threshold, branch)) != null) {
                MetrixVariable thresholdEndOr = MetrixVariable.THRESHOLD_ITAM_END_OR;
                createPostProcessingTimeSeries(branch, threshold, thresholdEndOr, ITAM_PREFIX_CONTAINER);
            }
        }
    }

    private void createPostProcessingTimeSeries(String branch,
                                                MetrixVariable threshold,
                                                MetrixVariable thresholdEndOr,
                                                BranchPostProcessingPrefixContainer postProcessingPrefixContainer) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating {} postprocessing time-series for {}", postProcessingPrefixContainer.postProcessingType(), branch);
        }
        NodeCalc flowTimeSeries = new TimeSeriesNameNodeCalc(MetrixDataName.getNameWithSchema(postProcessingPrefixContainer.maxThreatPrefix() + branch, nullableSchemaName));
        RatingTimeSeriesData ratingTimeSeriesData = new RatingTimeSeriesData(branch, threshold, thresholdEndOr);

        // load
        postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(postProcessingPrefixContainer.loadPrefix() + branch, nullableSchemaName), createLoadTimeSeries(flowTimeSeries, ratingTimeSeriesData.ratingTimeSeriesOrEx, ratingTimeSeriesData.ratingTimeSeriesExOr));
        // overload
        NodeCalc overloadTimeSeries = createOverloadTimeSeries(flowTimeSeries, ratingTimeSeriesData.ratingTimeSeriesOrEx, ratingTimeSeriesData.ratingTimeSeriesExOr);
        postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(postProcessingPrefixContainer.overloadPrefix() + branch, nullableSchemaName), overloadTimeSeries);
        NodeCalc basecaseOverLoadTimeSeries = postProcessingTimeSeries.get(BASECASE_OVERLOAD_PREFIX + branch);
        if (!Objects.isNull(basecaseOverLoadTimeSeries)) {
            postProcessingTimeSeries.put(MetrixDataName.getNameWithSchema(postProcessingPrefixContainer.overallOverloadPrefix() + branch, nullableSchemaName), createOverallOverloadTimeSeries(basecaseOverLoadTimeSeries, overloadTimeSeries));
        }
    }

    private class RatingTimeSeriesData {
        protected String ratingTimeSeriesName;
        protected NodeCalc ratingTimeSeriesOrEx;
        protected NodeCalc ratingTimeSeriesExOr;

        protected RatingTimeSeriesData(String branch,
                                       MetrixVariable threshold,
                                       MetrixVariable thresholdEndOr) {
            ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(threshold, branch));
            ratingTimeSeriesOrEx = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);
            ratingTimeSeriesExOr = ratingTimeSeriesOrEx;
            if (thresholdEndOr != null) {
                ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(thresholdEndOr, branch));
                if (ratingTimeSeriesName != null) {
                    ratingTimeSeriesExOr = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);
                }
            }
        }
    }
}
