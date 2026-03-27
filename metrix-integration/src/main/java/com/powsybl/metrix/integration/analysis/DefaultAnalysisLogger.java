/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.analysis;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class DefaultAnalysisLogger {

    public void warn(String section, String messageKey, Object... messageArgs) {
        // default empty implementation
    }

    public void error(String section, String messageKey, Object... messageArgs) {
        // default empty implementation
    }

    public void warnWithReason(String section, Reason reason, String messageKey, Object... messageArgs) {
        // default empty implementation
    }

    public void errorWithReason(String section, Reason reason, String messageKey, Object... messageArgs) {
        // default empty implementation
    }
}
