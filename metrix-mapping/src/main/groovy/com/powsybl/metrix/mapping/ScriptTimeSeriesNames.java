/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import java.util.Set;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class ScriptTimeSeriesNames {

    private final Set<String> inputNames;
    private final Set<String> calculatedNames;

    public Set<String> getInputNames() {
        return inputNames;
    }

    public Set<String> getCalculatedNames() {
        return calculatedNames;
    }

    public ScriptTimeSeriesNames(Set<String> inputNames, Set<String> calculatedNames) {
        this.calculatedNames = calculatedNames;
        this.inputNames = inputNames;
    }
}
