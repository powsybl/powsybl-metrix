/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.integration.io;

import com.powsybl.timeseries.TimeSeries;

import java.util.List;

public interface ResultListener {

    default void onVersionResultBegin(int version) {
        //default empty implementation
    }

    void onChunkResult(int version, int chunk, List<TimeSeries> timeSeriesList);

    default void onVersionResultEnd(int version) {
        //default empty implementation
    }

    void onEnd();

    default void onVersionBefore(int version) {
        // default empty implementation
    };
}
