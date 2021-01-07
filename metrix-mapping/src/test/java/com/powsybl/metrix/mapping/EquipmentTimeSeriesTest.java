package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class EquipmentTimeSeriesTest {

    @Test
    public void equipmentTimeSeriesTest() {

        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig(MappingTestNetwork.create());

        mappingConfig.addEquipmentTimeSeries("ts1", EquipmentVariable.p0, "generator1");
        mappingConfig.addEquipmentTimeSeries("ts2", EquipmentVariable.p0, "generator1");
        mappingConfig.addEquipmentTimeSeries("ts1", EquipmentVariable.p0, "generator2");
        mappingConfig.addEquipmentTimeSeries("ts1", EquipmentVariable.targetP, "generator1");
        mappingConfig.addEquipmentTimeSeries("ts1", EquipmentVariable.targetP, "generator2");
        mappingConfig.addEquipmentTimeSeries("ts2", EquipmentVariable.targetP, "generator2");

        // assertions
        MappingKey p0Generator1 = new MappingKey(EquipmentVariable.p0, "generator1");
        MappingKey p0Generator2 = new MappingKey(EquipmentVariable.p0, "generator2");
        MappingKey targetPGenerator1 = new MappingKey(EquipmentVariable.targetP, "generator1");
        MappingKey targetPGenerator2 = new MappingKey(EquipmentVariable.targetP, "generator2");
        assertEquals(2, mappingConfig.getTimeSeriesToEquipment().get("ts1").size());
        assertEquals(ImmutableSet.of(p0Generator2, targetPGenerator1), mappingConfig.getTimeSeriesToEquipment().get("ts1"));
        assertEquals(ImmutableSet.of(p0Generator1, targetPGenerator2), mappingConfig.getTimeSeriesToEquipment().get("ts2"));
    }

}
