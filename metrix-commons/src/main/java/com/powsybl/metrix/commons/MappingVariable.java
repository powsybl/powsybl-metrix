/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.commons;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public interface MappingVariable {

    ServiceLoaderCache<MappingVariableProvider> MAPPING_VARIABLE_LOADER = new ServiceLoaderCache<>(MappingVariableProvider.class);

    static void writeJson(MappingVariable variable, JsonGenerator generator) {
        Objects.requireNonNull(generator);
        try {
            List<MappingVariableProvider> mappingVariableProviders = MAPPING_VARIABLE_LOADER.getServices();
            List<MappingVariableProvider> providers = mappingVariableProviders.stream().filter(p -> p.getFieldName().equals(variable.getFieldName())).collect(Collectors.toList());
            if (providers.size() != 1) {
                throw new IllegalStateException("No MappingVariable provider found for fieldName " + variable.getFieldName());
            }
            MappingVariableProvider provider = providers.getFirst();
            provider.writeJson(variable, generator);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static MappingVariable parseJson(JsonParser parser) {
        Objects.requireNonNull(parser);
        MappingVariable variable = null;
        try {
            List<MappingVariableProvider> mappingVariableProviders = MAPPING_VARIABLE_LOADER.getServices();
            JsonToken token;
            while ((token = parser.nextToken()) != null) {
                if (token == JsonToken.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    List<MappingVariableProvider> providers = mappingVariableProviders.stream().filter(p -> p.getFieldName().equals(fieldName)).toList();
                    if (providers.size() != 1) {
                        throw new IllegalStateException("No MappingVariable provider found for fieldName " + fieldName);
                    }
                    MappingVariableProvider provider = providers.getFirst();
                    variable = provider.parseJson(parser);
                } else if (token.equals(JsonToken.END_OBJECT)) {
                    if (variable != null) {
                        return variable;
                    } else {
                        throw new IllegalStateException("Incomplete mapping variable json");
                    }
                }
            }
            return variable;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String getVariableName();

    String getFieldName();
}
