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
import com.powsybl.metrix.integration.exceptions.MetrixException
import com.powsybl.metrix.mapping.LogDslLoader

class PhaseShifterData {

    List<String> onContingencies
    MetrixPtcControlType controlType
    Integer preventiveUpperTapRange
    Integer preventiveLowerTapRange
    Boolean angleTapResults

    void onContingencies(String[] onContingencies) {
        this.onContingencies = onContingencies
    }

    void onContingencies(List<String> onContingencies) {
        this.onContingencies = onContingencies
    }

    void controlType(MetrixPtcControlType controlType) {
        this.controlType = controlType
    }

    void preventiveUpperTapRange(Integer preventiveUpperTapRange) {
        this.preventiveUpperTapRange = preventiveUpperTapRange
    }

    void preventiveLowerTapRange(Integer preventiveLowerTapRange) {
        this.preventiveLowerTapRange = preventiveLowerTapRange
    }

    void angleTapResults(boolean b) {
        this.angleTapResults = b
    }

    protected static phaseShifterData(Closure closure, String id, Network network, MetrixDslData data, LogDslLoader logDslLoader) {
        Identifiable twt = network.getTwoWindingsTransformer(id)
        if (twt == null) {
            logDslLoader.logWarn("transformer id %s not found in the network", id)
            return
        }
        if (!twt.hasPhaseTapChanger()) {
            throw new MetrixException("transformer id '" + id + "' without phase shifter")
        }
        def cloned = closure.clone()
        PhaseShifterData spec = new PhaseShifterData()
        cloned.delegate = spec
        cloned()

        if (spec.controlType) {
            data.addPtc(id, spec.controlType, spec.onContingencies)
            if (spec.controlType == MetrixPtcControlType.OPTIMIZED_ANGLE_CONTROL) {
                data.addPstAngleTapResults(id)
            }
            logDslLoader.logDebug("Found phaseTapChanger for id %s", id)
        }
        if (spec.preventiveLowerTapRange != null){
            data.addLowerTapChange(id, spec.preventiveLowerTapRange)
        }
        if (spec.preventiveUpperTapRange != null){
            data.addUpperTapChange(id, spec.preventiveUpperTapRange)
        }
        if (spec.angleTapResults) {
            data.addPstAngleTapResults(id)
        }
    }
}
