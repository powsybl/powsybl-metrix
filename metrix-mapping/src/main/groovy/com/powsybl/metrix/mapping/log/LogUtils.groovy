/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log

import com.powsybl.metrix.mapping.config.ScriptLogConfig

import java.time.Instant

import static java.lang.System.Logger.Level.valueOf

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
class LogUtils {

    static void bindLog(Binding binding, ScriptLogConfig scriptLogConfig) {
        binding.writeLog = { String type, String section, String message ->
            logOut(scriptLogConfig, type, section, message)
        }
    }

    static void logOut(ScriptLogConfig scriptLogConfig, System.Logger.Level logLevel, String message) {
        logOut(scriptLogConfig, logLevel.getName(), scriptLogConfig.getSection(), message)
    }

    static void logOut(ScriptLogConfig scriptLogConfig, String logLevel, String section, String message) {
        if (scriptLogConfig != null && scriptLogConfig.getWriter() != null && canLog(logLevel, scriptLogConfig.getMaxLogLevel())) {
            String timeStampFormatted = scriptLogConfig.getDateTimeFormatter().format(getInstantNow(scriptLogConfig))
            String line = buildLine(logLevel, section, message, scriptLogConfig.isWithTimeStamp(), timeStampFormatted)
            scriptLogConfig.getWriter().write(line)
        }
    }

    private static Instant getInstantNow(ScriptLogConfig scriptLogConfig) {
        Instant.now(scriptLogConfig.getClock())
    }

    static boolean canLog(String type, System.Logger.Level maxLevelToLog) {
        int maxSeverityToLog
        if (maxLevelToLog == null) {
            maxSeverityToLog = ScriptLogConfig.MAX_LOG_LEVEL_DEFAULT.getSeverity()
        } else {
            maxSeverityToLog = maxLevelToLog.getSeverity()
        }
        return getLogSeverity(type) >= maxSeverityToLog
    }

    static int getLogSeverity(String type) {
        try {
            return valueOf(type).severity
        } catch (IllegalArgumentException ignored) {
            return Integer.MAX_VALUE
        }
    }

    private static String buildLine(String logLevel, String section, String message, boolean isWithTimestamp, String timeStampFormatted) {
        List<String> line = new ArrayList<>()
        if (isWithTimestamp) {
            line.add(timeStampFormatted)
        }
        line.add(logLevel)
        line.add(section)
        line.add(message)
        return String.join(";", line) + System.lineSeparator()
    }

}