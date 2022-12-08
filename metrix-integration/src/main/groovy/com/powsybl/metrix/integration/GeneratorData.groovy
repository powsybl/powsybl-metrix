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
import com.powsybl.metrix.mapping.LogDslLoader
import com.powsybl.metrix.mapping.TimeSeriesMappingConfigLoader

class GeneratorData {

    List<String> onContingencies
    Object adequacyUpCosts
    Object adequacyDownCosts
    Object redispatchingUpCosts
    Object redispatchingDownCosts

    void adequacyUpCosts(Object timeSeriesNames) {
        this.adequacyUpCosts = timeSeriesNames
    }

    void adequacyDownCosts(Object timeSeriesNames) {
        this.adequacyDownCosts = timeSeriesNames
    }

    void redispatchingUpCosts(Object timeSeriesNames) {
        this.redispatchingUpCosts = timeSeriesNames
    }

    void redispatchingDownCosts(Object timeSeriesNames) {
        this.redispatchingDownCosts = timeSeriesNames
    }

    void onContingencies(String[] contingencies) {
        this.onContingencies = contingencies
    }

    void onContingencies(List<String> onContingencies) {
        this.onContingencies = onContingencies
    }

    protected static generatorData(Closure closure, String id, Network network, TimeSeriesMappingConfigLoader configLoader, MetrixDslData data, LogDslLoader logDslLoader) {
        Identifiable identifiable = network.getGenerator(id)
        if (identifiable == null) {
            logDslLoader.logWarn("generator id %s not found in the network", id)
            return
        }

        GeneratorData spec = generatorData(closure)

        if (spec) {
            if (spec.adequacyUpCosts != null || spec.adequacyDownCosts != null) {
                if (spec.adequacyUpCosts != null && spec.adequacyDownCosts != null) {
                    configLoader.addEquipmentTimeSeries(spec.adequacyDownCosts, MetrixVariable.offGridCostDown, id)
                    configLoader.addEquipmentTimeSeries(spec.adequacyUpCosts, MetrixVariable.offGridCostUp, id)
                    data.addGeneratorForAdequacy(id)
                } else if (spec.adequacyUpCosts == null) {
                    logDslLoader.logDebug("generator %s is missing adequacy up-cost time-series to be properly configured", id)
                } else {
                    logDslLoader.logDebug("generator %s is missing adequacy down-cost time-series to be properly configured", id)
                }
            }
            if (spec.redispatchingUpCosts != null || spec.redispatchingDownCosts != null) {
                if (spec.redispatchingUpCosts != null && spec.redispatchingDownCosts != null) {
                    configLoader.addEquipmentTimeSeries(spec.redispatchingDownCosts, MetrixVariable.onGridCostDown, id)
                    configLoader.addEquipmentTimeSeries(spec.redispatchingUpCosts, MetrixVariable.onGridCostUp, id)
                    data.addGeneratorForRedispatching(id, spec.onContingencies)
                } else if (spec.redispatchingUpCosts == null) {
                    logDslLoader.logDebug("generator %s is missing redispatching up-cost time-series to be properly configured", id)
                } else {
                    logDslLoader.logDebug("generator %s is missing redispatching down-cost time-series to be properly configured", id)
                }
            }
        }
    }

    protected static GeneratorData generatorData(Closure closure) {
        def cloned = closure.clone()
        GeneratorData spec = new GeneratorData()
        cloned.delegate = spec
        cloned()
        spec
    }
}
