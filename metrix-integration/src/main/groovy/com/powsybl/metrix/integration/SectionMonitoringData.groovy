/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration

import com.powsybl.iidm.network.HvdcLine
import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Line
import com.powsybl.iidm.network.Network
import com.powsybl.iidm.network.TwoWindingsTransformer
import com.powsybl.metrix.mapping.LogDslLoader

class SectionMonitoringData {

    Map<String, Float> branchList = new HashMap<>()
    float maxFlowN

    void branch(String branch, Float coef) {
        this.branchList.put(branch, coef)
    }

    void maxFlowN(float maxFlowN) {
        this.maxFlowN = maxFlowN
    }

    protected static sectionMonitoringData(Closure closure, String id, Network network, MetrixDslData data, LogDslLoader logDslLoader) {
        def cloned = closure.clone()
        SectionMonitoringData spec = new SectionMonitoringData()
        cloned.delegate = spec
        cloned()
        MetrixSection section = new MetrixSection(id)
        if (!spec.maxFlowN) {
            logDslLoader.logWarn("Section Monitoring '" + id + "' is missing flow limit")
            return
        } else {
            section.setMaxFlowN(spec.maxFlowN)
        }
        if (!spec.branchList) {
            logDslLoader.logWarn("Section Monitoring '" + id + "' without branches")
            return
        }
        for (Map.Entry<String, Float> branch : spec.branchList) {
            Identifiable identifiable = network.getIdentifiable(branch.getKey())
            if (identifiable == null) {
                logDslLoader.logWarn("sectionMonitoring '" + id + "' branch id '"+branch.getKey()+"' not found in the network")
                return
            }
            if (!(identifiable instanceof Line || identifiable instanceof TwoWindingsTransformer || identifiable instanceof HvdcLine)) {
                logDslLoader.logWarn("sectionMonitoring '" + id + "' type " + identifiable.getClass().name + " not supported")
                return
            }
        }
        section.setCoefFlowList(spec.branchList)
        data.addSection(section)
        logDslLoader.logDebug("Found sectionMonitoring %s", id)
    }
}
