/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.metrix.mapping.MappingKey;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.json.TimeSeriesJsonDeserializer;
import com.powsybl.timeseries.json.TimeSeriesJsonSerializer;

public class TimeSeriesMappingConfigJsonModule extends SimpleModule {

    public TimeSeriesMappingConfigJsonModule() {
        addSerializer(TimeSeriesMappingConfig.class, new TimeSeriesMappingConfigJsonSerializer());
        addSerializer(TimeSeries.class, new TimeSeriesJsonSerializer());
        addSerializer(MappingKey.class, new MappingKeyJsonSerializer());

        addDeserializer(TimeSeriesMappingConfig.class, new TimeSeriesMappingConfigJsonDeserializer());
        addDeserializer(TimeSeries.class, new TimeSeriesJsonDeserializer());
        addDeserializer(MappingKey.class, new MappingKeyJsonDeserializer());
    }
}
