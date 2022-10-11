/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping

class LogDslLoader {

    enum LogType {
        ERROR,
        WARNING,
        INFO,
        DEBUG
    }

    private static final char SEPARATOR = ';'

    private final Writer out

    LogDslLoader(Writer out) {
        this.out = out
    }

    static LogDslLoader create(Binding binding, Writer out) {
        LogDslLoader logDslLoader = new LogDslLoader(out)
        if (out != null) {
            binding.out = out
        }
        bindLog(binding, logDslLoader)
        logDslLoader
    }

    private static void bindLog(Binding binding, LogDslLoader logDslLoader) {
        binding.writeLog = { String type, String section, String message ->
            if (logDslLoader.out == null) {
                return
            }
            logDslLoader.out.write(type + SEPARATOR + section + SEPARATOR + message)
        }
    }

    private void logOut(String type, String section, String message) {
        if (out != null) {
            out.write(type + SEPARATOR + section + SEPARATOR + message)
            out.write(System.lineSeparator())
        }
    }

    void logError(String section, String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(String.valueOf(LogType.ERROR), section, formattedString)
    }

    void logError(String section, String message) {
        logOut(String.valueOf(LogType.ERROR), section, message)
    }

    void logWarn(String section, String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(String.valueOf(LogType.WARNING), section, formattedString)
    }

    void logWarn(String section, String message) {
        logOut(String.valueOf(LogType.WARNING), section, message)
    }

    void logInfo(String section, String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(String.valueOf(LogType.INFO), section, formattedString)
    }

    void logInfo(String section, String message) {
        logOut(String.valueOf(LogType.INFO), section, message)
    }

    void logDebug(String section, String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(String.valueOf(LogType.DEBUG), section, formattedString)
    }

    void logDebug(String section, String message) {
        logOut(String.valueOf(LogType.DEBUG), section, message)
    }
}
