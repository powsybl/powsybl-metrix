/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.metrix.commons.ComputationRange;
import com.powsybl.metrix.integration.configuration.MetrixRunParameters;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetrixRunParametersTest {

    @Test
    void parametersTest() {
        MetrixRunParameters parameters = new MetrixRunParameters(new ComputationRange(Set.of(1), 1, 2), 3, false, false, false, false, false);
        assertEquals(1, parameters.getFirstVariant());
        assertEquals(2, parameters.getVariantCount());
        assertEquals(3, parameters.getChunkSize());
    }

    @Test
    void ignoreParametersTest() {
        MetrixRunParameters ignoreLimitsParameters = new MetrixRunParameters(new ComputationRange(Set.of(1), 1, 2), 3, true, false, false, false, false);
        assertTrue(ignoreLimitsParameters.isIgnoreLimits());
        assertFalse(ignoreLimitsParameters.isIgnoreEmptyFilter());

        MetrixRunParameters ignoreEmptyFilter = new MetrixRunParameters(new ComputationRange(Set.of(1), 1, 2), 3, false, true, false, false, false);
        assertFalse(ignoreEmptyFilter.isIgnoreLimits());
        assertTrue(ignoreEmptyFilter.isIgnoreEmptyFilter());
    }

    @Test
    void writeMetrixParametersTest() {
        MetrixRunParameters writePtdfParameters = new MetrixRunParameters(new ComputationRange(Set.of(1), 1, 2), 3, false, false, false, true, false);
        assertTrue(writePtdfParameters.writePtdfMatrix());
        assertFalse(writePtdfParameters.writeLodfMatrix());

        MetrixRunParameters writeLodfParameters = new MetrixRunParameters(new ComputationRange(Set.of(1), 1, 2), 3, false, false, false, false, true);
        assertFalse(writeLodfParameters.writePtdfMatrix());
        assertTrue(writeLodfParameters.writeLodfMatrix());
    }
}
