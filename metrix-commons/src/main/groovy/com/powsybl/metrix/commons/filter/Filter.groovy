package com.powsybl.metrix.commons.filter

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
