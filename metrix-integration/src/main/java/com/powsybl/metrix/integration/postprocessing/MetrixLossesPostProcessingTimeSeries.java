/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.postprocessing;

import com.powsybl.metrix.integration.MetrixDataName;
import com.powsybl.metrix.integration.MetrixVariable;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ast.BinaryOperation;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.LOSSES;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class MetrixLossesPostProcessingTimeSeries {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixLossesPostProcessingTimeSeries.class);

    public static final String LOSSES_COST_PREFIX = "lossesCost";

    private final TimeSeriesMappingConfig mappingConfig;
    private final String nullableSchemaName;
    Map<String, NodeCalc> calculatedTimeSeries;

    private final Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();

    public MetrixLossesPostProcessingTimeSeries(TimeSeriesMappingConfig mappingConfig,
                                                String nullableSchemaName) {
        this.mappingConfig = mappingConfig;
        this.nullableSchemaName = nullableSchemaName;
        this.calculatedTimeSeries = new HashMap<>(mappingConfig.getTimeSeriesNodes());
    }

    /**
     * Create postprocessing calculated time series for losses
     * For metrix LOSSES result (MW), create cost time series :
     *    lossesCost = LOSSES * losses cost time series
     */
    public Map<String, NodeCalc> createPostProcessingTimeSeries() {
        // Retrieve doctrine costs time series
        Optional<String> lossesDoctrineCostsTimeSeriesName = mappingConfig.getEquipmentToTimeSeries().entrySet().stream()
                .filter(entry -> entry.getKey().mappingVariable() == MetrixVariable.LOSSES_DOCTRINE_COST)
                .findFirst()
                .map(Map.Entry::getValue);
        if (lossesDoctrineCostsTimeSeriesName.isEmpty()) {
            LOGGER.warn(DOCTRINE_COSTS_ARE_NOT_PROPERLY_CONFIGURED, "Losses");
        } else {
            String timeSeriesName = lossesDoctrineCostsTimeSeriesName.get();
            NodeCalc lossesDoctrineCostsTimeSeries = mappingConfig.getTimeSeriesNodes().computeIfAbsent(timeSeriesName, TimeSeriesNameNodeCalc::new);

            // Reference to Metrix losses time series result
            String lossesTimeSeriesName = MetrixDataName.getNameWithSchema(LOSSES, nullableSchemaName);
            NodeCalc lossesTimeSeries = new TimeSeriesNameNodeCalc(lossesTimeSeriesName);

            // Compute losses cost time series
            NodeCalc lossesCostTimeSeries = BinaryOperation.multiply(lossesTimeSeries, lossesDoctrineCostsTimeSeries);
            postProcessingTimeSeries.put(LOSSES_COST_PREFIX, lossesCostTimeSeries);
        }

        return postProcessingTimeSeries;
    }
}
