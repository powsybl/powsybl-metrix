/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;

import java.util.Collections;
import java.util.Map;

interface DefaultGenericMetadata {

    default Map<String, String> stringMetadatas(ReadOnlyTimeSeriesStore store) {
        return Collections.emptyMap();
    }

    default Map<String, Double> doubleMetadatas(ReadOnlyTimeSeriesStore store) {
        return Collections.emptyMap();
    }

    default Map<String, Integer> intMetadatas(ReadOnlyTimeSeriesStore store) {
        return Collections.emptyMap();
    }

    default Map<String, Boolean> booleanMetadatas(ReadOnlyTimeSeriesStore store) {
        return Collections.emptyMap();
    }
}
