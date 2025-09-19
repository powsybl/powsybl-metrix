/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Load
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigLoader
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException
import groovy.transform.CompileStatic

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class LoadGroupTsData extends EquipmentGroupTsData {

    @CompileStatic
    protected static void loadGroupTimeSeries(Binding binding, TimeSeriesMappingConfigLoader configLoader,
                                              Closure closure, Iterable<FilteringContext> filteringContexts,
                                              MappableEquipmentType equipmentType,
                                              LogDslLoader logDslLoader) {
        Closure cloned = (Closure) closure.clone()
        LoadGroupTsData spec = new LoadGroupTsData()
        cloned.delegate = spec
        cloned()

        if (spec.group == null) {
            throw new TimeSeriesMappingException("provideGroupTsLoads must define group")
        }

        // evaluate equipment filters for each variable
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        // for each filtered equipment, add it to the equipment time series config
        filteredEquipments.forEach({ Identifiable identifiable ->
            configLoader.addGroupLoadTimeSeries((Load) identifiable, spec.group, spec.name)
        })
    }
}
