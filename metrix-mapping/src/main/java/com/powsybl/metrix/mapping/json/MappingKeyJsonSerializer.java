/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.powsybl.metrix.mapping.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.metrix.mapping.MappingKey;

import java.util.Objects;

public class MappingKeyJsonSerializer extends StdSerializer<MappingKey> {

    public MappingKeyJsonSerializer() {
        super(MappingKey.class);
    }

    @Override
    public void serialize(MappingKey key, JsonGenerator generator, SerializerProvider serializerProvider) {
        Objects.requireNonNull(generator);
        key.toJson(generator);
    }
}
