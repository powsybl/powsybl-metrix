/**
 * Copyright (c) 2020-2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log

import com.google.auto.service.AutoService
import com.powsybl.metrix.mapping.config.ScriptLogConfig
import com.powsybl.scripting.groovy.GroovyScriptExtension

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
@AutoService(GroovyScriptExtension.class)
class LogScriptExtension implements GroovyScriptExtension {
    LogScriptExtension() {}

    @Override
    void load(Binding binding, Map<Class<?>, Object> contextObjects) {
        ScriptLogConfig config = Optional.ofNullable(contextObjects.get(ScriptLogConfig.class) as ScriptLogConfig).orElse(new ScriptLogConfig())
        Writer out = config.getWriter();
        if (out != null) {
            binding.out = out
        } else {
            try {
                out = binding.getProperty("out") as Writer
                config.withWriter(out)
            } catch (MissingPropertyException ignored){

            }
        }
        LogUtils.bindLog(binding, config)
    }

    @Override
    void unload() {}
}
