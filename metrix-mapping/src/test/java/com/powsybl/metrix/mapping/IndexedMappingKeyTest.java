package com.powsybl.metrix.mapping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
class IndexedMappingKeyTest {

    @Test
    void testIndexedMappingKey() {
        MappingKey mappingKey = new MappingKey(EquipmentVariable.P0, "generator1");
        IndexedMappingKey key = new IndexedMappingKey(mappingKey, 1);

        // Equals
        assertEquals(new IndexedMappingKey(new MappingKey(EquipmentVariable.P0, "generator1"), 1), key);
        assertNotEquals(new IndexedMappingKey(new MappingKey(EquipmentVariable.P0, "generator1"), 2), key);
        assertNotEquals(new IndexedMappingKey(new MappingKey(EquipmentVariable.Q0, "generator1"), 2), key);

        // String
        assertEquals("IndexedMappingKey(key=MappingKey(mappingVariable=p0, id=generator1), num=1)", key.toString());
    }
}
