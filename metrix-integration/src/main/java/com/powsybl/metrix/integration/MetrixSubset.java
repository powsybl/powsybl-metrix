/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.powsybl.metrix.integration;

import com.powsybl.commons.util.IntCounter;

/**
 * Created by marifunf on 12/05/17.
 */

enum MetrixSubset implements IntCounter {
    REGION,
    GROUPE,
    NOEUD,
    QUAD,
    DEPHA,
    HVDC,
    LOAD;

    @Override
    public int getInitialValue() {
        return 1;
    }
}
