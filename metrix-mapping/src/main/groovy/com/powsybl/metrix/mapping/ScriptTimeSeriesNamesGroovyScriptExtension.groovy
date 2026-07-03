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
                return new HashSet<>(scriptTimeSeriesNames.inputNames)
            }
            binding.calculatedTsNames = {
                return new HashSet<>(scriptTimeSeriesNames.calculatedNames)
            }
            binding.tsNames = {
                Set<String> tsNames = new HashSet<>()
                tsNames.addAll(scriptTimeSeriesNames.inputNames)
                tsNames.addAll(scriptTimeSeriesNames.calculatedNames)
                return tsNames
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
