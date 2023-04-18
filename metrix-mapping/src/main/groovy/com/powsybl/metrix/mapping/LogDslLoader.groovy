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
    private final String section

    LogDslLoader(Writer out, String section) {
        this.out = out
        this.section = section
    }

    static LogDslLoader create(Binding binding, Writer out, String section) {
        LogDslLoader logDslLoader = new LogDslLoader(out, section)
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

    private void logOut(String type, String message) {
        if (out != null) {
            out.write(type + SEPARATOR + section + SEPARATOR + message)
            out.write(System.lineSeparator())
        }
    }

    void logError(String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(String.valueOf(LogType.ERROR), formattedString)
    }

    void logError(String message) {
        logOut(String.valueOf(LogType.ERROR), message)
    }

    void logWarn(String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(String.valueOf(LogType.WARNING), formattedString)
    }

    void logWarn(String message) {
        logOut(String.valueOf(LogType.WARNING), message)
    }

    void logInfo(String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(String.valueOf(LogType.INFO), formattedString)
    }

    void logInfo(String message) {
        logOut(String.valueOf(LogType.INFO), message)
    }

    void logDebug(String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(String.valueOf(LogType.DEBUG), formattedString)
    }

    void logDebug(String message) {
        logOut(String.valueOf(LogType.DEBUG), message)
    }
}
