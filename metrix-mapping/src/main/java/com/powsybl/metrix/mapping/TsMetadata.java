/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesMetadata;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;

import java.util.Collections;
import java.util.Map;

public final class TsMetadata {

    private TsMetadata() {
    }

    static Map<String, String> tsMetadata(NodeCalc nodeCalc, ReadOnlyTimeSeriesStore store) {
        if (nodeCalc instanceof TimeSeriesNameNodeCalc) {
            var timeSeriesMetadata = store.getTimeSeriesMetadata(((TimeSeriesNameNodeCalc) nodeCalc).getTimeSeriesName());
            return timeSeriesMetadata.map(TimeSeriesMetadata::getTags).orElse(Collections.emptyMap());
        }
        return Collections.emptyMap();
    }
}
