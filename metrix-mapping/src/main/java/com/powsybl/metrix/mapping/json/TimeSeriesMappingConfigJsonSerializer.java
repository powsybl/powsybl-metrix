/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.json;

import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class TimeSeriesMappingConfigJsonSerializer extends StdSerializer<TimeSeriesMappingConfig> {

    public TimeSeriesMappingConfigJsonSerializer() {
        super(TimeSeriesMappingConfig.class);
    }

    @Override
    public void serialize(TimeSeriesMappingConfig config, JsonGenerator generator, SerializationContext serializationContext) {
        Objects.requireNonNull(generator);
        new TimeSeriesMappingConfigJson(config).toJson(generator);
    }
}
