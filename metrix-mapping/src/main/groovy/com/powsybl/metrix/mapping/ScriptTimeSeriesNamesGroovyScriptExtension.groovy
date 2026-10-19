/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping

import com.google.auto.service.AutoService
import com.powsybl.scripting.groovy.GroovyScriptExtension

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
@AutoService(GroovyScriptExtension.class)
class ScriptTimeSeriesNamesGroovyScriptExtension implements GroovyScriptExtension {

    ScriptTimeSeriesNamesGroovyScriptExtension() {}

    private static boolean exists(String tsName, Set<String> tsNames) {
        return tsNames.contains(tsName)
    }

    @Override
    void load(Binding binding, Map<Class<?>, Object> contextObjects) {
        if (contextObjects.keySet().contains(ScriptTimeSeriesNames.class)) {
            ScriptTimeSeriesNames scriptTimeSeriesNames = contextObjects.get(ScriptTimeSeriesNames.class) as ScriptTimeSeriesNames
            binding.inputTsNames = {
                return scriptTimeSeriesNames.inputNames.asImmutable()
            }
            binding.calculatedTsNames = {
                return scriptTimeSeriesNames.calculatedNames.asImmutable()
            }
            binding.tsNames = {
                return (scriptTimeSeriesNames.inputNames + scriptTimeSeriesNames.calculatedNames).asImmutable()
            }
            binding.inputTsExists = { String tsName ->
                return exists(tsName, scriptTimeSeriesNames.inputNames)
            }
            binding.calculatedTsExists = { String tsName ->
                return exists(tsName, scriptTimeSeriesNames.calculatedNames)
            }
            binding.tsExists = { String tsName ->
                return exists(tsName, scriptTimeSeriesNames.calculatedNames) || exists(tsName, scriptTimeSeriesNames.inputNames)
            }
        }
    }

    @Override
    void unload() {
    }
}
