/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.remedials;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public record Remedial(int lineFile, String contingency, List<String> constraint, List<String> branchToOpen,
                       List<String> branchToClose, String actions) {

    public String getNameFromActions() {
        List<String> name = new ArrayList<>(); //display branchToOpen ordered then branchToClose ordered
        name.addAll(branchToOpen.stream().sorted().toList());
        name.addAll(branchToClose.stream().map(action -> "+" + action).sorted().toList());
        return String.join(";", name);
    }
}
