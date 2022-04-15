/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.metrix.mapping.exception.TimeSeriesMetadataIndex;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TimeSeriesMetadataIndexTest {
    @Test
    public void crationTest() {
        RegularTimeSeriesIndex regularTimeSeriesIndex = new RegularTimeSeriesIndex(797558400, 800150400, 60000);
        TimeSeriesMetadataIndex timeSeriesMetadataIndex  = new TimeSeriesMetadataIndex(regularTimeSeriesIndex);
        Assertions.assertThat(timeSeriesMetadataIndex.endTime).isEqualTo(800150400);
        Assertions.assertThat(timeSeriesMetadataIndex.startTime).isEqualTo(797558400);
        Assertions.assertThat(timeSeriesMetadataIndex.pointCount).isEqualTo(44);
        Assertions.assertThat(timeSeriesMetadataIndex.spacing).isEqualTo(60000);
    }
}
