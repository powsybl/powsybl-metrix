/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesMetadata;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;

import java.util.Collections;
import java.util.Map;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class TsMetadata {

    private TsMetadata() {
    }

    static Map<String, String> tsMetadata(NodeCalc nodeCalc, ReadOnlyTimeSeriesStore store) {
        if (nodeCalc instanceof TimeSeriesNameNodeCalc timeSeriesNameNodeCalc) {
            var timeSeriesMetadata = store.getTimeSeriesMetadata(timeSeriesNameNodeCalc.getTimeSeriesName());
            return timeSeriesMetadata.map(TimeSeriesMetadata::getTags).orElse(Collections.emptyMap());
        }
        return Collections.emptyMap();
    }

    static Map<String, String> tsMetadata(String tsName, Map<String, Map<String, String>> tags) {
        return tags.getOrDefault(tsName, Collections.emptyMap());
    }
}
