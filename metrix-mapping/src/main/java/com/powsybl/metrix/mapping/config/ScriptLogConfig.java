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

import static java.lang.System.Logger.Level.INFO;

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
public class ScriptLogConfig {
    public static final System.Logger.Level MAX_LOG_LEVEL_DEFAULT = INFO;

    private Writer writer;
    private System.Logger.Level maxLogLevel;
    private String section;

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
        this.maxLogLevel = maxLogLevel;
        this.writer = writer;
        this.section = section;
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

    public System.Logger.Level getMaxLogLevel() {
        return maxLogLevel != null ? maxLogLevel : MAX_LOG_LEVEL_DEFAULT;
    }

    public Writer getWriter() {
        return writer;
    }

    public String getSection() {
        return section;
    }
}
