/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Identifiable
import groovy.transform.CompileStatic

class FilteredData {

    Closure<Boolean> filter

    void filter(Closure<Boolean> filter) {
        this.filter = filter
    }

    @CompileStatic
    protected static void unmappedEquipments(Binding binding, TimeSeriesMappingConfigLoader configLoader,
                                             Closure closure, Iterable<FilteringContext> filteringContexts,
                                             MappableEquipmentType equipmentType) {
        Closure cloned = (Closure) closure.clone()
        FilteredData spec = new FilteredData()
        cloned.delegate = spec
        cloned()

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        // for each filtered equipment, add it to the unmapped config
        filteredEquipments.forEach({ Identifiable identifiable ->
            configLoader.addUnmappedEquipment(equipmentType, identifiable.id)
        })
    }
}
