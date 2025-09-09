/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Injection
import com.powsybl.iidm.network.Substation
import com.powsybl.iidm.network.Switch
import com.powsybl.iidm.network.VoltageLevel

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class FilteringContext {

    Identifiable identifiable
    VoltageLevel voltageLevel
    Substation substation

    FilteringContext(Identifiable identifiable) {
        this.identifiable = identifiable
        if (identifiable instanceof Injection) {
            voltageLevel = ((Injection) identifiable).terminal.voltageLevel
            substation = voltageLevel.substation.get()
        } else if (identifiable instanceof Switch) {
            voltageLevel = ((Switch) identifiable).voltageLevel
            substation = voltageLevel.substation.get()
        }
    }
}
