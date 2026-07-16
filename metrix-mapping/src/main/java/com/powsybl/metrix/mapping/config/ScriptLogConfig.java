/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.config;

import org.apache.commons.lang3.StringUtils;

import java.io.Writer;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static java.lang.System.Logger.Level.INFO;

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
public class ScriptLogConfig {
    public static final System.Logger.Level MAX_LOG_LEVEL_DEFAULT = INFO;
    public static final DateTimeFormatter DATE_TIME_FORMATTER_DEFAULT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
    public static final boolean WITH_TIMESTAMP_DEFAULT = false;
    public static final boolean WITH_HEADER_DEFAULT = false;

    private Writer writer;
    private System.Logger.Level maxLogLevel;
    private String section;
    private boolean withTimestamp;
    private boolean withHeader;
    private DateTimeFormatter dateTimeFormatter;
    private final Clock clock;

    public ScriptLogConfig() {
        this(MAX_LOG_LEVEL_DEFAULT, null, StringUtils.EMPTY);
    }

    public ScriptLogConfig(Writer writer) {
        this(MAX_LOG_LEVEL_DEFAULT, writer, StringUtils.EMPTY);
    }

    public ScriptLogConfig(System.Logger.Level maxLogLevel, Writer writer) {
        this(maxLogLevel, writer, StringUtils.EMPTY);
    }

    public ScriptLogConfig(System.Logger.Level maxLogLevel, Writer writer, String section) {
        this(maxLogLevel, writer, section, WITH_TIMESTAMP_DEFAULT);
    }

    public ScriptLogConfig(System.Logger.Level maxLogLevel, Writer writer, String section, boolean withTimestamp) {
        this(maxLogLevel, writer, section, withTimestamp, DATE_TIME_FORMATTER_DEFAULT);
    }

    public ScriptLogConfig(System.Logger.Level maxLogLevel, Writer writer, String section, boolean withTimestamp, DateTimeFormatter dateTimeFormatter) {
        this.maxLogLevel = maxLogLevel;
        this.writer = writer;
        this.section = section;
        this.withTimestamp = withTimestamp;
        this.withHeader = WITH_HEADER_DEFAULT;
        this.dateTimeFormatter = dateTimeFormatter;
        this.clock = Clock.systemUTC();
    }

    private ScriptLogConfig(Builder builder) {
        this.writer = builder.writer;
        this.maxLogLevel = builder.maxLogLevel;
        this.section = builder.section;
        this.withTimestamp = builder.withTimestamp;
        this.withHeader = builder.withHeader;
        this.dateTimeFormatter = builder.dateTimeFormatter;
        this.clock = builder.clock;
    }

    public ScriptLogConfig withSection(String section) {
        this.section = section;
        return this;
    }

    public ScriptLogConfig withMaxLogLevel(System.Logger.Level maxLogLevel) {
        this.maxLogLevel = maxLogLevel;
        return this;
    }

    public ScriptLogConfig withWriter(Writer writer) {
        this.writer = writer;
        return this;
    }

    public ScriptLogConfig withTimeStamp(boolean withTimestamp) {
        this.withTimestamp = withTimestamp;
        return this;
    }

    public ScriptLogConfig withHeader(boolean withHeader) {
        this.withHeader = withHeader;
        return this;
    }

    public ScriptLogConfig withDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
        return this;
    }

    public System.Logger.Level getMaxLogLevel() {
        return maxLogLevel != null ? maxLogLevel : MAX_LOG_LEVEL_DEFAULT;
    }

    public Writer getWriter() {
        return writer;
    }

    public String getSection() {
        return section;
    }

    public boolean isWithTimeStamp() {
        return this.withTimestamp;
    }

    public boolean isWithHeader() {
        return this.withHeader;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public Clock getClock() {
        return this.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Writer writer = null;
        private System.Logger.Level maxLogLevel = MAX_LOG_LEVEL_DEFAULT;
        private String section = null;
        private boolean withTimestamp = WITH_TIMESTAMP_DEFAULT;
        private boolean withHeader = WITH_HEADER_DEFAULT;
        private DateTimeFormatter dateTimeFormatter = DATE_TIME_FORMATTER_DEFAULT;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        public Builder writer(Writer writer) {
            this.writer = writer;
            return this;
        }

        public Builder maxLogLevel(System.Logger.Level maxLogLevel) {
            this.maxLogLevel = maxLogLevel;
            return this;
        }

        public Builder section(String section) {
            this.section = section;
            return this;
        }

        public Builder withTimestamp(boolean withTimestamp) {
            this.withTimestamp = withTimestamp;
            return this;
        }

        public Builder withHeader(boolean withHeader) {
            this.withHeader = withHeader;
            return this;
        }

        public Builder dateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
            this.dateTimeFormatter = dateTimeFormatter;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public ScriptLogConfig build() {
            return new ScriptLogConfig(this);
        }
    }
}
