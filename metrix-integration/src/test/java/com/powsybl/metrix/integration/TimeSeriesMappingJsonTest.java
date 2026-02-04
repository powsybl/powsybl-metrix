/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.metrix.mapping.references.MappingKey;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.json.TimeSeriesMappingConfigJsonModule;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesMappingJsonTest {

    @Test
    void test() throws JsonProcessingException {
        ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                .registerModule(new TimeSeriesMappingConfigJsonModule());
        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig();

        mappingConfig.setUnmappedVariableActivePowerLoads(Collections.singleton("a"));
        mappingConfig.setBreakerTimeSeries(Collections.singleton(new MappingKey(EquipmentVariable.TARGET_P, "a")));
        mappingConfig.setGeneratorTimeSeries(Collections.singleton(new MappingKey(MetrixVariable.THRESHOLD_N, "a")));

        String serialized = objectMapper.writeValueAsString(mappingConfig);
        TimeSeriesMappingConfig mappingConfig2 = objectMapper.readValue(serialized, TimeSeriesMappingConfig.class);
        assertThat(mappingConfig).isEqualTo(mappingConfig2);
    }
}
