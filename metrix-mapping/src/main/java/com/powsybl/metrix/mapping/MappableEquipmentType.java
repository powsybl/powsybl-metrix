/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public enum MappableEquipmentType {
    GENERATOR("generator"),
    HVDC_LINE("hvdcLine"),
    LOAD("load"),
    BOUNDARY_LINE("boundaryLine"),
    SWITCH("breaker"),
    TRANSFORMER("transformer"),
    PHASE_TAP_CHANGER("transformer"),
    RATIO_TAP_CHANGER("transformer"),
    LCC_CONVERTER_STATION("lccConverterStation"),
    VSC_CONVERTER_STATION("vscConverterStation"),
    @Deprecated PST("pst"); // Kept for compatibility

    private final String scriptVariable;

    MappableEquipmentType(String scriptVariable) {
        this.scriptVariable = Objects.requireNonNull(scriptVariable);
    }

    public String getScriptVariable() {
        return scriptVariable;
    }
}
