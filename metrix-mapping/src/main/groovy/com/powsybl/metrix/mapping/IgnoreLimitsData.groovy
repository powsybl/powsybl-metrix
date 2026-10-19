/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping

import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigLoader
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException
import groovy.transform.CompileStatic

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class IgnoreLimitsData {
    @CompileStatic
    protected static void ignoreLimits(TimeSeriesMappingConfigLoader configLoader, Closure closure) {
        Object value = closure.call()
        if (value instanceof String) {
            configLoader.timeSeriesExists(String.valueOf(value))
            configLoader.addIgnoreLimits(String.valueOf(value))
        } else {
            throw new TimeSeriesMappingException("Closure ignore limits must return a time series name")
        }
    }
}
