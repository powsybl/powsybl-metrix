/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration

import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Network
import com.powsybl.metrix.mapping.LogDslLoader

class HvdcData {

    List<String> onContingencies
    MetrixHvdcControlType controlType
    Boolean flowResults

    void onContingencies(String[] onContingencies) {
        this.onContingencies = onContingencies
    }

    void onContingencies(List<String> onContingencies) {
        this.onContingencies = onContingencies
    }

    void controlType(MetrixHvdcControlType controlType) {
        this.controlType = controlType
    }

    void flowResults(boolean b) {
        this.flowResults = b
    }

    protected static hvdcData(Closure closure, String id, Network network, MetrixDslData data, LogDslLoader logDslLoader) {
        Identifiable identifiable = network.getHvdcLine(id)
        if (identifiable == null) {
            logDslLoader.logWarn("hvdc id %s not found in the network", id)
            return
        }
        def cloned = closure.clone()
        HvdcData spec = new HvdcData()
        cloned.delegate = spec
        cloned()

        if (spec.controlType) {
            data.addHvdc(id, spec.controlType, spec.onContingencies)
            if (spec.controlType == MetrixHvdcControlType.OPTIMIZED) {
                data.addHvdcFlowResults(id)
            }
            logDslLoader.logDebug("Found hvdc for id %s", id)
        }
        if (spec.flowResults) {
            data.addHvdcFlowResults(id)
        }
    }
}
