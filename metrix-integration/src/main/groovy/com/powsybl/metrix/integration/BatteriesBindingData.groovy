/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration

import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Network
import com.powsybl.metrix.mapping.Filter
import com.powsybl.metrix.mapping.FilteringContext
import com.powsybl.metrix.mapping.LogDslLoader

class BatteriesBindingData extends LoadsBindingData {

    MetrixBatteriesBinding.ReferenceVariable referenceVariable

    void referenceVariable(MetrixBatteriesBinding.ReferenceVariable referenceVariable) {
        this.referenceVariable = referenceVariable
    }

    protected static batteriesBindingData(Binding binding, Closure closure, String id, Network network, MetrixDslData data, LogDslLoader logDslLoader) {

        if (id.isEmpty()) {
            logDslLoader.logError("missing batteries group name")
            return
        }
        if (id.contains(';')) {
            logDslLoader.logError("semi-colons are forbidden in batteries group name %s", id)
            return
        }

        BatteriesBindingData spec = batteriesBindingData(closure)

        if (!spec || !spec.filter) {
            logDslLoader.logError("missing filter for batteries group %s", id)
            return
        }

        // evaluate filter
        def filteringContext = network.getBatteries().collect { battery -> new FilteringContext(battery) }
        Collection<Identifiable> filteredBatteries = Filter.evaluate(binding, filteringContext, "battery", spec.filter)
        List<String> batteryIds = filteredBatteries.collect { it -> it.id }
        if (batteryIds.size() <= 1) {
            logDslLoader.logWarn("batteries group %s ignored because it contains %d element", id, batteryIds.size())
            return
        }

        if (spec.referenceVariable) {
            data.addBatteriesBinding(id, batteryIds, spec.referenceVariable)
        } else {
            data.addBatteriesBinding(id, batteryIds)
        }
    }

    protected static BatteriesBindingData batteriesBindingData(Closure closure) {
        def cloned = closure.clone()
        BatteriesBindingData spec = new BatteriesBindingData()
        cloned.delegate = spec
        cloned()
        spec
    }
}
