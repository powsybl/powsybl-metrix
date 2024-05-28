/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.remedials;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Nicolas Rol {@literal <135979730+rolnico at users.noreply.github.com>}
 */
class RemedialTest {

    @Test
    void remedialTest() {
        Remedial remedial = new Remedial(1, "cty", List.of("constraint"), List.of("branchToOpen"), List.of("branchToClose"), "actions");
        assertEquals("cty", remedial.getContingency());
        assertEquals(List.of("constraint"), remedial.getConstraint());
        assertEquals(List.of("branchToOpen"), remedial.getBranchToOpen());
        assertEquals(List.of("branchToClose"), remedial.getBranchToClose());
        assertEquals("actions", remedial.getActions());
    }

    @Test
    void remedialNameFromActionsTest() {
        Remedial remedial = new Remedial(1, "", Collections.emptyList(), List.of("branchToOpen"), List.of("branchToClose"), "");
        assertEquals("branchToOpen;+branchToClose", remedial.getNameFromActions());
    }
}
