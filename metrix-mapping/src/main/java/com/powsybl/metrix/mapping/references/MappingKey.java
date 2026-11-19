/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.references;

import com.powsybl.commons.json.JsonUtil;
import com.powsybl.metrix.commons.MappingVariable;
import com.powsybl.timeseries.TimeSeriesException;
import org.jspecify.annotations.NonNull;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public record MappingKey(MappingVariable mappingVariable, String id) {

    public static String toJson(MappingKey key) {
        Objects.requireNonNull(key);
        return JsonUtil.toJson(key::toJson);
    }

    public void toJson(JsonGenerator generator) {
        writeJson(generator, this);
    }

    public static void writeJson(JsonGenerator generator, MappingKey key) {
        Objects.requireNonNull(generator);
        generator.writeStartObject();
        generator.writeName("mappingVariable");
        MappingVariable.writeJson(key.mappingVariable(), generator);
        generator.writeStringProperty("id", key.id());
        generator.writeEndObject();
    }

    public static MappingKey parseJson(JsonParser parser) {
        Objects.requireNonNull(parser);
        MappingVariable mappingVariable = null;
        String id = null;
        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            if (token.equals(JsonToken.PROPERTY_NAME)) {
                String fieldName = parser.currentName();
                switch (fieldName) {
                    case "mappingVariable" -> mappingVariable = MappingVariable.parseJson(parser);
                    case "id" -> {
                        parser.nextToken();
                        id = parser.getValueAsString();
                    }
                    default -> throw new IllegalStateException("Unexpected field name " + fieldName);
                }
            } else if (token.equals(JsonToken.END_OBJECT)) {
                if (mappingVariable != null && id != null) {
                    return new MappingKey(mappingVariable, id);
                } else {
                    throw new IllegalStateException("Incomplete mapping key json");
                }
            }
        }
        throw new TimeSeriesException("Invalid mapping key JSON");
    }

    public MappingKey(MappingVariable mappingVariable, String id) {
        Objects.requireNonNull(mappingVariable);
        Objects.requireNonNull(id);
        this.mappingVariable = Objects.requireNonNull(mappingVariable);
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode() + mappingVariable.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MappingKey(MappingVariable variable, String id1)) {
            return id.equals(id1) && mappingVariable.equals(variable);
        }
        return false;
    }

    @Override
    @NonNull
    public String toString() {
        return "MappingKey(mappingVariable=" + mappingVariable + ", id=" + id + ")";
    }
}
