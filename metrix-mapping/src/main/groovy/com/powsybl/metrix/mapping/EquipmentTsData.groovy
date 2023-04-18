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

class EquipmentTsData extends FilteredData {

    Set<EquipmentVariable> variables

    void variables(EquipmentVariable[] variables) {
        this.variables = variables
    }

    void variables(Set<EquipmentVariable> variables) {
        this.variables = variables
    }

    @CompileStatic
    protected static void equipmentTimeSeries(Binding binding, TimeSeriesMappingConfigLoader configLoader,
                                              Closure closure, Iterable<FilteringContext> filteringContexts,
                                              MappableEquipmentType equipmentType,
                                              LogDslLoader logDslLoader) {
        Closure cloned = (Closure) closure.clone()
        EquipmentTsData spec = new EquipmentTsData()
        cloned.delegate = spec
        cloned()

        // check variable
        Set<EquipmentVariable> variables = EquipmentVariable.check(equipmentType, spec.variables)

        // evaluate equipment filters for each variable
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        if (filteredEquipments.isEmpty()) {
            logDslLoader.logWarn("provideTs - Empty filtered list for equipment type " + equipmentType.toString() + " and variables " + variables.toString())
        }

        // for each filtered equipment, add it to the equipment time series config
        filteredEquipments.forEach({ Identifiable identifiable ->
            configLoader.addEquipmentTimeSeries(equipmentType, identifiable.id, variables)
        })
    }
}
