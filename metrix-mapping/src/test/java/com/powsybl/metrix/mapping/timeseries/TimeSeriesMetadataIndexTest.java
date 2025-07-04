/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.metrix.mapping.exception.TimeSeriesMetadataIndex;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
class TimeSeriesMetadataIndexTest {
    @Test
    void creationTest() {
        RegularTimeSeriesIndex regularTimeSeriesIndex = new RegularTimeSeriesIndex(Instant.ofEpochMilli(797558400), Instant.ofEpochMilli(800150400), Duration.ofMillis(60000));
        TimeSeriesMetadataIndex timeSeriesMetadataIndex = new TimeSeriesMetadataIndex(regularTimeSeriesIndex);
        Assertions.assertThat(timeSeriesMetadataIndex.endTime).isEqualTo(800150400);
        Assertions.assertThat(timeSeriesMetadataIndex.startTime).isEqualTo(797558400);
        Assertions.assertThat(timeSeriesMetadataIndex.pointCount).isEqualTo(44);
        Assertions.assertThat(timeSeriesMetadataIndex.spacing).isEqualTo(60000);
    }
}
