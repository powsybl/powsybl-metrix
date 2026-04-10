/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public enum MappableEquipmentType {
    GENERATOR("generator"),
    BATTERY("battery"),
    HVDC_LINE("hvdcLine"),
    LOAD("load"),
    BOUNDARY_LINE("boundaryLine"),
    SWITCH("breaker"),
    TRANSFORMER(Constants.TWO_WINDINGS_TRANSFORMER),
    PHASE_TAP_CHANGER(Constants.TWO_WINDINGS_TRANSFORMER),
    RATIO_TAP_CHANGER(Constants.TWO_WINDINGS_TRANSFORMER),
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

    private static final class Constants {
        public static final String TWO_WINDINGS_TRANSFORMER = "twoWindingsTransformer";
    }
}
