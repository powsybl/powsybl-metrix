/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration

import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Network
import com.powsybl.metrix.mapping.log.LogDslLoader
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigLoader

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class GeneratorData {

    List<String> onContingencies
    Object adequacyUpCosts
    Object adequacyDownCosts
    Object redispatchingUpCosts
    Object redispatchingDownCosts
    Object redispatchingUpDoctrineCosts
    Object redispatchingDownDoctrineCosts

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

    void redispatchingUpDoctrineCosts(Object timeSeriesNames) {
        this.redispatchingUpDoctrineCosts = timeSeriesNames
    }

    void redispatchingDownDoctrineCosts(Object timeSeriesNames) {
        this.redispatchingDownDoctrineCosts = timeSeriesNames
    }

    void onContingencies(String[] contingencies) {
        this.onContingencies = contingencies
    }

    void onContingencies(List<String> onContingencies) {
        this.onContingencies = onContingencies
    }

    /**
     * Set the generator available for adequacy when up and down adequacy costs are defined
     */
    private static generatorForAdequacy(String id, GeneratorData spec, TimeSeriesMappingConfigLoader configLoader, MetrixDslData data, LogDslLoader logDslLoader) {
        if (spec.adequacyUpCosts != null || spec.adequacyDownCosts != null) {
            if (spec.adequacyUpCosts != null && spec.adequacyDownCosts != null) {
                configLoader.addEquipmentTimeSeries(spec.adequacyDownCosts, MetrixVariable.OFF_GRID_COST_DOWN, id)
                configLoader.addEquipmentTimeSeries(spec.adequacyUpCosts, MetrixVariable.OFF_GRID_COST_UP, id)
                data.addGeneratorForAdequacy(id)
            } else if (spec.adequacyUpCosts == null) {
                logDslLoader.logDebug("generator %s is missing adequacy up-cost time-series to be properly configured", id)
            } else {
                logDslLoader.logDebug("generator %s is missing adequacy down-cost time-series to be properly configured", id)
            }
        }
    }

    /**
     * Set the generator available for redispatching when up and down redispatching costs are defined
     */
    private static boolean generatorForRedispatching(String id, GeneratorData spec, TimeSeriesMappingConfigLoader configLoader, MetrixDslData data, LogDslLoader logDslLoader) {
        if (spec.redispatchingUpCosts != null || spec.redispatchingDownCosts != null) {
            if (spec.redispatchingUpCosts != null && spec.redispatchingDownCosts != null) {
                configLoader.addEquipmentTimeSeries(spec.redispatchingDownCosts, MetrixVariable.ON_GRID_COST_DOWN, id)
                configLoader.addEquipmentTimeSeries(spec.redispatchingUpCosts, MetrixVariable.ON_GRID_COST_UP, id)
                data.addGeneratorForRedispatching(id, spec.onContingencies)
                return true
            } else if (spec.redispatchingUpCosts == null) {
                logDslLoader.logDebug("generator %s is missing redispatching up-cost time-series to be properly configured", id)
            } else {
                logDslLoader.logDebug("generator %s is missing redispatching down-cost time-series to be properly configured", id)
            }
        }
        return false
    }

    /**
     * Define up and down redispatching doctrine costs when generator is available for redispatching
     */
    private static generatorRedispatchingDoctrineCosts(String id, GeneratorData spec, TimeSeriesMappingConfigLoader configLoader, LogDslLoader logDslLoader) {
        if (spec.redispatchingUpDoctrineCosts == null && spec.redispatchingDownDoctrineCosts == null) {
            logDslLoader.logWarn("generator %s is missing redispatching doctrine cost to be properly configured", id)
            return
        }
        if (spec.redispatchingUpDoctrineCosts == null) {
            logDslLoader.logWarn("generator %s is missing redispatching doctrine up cost to be properly configured", id)
            return
        }
        if (spec.redispatchingDownDoctrineCosts == null) {
            logDslLoader.logWarn("generator %s is missing redispatching doctrine down cost to be properly configured", id)
            return
        }
        configLoader.addEquipmentTimeSeries(spec.redispatchingDownDoctrineCosts, MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN, id)
        configLoader.addEquipmentTimeSeries(spec.redispatchingUpDoctrineCosts, MetrixVariable.ON_GRID_DOCTRINE_COST_UP, id)
    }

    protected static generatorData(Closure closure, String id, Network network, TimeSeriesMappingConfigLoader configLoader, MetrixDslData data, LogDslLoader logDslLoader) {
        Identifiable identifiable = network.getGenerator(id)
        if (identifiable == null) {
            logDslLoader.logWarn("generator id %s not found in the network", id)
            return
        }

        GeneratorData spec = generatorData(closure)

        if (spec) {
            generatorForAdequacy(id, spec, configLoader, data, logDslLoader)
            boolean isGeneratorAvailableForRedispatching = generatorForRedispatching(id, spec, configLoader, data, logDslLoader)
            if (isGeneratorAvailableForRedispatching) {
                generatorRedispatchingDoctrineCosts(id, spec, configLoader, logDslLoader)
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
