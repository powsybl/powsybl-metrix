/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.integration;

public enum MetrixHvdcRegulationType {
    CONTROL_OFF(0), // defined on METRIX side but not used for the moment
    OPTIMIZED_SETPOINT(1),
    FIXED_SETPOINT(2),
    OPTIMIZED_AC_EMULATION(3),
    FIXED_AC_EMULATION(4);

    private final int type;

    MetrixHvdcRegulationType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
