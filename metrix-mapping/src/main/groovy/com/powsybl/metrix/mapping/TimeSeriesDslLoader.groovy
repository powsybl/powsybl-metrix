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
import com.powsybl.iidm.network.TwoWindingsTransformer
import com.powsybl.timeseries.CalculatedTimeSeries
import com.powsybl.timeseries.CalculatedTimeSeriesDslLoader
import com.powsybl.timeseries.FromStoreTimeSeriesNameResolver
import com.powsybl.timeseries.InfiniteTimeSeriesIndex
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore
import com.powsybl.timeseries.TimeSeriesFilter
import com.powsybl.timeseries.TimeSeriesIndex
import com.powsybl.timeseries.ast.NodeCalc
import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
class TimeSeriesDslLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesDslLoader.class)

    static final String WARNING = "WARNING - "

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

    private static logOut(Writer out, String message) {
        if (out != null) {
            out.write(message + "\n")
        }
    }

    private static logWarn(Writer out, String pattern, Object... arguments) {
        String formattedString = String.format(pattern, arguments);
        LOGGER.warn(formattedString)
        logOut(out, WARNING + formattedString)
    }

    private static timeSeriesExists(String timeSeriesName, ReadOnlyTimeSeriesStore store, TimeSeriesMappingConfig config) {
        if (!timeSeriesName) {
            throw new TimeSeriesMappingException("'timeSeriesName' is not set")
        }
        if (!store.timeSeriesExists(timeSeriesName) && !config.getTimeSeriesNodes().containsKey(timeSeriesName)) {
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
    private static void mapToEquipments(Binding binding, ReadOnlyTimeSeriesStore store, TimeSeriesMappingConfig config,
                                        Closure closure, Iterable<FilteringContext> filteringContexts,
                                        MappableEquipmentType equipmentType) {
        Closure cloned = (Closure) closure.clone()
        EquipmentMappingSpec spec = new EquipmentMappingSpec()
        cloned.delegate = spec
        cloned()

        timeSeriesExists(spec.timeSeriesName, store, config)

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
            });
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
                    timeSeriesExists(String.valueOf(value), store, config);
                    distributionKey = new TimeSeriesDistributionKey(String.valueOf(value))
                } else {
                    throw new TimeSeriesMappingException("Closure distribution key of equipment '" + identifiable.id
                            + "' must return a number or a time series name")
                }
                binding.setVariable(equipmentType.getScriptVariable(), null)
            } else if (spec.timeSeriesNameDistributionKey != null) {
                timeSeriesExists(spec.timeSeriesNameDistributionKey, store, config);
                distributionKey = new TimeSeriesDistributionKey(spec.timeSeriesNameDistributionKey)
            } else {
                distributionKey = NumberDistributionKey.ONE;
            }
            variables.forEach({ EquipmentVariable variable ->
                config.addEquipmentMapping(equipmentType, spec.timeSeriesName, identifiable.id, distributionKey, variable)
            });
        })
    }

    @CompileStatic
    private static void mapToBreakers(Binding binding, ReadOnlyTimeSeriesStore store, TimeSeriesMappingConfig config,
                                      Closure closure, Iterable<FilteringContext> filteringContexts) {
        Closure cloned = (Closure) closure.clone()
        SimpleMappingSpec spec = new SimpleMappingSpec()
        cloned.delegate = spec
        cloned()

        timeSeriesExists(spec.timeSeriesName, store, config)

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
    private static void mapToPsts(Binding binding, ReadOnlyTimeSeriesStore store, TimeSeriesMappingConfig config,
                                  Closure closure, Iterable<FilteringContext> filteringContexts, Writer out) {
        Closure cloned = (Closure) closure.clone()
        SimpleMappingSpec spec = new SimpleMappingSpec()
        cloned.delegate = spec
        cloned()

        timeSeriesExists(spec.timeSeriesName, store, config)

        def pstType = MappableEquipmentType.PST

        // evaluate equipment filters
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, pstType.scriptVariable, spec.filter)

        // for each filtered equipment, compute the distribution key and add it to the config
        if (!filteredEquipments.isEmpty()) {
            for (Identifiable identifiable in filteredEquipments) {

                if (((TwoWindingsTransformer) identifiable).phaseTapChanger == null) {
                    logWarn(out, "TwoWindingsTransformer %s is not a Phase Tap Changer", identifiable)
                } else {
                    config.addEquipmentMapping(pstType, spec.timeSeriesName, identifiable.id, NumberDistributionKey.ONE, EquipmentVariable.currentTap)
                }
            }
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
    private static void ignoreLimits(Binding binding, ReadOnlyTimeSeriesStore store, TimeSeriesMappingConfig config, Closure closure) {
        Object value = closure.call()
        if (value instanceof String) {
            timeSeriesExists(String.valueOf(value), store, config);
            config.addIgnoreLimits(String.valueOf(value))
        } else {
            throw new TimeSeriesMappingException("Closure ignore limits must return a time series name")
        }
    }

    @CompileStatic
    private static void equipmentTimeSeries(Binding binding, TimeSeriesMappingConfig config,
                                            Closure closure, Iterable<FilteringContext> filteringContexts,
                                            MappableEquipmentType equipmentType,
                                            Writer out) {
        Closure cloned = (Closure) closure.clone()
        EquipmentTs spec = new EquipmentTs()
        cloned.delegate = spec
        cloned()

        // check variable
        Set<EquipmentVariable> variables = EquipmentVariable.check(equipmentType, spec.variables)

        // evaluate equipment filters for each variable
        Collection<Identifiable> filteredEquipments = Filter.evaluate(binding, filteringContexts, equipmentType.scriptVariable, spec.filter)

        if (filteredEquipments.isEmpty()) {
            logWarn(out, "provideTs - Empty filtered list for equipment type " + equipmentType.toString() + " and variables " + variables.toString())
        }

        // for each filtered equipment, add it to the equipment time series config
        filteredEquipments.forEach({ Identifiable identifiable ->
            config.addEquipmentTimeSeries(equipmentType, identifiable.id, variables)
        })
    }

    static void bind(Binding binding, Network network, ReadOnlyTimeSeriesStore store, MappingParameters parameters, TimeSeriesMappingConfig config, Writer out, ComputationRange computationRange) {
        ComputationRange checkedComputationRange = checkComputationRange(computationRange, store);
        CalculatedTimeSeriesDslLoader.bind(binding, store, config.getTimeSeriesNodes())

        // map the base case to network variable
        binding.network = network

        def mappeable = { injection ->
            Bus bus = injection.getTerminal().getBusView().getBus()
            bus != null && bus.isInMainConnectedComponent()
        }

        def generatorsFilteringContext = network.getGenerators().findAll(mappeable).collect { injection -> new FilteringContext(injection)}
        def loadsFilteringContext = network.getLoads().findAll(mappeable).collect { injection -> new FilteringContext(injection)}
        def danglingLinesFilteringContext = network.getDanglingLines().findAll(mappeable).collect { injection -> new FilteringContext(injection)}
        def hvdcLinesFilteringContext = network.getHvdcLines().collect { injection -> new FilteringContext(injection)}
        def pstFilteringContext = network.getTwoWindingsTransformers().findAll {injection -> injection.phaseTapChanger != null}
                .collect { injection -> new FilteringContext(injection)}
        def switchesFilteringContext = network.getSwitchStream().collect { s -> new FilteringContext(s)}

        // parameters
        binding.parameters = { Closure<Void> closure ->
            parametersData(closure, parameters)
        }

        // mapping
        binding.mapToGenerators = { Closure closure ->
            mapToEquipments(binding, store, config, closure, generatorsFilteringContext, MappableEquipmentType.GENERATOR)
        }
        binding.mapToLoads = { Closure closure ->
            mapToEquipments(binding, store, config, closure, loadsFilteringContext, MappableEquipmentType.LOAD)
        }
        binding.mapToBoundaryLines = { Closure closure ->
            mapToEquipments(binding, store, config, closure, danglingLinesFilteringContext, MappableEquipmentType.BOUNDARY_LINE)
        }
        binding.mapToHvdcLines = { Closure closure ->
            mapToEquipments(binding, store, config, closure, hvdcLinesFilteringContext, MappableEquipmentType.HVDC_LINE)
        }
        binding.mapToPsts = { Closure closure ->
            mapToPsts(binding, store, config, closure, pstFilteringContext, out)
        }
        binding.mapToBreakers = { Closure closure ->
            mapToBreakers(binding, store, config, closure, switchesFilteringContext)
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
        binding.unmappedPst = { Closure closure ->
            unmappedEquipments(binding, config, closure, pstFilteringContext, MappableEquipmentType.PST)
        }

        // time series with specific ignore limits
        binding.ignoreLimits = { Closure closure ->
            ignoreLimits(binding, store, config, closure)
        }

        // equipments for which time series must be provided
        binding.provideTsGenerators = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, generatorsFilteringContext, MappableEquipmentType.GENERATOR, out)
        }
        binding.provideTsLoads = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, loadsFilteringContext, MappableEquipmentType.LOAD, out)
        }
        binding.provideTsHvdcLines = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, hvdcLinesFilteringContext, MappableEquipmentType.HVDC_LINE, out)
        }
        binding.provideTsBoundaryLines = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, danglingLinesFilteringContext, MappableEquipmentType.BOUNDARY_LINE, out)
        }
        binding.provideTsPsts = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, pstFilteringContext, MappableEquipmentType.PST, out)
        }
        binding.provideTsBreakers = { Closure closure ->
            equipmentTimeSeries(binding, config, closure, switchesFilteringContext, MappableEquipmentType.SWITCH, out)
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
        ComputationRange fixed = computationRange;
        if (computationRange == null) {
            fixed = new ComputationRange(store.getTimeSeriesDataVersions(), 0, TimeSeriesMappingConfig.checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).pointCount);
        }
        if (fixed.versions == null || fixed.versions.isEmpty()) {
            fixed.setVersions(store.getTimeSeriesDataVersions());
        }
        if (fixed.versions.isEmpty()) {
            fixed.setVersions(Collections.singleton(1));
        }
        if (fixed.getFirstVariant() == -1) {
            fixed.setFirstVariant(0);
        }
        if (fixed.getVariantCount() == -1) {
            fixed.setVariantCount(TimeSeriesMappingConfig.checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).pointCount);
        }
        return fixed;
    }

    private static CalculatedTimeSeries createCalculatedTimeSeries(NodeCalc tsNode, ReadOnlyTimeSeriesStore store, int version) {
        CalculatedTimeSeries calculatedTimeSeries = new CalculatedTimeSeries('', tsNode, new FromStoreTimeSeriesNameResolver(store, version))
        if (calculatedTimeSeries.getIndex() instanceof InfiniteTimeSeriesIndex) {
            Optional<TimeSeriesIndex> regularIndex = store
                    .getTimeSeriesMetadata(store.getTimeSeriesNames(null))
                    .stream()
                    .map({ metadata -> metadata.getIndex() })
                    .filter({ index -> ! (index instanceof InfiniteTimeSeriesIndex) })
                    .findFirst()
            regularIndex.ifPresent({index -> calculatedTimeSeries.synchronize(index)})
        }
        return calculatedTimeSeries
    }

    private static CompilerConfiguration createCompilerConfig() {
        def imports = new ImportCustomizer()
        imports.addStaticStars("com.powsybl.iidm.network.EnergySource")
        imports.addStaticStars("com.powsybl.iidm.network.Country")
        imports.addStaticStars("com.powsybl.metrix.mapping.EquipmentVariable")
        def config = CalculatedTimeSeriesDslLoader.createCompilerConfig()
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
        bind(binding, network, store, parameters, config, out, computationRange)

        if (out != null) {
            binding.out = out
        }

        evaluate(dslSrc, binding)

        config.checkMappedVariables()
        Set<MappingKey> keys = config.checkEquipmentTimeSeries()
        keys.forEach( { key ->
            logWarn(out, "provideTs - Time series can not be provided for id " + key.getId() + " because id is not mapped on " + key.getMappingVariable().getVariableName())
        })

        LOGGER.trace("Dsl Loading done in {} ms", (System.currentTimeMillis() -start))

        config
    }
}
