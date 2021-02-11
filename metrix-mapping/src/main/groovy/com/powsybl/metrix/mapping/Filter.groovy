/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Identifiable
import groovy.transform.CompileStatic

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier@rte-france.com>
 */
class Filter {

    @CompileStatic
    static Collection<Identifiable> evaluate(Binding binding, Iterable<FilteringContext> filteringContexts,
                                             String scriptVariable, Closure<Boolean> filter) {
        Iterable<FilteringContext> eval = filteringContexts.findAll({ FilteringContext filteringContext ->

            def savedVariable = binding.hasVariable(scriptVariable) ? binding.getVariable(scriptVariable) : null

            binding.setVariable(scriptVariable, filteringContext.identifiable)
            binding.setVariable("voltageLevel", filteringContext.voltageLevel)
            binding.setVariable("substation", filteringContext.substation)
            try {
                return filter != null ? filter.call() : true
            } finally {
                binding.setVariable(scriptVariable, savedVariable)
                binding.setVariable("voltageLevel", null)
                binding.setVariable("substation", null)
            }
        })
        return eval.collect { filteringContext -> ((FilteringContext) filteringContext).identifiable }
    }

}
