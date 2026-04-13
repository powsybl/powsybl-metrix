/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration

import com.powsybl.iidm.network.Network
import com.powsybl.metrix.mapping.log.LogDslLoader
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigLoader

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class LossesData {

    Object costs

    void costs(Object timeSeriesNames) {
        this.costs = timeSeriesNames
    }

    private static lossesDoctrineCosts(String id, LossesData spec, TimeSeriesMappingConfigLoader configLoader, LogDslLoader logDslLoader) {
        if (spec.costs != null) {
            configLoader.addEquipmentTimeSeries(spec.costs, MetrixVariable.LOSSES_DOCTRINE_COST, id)
        }
    }

    protected static lossesData(Closure closure, Network network, TimeSeriesMappingConfigLoader configLoader, LogDslLoader logDslLoader) {
        LossesData spec = lossesData(closure)
        if (spec) {
            lossesDoctrineCosts(network.getId(), spec, configLoader, logDslLoader)
        }
    }

    protected static LossesData lossesData(Closure closure) {
        def cloned = closure.clone()
        LossesData spec = new LossesData()
        cloned.delegate = spec
        cloned()
        spec
    }
}
