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

class LoadData {

    List<String> onContingencies
    Integer preventiveSheddingPercentage
    Float preventiveSheddingCost
    Integer curativeSheddingPercentage
    Object curativeSheddingCost

    void preventiveSheddingPercentage(int percent) {
        this.preventiveSheddingPercentage = percent
    }

    void preventiveSheddingCost(float loadSheddingCost) {
        this.preventiveSheddingCost = loadSheddingCost
    }

    void curativeSheddingPercentage(int percent) {
        this.curativeSheddingPercentage = percent
    }

    void curativeSheddingCost(Object timeSeriesName) {
        this.curativeSheddingCost = timeSeriesName
    }

    void onContingencies(String[] onContingencies) {
        this.onContingencies = onContingencies
    }

    void onContingencies(List<String> onContingencies) {
        this.onContingencies = onContingencies
    }

    protected static loadData(Closure closure, String id, Network network, TimeSeriesMappingConfigLoader configLoader, MetrixDslData data, LogDslLoader logDslLoader) {
        Identifiable identifiable = network.getLoad(id)
        if (identifiable == null) {
            logDslLoader.logWarn("load id %s not found in the network", id)
            return
        }

        LoadData loadSpec = loadData(closure)

        if (loadSpec.preventiveSheddingPercentage != null) {
            if (loadSpec.preventiveSheddingPercentage > 100 || loadSpec.preventiveSheddingPercentage < 0) {
                logDslLoader.logWarn("preventive shedding percentage for load %s is not valid", id)
            } else {
                data.addPreventiveLoad(id, loadSpec.preventiveSheddingPercentage)
                if (loadSpec.preventiveSheddingCost != null) {
                    data.addPreventiveLoadCost(id, loadSpec.preventiveSheddingCost)
                }
            }
        }

        if (loadSpec.curativeSheddingPercentage || loadSpec.curativeSheddingCost != null || loadSpec.onContingencies) {
            if (loadSpec.curativeSheddingPercentage && loadSpec.curativeSheddingCost != null && loadSpec.onContingencies) {
                if (loadSpec.curativeSheddingPercentage > 100 || loadSpec.curativeSheddingPercentage < 0) {
                    logDslLoader.logWarn("curative shedding percentage for load %s is not valid", id)
                } else {
                    configLoader.addEquipmentTimeSeries(loadSpec.curativeSheddingCost, MetrixVariable.curativeCostDown, id)
                    data.addCurativeLoad(id, loadSpec.curativeSheddingPercentage, loadSpec.onContingencies)
                }
            } else {
                logDslLoader.logWarn("configuration error for load %s : curative costs, percentage and contingencies list must be set altogether", id)
            }
        }
    }

    protected static LoadData loadData(Closure closure) {
        def cloned = closure.clone()
        LoadData spec = new LoadData()
        cloned.delegate = spec
        cloned()
        spec
    }
}
