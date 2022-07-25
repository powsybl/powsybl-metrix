/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.mapping.timeseries;

public enum ScalingDownPowerChange {
    BASE_CASE_MINP,
    BASE_CASE_MAXP,
    ZERO,
    MAPPED_MINP,
    MAPPED_MAXP,
    MAPPED_MINP_DISABLED,
    MAPPED_MAXP_DISABLED,
    ZERO_DISABLED,
    BASE_CASE_CS1TOCS2,
    BASE_CASE_CS2TOCS1,
    BASE_CASE_MINUS_MAXP
}
