/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.metrix.mapping.MappingVariable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public enum MetrixVariable implements MappingVariable {
    OFF_GRID_COST_DOWN("offGridCostDown"),
    OFF_GRID_COST_UP("offGridCostUp"),
    ON_GRID_COST_DOWN("onGridCostDown"),
    ON_GRID_COST_UP("onGridCostUp"),
    ON_GRID_DOCTRINE_COST_DOWN("onGridDoctrineCostDown"),
    ON_GRID_DOCTRINE_COST_UP("onGridDoctrineCostUp"),
    ANALYSIS_THRESHOLD_N("analysisThresholdN"),
    ANALYSIS_THRESHOLD_NK("analysisThresholdNk"),
    ANALYSIS_THRESHOLD_N_END_OR("analysisThresholdNEndOr"),
    ANALYSIS_THRESHOLD_NK_END_OR("analysisThresholdNkEndOr"),
    THRESHOLD_N("thresholdN"),
    THRESHOLD_N1("thresholdN1"),
    THRESHOLD_NK("thresholdNk"),
    THRESHOLD_ITAM("thresholdITAM"),
    THRESHOLD_ITAM_NK("thresholdITAMNk"),
    THRESHOLD_N_END_OR("thresholdNEndOr"),
    THRESHOLD_N1_END_OR("thresholdN1EndOr"),
    THRESHOLD_NK_END_OR("thresholdNkEndOr"),
    THRESHOLD_ITAM_END_OR("thresholdITAMEndOr"),
    THRESHOLD_ITAM_NK_END_OR("thresholdITAMNkEndOr"),
    CURATIVE_COST_DOWN("curativeCostDown"),
    PREVENTIVE_DOCTRINE_COST_DOWN("preventiveDoctrineCostDown"),
    CURATIVE_DOCTRINE_COST_DOWN("curativeDoctrineCostDown"),
    LOSSES_DOCTRINE_COST("lossesDoctrineCost");

    protected static final String NAME = "metrix";

    @Override
    public String getFieldName() {
        return NAME;
    }

    static void writeJson(MetrixVariable variable, JsonGenerator generator) {
        Objects.requireNonNull(generator);
        try {
            generator.writeStartObject();
            generator.writeFieldName(variable.getFieldName());
            generator.writeString(variable.getVariableName());
            generator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static MappingVariable parseJson(JsonParser parser) throws IOException {
        JsonToken token = parser.nextToken();
        if (token != null) {
            if (token == JsonToken.VALUE_STRING) {
                return MetrixVariable.nameOf(parser.getValueAsString());
            } else {
                throw new IllegalStateException("Unexpected JSON token: " + token);
            }
        }
        throw new IllegalStateException("Invalid MetrixVariable JSON");
    }

    private final String variable;

    MetrixVariable(String variable) {
        this.variable = Objects.requireNonNull(variable);
    }

    @Override
    public String getVariableName() {
        return variable;
    }

    public static MetrixVariable nameOf(String value) {
        return Arrays.stream(MetrixVariable.values()).filter(name -> name.variable.equals(value)).findFirst().orElse(null);
    }
}
