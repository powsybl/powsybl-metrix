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
    PST("pst"),
    SWITCH("breaker");

    private final String scriptVariable;

    MappableEquipmentType(String scriptVariable) {
        this.scriptVariable = Objects.requireNonNull(scriptVariable);
    }

    public String getScriptVariable() {
        return scriptVariable;
    }
}
