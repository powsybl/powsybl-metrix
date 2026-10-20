/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log;

import com.powsybl.metrix.mapping.config.ScriptLogConfig;
import groovy.lang.Binding;
import groovy.lang.Closure;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.Map;

import static com.powsybl.metrix.mapping.log.LogUtils.HEADER_LOG_LEVEL;
import static com.powsybl.metrix.mapping.log.LogUtils.HEADER_LOG_MESSAGE;
import static com.powsybl.metrix.mapping.log.LogUtils.HEADER_LOG_SECTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
class LogScriptExtensionTest {

    private static final String SEPARATOR_COLUMN = ";";
    private static final String HEADER_LINE = HEADER_LOG_LEVEL + SEPARATOR_COLUMN + HEADER_LOG_SECTION + SEPARATOR_COLUMN + HEADER_LOG_MESSAGE + System.lineSeparator();

    @Test
    void loadBindsWriterFromConfig() {
        StringWriter writer = new StringWriter();
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(writer);
        Binding binding = new Binding();

        new LogScriptExtension().load(binding, Map.of(ScriptLogConfig.class, scriptLogConfig));

        assertSame(writer, binding.getProperty("out"));

        Closure<?> writeLog = (Closure<?>) binding.getProperty("writeLog");
        writeLog.call("INFO", "SECTION", "MESSAGE");

        assertEquals("INFO;SECTION;MESSAGE" + System.lineSeparator(), writer.toString());
    }

    @Test
    void loadUsesExistingBindingOutWhenConfigHasNoWriter() {
        StringWriter writer = new StringWriter();
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig();
        Binding binding = new Binding();
        binding.setProperty("out", writer);

        new LogScriptExtension().load(binding, Map.of(ScriptLogConfig.class, scriptLogConfig));

        assertSame(writer, scriptLogConfig.getWriter());
        assertSame(writer, binding.getProperty("out"));

        Closure<?> writeLog = (Closure<?>) binding.getProperty("writeLog");
        writeLog.call("INFO", "SECTION", "MESSAGE");

        assertEquals("INFO;SECTION;MESSAGE" + System.lineSeparator(), writer.toString());
    }

    @Test
    void loadWritesHeaderOnceWhenHeaderIsEnabled() {
        StringWriter writer = new StringWriter();
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(writer)
                .withHeader(true);
        Binding binding = new Binding();

        new LogScriptExtension().load(binding, Map.of(ScriptLogConfig.class, scriptLogConfig));

        Closure<?> writeLog = (Closure<?>) binding.getProperty("writeLog");
        writeLog.call("INFO", "SECTION", "MESSAGE");

        String expected = HEADER_LINE
                + "INFO;SECTION;MESSAGE" + System.lineSeparator();

        assertEquals(expected, writer.toString());
    }

    @Test
    void loadDoesNotThrowWhenNoWriterIsAvailable() {
        Binding binding = new Binding();

        assertDoesNotThrow(() -> new LogScriptExtension().load(binding, Map.of()));

        Closure<?> writeLog = (Closure<?>) binding.getProperty("writeLog");
        assertDoesNotThrow(() -> writeLog.call("INFO", "SECTION", "MESSAGE"));
    }

    @Test
    void loadWritesHeaderOnlyOnceWithTwoBindingsAndOneConfig() {
        StringWriter writer = new StringWriter();
        ScriptLogConfig scriptLogConfig = new ScriptLogConfig(writer)
            .withHeader(true);
        Map<Class<?>, Object> contextObjects = Map.of(ScriptLogConfig.class, scriptLogConfig);

        LogScriptExtension extension = new LogScriptExtension();
        Binding binding1 = new Binding();
        Binding binding2 = new Binding();

        extension.load(binding1, contextObjects);
        extension.load(binding2, contextObjects);

        Closure<?> writeLog1 = (Closure<?>) binding1.getProperty("writeLog");
        Closure<?> writeLog2 = (Closure<?>) binding2.getProperty("writeLog");
        writeLog1.call("INFO", "SECTION", "MESSAGE_1");
        writeLog2.call("INFO", "SECTION", "MESSAGE_2");

        String expected = HEADER_LINE
            + "INFO;SECTION;MESSAGE_1" + System.lineSeparator()
            + "INFO;SECTION;MESSAGE_2" + System.lineSeparator();

        assertEquals(expected, writer.toString());
    }
}
