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
