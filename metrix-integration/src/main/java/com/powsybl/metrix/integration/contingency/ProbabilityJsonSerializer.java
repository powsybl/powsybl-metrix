/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.contingency;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.extensions.ExtensionJsonSerializer;
import com.powsybl.contingency.Contingency;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
@AutoService(ExtensionJsonSerializer.class)
public class ProbabilityJsonSerializer implements ExtensionJsonSerializer<Contingency, Probability> {
    @Override
    public void serialize(Probability extension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        if (extension.getProbabilityBase() != null && !Double.isNaN(extension.getProbabilityBase())) {
            jsonGenerator.writeNumberField("probabilityBase", extension.getProbabilityBase());
        }
        if (StringUtils.isNotBlank(extension.getProbabilityTimeSeriesRef())) {
            jsonGenerator.writeStringField("probabilityTs", extension.getProbabilityTimeSeriesRef());
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public Probability deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        Double base = null;
        String ts = null;
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentName().equals("probabilityBase")) {
                parser.nextToken();
                base = parser.readValueAs(Double.class);
            } else if (parser.currentName().equals("probabilityTs")) {
                parser.nextToken();
                ts = parser.readValueAs(String.class);
            } else {
                throw new PowsyblException("Unexpected field: " + parser.currentName());
            }
        }

        if (base != null || ts != null) {
            return new Probability(base, ts);
        }
        return null;
    }

    @Override
    public String getExtensionName() {
        return Probability.EXTENSION_NAME;
    }

    @Override
    public String getCategoryName() {
        return "security-analysis";
    }

    @Override
    public Class<? super Probability> getExtensionClass() {
        return Probability.class;
    }
}
