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
import org.apache.commons.io.output.TeeWriter

/**
 * @author Matthieu SAUR {@literal <matthieu.saur at rte-france.com>}
 */
@AutoService(GroovyScriptExtension.class)
class LogScriptExtension implements GroovyScriptExtension {
    LogScriptExtension() {}

    @Override
    void load(Binding binding, Map<Class<?>, Object> contextObjects) {
        ScriptLogConfig config = Optional.ofNullable(contextObjects.get(ScriptLogConfig.class) as ScriptLogConfig).orElse(new ScriptLogConfig())
        List<Writer> writers = new ArrayList<>()
        def writerBound
        try {
            writerBound = binding.getProperty("out") as Writer
        } catch (MissingPropertyException ignored){
            writerBound = null
        }
        // The writer bound to the binding in "out" not null meaning a writer is already set and we must use it
        if (writerBound != null) {
            writers.add(writerBound)
        }
        Writer writerFromScriptLogConfig = config.getWriter()
        // If our config holds a writer and the out is already set we will use the TeeWriter to write in both writer
        if (writerFromScriptLogConfig != null && writerBound != null) {
            writers.add(writerFromScriptLogConfig)
        }
        if (!writers.isEmpty()) {
            TeeWriter teeWriter = new TeeWriter(writers)
            binding.setProperty("out", teeWriter)
            config.withWriter(teeWriter)
        }
        LogUtils.bindLog(binding, config)
        LogUtils.writeHeader(config)
    }

    @Override
    void unload() {}
}
