package com.powsybl.metrix.mapping;

import com.powsybl.metrix.mapping.references.IndexedName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
class IndexedNameTest {

    @Test
    void testIndexedName() {
        IndexedName indexedName = new IndexedName("generator1", 1);

        // Equals
        assertEquals(new IndexedName("generator1", 1), indexedName);
        assertNotEquals(new IndexedName("generator1", 2), indexedName);
        assertNotEquals(new IndexedName("generator2", 1), indexedName);

        // String
        assertEquals("IndexedName(name=generator1, num=1)", indexedName.toString());
    }
}
