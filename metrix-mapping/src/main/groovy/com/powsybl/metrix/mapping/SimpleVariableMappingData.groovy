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

class SimpleVariableMappingData extends SimpleMappingData {

    EquipmentVariable variable

    void variable(EquipmentVariable variable) {
        this.variable = variable
    }

    @CompileStatic
    protected static void mapToSimpleVariableEquipments(Binding binding, TimeSeriesMappingConfigLoader configLoader,
                                                        Closure closure, Iterable<FilteringContext> filteringContexts, MappableEquipmentType equipmentType) {
        Closure cloned = (Closure) closure.clone()
        SimpleVariableMappingData spec = new SimpleVariableMappingData()
        cloned.delegate = spec
        cloned()

        configLoader.timeSeriesExists(spec.timeSeriesName)

        // check variable
        EquipmentVariable variable = EquipmentVariable.check(equipmentType, spec.variable)

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        // create at least one entry in the config even if no equipment match the filter (mandatory for ignore-empty-filter option)
        if (filteredEquipments.isEmpty()) {
            configLoader.addEquipmentMapping(equipmentType, spec.timeSeriesName, null, NumberDistributionKey.ONE, variable)
        }

        // for each filtered equipment, add it to the config
        for (Identifiable identifiable in filteredEquipments) {
            configLoader.addEquipmentMapping(equipmentType, spec.timeSeriesName, identifiable.id, NumberDistributionKey.ONE, variable)
        }
    }
}
