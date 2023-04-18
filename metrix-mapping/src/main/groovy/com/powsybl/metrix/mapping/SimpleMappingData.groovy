/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Switch
import com.powsybl.iidm.network.TopologyKind
import groovy.transform.CompileStatic

class SimpleMappingData extends FilteredData {

    String timeSeriesName

    void timeSeriesName(String timeSeriesName) {
        assert timeSeriesName != null
        this.timeSeriesName = timeSeriesName
    }

    @CompileStatic
    protected static void mapToBreakers(Binding binding, TimeSeriesMappingConfigLoader configLoader,
                                        Closure closure, Iterable<FilteringContext> filteringContexts) {
        Closure cloned = (Closure) closure.clone()
        SimpleMappingData spec = new SimpleMappingData()
        cloned.delegate = spec
        cloned()

        configLoader.timeSeriesExists(spec.timeSeriesName)

        def breakerType = MappableEquipmentType.SWITCH

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, breakerType.scriptVariable, spec.filter)

        // for each filtered equipment, compute the distribution key and add it to the config
        if (!filteredEquipments.isEmpty()) {

            if (((Switch) filteredEquipments[0]).voltageLevel.topologyKind == TopologyKind.BUS_BREAKER) {
                throw new TimeSeriesMappingException("Bus breaker topology not supported for switch mapping")
            }

            filteredEquipments.forEach({ Identifiable identifiable ->
                configLoader.addEquipmentMapping(breakerType, spec.timeSeriesName, identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.open)
            })
        }
    }
}
