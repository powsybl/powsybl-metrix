/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration

import com.powsybl.iidm.network.*
import com.powsybl.metrix.mapping.LogDslLoader
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig
import com.powsybl.metrix.mapping.TimeSeriesMappingConfigLoader
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore
import com.powsybl.timeseries.TimeSeriesFilter
import com.powsybl.timeseries.dsl.CalculatedTimeSeriesGroovyDslLoader
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import static BranchMonitoringData.branchData
import static ContingenciesData.contingenciesData
import static GeneratorData.generatorData
import static GeneratorsBindingData.generatorsBindingData
import static HvdcData.hvdcData
import static LoadData.loadData
import static LoadsBindingData.loadsBindingData
import static ParametersData.parametersData
import static PhaseShifterData.phaseShifterData
import static SectionMonitoringData.sectionMonitoringData

class MetrixDslDataLoader {

    private static final String METRIX_SCRIPT_SECTION = "Metrix script"

    protected final GroovyCodeSource dslSrc

    MetrixDslDataLoader(GroovyCodeSource dslSrc) {
        this.dslSrc = Objects.requireNonNull(dslSrc)
    }

    MetrixDslDataLoader(File dslFile) {
        this(new GroovyCodeSource(dslFile))
    }

    MetrixDslDataLoader(String script) {
        this(new GroovyCodeSource(script, "script", GroovyShell.DEFAULT_CODE_BASE))
    }

    MetrixDslDataLoader(Reader reader, String fileName) {
        this(new GroovyCodeSource(reader, fileName, GroovyShell.DEFAULT_CODE_BASE))
    }

    private static logError(LogDslLoader logDslLoader, String message) {
        if (logDslLoader == null) {
            return
        }
        logDslLoader.logError(message)
    }

    private static logError(LogDslLoader logDslLoader, String pattern, Object... arguments) {
        if (logDslLoader == null) {
            return
        }
        String formattedString = String.format(pattern, arguments)
        logDslLoader.logError(formattedString)
    }

    private static logWarn(LogDslLoader logDslLoader, String message) {
        if (logDslLoader == null) {
            return
        }
        logDslLoader.logWarn(message)
    }

    private static logWarn(LogDslLoader logDslLoader, String pattern, Object... arguments) {
        if (logDslLoader == null) {
            return
        }
        String formattedString = String.format(pattern, arguments)
        logDslLoader.logWarn(formattedString)
    }

    private static logDebug(LogDslLoader logDslLoader, String pattern, Object... arguments) {
        if (logDslLoader == null) {
            return
        }
        String formattedString = String.format(pattern, arguments)
        logDslLoader.logDebug(formattedString)
    }

    private static CompilerConfiguration createCompilerConfig() {
        def imports = new ImportCustomizer()
        imports.addStaticStars("com.powsybl.metrix.integration.MetrixPtcControlType")
        imports.addStaticStars("com.powsybl.metrix.integration.MetrixHvdcControlType")
        imports.addStaticStars("com.powsybl.metrix.integration.MetrixComputationType")
        imports.addStaticStars("com.powsybl.metrix.integration.MetrixGeneratorsBinding.ReferenceVariable")
        def config = CalculatedTimeSeriesGroovyDslLoader.createCompilerConfig()
        config.addCompilationCustomizers(imports)
    }

    static void evaluate(GroovyCodeSource dslSrc, Binding binding) {
        def config = createCompilerConfig()
        def shell = new GroovyShell(binding, config)
        shell.evaluate(dslSrc)
    }

    static void bind(Binding binding, Network network, ReadOnlyTimeSeriesStore store, MetrixParameters parameters, TimeSeriesMappingConfig mappingConfig, MetrixDslData data, LogDslLoader logDslLoader) {
        CalculatedTimeSeriesGroovyDslLoader.bind(binding, store, mappingConfig.getTimeSeriesNodes())
        TimeSeriesMappingConfigLoader loader = new TimeSeriesMappingConfigLoader(mappingConfig, store.getTimeSeriesNames(new TimeSeriesFilter()))

        // map the base case to network variable
        binding.network = network

        // parameters
        binding.parameters = { Closure<Void> closure ->
            parametersData(closure, parameters)
        }

        // branch monitoring
        binding.branch = { String id, Closure<Void> closure ->
            branchData(closure, id, network, loader, data, logDslLoader)
        }

        // generator costs
        binding.generator = { String id, Closure<Void> closure ->
            generatorData(closure, id, network, loader, data, logDslLoader)
        }

        // load shedding costs
        binding.load = { String id, Closure<Void> closure ->
            loadData(closure, id, network, loader, data, logDslLoader)
        }

        // phase tap changer
        binding.phaseShifter = { String id, Closure<Void> closure ->
            phaseShifterData(closure, id, network, data, logDslLoader)
        }

        // hvdc
        binding.hvdc = { String id, Closure<Void> closure ->
            hvdcData(closure, id, network, data, logDslLoader)
        }

        // section monitoring
        binding.sectionMonitoring = { String id, Closure<Void> closure ->
            sectionMonitoringData(closure, id, network, data, logDslLoader)
        }

        // bound generators
        binding.generatorsGroup = { String id, Closure<Void> closure ->
            generatorsBindingData(binding, closure, id, network, data, logDslLoader)
        }

        // bound loads
        binding.loadsGroup = { String id, Closure<Void> closure ->
            loadsBindingData(binding, closure, id, network, data, logDslLoader)
        }

        // specific contingency list
        binding.contingencies = { Closure<Void> closure ->
            contingenciesData(closure, data, logDslLoader)
        }
    }

    static MetrixDslData load(Reader reader, Network network, MetrixParameters parameters, ReadOnlyTimeSeriesStore store,
                              TimeSeriesMappingConfig mappingConfig, Writer out) {
        MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, "metrixDsl.groovy")
        dslLoader.load(network, parameters, store, mappingConfig, out)
    }

    static MetrixDslData load(Path metrixDslFile, Network network, MetrixParameters parameters, ReadOnlyTimeSeriesStore store,
                              TimeSeriesMappingConfig mappingConfig) {
        load(metrixDslFile, network, parameters, store, mappingConfig, null)
    }

    static MetrixDslData load(Path metrixDslFile, Network network, MetrixParameters parameters, ReadOnlyTimeSeriesStore store,
                              TimeSeriesMappingConfig mappingConfig, Writer out) {

        Files.newBufferedReader(metrixDslFile, StandardCharsets.UTF_8).withReader { Reader reader ->
            load(reader, network, parameters, store, mappingConfig, out)
        }
    }

    MetrixDslData load(Network network, MetrixParameters parameters, ReadOnlyTimeSeriesStore store,
                              TimeSeriesMappingConfig mappingConfig, Writer out) {

        MetrixDslData data = new MetrixDslData()
        Binding binding = new Binding()
        LogDslLoader logDslLoader = LogDslLoader.create(binding, out, METRIX_SCRIPT_SECTION)
        bind(binding, network, store, parameters, mappingConfig, data, logDslLoader)

        evaluate(dslSrc, binding)

        TimeSeriesMetrixConfigChecker configChecker = new TimeSeriesMetrixConfigChecker(mappingConfig, logDslLoader)
        configChecker.checkBranchThreshold()

        data
    }
}
