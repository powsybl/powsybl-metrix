/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.timeseries.ast.NodeCalc;

import java.util.Map;

/**
 * @author Nicolas Lhuillier <nicolas.lhuillier@rte-france.com>
 */
public class MetrixRunResult {

    private Map<String, NodeCalc> postProcessingTimeSeries;

    public void setPostProcessingTimeSeries(Map<String, NodeCalc> postProcessingTimeSeries) {
        this.postProcessingTimeSeries = postProcessingTimeSeries;
    }

    public Map<String, NodeCalc> getPostProcessingTimeSeries() {
        return postProcessingTimeSeries;
    }
}
