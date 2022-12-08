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

class GeneratorsBindingData extends LoadsBindingData {

    MetrixGeneratorsBinding.ReferenceVariable referenceVariable

    void referenceVariable(MetrixGeneratorsBinding.ReferenceVariable referenceVariable) {
        this.referenceVariable = referenceVariable
    }

    protected static generatorsBindingData(Binding binding, Closure closure, String id, Network network, MetrixDslData data, LogDslLoader logDslLoader) {

        if (id.isEmpty()) {
            logDslLoader.logError("missing generators group name")
            return
        } else if (id.contains(';')) {
            logDslLoader.logError("semi-colons are forbidden in generators group name %s", id)
            return
        }

        GeneratorsBindingData spec = generatorsBindingData(closure)

        if (spec && spec.filter) {

            // evaluate filter
            def filteringContext = network.getGenerators().collect { g -> new FilteringContext(g) }
            Collection<Identifiable> filteredGenerators = Filter.evaluate(binding, filteringContext, "generator", spec.filter)

            List<String> generatorIds = filteredGenerators.collect { it -> it.id }

            if (generatorIds.size() > 1) {
                if (spec.referenceVariable) {
                    data.addGeneratorsBinding(id, generatorIds, spec.referenceVariable)
                } else {
                    data.addGeneratorsBinding(id, generatorIds)
                }
            } else {
                logDslLoader.logWarn("generators group %s ignored because it contains %d element", id, generatorIds.size())
            }
        } else {
            logDslLoader.logError("missing filter for generators group %s", id)
        }
    }

    protected static GeneratorsBindingData generatorsBindingData(Closure closure) {
        def cloned = closure.clone()
        GeneratorsBindingData spec = new GeneratorsBindingData()
        cloned.delegate = spec
        cloned()
        spec
    }
}
