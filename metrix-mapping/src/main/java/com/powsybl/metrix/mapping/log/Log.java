package com.powsybl.metrix.mapping.log;

import com.powsybl.timeseries.TimeSeriesIndex;

import java.util.Objects;

public class Log {

    private final System.Logger.Level level;

    private final TimeSeriesIndex index;

    private final int version;

    private final int point;

    private final String label;

    private final String message;

    public Log(System.Logger.Level level, TimeSeriesIndex index, int version, int point, String label, String message) {
        this.level = level;
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

    public final System.Logger.Level getLevel() {
        return level;
    }

    public final String getLabel() {
        return label;
    }

    public final String getMessage() {
        return message;
    }
}

