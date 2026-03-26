/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ResourceBundle;

import static java.lang.String.format;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class AnalysisLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisLogger.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.MetrixAnalysis");
    private static final char SEPARATOR = ';';

    private final BufferedWriter logWriter;

    public AnalysisLogger(BufferedWriter logWriter) {
        this.logWriter = logWriter;
    }

    public static AnalysisLogger defaultLogger() {
        return new AnalysisLogger(null) {
            @Override public void warn(String section, String key, Object... args) {
                // Default implementation does nothing
            }

            @Override public void error(String section, String key, Object... args) {
                // Default implementation does nothing
            }

            @Override public void warnWithReason(String section, Reason reason, String key, Object... args) {
                // Default implementation does nothing
            }

            @Override public void errorWithReason(String section, Reason reason, String key, Object... args) {
                // Default implementation does nothing
            }
        };
    }

    /**
     * Simple message of WARNING level
     */
    public void warn(String section, String messageKey, Object... messageArgs) {
        log(System.Logger.Level.WARNING, section, messageKey, null, messageArgs);
    }

    /**
     * Simple message of ERROR level
     */
    public void error(String section, String messageKey, Object... messageArgs) {
        log(System.Logger.Level.ERROR, section, messageKey, null, messageArgs);
    }

    /**
     * Message of WARNING level with reason explanation
     */
    public void warnWithReason(String section, Reason reason, String messageKey, Object... messageArgs) {
        log(System.Logger.Level.WARNING, section, messageKey, reason, messageArgs);
    }

    /**
     * Message of ERROR level with reason explanation
     */
    public void errorWithReason(String section, Reason reason, String messageKey, Object... messageArgs) {
        log(System.Logger.Level.ERROR, section, messageKey, reason, messageArgs);
    }

    /**
     * Write a message to the log file formatted as level;section;message.
     */
    private void log(System.Logger.Level level,
                     String section,
                     String messageKey,
                     Reason reason,
                     Object[] messageArgs) {

        if (logWriter == null) {
            return;
        }

        try {
            StringBuilder message = new StringBuilder(formatMessage(messageKey, messageArgs));
            if (reason != null) {
                message.append(' ').append(formatMessage(reason.key(), reason.args()));
            }
            String line = level.getName() + SEPARATOR + section + SEPARATOR + message;
            logWriter.write(line);
            logWriter.newLine();
        } catch (IOException e) {
            LOGGER.error("Error encountered while logging message {} in {} section", messageKey, section, e);
            throw new UncheckedIOException(e);
        }
    }

    private String formatMessage(String key, Object[] args) {
        String pattern;
        try {
            pattern = RESOURCE_BUNDLE.getString(key);
        } catch (Exception e) {
            return key + " " + java.util.Arrays.toString(args);
        }

        try {
            return format(pattern, args == null ? new Object[0] : args);
        } catch (Exception e) {
            return pattern + " " + java.util.Arrays.toString(args);
        }
    }
}
