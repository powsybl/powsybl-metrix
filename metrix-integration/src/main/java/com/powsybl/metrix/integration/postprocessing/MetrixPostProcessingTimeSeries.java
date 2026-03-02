/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.contingency.Contingency;
import com.powsybl.metrix.integration.MetrixVariable;
import com.powsybl.metrix.integration.contingency.Probability;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.references.MappingKey;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesFilter;
import com.powsybl.timeseries.ast.DoubleNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixPostProcessingTimeSeries {

    public static final String CURATIVE_PREFIX = "cur_";
    public static final String PREVENTIVE_PREFIX = "pre_";

    public static final String DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED = "{} doctrine costs are not properly configured";

    private MetrixPostProcessingTimeSeries() {
    }

    /**
     * Applies a filter on allIds : id is kept if allTimeSeriesNames contains a String equals to 'prefix' + 'id'
     * Search for each id of allIds list if there is a time series of (prefix + id) name
     * @param allIds             list of equipment ids to filter
     * @param allTimeSeriesNames all metrix results time series names
     * @param prefix             prefix of metrix results time series names to keep
     */
    public static List<String> findIdsToProcess(Set<String> allIds, Set<String> allTimeSeriesNames, String prefix) {
        return allIds.stream().filter(id -> allTimeSeriesNames.stream().anyMatch(s -> s.equals(prefix + id))).toList();
    }

    /**
     * Applies a filter on allIds : id is kept if allTimeSeriesNames contains a String starting with 'prefix' + 'id' + '_'
     * and followed by one element of contingencyIds
     * For curative results only (redispatching and load shedding)
     * @param allIds             list of equipment ids to filter
     * @param allTimeSeriesNames all metrix results time series names
     * @param prefix             prefix of metrix results time series names to keep
     * @param contingencyIds     list of contingency ids
     */
    public static List<String> findIdsToProcess(Set<String> allIds, Set<String> allTimeSeriesNames, String prefix, Set<String> contingencyIds) {
        return allIds.stream().filter(id -> {
            String prefixAndId = prefix + id + "_";
            return allTimeSeriesNames.stream().filter(tsName -> tsName.startsWith(prefixAndId)).anyMatch(s -> {
                String contingencyId = s.substring(prefixAndId.length());
                return contingencyIds.contains(contingencyId);
            });
        }).toList();
    }

    /**
     * Check if equipmentToTimeSeries contains configuration for each pair (id, variable)
     * @param ids                   list of equipment ids to check
     * @param variables             list of variables to check
     * @param equipmentToTimeSeries map of configured equipment time series
     */
    public static boolean checkAllConfigured(Set<String> ids, List<MetrixVariable> variables, Map<MappingKey, String> equipmentToTimeSeries) {
        return ids.stream()
            .flatMap(id -> variables.stream()
                .map(variable -> new MappingKey(variable, id)))
            .noneMatch(key -> equipmentToTimeSeries.get(key) == null);
    }

    /**
     * Find contingency id from time series name
     * @param tsName time series name formatted as a String equals to 'prefix' + 'contingency id'
     * @param prefix time series name prefix
     * @return contingency id
     */
    public static String getContingencyIdFromTsName(String tsName, String prefix) {
        if (tsName == null || prefix == null) {
            throw new IllegalArgumentException("Time series name and/or prefix cannot be null");
        }
        if (tsName.isEmpty() || prefix.isEmpty()) {
            throw new IllegalArgumentException("Time series name and/or prefix cannot be empty");
        }
        if (!tsName.startsWith(prefix)) {
            throw new IllegalArgumentException("Time series name '" + tsName + "' does not start with prefix '" + prefix + "'");
        }
        if (tsName.length() == prefix.length()) {
            throw new IllegalArgumentException("Time series name '" + tsName + "' does not contain a contingency id after prefix '" + prefix + "'");
        }
        return tsName.substring(prefix.length());
    }

    /**
     * Find probability NodeCalc associated with a contingency and add it to the map of calculated time series
     * If no probability is associated with the contingency, return a default probability of 0.001
     * @param contingency          contingency
     * @param calculatedTimeSeries map of existing calculated time series
     * @return if a probability time series reference is present, return TimeSeriesNameNodeCalc, otherwise return DoubleNodeCalc
     */
    public static NodeCalc getProbabilityNodeCalc(Contingency contingency, Map<String, NodeCalc> calculatedTimeSeries) {
        Probability probability = contingency.getExtension(Probability.class);
        if (probability != null && probability.getProbabilityTimeSeriesRef() != null) {
            return calculatedTimeSeries.computeIfAbsent(probability.getProbabilityTimeSeriesRef(), TimeSeriesNameNodeCalc::new);
        }
        if (probability != null && probability.getProbabilityBase() != null) {
            return calculatedTimeSeries.computeIfAbsent(probability.getProbabilityBase().toString(), k -> new DoubleNodeCalc(probability.getProbabilityBase()));
        }
        // No probability defined > default value
        return calculatedTimeSeries.computeIfAbsent("defaultProbability", k -> new DoubleNodeCalc(0.001F));
    }

    /**
     * Create branch, generator, load, losses calculated time series for postprocessing
     * @param dslData            metrix configuration
     * @param mappingConfig      mapping configuration
     * @param store              time series store containing metrix results
     * @param nullableSchemaName schema name, otherwise null
     */
    public static Map<String, NodeCalc> getPostProcessingTimeSeries(MetrixDslData dslData,
                                                                    TimeSeriesMappingConfig mappingConfig,
                                                                    List<Contingency> contingencies,
                                                                    ReadOnlyTimeSeriesStore store,
                                                                    String nullableSchemaName) {
        if (dslData == null || mappingConfig == null) {
            return Collections.emptyMap();
        }

        Set<String> allTimeSeriesNames = store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(false));

        // Branch
        MetrixBranchPostProcessingTimeSeries branchProcessing = new MetrixBranchPostProcessingTimeSeries(dslData, mappingConfig, allTimeSeriesNames, nullableSchemaName);

        // Initialize post-processing TimeSeries
        Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>(branchProcessing.createPostProcessingTimeSeries());

        // Map contingency id -> contingency probability
        Map<String, NodeCalc> contingencyProbabilityById = contingencies.stream().collect(Collectors.toMap(Contingency::getId, cty -> getProbabilityNodeCalc(cty, postProcessingTimeSeries)));

        // Generator
        MetrixGeneratorPostProcessingTimeSeries generatorProcessing = new MetrixGeneratorPostProcessingTimeSeries(dslData, mappingConfig, contingencyProbabilityById, allTimeSeriesNames, nullableSchemaName);
        postProcessingTimeSeries.putAll(generatorProcessing.createPostProcessingTimeSeries());

        // Load
        MetrixLoadPostProcessingTimeSeries loadProcessing = new MetrixLoadPostProcessingTimeSeries(dslData, mappingConfig, contingencyProbabilityById, allTimeSeriesNames, nullableSchemaName);
        postProcessingTimeSeries.putAll(loadProcessing.createPostProcessingTimeSeries());

        // Losses
        MetrixLossesPostProcessingTimeSeries lossesProcessing = new MetrixLossesPostProcessingTimeSeries(mappingConfig, nullableSchemaName);
        postProcessingTimeSeries.putAll(lossesProcessing.createPostProcessingTimeSeries());

        return postProcessingTimeSeries;
    }
}
