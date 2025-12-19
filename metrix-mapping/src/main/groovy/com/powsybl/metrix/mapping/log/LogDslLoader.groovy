/**
 * Copyright (c) 2020-2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log


import com.powsybl.metrix.mapping.config.ScriptLogConfig

import static java.lang.System.Logger.Level.*

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class LogDslLoader {

    private final ScriptLogConfig scriptLogConfig

    LogDslLoader(ScriptLogConfig scriptLogConfig) {
        this.scriptLogConfig = scriptLogConfig
    }

    private void logOut(System.Logger.Level logLevel, String message) {
        LogUtils.logOut(scriptLogConfig, logLevel, message)
    }

    void logError(String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(ERROR, formattedString)
    }

    void logError(String message) {
        logOut(ERROR, message)
    }

    void logWarn(String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(WARNING, formattedString)
    }

    void logWarn(String message) {
        logOut(WARNING, message)
    }

    void logInfo(String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(INFO, formattedString)
    }

    void logInfo(String message) {
        logOut(INFO, message)
    }

    void logDebug(String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments)
        logOut(DEBUG, formattedString)
    }

    void logDebug(String message) {
        logOut(DEBUG, message)
    }
}
