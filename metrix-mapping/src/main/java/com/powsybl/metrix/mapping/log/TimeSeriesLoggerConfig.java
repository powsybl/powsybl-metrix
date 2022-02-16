package com.powsybl.metrix.mapping.log;

import java.time.format.DateTimeFormatter;

public class TimeSeriesLoggerConfig {
    public final char separator;

    public final DateTimeFormatter dateTimeFormatter;

    public TimeSeriesLoggerConfig(char separator, DateTimeFormatter dateTimeFormatter) {
        this.separator = separator;
        this.dateTimeFormatter = dateTimeFormatter;
    }
}
