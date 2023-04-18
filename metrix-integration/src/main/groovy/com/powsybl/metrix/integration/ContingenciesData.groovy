/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration

import com.powsybl.metrix.mapping.LogDslLoader

class ContingenciesData {

    List<String> specificContingencies

    void specificContingencies(String[] contingencies) {
        this.specificContingencies = contingencies
    }

    void specificContingencies(List<String> contingencies) {
        this.specificContingencies = contingencies
    }

    protected static contingenciesData(Closure closure, MetrixDslData data, LogDslLoader logDslLoader) {
        def cloned = closure.clone()
        ContingenciesData spec = new ContingenciesData()
        cloned.delegate = spec
        cloned()

        if (spec.specificContingencies != null) {
            data.setSpecificContingenciesList(spec.specificContingencies)
            logDslLoader.logDebug("Specific contingencies list : %s", data.getSpecificContingenciesList())
        }
    }
}
