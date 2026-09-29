/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log;

import com.powsybl.metrix.mapping.config.ScriptLogConfig;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.powsybl.metrix.mapping.log.LogUtils.HEADER_LOG_LEVEL;
import static com.powsybl.metrix.mapping.log.LogUtils.HEADER_LOG_MESSAGE;
import static com.powsybl.metrix.mapping.log.LogUtils.HEADER_LOG_SECTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
class LogUtilsTest {

    private static final String SEPARATOR_COLUMN = ";";
    private static final String HEADER_LINE = HEADER_LOG_LEVEL + SEPARATOR_COLUMN + HEADER_LOG_SECTION + SEPARATOR_COLUMN + HEADER_LOG_MESSAGE + System.lineSeparator();

    @Test
    void logOutWritesLogLine() {
        StringWriter writer = new StringWriter();
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(writer);

        LogUtils.logOut(scriptLogConfig, "INFO", "SECTION", "MESSAGE");

        assertEquals("INFO;SECTION;MESSAGE" + System.lineSeparator(), writer.toString());
    }

    @Test
    void logOutWritesHeaderOnlyOnce() {
        StringWriter writer = new StringWriter();
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(writer)
            .withHeader(true);

        LogUtils.logOut(scriptLogConfig, "INFO", "SECTION", "MESSAGE_1");
        LogUtils.logOut(scriptLogConfig, "INFO", "SECTION", "MESSAGE_2");

        String expected = HEADER_LINE
            + "INFO;SECTION;MESSAGE_1" + System.lineSeparator()
            + "INFO;SECTION;MESSAGE_2" + System.lineSeparator();

        assertEquals(expected, writer.toString());
    }

    @Test
    void logOutWritesTimestampWhenEnabled() {
        StringWriter writer = new StringWriter();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-03T07:15:38Z"), ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'")
            .withZone(ZoneOffset.UTC);

        ScriptLogConfig scriptLogConfig = ScriptLogConfig.builder()
            .writer(writer)
            .withTimestamp(true)
            .dateTimeFormatter(formatter)
            .clock(fixedClock)
            .build();

        LogUtils.logOut(scriptLogConfig, "INFO", "SECTION", "MESSAGE");

        assertEquals(
            "2026-06-03T07:15Z;INFO;SECTION;MESSAGE" + System.lineSeparator(),
            writer.toString()
        );
    }

    @Test
    void logOutFiltersMessagesBelowConfiguredLevel() {
        StringWriter writer = new StringWriter();
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(System.Logger.Level.ERROR, writer);

        LogUtils.logOut(scriptLogConfig, "INFO", "SECTION", "INFO_MESSAGE");
        LogUtils.logOut(scriptLogConfig, "ERROR", "SECTION", "ERROR_MESSAGE");

        assertEquals(
            "ERROR;SECTION;ERROR_MESSAGE" + System.lineSeparator(),
            writer.toString()
        );
    }

    @Test
    void logOutDoesNothingWhenConfigOrWriterIsMissing() {
        assertDoesNotThrow(() -> LogUtils.logOut(null, "INFO", "SECTION", "MESSAGE"));

        ScriptLogConfig scriptLogConfig = new ScriptLogConfig();
        assertDoesNotThrow(() -> LogUtils.logOut(scriptLogConfig, "INFO", "SECTION", "MESSAGE"));
    }
}
