package com.powsybl.metrix.mapping.log;

import com.powsybl.timeseries.TimeSeriesIndex;

public class LogBuilder {
    private LogType type;

    private TimeSeriesIndex index;

    private int point;

    private int version;

    private String message;

    private String label;

    public LogBuilder logDescription(AbstractLogBuilder logDescription) {
        this.message = logDescription.message;
        this.label = logDescription.label;
        return this;
    }

    public LogBuilder type(LogType type) {
        this.type = type;
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

    public Log buildLog() {
        return new Log(type, index, version, point, label, message);
    }
}
