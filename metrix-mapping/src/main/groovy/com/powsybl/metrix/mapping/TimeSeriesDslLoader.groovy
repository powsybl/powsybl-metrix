/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping

import com.powsybl.iidm.network.Bus
import com.powsybl.iidm.network.Identifiable
import com.powsybl.iidm.network.Network
import com.powsybl.iidm.network.Switch
import com.powsybl.iidm.network.TopologyKind
import com.powsybl.timeseries.CalculatedTimeSeries
import com.powsybl.timeseries.FromStoreTimeSeriesNameResolver
import com.powsybl.timeseries.InfiniteTimeSeriesIndex
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore
import com.powsybl.timeseries.StringTimeSeries
import com.powsybl.timeseries.TimeSeriesFilter
import com.powsybl.timeseries.TimeSeriesIndex
import com.powsybl.timeseries.ast.NodeCalc
import com.powsybl.timeseries.dsl.CalculatedTimeSeriesGroovyDslLoader
import groovy.transform.CompileStatic
import org.apache.commons.lang3.StringUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class TimeSeriesDslLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesDslLoader.class)

    private static final String MAPPING_SCRIPT_SECTION = "Mapping script"

    static class ParametersSpec {

        Float toleranceThreshold
        Boolean withTimeSeriesStats

        void toleranceThreshold(Float toleranceThreshold) {
            this.toleranceThreshold = toleranceThreshold
        }

        void withTimeSeriesStats(Boolean withTimeSeriesStats) {
            this.withTimeSeriesStats = withTimeSeriesStats
        }
    }

    static class FilteredSpec {

        Closure<Boolean> filter

        void filter(Closure<Boolean> filter) {
            this.filter = filter
        }
    }

    static class SimpleMappingSpec extends FilteredSpec {

        String timeSeriesName

        void timeSeriesName(String timeSeriesName) {
            assert timeSeriesName != null
            this.timeSeriesName = timeSeriesName
        }
    }

    static class SimpleVariableMappingSpec extends SimpleMappingSpec {

        EquipmentVariable variable

        void variable(EquipmentVariable variable) {
            this.variable = variable
        }
    }

    static class EquipmentMappingSpec extends SimpleMappingSpec {

        Closure closureDistributionKey
        String timeSeriesNameDistributionKey
        Set<EquipmentVariable> variableSet = new HashSet<>()

        void distributionKey(Closure closure) {
            this.closureDistributionKey = closure
        }

        void distributionKey(String timeSeriesName) {
            this.timeSeriesNameDistributionKey = timeSeriesName
        }

        void variable(EquipmentVariable variable) {
            this.variableSet.add(variable)
        }

        void variables(EquipmentVariable[] variables) {
            this.variableSet.addAll(variables)
        }
    }

    static class EquipmentTs extends FilteredSpec {

        Set<EquipmentVariable> variables

        void variables(EquipmentVariable[] variables) {
            this.variables = variables
        }

        void variables(Set<EquipmentVariable> variables) {
            this.variables = variables
        }
    }

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
            return;
        }
        logDslLoader.logWarn(MAPPING_SCRIPT_SECTION, message)
    }
    private static timeSeriesExists(String timeSeriesName, Set<String> existingTimeSeriesNames, Set<String> nodeKeys) {
        if (!timeSeriesName) {
            throw new TimeSeriesMappingException("'timeSeriesName' is not set")
        }
        if (!existingTimeSeriesNames.contains(timeSeriesName) && !nodeKeys.contains(timeSeriesName)) {
            throw new TimeSeriesMappingException("Time Series '" + timeSeriesName + "' not found")
        }
    }

    private static parametersData(Closure<Void> closure, MappingParameters parameters) {
        def cloned = closure.clone()
        ParametersSpec spec = new ParametersSpec()
        cloned.delegate = spec
        cloned()
        if (spec.toleranceThreshold) {
            parameters.setToleranceThreshold(spec.toleranceThreshold)
        }
        if (spec.withTimeSeriesStats) {
            parameters.setWithTimeSeriesStats(spec.withTimeSeriesStats)
        }
    }

    @CompileStatic
    private static void mapToEquipments(Binding binding, Set<String> existingTimeSeriesNames, TimeSeriesMappingConfig config,
                                        Closure closure, Iterable<FilteringContext> filteringContexts,
                                        MappableEquipmentType equipmentType) {
        Closure cloned = (Closure) closure.clone()
        EquipmentMappingSpec spec = new EquipmentMappingSpec()
        cloned.delegate = spec
        cloned()

        timeSeriesExists(spec.timeSeriesName, existingTimeSeriesNames, config.getTimeSeriesNodesKeys())

        // check variable
        Set<EquipmentVariable> variables = new HashSet<>()
        if (spec.variableSet.isEmpty()) {
            variables.add(EquipmentVariable.getByDefaultVariable(equipmentType))
        } else {
            variables.addAll(EquipmentVariable.check(equipmentType, spec.variableSet))
        }

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        // create at least one entry in the config even if no equipment match the filter (mandatory for ignore-empty-filter option)
        if (filteredEquipments.isEmpty()) {
            variables.forEach({ EquipmentVariable variable ->
                config.addEquipmentMapping(equipmentType, spec.timeSeriesName, null, NumberDistributionKey.ONE, variable)
            })
        }

        // for each filtered equipment, compute the distribution key and add it to the config
        filteredEquipments.forEach({ Identifiable identifiable ->
            DistributionKey distributionKey
            if (spec.closureDistributionKey != null && spec.timeSeriesNameDistributionKey != null) {
                throw new TimeSeriesMappingException("Closure and time series name distribution key are exclusives")
            }
            if (spec.closureDistributionKey != null) {
                binding.setVariable(equipmentType.getScriptVariable(), identifiable)
                Object value = spec.closureDistributionKey.call()
                if (value instanceof Number) {
                    distributionKey = new NumberDistributionKey(((Number) value).doubleValue())
                } else if (value instanceof String) {
                    timeSeriesExists(String.valueOf(value), existingTimeSeriesNames, config.getTimeSeriesNodesKeys())
                    distributionKey = new TimeSeriesDistributionKey(String.valueOf(value))
                } else {
                    throw new TimeSeriesMappingException("Closure distribution key of equipment '" + identifiable.id
                            + "' must return a number or a time series name")
                }
                binding.setVariable(equipmentType.getScriptVariable(), null)
            } else if (spec.timeSeriesNameDistributionKey != null) {
                timeSeriesExists(spec.timeSeriesNameDistributionKey, existingTimeSeriesNames, config.getTimeSeriesNodesKeys())
                distributionKey = new TimeSeriesDistributionKey(spec.timeSeriesNameDistributionKey)
            } else {
                distributionKey = NumberDistributionKey.ONE
            }
            variables.forEach({ EquipmentVariable variable ->
                config.addEquipmentMapping(equipmentType, spec.timeSeriesName, identifiable.id, distributionKey, variable)
            })
        })
    }

    @CompileStatic
    private static void mapToBreakers(Binding binding, Set<String> existingTimeSeriesNames, TimeSeriesMappingConfig config,
                                      Closure closure, Iterable<FilteringContext> filteringContexts) {
        Closure cloned = (Closure) closure.clone()
        SimpleMappingSpec spec = new SimpleMappingSpec()
        cloned.delegate = spec
        cloned()

        timeSeriesExists(spec.timeSeriesName, existingTimeSeriesNames, config.getTimeSeriesNodesKeys())

        def breakerType = MappableEquipmentType.SWITCH

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, breakerType.scriptVariable, spec.filter)

        // for each filtered equipment, compute the distribution key and add it to the config
        if (!filteredEquipments.isEmpty()) {

            if (((Switch)filteredEquipments[0]).voltageLevel.topologyKind == TopologyKind.BUS_BREAKER) {
                throw new TimeSeriesMappingException("Bus breaker topology not supported for switch mapping")
            }

            filteredEquipments.forEach({ Identifiable identifiable ->
                config.addEquipmentMapping(breakerType, spec.timeSeriesName, identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.open)
            })
        }
    }

    @CompileStatic
    private static void mapToSimpleVariableEquipments(Binding binding, Set<String> existingTimeSeriesNames, TimeSeriesMappingConfig config,
                                                      Closure closure, Iterable<FilteringContext> filteringContexts, MappableEquipmentType equipmentType) {
        Closure cloned = (Closure) closure.clone()
        SimpleVariableMappingSpec spec = new SimpleVariableMappingSpec()
        cloned.delegate = spec
        cloned()

        timeSeriesExists(spec.timeSeriesName, existingTimeSeriesNames, config.getTimeSeriesNodesKeys())

        // check variable
        EquipmentVariable variable = EquipmentVariable.check(equipmentType, spec.variable)

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        // create at least one entry in the config even if no equipment match the filter (mandatory for ignore-empty-filter option)
        if (filteredEquipments.size() == 0) {
            config.addEquipmentMapping(equipmentType, spec.timeSeriesName, null, NumberDistributionKey.ONE, variable)
        }

        // for each filtered equipment, add it to the config
        for (Identifiable identifiable in filteredEquipments) {
            config.addEquipmentMapping(equipmentType, spec.timeSeriesName, identifiable.id, NumberDistributionKey.ONE, variable)
        }
    }

    @CompileStatic
    private static void unmappedEquipments(Binding binding, TimeSeriesMappingConfig config,
                                           Closure closure, Iterable<FilteringContext> filteringContexts,
                                           MappableEquipmentType equipmentType) {
        Closure cloned = (Closure) closure.clone()
        FilteredSpec spec = new FilteredSpec()
        cloned.delegate = spec
        cloned()

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        // for each filtered equipment, add it to the unmapped config
        filteredEquipments.forEach({ Identifiable identifiable ->
            config.addUnmappedEquipment(equipmentType, identifiable.id)
        })
    }

    @CompileStatic
    private static void ignoreLimits(Set<String> existingTimeSeriesNames, TimeSeriesMappingConfig config, Closure closure) {
        Object value = closure.call()
        if (value instanceof String) {
            timeSeriesExists(String.valueOf(value), existingTimeSeriesNames, config.getTimeSeriesNodesKeys())
            config.addIgnoreLimits(String.valueOf(value))
        } else {
            throw new TimeSeriesMappingException("Closure ignore limits must return a time series name")
        }
    }

    @CompileStatic
    private static void mapPlannedOutages(Binding binding, Set<String> existingTimeSeriesNames, ReadOnlyTimeSeriesStore store, TimeSeriesMappingConfig config, Closure closure,
                                          Iterable<FilteringContext> transformersFilteringContext, Iterable<FilteringContext> linesFilteringContext, Iterable<FilteringContext> generatorsFilteringContext, Set<Integer> versions) {
        Object value = closure.call()
        if (!value instanceof String) {
            throw new TimeSeriesMappingException("Closure plannedOutages must return a time series name")
        }

        String timeSeriesName = String.valueOf(value)
        timeSeriesExists(timeSeriesName, existingTimeSeriesNames, config.getTimeSeriesNodesKeys())

        Set<String> disconnectedIds = new HashSet<>()
        for (int version : versions) {
            StringTimeSeries plannedOutagesTimeSeries = store.getStringTimeSeries(timeSeriesName, version).orElseThrow({ new TimeSeriesMappingException("Invalid planned outages time series name " + timeSeriesName) })
            String[] array = plannedOutagesTimeSeries.toArray()
            for (int i = 0; i < array.length; i++) {
                String[] ids = array[i].split(",")
                disconnectedIds.addAll(ids)
            }
        }
        disconnectedIds.remove(StringUtils.EMPTY)
        binding.setVariable("disconnectedIds", disconnectedIds)

        // add time series to the config
        config.addPlannedOutages(timeSeriesName, disconnectedIds)

        // evaluate equipment filters
        Collection<Identifiable> filteredTransformers = Filter.evaluate(binding, transformersFilteringContext, MappableEquipmentType.TRANSFORMER.scriptVariable,
                { e -> return disconnectedIds.contains(((Identifiable) binding.getVariable(MappableEquipmentType.TRANSFORMER.getScriptVariable())).id) })
        Collection<Identifiable> filteredLines = Filter.evaluate(binding, linesFilteringContext, MappableEquipmentType.LINE.scriptVariable,
                { e -> return disconnectedIds.contains(((Identifiable) binding.getVariable(MappableEquipmentType.LINE.getScriptVariable())).id) })
        Collection<Identifiable> filteredGenerators = Filter.evaluate(binding, generatorsFilteringContext, MappableEquipmentType.GENERATOR.scriptVariable,
                { e -> return disconnectedIds.contains(((Identifiable) binding.getVariable(MappableEquipmentType.GENERATOR.getScriptVariable())).id) })

        // for each filtered equipment, add it to the config
        for (Identifiable identifiable in filteredTransformers) {
            config.addEquipmentMapping(MappableEquipmentType.TRANSFORMER, timeSeriesName + "_" + identifiable.id, identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.disconnected)
        }
        for (Identifiable identifiable in filteredLines) {
            config.addEquipmentMapping(MappableEquipmentType.LINE, timeSeriesName + "_" + identifiable.id, identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.disconnected)
        }
        for (Identifiable identifiable in filteredGenerators) {
            config.addEquipmentMapping(MappableEquipmentType.GENERATOR, timeSeriesName + "_" + identifiable.id, identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.disconnected)
        }
    }

    @CompileStatic
    private static void equipmentTimeSeries(Binding binding, TimeSeriesMappingConfig config,
                                            Closure closure, Iterable<FilteringContext> filteringContexts,
                                            MappableEquipmentType equipmentType,
                                            LogDslLoader logDslLoader) {
        Closure cloned = (Closure) closure.clone()
        EquipmentTs spec = new EquipmentTs()
        cloned.delegate = spec
        cloned()

        // check variable
        Set<EquipmentVariable> variables = EquipmentVariable.check(equipmentType, spec.variables)

        // evaluate equipment filters for each variable
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        if (filteredEquipments.isEmpty()) {
            logWarn(logDslLoader, "provideTs - Empty filtered list for equipment type " + equipmentType.toString() + " and variables " + variables.toString())
        }

        // for each filtered equipment, add it to the equipment time series config
        filteredEquipments.forEach({ Identifiable identifiable ->
            config.addEquipmentTimeSeries(equipmentType, identifiable.id, variables)
        })
    }

    static void bind(Binding binding, Network network, ReadOnlyTimeSeriesStore store, MappingParameters parameters, TimeSeriesMappingConfig config, LogDslLoader logDslLoader, ComputationRange computationRange) {
        Set<String> existingTimeSeriesNames = store.getTimeSeriesNames(new TimeSeriesFilter())

        ComputationRange checkedComputationRange = checkComputationRange(computationRange, store)
        CalculatedTimeSeriesGroovyDslLoader.bind(binding, store, config.getTimeSeriesNodes())

        // map the base case to network variable
        binding.network = network

        def mappeable = { injection ->
            Bus bus = injection.getTerminal().getBusView().getBus()
            bus != null && bus.isInMainConnectedComponent()
        }

        def generatorsFilteringContext = network.getGenerators().findAll(mappeable).collect { injection -> new FilteringContext(injection)}
        def loadsFilteringContext = network.getLoads().findAll(mappeable).collect { injection -> new FilteringContext(injection)}
        def danglingLinesFilteringContext = network.getDanglingLines().findAll(mappeable).collect { injection -> new FilteringContext(injection)}
        def hvdcLinesFilteringContext = network.getHvdcLines().collect { hvdcLine -> new FilteringContext(hvdcLine)}
        def lccConverterStationsFilteringContext = network.getLccConverterStations().collect { converter -> new FilteringContext(converter)}
        def vscConverterStationsFilteringContext = network.getVscConverterStations().collect { converter -> new FilteringContext(converter)}
        def transformersFilteringContext = network.getTwoWindingsTransformers().collect { transformer -> new FilteringContext(transformer)}
        def linesFilteringContext = network.getLines().collect { line -> new FilteringContext(line)}
        def phaseTapChangersFilteringContext = network.getTwoWindingsTransformers().findAll {transformer -> transformer.hasPhaseTapChanger()}
                .collect { transformer -> new FilteringContext(transformer)}
        def ratioTapChangersFilteringContext = network.getTwoWindingsTransformers().findAll {transformer -> transformer.hasRatioTapChanger()}
                .collect { transformer -> new FilteringContext(transformer)}
        def switchesFilteringContext = network.getSwitchStream().collect { s -> new FilteringContext(s)}

        // parameters
        binding.parameters = { Closure<Void> closure ->
            parametersData(closure, parameters)
        }

        // mapping
        binding.mapToGenerators = { Closure closure ->
            mapToEquipments(binding, existingTimeSeriesNames, config, closure, generatorsFilteringContext, MappableEquipmentType.GENERATOR)
        }
        binding.mapToLoads = { Closure closure ->
            mapToEquipments(binding, existingTimeSeriesNames, config, closure, loadsFilteringContext, MappableEquipmentType.LOAD)
        }
        binding.mapToBoundaryLines = { Closure closure ->
            mapToEquipments(binding, existingTimeSeriesNames, config, closure, danglingLinesFilteringContext, MappableEquipmentType.BOUNDARY_LINE)
        }
        binding.mapToHvdcLines = { Closure closure ->
            mapToEquipments(binding, existingTimeSeriesNames, config, closure, hvdcLinesFilteringContext, MappableEquipmentType.HVDC_LINE)
        }
        binding.mapToTransformers = { Closure closure ->
            mapToEquipments(binding, existingTimeSeriesNames, config, closure, transformersFilteringContext, MappableEquipmentType.TRANSFORMER)
        }
        binding.mapToLines = { Closure closure ->
            mapToEquipments(binding, existingTimeSeriesNames, config, closure, linesFilteringContext, MappableEquipmentType.LINE)
        }
        binding.mapToPhaseTapChangers = { Closure closure ->
            mapToSimpleVariableEquipments(binding, existingTimeSeriesNames, config, closure, phaseTapChangersFilteringContext, MappableEquipmentType.PHASE_TAP_CHANGER)
        }
        binding.mapToRatioTapChangers = { Closure closure ->
            mapToSimpleVariableEquipments(binding, existingTimeSeriesNames, config, closure, ratioTapChangersFilteringContext, MappableEquipmentType.RATIO_TAP_CHANGER)
        }
        binding.mapToLccConverterStations =  { Closure closure ->
            mapToSimpleVariableEquipments(binding, existingTimeSeriesNames, config, closure, lccConverterStationsFilteringContext, MappableEquipmentType.LCC_CONVERTER_STATION)
        }
        binding.mapToVscConverterStations =  { Closure closure ->
            mapToSimpleVariableEquipments(binding, existingTimeSeriesNames, config, closure, vscConverterStationsFilteringContext, MappableEquipmentType.VSC_CONVERTER_STATION)
        }
        binding.mapToBreakers = { Closure closure ->
            mapToBreakers(binding, existingTimeSeriesNames, config, closure, switchesFilteringContext)
        }
        binding.mapPlannedOutages = { Closure closure ->
            mapPlannedOutages(binding, existingTimeSeriesNames, store, config, closure, transformersFilteringContext, linesFilteringContext, generatorsFilteringContext, checkedComputationRange.getVersions())
        }
        binding.mapToPsts = { @Deprecated Closure closure ->
            mapToSimpleVariableEquipments(binding, existingTimeSeriesNames, config, closure, phaseTapChangersFilteringContext, MappableEquipmentType.PST)
        }

        // unmapped
        binding.unmappedGenerators = { Closure closure ->
            unmappedEquipments(binding, config, closure, generatorsFilteringContext, MappableEquipmentType.GENERATOR)
        }
        binding.unmappedLoads = { Closure closure ->
            unmappedEquipments(binding, config, closure, loadsFilteringContext, MappableEquipmentType.LOAD)
        }
        binding.unmappedBoundaryLines = { Closure closure ->
            unmappedEquipments(binding, config, closure, danglingLinesFilteringContext, MappableEquipmentType.BOUNDARY_LINE)
        }
        binding.unmappedHvdcLines = { Closure closure ->
            unmappedEquipments(binding, config, closure, hvdcLinesFilteringContext, MappableEquipmentType.HVDC_LINE)
        }
        binding.unmappedPhaseTapChangers = { Closure closure ->
            unmappedEquipments(binding, config, closure, phaseTapChangersFilteringContext, MappableEquipmentType.PHASE_TAP_CHANGER)
        }

        // time series with specific ignore limits
        binding.ignoreLimits = { Closure closure ->
            ignoreLimits(existingTimeSeriesNames, config, closure)
        }

        // equipments for which time series must be provided
        binding.provideTsGenerators = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, generatorsFilteringContext, MappableEquipmentType.GENERATOR, logDslLoader)
        }
        binding.provideTsLoads = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, loadsFilteringContext, MappableEquipmentType.LOAD, logDslLoader)
        }
        binding.provideTsHvdcLines = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, hvdcLinesFilteringContext, MappableEquipmentType.HVDC_LINE, logDslLoader)
        }
        binding.provideTsTransformers = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, transformersFilteringContext, MappableEquipmentType.TRANSFORMER, logDslLoader)
        }
        binding.provideTsLines = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, linesFilteringContext, MappableEquipmentType.LINE, logDslLoader)
        }
        binding.provideTsBoundaryLines = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, danglingLinesFilteringContext, MappableEquipmentType.BOUNDARY_LINE, logDslLoader)
        }
        binding.provideTsPhaseTapChangers = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, phaseTapChangersFilteringContext, MappableEquipmentType.PHASE_TAP_CHANGER, logDslLoader)
        }
        binding.provideTsRatioTapChangers = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, ratioTapChangersFilteringContext, MappableEquipmentType.RATIO_TAP_CHANGER, logDslLoader)
        }
        binding.provideTsBreakers = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, switchesFilteringContext, MappableEquipmentType.SWITCH, logDslLoader)
        }
        binding.provideTsLccConverterStations = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, lccConverterStationsFilteringContext, MappableEquipmentType.LCC_CONVERTER_STATION, logDslLoader)
        }
        binding.provideTsVscConverterStations = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, vscConverterStationsFilteringContext, MappableEquipmentType.VSC_CONVERTER_STATION, logDslLoader)
        }

        binding.sum = { NodeCalc tsNode ->
            TimeSeriesMappingConfig.getTimeSeriesSum(tsNode, store, checkedComputationRange)
        }

        binding.min = { NodeCalc tsNode ->
            TimeSeriesMappingConfig.getTimeSeriesMin(tsNode, store, checkedComputationRange)
        }

        binding.max = { NodeCalc tsNode ->
            TimeSeriesMappingConfig.getTimeSeriesMax(tsNode, store, checkedComputationRange)
        }

        binding.avg = { NodeCalc tsNode ->
            TimeSeriesMappingConfig.getTimeSeriesAvg(tsNode, store, checkedComputationRange)
        }

        binding.median = { NodeCalc tsNode ->
            TimeSeriesMappingConfig.getTimeSeriesMedian(tsNode, store, checkedComputationRange)
        }
    }

    private static ComputationRange checkComputationRange(ComputationRange computationRange, ReadOnlyTimeSeriesStore store) {
        ComputationRange fixed = computationRange
        if (computationRange == null) {
            fixed = new ComputationRange(store.getTimeSeriesDataVersions(), 0, TimeSeriesMappingConfig.checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).pointCount);
        }
        if (fixed.versions == null || fixed.versions.isEmpty()) {
            fixed.setVersions(store.getTimeSeriesDataVersions())
        }
        if (fixed.versions.isEmpty()) {
            fixed.setVersions(Collections.singleton(1))
        }
        if (fixed.getFirstVariant() == -1) {
            fixed.setFirstVariant(0)
        }
        if (fixed.getVariantCount() == -1) {
            fixed.setVariantCount(TimeSeriesMappingConfig.checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).pointCount)
        }
        return fixed;
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
        LogDslLoader logDslLoader = LogDslLoader.create(binding, out)
        bind(binding, network, store, parameters, config, logDslLoader, computationRange)

        if (out != null) {
            binding.out = out
        }

        evaluate(dslSrc, binding)

        config.checkMappedVariables()
        Set<MappingKey> keys = config.checkEquipmentTimeSeries()
        keys.forEach( { key ->
            logWarn(logDslLoader, "provideTs - Time series can not be provided for id " + key.getId() + " because id is not mapped on " + key.getMappingVariable().getVariableName())
        })

        LOGGER.trace("Dsl Loading done in {} ms", (System.currentTimeMillis() -start))

        config
    }
}
