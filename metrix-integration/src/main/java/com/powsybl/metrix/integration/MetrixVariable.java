/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.metrix.mapping.MappingVariable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

public enum MetrixVariable implements MappingVariable {
    offGridCostDown("offGridCostDown"),
    offGridCostUp("offGridCostUp"),
    onGridCostDown("onGridCostDown"),
    onGridCostUp("onGridCostUp"),
    activePowerSetpoint("activePowerSetpoint"),
    analysisThresholdN("analysisThresholdN"),
    analysisThresholdNk("analysisThresholdNk"),
    analysisThresholdNEndOr("analysisThresholdNEndOr"),
    analysisThresholdNkEndOr("analysisThresholdNkEndOr"),
    thresholdN("thresholdN"),
    thresholdN1("thresholdN1"),
    thresholdNk("thresholdNk"),
    thresholdITAM("thresholdITAM"),
    thresholdITAMNk("thresholdITAMNk"),
    thresholdNEndOr("thresholdNEndOr"),
    thresholdN1EndOr("thresholdN1EndOr"),
    thresholdNkEndOr("thresholdNkEndOr"),
    thresholdITAMEndOr("thresholdITAMEndOr"),
    thresholdITAMNkEndOr("thresholdITAMNkEndOr"),
    curativeCostDown("curativeCostDown");

    private static final String NAME = "metrix";

    static String getName() {
        return NAME;
    }

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
                return MetrixVariable.valueOf(parser.getValueAsString());
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
}
