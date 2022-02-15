package com.powsybl.metrix.mapping.log;

import com.powsybl.timeseries.TimeSeriesIndex;

public class LogBuilder {
    private System.Logger.Level level;

    private TimeSeriesIndex index;

    private int point;

    private int version;

    private String message;

    private String label;

    public LogBuilder logDescription(LogContent log) {
        this.message = log.message;
        this.label = log.label;
        return this;
    }

    public LogBuilder level(System.Logger.Level level) {
        this.level = level;
        return this;
    }

    public LogBuilder index(TimeSeriesIndex index) {
        this.index = index;
        return this;
    }

    public LogBuilder point(int point) {
        this.point = point;
        return this;
    }

    public LogBuilder version(int version) {
        this.version = version;
        return this;
    }

    public Log build() {
        return new Log(level, index, version, point, label, message);
    }
}
