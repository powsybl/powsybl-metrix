/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesMapToTest {

    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    void setUp() {
        // create test network
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @Test
    void mapToDefaultVariableTest() {

        List<MappingKey> results = new LinkedList<>();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "}",
                "mapToBreakers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        breaker.id==\"FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0\"",
                "    }",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "}",
                "mapToRatioTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "}",
                "mapToLccConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        lccConverterStation.id==\"FVALDI1_FVALDI1_HVDC1\"",
                "    }",
                "}",
                "mapToVscConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "}",
                "mapToTransformers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "}",
                "mapToLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        line.id==\"FP.AND1  FVERGE1  1\"",
                "    }",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 10d, 11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), false, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        // launch TimeSeriesMapper test
        DefaultTimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
                if (!timeSeriesName.isEmpty()) {
                    results.add(new MappingKey(variable, identifiable.getId()));
                }
            }
        };
        mapper.mapToNetwork(store, List.of(observer));

        assertEquals(10, results.size());
        assertEquals(List.of(new MappingKey(EquipmentVariable.P0, "FSSV.O11_L"),
                        new MappingKey(EquipmentVariable.PHASE_TAP_POSITION, "FP.AND1  FTDPRA1  1"),
                        new MappingKey(EquipmentVariable.OPEN, "FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0"),
                        new MappingKey(EquipmentVariable.DISCONNECTED, "FP.AND1  FTDPRA1  1"),
                        new MappingKey(EquipmentVariable.RATIO_TAP_POSITION, "FP.AND1  FTDPRA1  1"),
                        new MappingKey(EquipmentVariable.POWER_FACTOR, "FVALDI1_FVALDI1_HVDC1"),
                        new MappingKey(EquipmentVariable.VOLTAGE_SETPOINT, "FSSV.O1_FSSV.O1_HVDC1"),
                        new MappingKey(EquipmentVariable.DISCONNECTED, "FP.AND1  FVERGE1  1"),
                        new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_G"),
                        new MappingKey(EquipmentVariable.ACTIVE_POWER_SETPOINT, "HVDC1")),
                results);
    }

    @Test
    void mapToVariableTest() {

        Map<String, List<MappingVariable>> results = new HashMap<>();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable targetP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable targetQ",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O12_G\"",
                "    }",
                "    variable minP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O12_G\"",
                "    }",
                "    variable maxP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FVALDI11_G\"",
                "    }",
                "    variable voltageRegulatorOn",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable targetV",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable disconnected",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "    variable p0",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "    variable q0",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L\"",
                "    }",
                "    variable fixedActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L2\"",
                "    }",
                "    variable variableActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L\"",
                "    }",
                "    variable fixedReactivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L2\"",
                "    }",
                "    variable variableReactivePower",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "    variable activePowerSetpoint",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts2'",
                "    filter {",
                "        hvdcLine.id==\"HVDC2\"",
                "    }",
                "    variable minP",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC2\"",
                "    }",
                "    variable maxP",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "    variable nominalV",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable phaseTapPosition",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable regulationMode",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable phaseRegulating",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable targetDeadband",
                "}",
                "mapToTransformers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable ratedU1",
                "}",
                "mapToTransformers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable ratedU2",
                "}",
                "mapToTransformers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable disconnected",
                "}",
                "mapToRatioTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable ratioTapPosition",
                "}",
                "mapToRatioTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable loadTapChangingCapabilities",
                "}",
                "mapToRatioTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable ratioRegulating",
                "}",
                "mapToRatioTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable targetV",
                "}",
                "mapToLccConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        lccConverterStation.id==\"FVALDI1_FVALDI1_HVDC1\"",
                "    }",
                "    variable powerFactor",
                "}",
                "mapToVscConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "    variable voltageRegulatorOn",
                "}",
                "mapToVscConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "    variable voltageSetpoint",
                "}",
                "mapToVscConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "    variable reactivePowerSetpoint",
                "}",
                "mapToLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        line.id==\"FP.AND1  FVERGE1  1\"",
                "    }",
                "    variable disconnected",
                "}");

                // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("ts1", index, 10d, 11d),
                TimeSeries.createDouble("ts2", index, -10d, -11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), false, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        // launch TimeSeriesMapper test
        DefaultTimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
                if (!timeSeriesName.isEmpty()) {
                    if (results.containsKey(identifiable.getId())) {
                        results.get(identifiable.getId()).add(variable);
                    } else {
                        List<MappingVariable> list = new ArrayList<>();
                        list.add(variable);
                        results.put(identifiable.getId(), list);
                    }
                }
            }
        };
        mapper.mapToNetwork(store, List.of(observer));

        assertEquals(12, results.size());
        assertAll(
            () -> assertThat(results).containsEntry("FSSV.O11_G", List.of(EquipmentVariable.TARGET_Q, EquipmentVariable.TARGET_V, EquipmentVariable.DISCONNECTED, EquipmentVariable.TARGET_P)),
            () -> assertThat(results).containsEntry("FSSV.O12_G", List.of(EquipmentVariable.MIN_P, EquipmentVariable.MAX_P)),
            () -> assertThat(results).containsEntry("FVALDI11_G", List.of(EquipmentVariable.VOLTAGE_REGULATOR_ON)),
            () -> assertThat(results).containsEntry("FSSV.O11_L", List.of(EquipmentVariable.P0, EquipmentVariable.Q0)),
            () -> assertThat(results).containsEntry("FVALDI11_L", List.of(EquipmentVariable.FIXED_ACTIVE_POWER, EquipmentVariable.FIXED_REACTIVE_POWER)),
            () -> assertThat(results).containsEntry("FVALDI11_L2", List.of(EquipmentVariable.VARIABLE_ACTIVE_POWER, EquipmentVariable.VARIABLE_REACTIVE_POWER)),
            () -> assertThat(results).containsEntry("HVDC1", List.of(EquipmentVariable.NOMINAL_V, EquipmentVariable.ACTIVE_POWER_SETPOINT)),
            () -> assertThat(results).containsEntry("HVDC2", List.of(EquipmentVariable.MIN_P, EquipmentVariable.MAX_P)),
            () -> assertThat(results).containsEntry("FP.AND1  FTDPRA1  1", List.of(
                // phaseTapChanger variables
                EquipmentVariable.PHASE_TAP_POSITION, EquipmentVariable.REGULATION_MODE, EquipmentVariable.PHASE_REGULATING, EquipmentVariable.TARGET_DEADBAND,
                // transformer variables
                EquipmentVariable.RATED_U1, EquipmentVariable.RATED_U2, EquipmentVariable.DISCONNECTED,
                // ratioTapChanger variables
                EquipmentVariable.RATIO_TAP_POSITION, EquipmentVariable.LOAD_TAP_CHANGING_CAPABILITIES, EquipmentVariable.RATIO_REGULATING, EquipmentVariable.TARGET_V)),
            () -> assertThat(results).containsEntry("FVALDI1_FVALDI1_HVDC1", List.of(EquipmentVariable.POWER_FACTOR)),
            () -> assertThat(results).containsEntry("FSSV.O1_FSSV.O1_HVDC1", List.of(EquipmentVariable.VOLTAGE_REGULATOR_ON, EquipmentVariable.VOLTAGE_SETPOINT, EquipmentVariable.REACTIVE_POWER_SETPOINT)),
            () -> assertThat(results).containsEntry("FP.AND1  FVERGE1  1", List.of(EquipmentVariable.DISCONNECTED))
        );
    }

    @Test
    void mapToGeneratorsWithDistributionKeyAllEqualToZeroTest() {

        Map<String, MappingVariable> results = new HashMap<>();
        Map<String, Double> values = new HashMap<>();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FVALDI11_G\" || generator.id==\"FVALDI12_G\"",
                "    }",
                "    distributionKey {",
                "        generator.targetP",
                "    }",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 1000d, 2000d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), false, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        // launch TimeSeriesMapper test
        DefaultTimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
                if (!timeSeriesName.isEmpty()) {
                    results.put(identifiable.getId(), variable);
                    values.put(identifiable.getId(), equipmentValue);
                }
            }
        };
        mapper.mapToNetwork(store, List.of(observer));

        assertEquals(2, results.size());
        assertEquals(ImmutableMap.of("FVALDI11_G", EquipmentVariable.TARGET_P, "FVALDI12_G", EquipmentVariable.TARGET_P), results);
        assertEquals(2, values.size());
        assertEquals(ImmutableMap.of("FVALDI11_G", 500., "FVALDI12_G", 500.), values);
    }

    @Test
    void mapToGeneratorsWithSpecificIgnoreLimitsTest() {
        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id == \"FVALDI11_G\"",
                "    }",
                "}",
                "ignoreLimits { 'ts' + '1'}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 10000d, 20000d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // assertions
        assertEquals(1, mappingConfig.getIgnoreLimitsTimeSeriesNames().size());
        assertEquals(ImmutableSet.of("ts1"), mappingConfig.getIgnoreLimitsTimeSeriesNames());

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 0), false, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);
        // launch TimeSeriesMapper test
        DefaultTimeSeriesMapperObserver observer = new DefaultTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
                if (timeSeriesName.compareTo("ts1") == 0) {
                    assertEquals(0, point);
                    assertEquals("ts1", timeSeriesName);
                    assertEquals("FVALDI11_G", identifiable.getId());
                    assertEquals(EquipmentVariable.TARGET_P, variable);
                    assertEquals(10000, equipmentValue, 0);
                }
            }
        };
        mapper.mapToNetwork(store, List.of(observer));
    }
}
