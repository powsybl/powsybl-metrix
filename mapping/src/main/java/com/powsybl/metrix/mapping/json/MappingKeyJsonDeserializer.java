/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.powsybl.metrix.mapping.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.metrix.mapping.MappingKey;

import java.util.Objects;

/**
 * @author Marianne Funfrock <marianne.funfrock at rte-france.com>
 */
public class MappingKeyJsonDeserializer extends StdDeserializer<MappingKey> {

    public MappingKeyJsonDeserializer() {
        super(MappingKey.class);
    }

    @Override
    public MappingKey deserialize(JsonParser parser, DeserializationContext deserializationContext) {
        Objects.requireNonNull(parser);
        return MappingKey.parseJson(parser);
    }

}
