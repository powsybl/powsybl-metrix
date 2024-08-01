/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class EquipmentTimeSeriesTest {

    @Test
    void equipmentTimeSeriesTest() {

        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig(MappingTestNetwork.create());

        mappingConfig.addEquipmentTimeSeries("ts1", EquipmentVariable.P_0, "generator1");
        mappingConfig.addEquipmentTimeSeries("ts2", EquipmentVariable.P_0, "generator1");
        mappingConfig.addEquipmentTimeSeries("ts1", EquipmentVariable.P_0, "generator2");
        mappingConfig.addEquipmentTimeSeries("ts1", EquipmentVariable.TARGET_P, "generator1");
        mappingConfig.addEquipmentTimeSeries("ts1", EquipmentVariable.TARGET_P, "generator2");
        mappingConfig.addEquipmentTimeSeries("ts2", EquipmentVariable.TARGET_P, "generator2");

        // assertions
        MappingKey p0Generator1 = new MappingKey(EquipmentVariable.P_0, "generator1");
        MappingKey p0Generator2 = new MappingKey(EquipmentVariable.P_0, "generator2");
        MappingKey targetPGenerator1 = new MappingKey(EquipmentVariable.TARGET_P, "generator1");
        MappingKey targetPGenerator2 = new MappingKey(EquipmentVariable.TARGET_P, "generator2");
        assertEquals(2, mappingConfig.getTimeSeriesToEquipment().get("ts1").size());
        assertEquals(ImmutableSet.of(p0Generator2, targetPGenerator1), mappingConfig.getTimeSeriesToEquipment().get("ts1"));
        assertEquals(ImmutableSet.of(p0Generator1, targetPGenerator2), mappingConfig.getTimeSeriesToEquipment().get("ts2"));
    }

}
