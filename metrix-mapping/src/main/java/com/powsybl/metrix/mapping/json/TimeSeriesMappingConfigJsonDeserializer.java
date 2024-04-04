/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfigJson;

import java.util.Objects;

public class TimeSeriesMappingConfigJsonDeserializer extends StdDeserializer<TimeSeriesMappingConfig> {

    public TimeSeriesMappingConfigJsonDeserializer() {
        super(TimeSeriesMappingConfig.class);
    }

    @Override
    public TimeSeriesMappingConfig deserialize(JsonParser parser, DeserializationContext deserializationContext) {
        Objects.requireNonNull(parser);
        return TimeSeriesMappingConfigJson.parseJsonWithExtendedThreadStackSize(parser);
    }

}
