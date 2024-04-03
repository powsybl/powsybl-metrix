/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.mapping;

import java.util.Objects;

public enum MappableEquipmentType {
    GENERATOR("generator"),
    HVDC_LINE("hvdcLine"),
    LOAD("load"),
    BOUNDARY_LINE("boundaryLine"),
    SWITCH("breaker"),
    TRANSFORMER("twoWindingsTransformer"),
    PHASE_TAP_CHANGER("twoWindingsTransformer"),
    RATIO_TAP_CHANGER("twoWindingsTransformer"),
    LCC_CONVERTER_STATION("lccConverterStation"),
    VSC_CONVERTER_STATION("vscConverterStation"),
    LINE("line");

    private final String scriptVariable;

    MappableEquipmentType(String scriptVariable) {
        this.scriptVariable = Objects.requireNonNull(scriptVariable);
    }

    public String getScriptVariable() {
        return scriptVariable;
    }
}
