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

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
class ScriptLogConfigTest {

    private static final String SECTION = "SECTION";

    @Test
    void testConstructorEmpty() {
        // GIVEN
        // WHEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig();
        // THEN
        assertNotNull(scriptLogConfig);
        assertNull(scriptLogConfig.getWriter());
        assertEquals(StringUtils.EMPTY, scriptLogConfig.getSection());
        assertEquals(ScriptLogConfig.MAX_LOG_LEVEL_DEFAULT, scriptLogConfig.getMaxLogLevel());
    }

    @Test
    void testConstructorFull() {
        // GIVEN
        StringWriter sw = new StringWriter();
        // WHEN
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(ERROR, sw, SECTION);
        // THEN
        assertNotNull(scriptLogConfig);
        assertEquals(sw, scriptLogConfig.getWriter());
        assertEquals(SECTION, scriptLogConfig.getSection());
        assertEquals(ERROR, scriptLogConfig.getMaxLogLevel());
    }

    @Test
    void testConstructors() {
        // GIVEN
        StringWriter sw = new StringWriter();
        // WHEN
        ScriptLogConfig scriptLogConfig1 = new ScriptLogConfig(sw);
        ScriptLogConfig scriptLogConfig2 = new ScriptLogConfig(ERROR, sw);
        // THEN
        assertNotNull(scriptLogConfig1);
        assertEquals(sw, scriptLogConfig1.getWriter());
        assertEquals(StringUtils.EMPTY, scriptLogConfig1.getSection());
        assertEquals(ScriptLogConfig.MAX_LOG_LEVEL_DEFAULT, scriptLogConfig1.getMaxLogLevel());

        assertNotNull(scriptLogConfig2);
        assertEquals(sw, scriptLogConfig2.getWriter());
        assertEquals(StringUtils.EMPTY, scriptLogConfig2.getSection());
        assertEquals(ERROR, scriptLogConfig2.getMaxLogLevel());
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
        assertEquals(ScriptLogConfig.MAX_LOG_LEVEL_DEFAULT, scriptLogConfig.getMaxLogLevel());
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
}
