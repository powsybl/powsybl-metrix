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
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;

import java.util.Objects;

public class TimeSeriesMappingConfigJsonSerializer extends StdSerializer<TimeSeriesMappingConfig> {

    public TimeSeriesMappingConfigJsonSerializer() {
        super(TimeSeriesMappingConfig.class);
    }

    @Override
    public void serialize(TimeSeriesMappingConfig config, JsonGenerator generator, SerializerProvider serializerProvider) {
        Objects.requireNonNull(generator);
        config.toJson(generator);
    }
}
