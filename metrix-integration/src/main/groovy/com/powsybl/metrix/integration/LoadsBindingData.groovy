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
import com.powsybl.metrix.mapping.Filter
import com.powsybl.metrix.mapping.FilteringContext
import com.powsybl.metrix.mapping.LogDslLoader

class LoadsBindingData {

    Closure<Boolean> filter

    void filter(Closure<Boolean> filter) {
        this.filter = filter
    }

    protected static loadsBindingData(Binding binding, Closure closure, String id, Network network, MetrixDslData data, LogDslLoader logDslLoader) {

        if (id.isEmpty()) {
            logDslLoader.logWarn("missing a name for loads group")
            return
        } else if (id.contains(';')) {
            logDslLoader.logError("semi-colons are forbidden in loads group name %s", id)
            return
        }

        LoadsBindingData spec = loadsBindingData(closure)

        if (spec && spec.filter) {

            // evaluate filter
            def filteringContext = network.getLoads().collect { l -> new FilteringContext(l) }
            Collection<Identifiable> filteredLoads = Filter.evaluate(binding, filteringContext, "load", spec.filter)

            List<String> loadIds = filteredLoads.collect { it -> it.id }
            if (loadIds.size() > 1) {
                data.addLoadsBinding(id, loadIds)
            } else {
                logDslLoader.logWarn("loads group %s ignored because it contains %d element", id, loadIds.size())
            }
        } else {
            logDslLoader.logError("missing filter for loads group %s", id)
        }
    }

    protected static LoadsBindingData loadsBindingData(Closure closure) {
        def cloned = closure.clone()
        LoadsBindingData spec = new LoadsBindingData()
        cloned.delegate = spec
        cloned()
        spec
    }
}
