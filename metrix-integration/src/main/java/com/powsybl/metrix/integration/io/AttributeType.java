/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration.io;

enum AttributeType {
    INTEGER(1),
    FLOAT(2),
    DOUBLE(3),
    STRING(4),
    BOOLEAN(5);

    private final int value;

    AttributeType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
