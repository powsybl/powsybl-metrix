/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping

class ParametersData {

    Float toleranceThreshold
    Boolean withTimeSeriesStats

    void toleranceThreshold(Float toleranceThreshold) {
        this.toleranceThreshold = toleranceThreshold
    }

    void withTimeSeriesStats(Boolean withTimeSeriesStats) {
        this.withTimeSeriesStats = withTimeSeriesStats
    }

    protected static parametersData(Closure<Void> closure, MappingParameters parameters) {
        def cloned = closure.clone()
        ParametersData spec = new ParametersData()
        cloned.delegate = spec
        cloned()
        if (spec.toleranceThreshold) {
            parameters.setToleranceThreshold(spec.toleranceThreshold)
        }
        if (spec.withTimeSeriesStats) {
            parameters.setWithTimeSeriesStats(spec.withTimeSeriesStats)
        }
    }
}
