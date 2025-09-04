/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.timeseries.RegularTimeSeriesIndex;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public class TimeSeriesMetadataIndex {
    @JsonProperty("startTime")
    public final long startTime;
    @JsonProperty("endTime")
    public final long endTime;
    @JsonProperty("spacing")
    public final long spacing;
    @JsonProperty("pointCount")
    public final int pointCount;

    @JsonCreator
    public TimeSeriesMetadataIndex(RegularTimeSeriesIndex regularTimeSeriesIndex) {
        this.startTime = regularTimeSeriesIndex.getStartTime();
        this.endTime = regularTimeSeriesIndex.getEndTime();
        this.spacing = regularTimeSeriesIndex.getSpacing();
        this.pointCount = regularTimeSeriesIndex.getPointCount();
    }
}
