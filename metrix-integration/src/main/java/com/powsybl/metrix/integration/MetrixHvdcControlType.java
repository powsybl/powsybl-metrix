/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public enum MetrixHvdcControlType {
    OPTIMIZED(1),
    FIXED(2); // default value

    private final int type;

    MetrixHvdcControlType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
