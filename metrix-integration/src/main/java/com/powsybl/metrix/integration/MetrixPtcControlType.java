/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.integration;

public enum MetrixPtcControlType {
    CONTROL_OFF(0),
    OPTIMIZED_ANGLE_CONTROL(1),
    FIXED_ANGLE_CONTROL(2), // default value
    OPTIMIZED_POWER_CONTROL(3),
    FIXED_POWER_CONTROL(4);

    private final int type;

    MetrixPtcControlType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
