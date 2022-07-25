/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.mapping.timeseries;

public enum ScalingDownLimitViolation {
    BASE_CASE_MINP_BY_TARGETP,
    MAPPED_MINP_BY_TARGETP,
    MAXP_BY_TARGETP,
    MAXP_BY_ACTIVEPOWER,
    CS1TOCS2_BY_ACTIVEPOWER,
    MINP_BY_TARGETP,
    MINP_BY_ACTIVEPOWER,
    CS2TOCS1_BY_ACTIVEPOWER
}
