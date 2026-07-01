/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.config;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.powsybl.metrix.mapping.config.ScriptLogConfig.DATE_TIME_FORMATTER_DEFAULT;
import static com.powsybl.metrix.mapping.config.ScriptLogConfig.MAX_LOG_LEVEL_DEFAULT;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
class ScriptLogConfigTest {

    private static final String SECTION = "SECTION";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'").withZone(ZoneOffset.UTC);

    @Test
    void testConstructorEmpty() {
        // GIVEN
        // WHEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig();
        // THEN
        assertNotNull(scriptLogConfig);
        assertNull(scriptLogConfig.getWriter());
        assertEquals(MAX_LOG_LEVEL_DEFAULT, scriptLogConfig.getMaxLogLevel());
        assertEquals(StringUtils.EMPTY, scriptLogConfig.getSection());
        assertFalse(scriptLogConfig.isWithTimeStamp());
        assertEquals(DATE_TIME_FORMATTER_DEFAULT, scriptLogConfig.getDateTimeFormatter());
        assertNotNull(scriptLogConfig.getClock());
    }

    @Test
    void testConstructorWriter() {
        // GIVEN
        StringWriter sw = new StringWriter();
        // WHEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(sw);
        // THEN
        assertNotNull(scriptLogConfig);
        assertEquals(sw, scriptLogConfig.getWriter());
        assertEquals(MAX_LOG_LEVEL_DEFAULT, scriptLogConfig.getMaxLogLevel());
        assertEquals(StringUtils.EMPTY, scriptLogConfig.getSection());
        assertFalse(scriptLogConfig.isWithTimeStamp());
        assertEquals(DATE_TIME_FORMATTER_DEFAULT, scriptLogConfig.getDateTimeFormatter());
        assertNotNull(scriptLogConfig.getClock());
    }

    @Test
    void testConstructorMaxLogLevelWriter() {
        // GIVEN
        StringWriter sw = new StringWriter();
        // WHEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(ERROR, sw);
        // THEN
        assertNotNull(scriptLogConfig);
        assertEquals(sw, scriptLogConfig.getWriter());
        assertEquals(ERROR, scriptLogConfig.getMaxLogLevel());
        assertEquals(StringUtils.EMPTY, scriptLogConfig.getSection());
        assertFalse(scriptLogConfig.isWithTimeStamp());
        assertEquals(DATE_TIME_FORMATTER_DEFAULT, scriptLogConfig.getDateTimeFormatter());
        assertNotNull(scriptLogConfig.getClock());
    }

    @Test
    void testConstructorMaxLogLevelWriterSection() {
        // GIVEN
        StringWriter sw = new StringWriter();
        // WHEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(ERROR, sw, SECTION);
        // THEN
        assertNotNull(scriptLogConfig);
        assertEquals(sw, scriptLogConfig.getWriter());
        assertEquals(ERROR, scriptLogConfig.getMaxLogLevel());
        assertEquals(SECTION, scriptLogConfig.getSection());
        assertFalse(scriptLogConfig.isWithTimeStamp());
        assertEquals(DATE_TIME_FORMATTER_DEFAULT, scriptLogConfig.getDateTimeFormatter());
        assertNotNull(scriptLogConfig.getClock());
    }

    @Test
    void testConstructorMaxLogLevelWriterSectionWithTimestamp() {
        // GIVEN
        StringWriter sw = new StringWriter();
        boolean withTimestamp = true;
        // WHEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(ERROR, sw, SECTION, withTimestamp);
        // THEN
        assertNotNull(scriptLogConfig);
        assertEquals(sw, scriptLogConfig.getWriter());
        assertEquals(ERROR, scriptLogConfig.getMaxLogLevel());
        assertEquals(SECTION, scriptLogConfig.getSection());
        assertTrue(scriptLogConfig.isWithTimeStamp());
        assertEquals(DATE_TIME_FORMATTER_DEFAULT, scriptLogConfig.getDateTimeFormatter());
        assertNotNull(scriptLogConfig.getClock());
    }

    @Test
    void testConstructorMaxLogLevelWriterSectionWithTimestampDateTimeFormatter() {
        // GIVEN
        StringWriter sw = new StringWriter();
        boolean withTimestamp = true;
        // WHEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(ERROR, sw, SECTION, withTimestamp, DATE_TIME_FORMATTER);
        // THEN
        assertNotNull(scriptLogConfig);
        assertEquals(sw, scriptLogConfig.getWriter());
        assertEquals(ERROR, scriptLogConfig.getMaxLogLevel());
        assertEquals(SECTION, scriptLogConfig.getSection());
        assertTrue(scriptLogConfig.isWithTimeStamp());
        assertEquals(DATE_TIME_FORMATTER, scriptLogConfig.getDateTimeFormatter());
        assertNotNull(scriptLogConfig.getClock());
    }

    @Test
    void testWithMaxLogLevel() {
        // GIVEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(DEBUG, null);
        // WHEN
        ScriptLogConfig scriptLogConfigWith = scriptLogConfig.withMaxLogLevel(INFO);
        // THEN
        assertEquals(scriptLogConfig, scriptLogConfigWith);
        assertEquals(INFO, scriptLogConfig.getMaxLogLevel());
    }

    @Test
    void testWithMaxLogLevelNull() {
        // GIVEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig();
        // WHEN
        ScriptLogConfig scriptLogConfigWith = scriptLogConfig.withMaxLogLevel(null);
        // THEN
        assertEquals(scriptLogConfig, scriptLogConfigWith);
        assertEquals(MAX_LOG_LEVEL_DEFAULT, scriptLogConfig.getMaxLogLevel());
    }

    @Test
    void testWithWriter() {
        // GIVEN
        StringWriter sw1 = new StringWriter();
        StringWriter sw2 = new StringWriter();
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(sw1);
        // WHEN
        ScriptLogConfig scriptLogConfigWith = scriptLogConfig.withWriter(sw2);
        // THEN
        assertEquals(scriptLogConfig, scriptLogConfigWith);
        assertEquals(sw2, scriptLogConfig.getWriter());
    }

    @Test
    void testWithSection() {
        // GIVEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig();
        // WHEN
        ScriptLogConfig scriptLogConfigWith = scriptLogConfig.withSection(SECTION);
        // THEN
        assertEquals(scriptLogConfig, scriptLogConfigWith);
        assertEquals(SECTION, scriptLogConfig.getSection());
    }

    @Test
    void testWithTimeStamp() {
        // GIVEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig();
        // WHEN
        ScriptLogConfig scriptLogConfigWith = scriptLogConfig.withTimeStamp(true);
        // THEN
        assertEquals(scriptLogConfig, scriptLogConfigWith);
        assertTrue(scriptLogConfig.isWithTimeStamp());
    }

    @Test
    void testWithDateTimeFormatter() {
        // GIVEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig();
        // WHEN
        ScriptLogConfig scriptLogConfigWith = scriptLogConfig.withDateTimeFormatter(DATE_TIME_FORMATTER);
        // THEN
        assertEquals(scriptLogConfig, scriptLogConfigWith);
        assertEquals(DATE_TIME_FORMATTER, scriptLogConfig.getDateTimeFormatter());
    }

    @Test
    void testBuilder() {
        // GIVEN
        StringWriter sw = new StringWriter();
        boolean withTimestamp = true;
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-03T07:15:38Z"), ZoneOffset.UTC);
        // WHEN
        ScriptLogConfig scriptLogConfig = ScriptLogConfig.builder()
            .writer(sw)
            .maxLogLevel(ERROR)
            .section(SECTION)
            .withTimestamp(withTimestamp)
            .dateTimeFormatter(DATE_TIME_FORMATTER)
            .clock(fixedClock)
            .build();
        // THEN
        assertNotNull(scriptLogConfig);
        assertEquals(sw, scriptLogConfig.getWriter());
        assertEquals(ERROR, scriptLogConfig.getMaxLogLevel());
        assertEquals(SECTION, scriptLogConfig.getSection());
        assertEquals(withTimestamp, scriptLogConfig.isWithTimeStamp());
        assertEquals(DATE_TIME_FORMATTER, scriptLogConfig.getDateTimeFormatter());
        assertEquals(fixedClock, scriptLogConfig.getClock());
    }
}
