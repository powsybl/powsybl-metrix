package com.powsybl.metrix.mapping.log;

import com.powsybl.timeseries.TimeSeriesIndex;

import java.util.Objects;

public class Log {

    private final LogType type;

    private final TimeSeriesIndex index;

    private final int version;

    private final int point;

    private final String label;

    private final String message;

    public Log(LogType type, TimeSeriesIndex index, int version, int point, String label, String message) {
        this.type = type;
        this.index = Objects.requireNonNull(index);
        this.version = version;
        this.point = point;
        this.label = label;
        this.message = message;
    }

    public final TimeSeriesIndex getIndex() {
        return index;
    }

    public final int getVersion() {
        return version;
    }

    public final int getPoint() {
        return point;
    }

    public final LogType getType() {
        return type;
    }

    public final String getLabel() {
        return label;
    }

    public final String getMessage() {
        return message;
    }
}

