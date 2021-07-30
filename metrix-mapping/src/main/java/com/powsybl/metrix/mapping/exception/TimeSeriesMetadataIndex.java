package com.powsybl.metrix.mapping.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.timeseries.RegularTimeSeriesIndex;

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
