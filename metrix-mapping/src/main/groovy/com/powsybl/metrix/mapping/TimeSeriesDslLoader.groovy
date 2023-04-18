/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Bus
import com.powsybl.iidm.network.Injection
import com.powsybl.iidm.network.Network
import com.powsybl.iidm.network.TwoWindingsTransformer
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore
import com.powsybl.timeseries.TimeSeriesFilter
import com.powsybl.timeseries.ast.NodeCalc
import com.powsybl.timeseries.dsl.CalculatedTimeSeriesGroovyDslLoader
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import static com.powsybl.metrix.mapping.EquipmentMappingData.mapToEquipments
import static com.powsybl.metrix.mapping.EquipmentTsData.equipmentTimeSeries
import static com.powsybl.metrix.mapping.FilteredData.unmappedEquipments
import static com.powsybl.metrix.mapping.IgnoreLimitsData.ignoreLimits
import static com.powsybl.metrix.mapping.ParametersData.parametersData
import static com.powsybl.metrix.mapping.PlannedOutagesData.mapPlannedOutages
import static com.powsybl.metrix.mapping.SimpleMappingData.mapToBreakers
import static com.powsybl.metrix.mapping.SimpleVariableMappingData.mapToSimpleVariableEquipments

class TimeSeriesDslLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesDslLoader.class)

    private static final String MAPPING_SCRIPT_SECTION = "Mapping script"

    protected final GroovyCodeSource dslSrc

    TimeSeriesDslLoader(GroovyCodeSource dslSrc) {
        this.dslSrc = Objects.requireNonNull(dslSrc)
    }

    TimeSeriesDslLoader(File dslFile) {
        this(new GroovyCodeSource(dslFile))
    }

    TimeSeriesDslLoader(String script) {
        this(new GroovyCodeSource(script, "script", GroovyShell.DEFAULT_CODE_BASE))
    }

    TimeSeriesDslLoader(Reader reader, String fileName) {
        this(new GroovyCodeSource(reader, fileName, GroovyShell.DEFAULT_CODE_BASE))
    }

    private static logWarn(LogDslLoader logDslLoader, String message) {
        if (logDslLoader == null) {
            return
        }
        logDslLoader.logWarn(message)
    }

    private static CompilerConfiguration createCompilerConfig() {
        def imports = new ImportCustomizer()
        imports.addStaticStars("com.powsybl.iidm.network.EnergySource")
        imports.addStaticStars("com.powsybl.iidm.network.Country")
        imports.addStaticStars("com.powsybl.metrix.mapping.EquipmentVariable")
        def config = CalculatedTimeSeriesGroovyDslLoader.createCompilerConfig()
        config.addCompilationCustomizers(imports)
    }

    static void evaluate(GroovyCodeSource dslSrc, Binding binding) {
        def config = createCompilerConfig()
        def shell = new GroovyShell(binding, config)
        shell.evaluate(dslSrc)
    }

    static void bind(Binding binding, Network network, ReadOnlyTimeSeriesStore store, MappingParameters parameters, TimeSeriesMappingConfig config, LogDslLoader logDslLoader, ComputationRange computationRange) {
        ComputationRange checkedComputationRange = ComputationRange.check(computationRange, store)
        ComputationRange fullComputationRange = ComputationRange.check(store)
        CalculatedTimeSeriesGroovyDslLoader.bind(binding, store, config.getTimeSeriesNodes())
        TimeSeriesMappingConfigLoader loader = new TimeSeriesMappingConfigLoader(config, store.getTimeSeriesNames(new TimeSeriesFilter()))
        TimeSeriesMappingConfigStats stats = new TimeSeriesMappingConfigStats(store, checkedComputationRange)

        // map the base case to network variable
        binding.network = network

        def mappeable = { injection ->
            Bus bus = injection.getTerminal().getBusView().getBus()
            bus != null && bus.isInMainConnectedComponent()
        }

        def generatorsFilteringContext = network.getGenerators().findAll(mappeable).collect { injection -> new FilteringContext((Injection) injection) }
        def loadsFilteringContext = network.getLoads().findAll(mappeable).collect { injection -> new FilteringContext((Injection) injection) }
        def danglingLinesFilteringContext = network.getDanglingLines().findAll(mappeable).collect { injection -> new FilteringContext((Injection) injection) }
        def hvdcLinesFilteringContext = network.getHvdcLines().collect { hvdcLine -> new FilteringContext(hvdcLine) }
        def lccConverterStationsFilteringContext = network.getLccConverterStations().collect { converter -> new FilteringContext(converter) }
        def vscConverterStationsFilteringContext = network.getVscConverterStations().collect { converter -> new FilteringContext(converter) }
        def transformersFilteringContext = network.getTwoWindingsTransformers().collect { transformer -> new FilteringContext(transformer) }
        def linesFilteringContext = network.getLines().collect { line -> new FilteringContext(line) }
        def phaseTapChangersFilteringContext = network.getTwoWindingsTransformers().findAll {transformer -> transformer.hasPhaseTapChanger() }
                .collect { transformer -> new FilteringContext((TwoWindingsTransformer) transformer) }
        def ratioTapChangersFilteringContext = network.getTwoWindingsTransformers().findAll {transformer -> transformer.hasRatioTapChanger() }
                .collect { transformer -> new FilteringContext((TwoWindingsTransformer) transformer) }
        def switchesFilteringContext = network.getSwitchStream().collect { s -> new FilteringContext(s) }

        // parameters
        binding.parameters = { Closure<Void> closure ->
            parametersData(closure, parameters)
        }

        // mapping
        binding.mapToGenerators = { Closure closure ->
            mapToEquipments(binding, loader, closure, generatorsFilteringContext, MappableEquipmentType.GENERATOR)
        }
        binding.mapToLoads = { Closure closure ->
            mapToEquipments(binding, loader, closure, loadsFilteringContext, MappableEquipmentType.LOAD)
        }
        binding.mapToBoundaryLines = { Closure closure ->
            mapToEquipments(binding, loader, closure, danglingLinesFilteringContext, MappableEquipmentType.BOUNDARY_LINE)
        }
        binding.mapToHvdcLines = { Closure closure ->
            mapToEquipments(binding, loader, closure, hvdcLinesFilteringContext, MappableEquipmentType.HVDC_LINE)
        }
        binding.mapToTransformers = { Closure closure ->
            mapToEquipments(binding, loader, closure, transformersFilteringContext, MappableEquipmentType.TRANSFORMER)
        }
        binding.mapToLines = { Closure closure ->
            mapToEquipments(binding, loader, closure, linesFilteringContext, MappableEquipmentType.LINE)
        }
        binding.mapToPhaseTapChangers = { Closure closure ->
            mapToSimpleVariableEquipments(binding, loader, closure, phaseTapChangersFilteringContext, MappableEquipmentType.PHASE_TAP_CHANGER)
        }
        binding.mapToRatioTapChangers = { Closure closure ->
            mapToSimpleVariableEquipments(binding, loader, closure, ratioTapChangersFilteringContext, MappableEquipmentType.RATIO_TAP_CHANGER)
        }
        binding.mapToLccConverterStations = { Closure closure ->
            mapToSimpleVariableEquipments(binding, loader, closure, lccConverterStationsFilteringContext, MappableEquipmentType.LCC_CONVERTER_STATION)
        }
        binding.mapToVscConverterStations = { Closure closure ->
            mapToSimpleVariableEquipments(binding, loader, closure, vscConverterStationsFilteringContext, MappableEquipmentType.VSC_CONVERTER_STATION)
        }
        binding.mapToBreakers = { Closure closure ->
            mapToBreakers(binding, loader, closure, switchesFilteringContext)
        }
        binding.mapPlannedOutages = { Closure closure ->
            mapPlannedOutages(binding, store, loader, closure, transformersFilteringContext, linesFilteringContext, generatorsFilteringContext, checkedComputationRange.getVersions())
        }
        binding.mapToPsts = { @Deprecated Closure closure ->
            mapToSimpleVariableEquipments(binding, loader, closure, phaseTapChangersFilteringContext, MappableEquipmentType.PST)
        }

        // unmapped
        binding.unmappedGenerators = { Closure closure ->
            unmappedEquipments(binding, loader, closure, generatorsFilteringContext, MappableEquipmentType.GENERATOR)
        }
        binding.unmappedLoads = { Closure closure ->
            unmappedEquipments(binding, loader, closure, loadsFilteringContext, MappableEquipmentType.LOAD)
        }
        binding.unmappedBoundaryLines = { Closure closure ->
            unmappedEquipments(binding, loader, closure, danglingLinesFilteringContext, MappableEquipmentType.BOUNDARY_LINE)
        }
        binding.unmappedHvdcLines = { Closure closure ->
            unmappedEquipments(binding, loader, closure, hvdcLinesFilteringContext, MappableEquipmentType.HVDC_LINE)
        }
        binding.unmappedPhaseTapChangers = { Closure closure ->
            unmappedEquipments(binding, loader, closure, phaseTapChangersFilteringContext, MappableEquipmentType.PHASE_TAP_CHANGER)
        }

        // time series with specific ignore limits
        binding.ignoreLimits = { Closure closure ->
            ignoreLimits(loader, closure)
        }

        // equipments for which time series must be provided
        binding.provideTsGenerators = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, generatorsFilteringContext, MappableEquipmentType.GENERATOR, logDslLoader)
        }
        binding.provideTsLoads = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, loadsFilteringContext, MappableEquipmentType.LOAD, logDslLoader)
        }
        binding.provideTsHvdcLines = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, hvdcLinesFilteringContext, MappableEquipmentType.HVDC_LINE, logDslLoader)
        }
        binding.provideTsTransformers = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, transformersFilteringContext, MappableEquipmentType.TRANSFORMER, logDslLoader)
        }
        binding.provideTsLines = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, linesFilteringContext, MappableEquipmentType.LINE, logDslLoader)
        }
        binding.provideTsBoundaryLines = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, danglingLinesFilteringContext, MappableEquipmentType.BOUNDARY_LINE, logDslLoader)
        }
        binding.provideTsPhaseTapChangers = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, phaseTapChangersFilteringContext, MappableEquipmentType.PHASE_TAP_CHANGER, logDslLoader)
        }
        binding.provideTsRatioTapChangers = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, ratioTapChangersFilteringContext, MappableEquipmentType.RATIO_TAP_CHANGER, logDslLoader)
        }
        binding.provideTsBreakers = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, switchesFilteringContext, MappableEquipmentType.SWITCH, logDslLoader)
        }
        binding.provideTsLccConverterStations = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, lccConverterStationsFilteringContext, MappableEquipmentType.LCC_CONVERTER_STATION, logDslLoader)
        }
        binding.provideTsVscConverterStations = { Closure closure ->
            equipmentTimeSeries(binding, loader, closure, vscConverterStationsFilteringContext, MappableEquipmentType.VSC_CONVERTER_STATION, logDslLoader)
        }

        // statistics
        binding.sum = { NodeCalc tsNode, Boolean all_versions = false ->
            stats.getTimeSeriesSum(tsNode, all_versions ? fullComputationRange : checkedComputationRange)
        }
        binding.min = { NodeCalc tsNode, Boolean all_versions = false ->
            stats.getTimeSeriesMin(tsNode, all_versions ? fullComputationRange : checkedComputationRange)
        }
        binding.max = { NodeCalc tsNode, Boolean all_versions = false ->
            stats.getTimeSeriesMax(tsNode, all_versions ? fullComputationRange : checkedComputationRange)
        }
        binding.avg = { NodeCalc tsNode, Boolean all_versions = false ->
            stats.getTimeSeriesAvg(tsNode, all_versions ? fullComputationRange : checkedComputationRange)
        }
        binding.median = { NodeCalc tsNode, Boolean all_versions = false ->
            stats.getTimeSeriesMedian(tsNode, all_versions ? fullComputationRange : checkedComputationRange)
        }
    }

    static TimeSeriesMappingConfig load(Reader reader, Network network, MappingParameters parameters, ReadOnlyTimeSeriesStore store, Writer out, ComputationRange computationRange) {
        TimeSeriesDslLoader dslLoader = new TimeSeriesDslLoader(reader, "mapping.groovy")
        dslLoader.load(network, parameters, store, out, computationRange)
    }

    static TimeSeriesMappingConfig load(Path mappingFile, Network network, MappingParameters parameters, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        load(mappingFile, network, parameters, store, null, computationRange)
    }

    static TimeSeriesMappingConfig load(Path mappingFile, Network network, MappingParameters parameters, ReadOnlyTimeSeriesStore store, Writer out, ComputationRange computationRange) {
        Files.newBufferedReader(mappingFile, StandardCharsets.UTF_8).withReader { Reader reader ->
            TimeSeriesDslLoader dslLoader = new TimeSeriesDslLoader(reader, mappingFile.getFileName().toString())
            dslLoader.load(network, parameters, store, out, computationRange)
        }
    }

    TimeSeriesMappingConfig load(Network network, MappingParameters parameters, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        load(network, parameters, store, null, computationRange)
    }

    TimeSeriesMappingConfig load(Network network, MappingParameters parameters, ReadOnlyTimeSeriesStore store, Writer out, ComputationRange computationRange) {
        long start = System.currentTimeMillis()

        TimeSeriesMappingConfig config = new TimeSeriesMappingConfig(network)
        Binding binding = new Binding()
        LogDslLoader logDslLoader = LogDslLoader.create(binding, out, MAPPING_SCRIPT_SECTION)
        bind(binding, network, store, parameters, config, logDslLoader, computationRange)

        if (out != null) {
            binding.out = out
        }

        evaluate(dslSrc, binding)

        TimeSeriesMappingConfigChecker configChecker = new TimeSeriesMappingConfigChecker(config)
        configChecker.checkMappedVariables()
        Set<MappingKey> keys = configChecker.checkEquipmentTimeSeries()
        keys.forEach({ key ->
            logWarn(logDslLoader, "provideTs - Time series can not be provided for id " + key.getId() + " because id is not mapped on " + key.getMappingVariable().getVariableName())
        })

        LOGGER.trace("Dsl Loading done in {} ms", (System.currentTimeMillis() - start))

        config
    }
}
