/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

/**
 * Created by marifunf on 17/07/17.
 */
public enum MetrixComputationType {
    OPF(0),
    LF(1),
    OPF_WITHOUT_REDISPATCHING(2),
    UNKNOWN(3);

    private final int type;

    MetrixComputationType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
