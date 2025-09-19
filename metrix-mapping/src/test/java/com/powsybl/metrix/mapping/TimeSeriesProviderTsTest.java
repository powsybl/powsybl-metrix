/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import com.powsybl.commons.test.TestUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.references.MappingKey;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author amichaut {@literal <arthur.michaut at artelys.com>}
 */
class TimeSeriesProviderTsTest {

    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    void setUp() {
        // create test network
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @Test
    void provideTsDefaultVariableTest() {

        // mapping script
        String script = String.join(System.lineSeparator(),
                "provideTsGenerators {",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "}",
                "provideTsLoads {",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "}",
                "provideTsHvdcLines {",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "}",
                "provideTsBreakers {",
                "    filter {",
                "        breaker.id==\"FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0\"",
                "    }",
                "}",
                "provideTsPhaseTapChangers {",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "}",
                "provideTsRatioTapChangers {",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "}",
                "provideTsLccConverterStations {",
                "    filter {",
                "        lccConverterStation.id==\"FVALDI1_FVALDI1_HVDC1\"",
                "    }",
                "}",
                "provideTsVscConverterStations {",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 10d, 11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_G")), mappingConfig.getGeneratorTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.P0, "FSSV.O11_L"), new MappingKey(EquipmentVariable.VARIABLE_ACTIVE_POWER, "FSSV.O11_L"), new MappingKey(EquipmentVariable.FIXED_ACTIVE_POWER, "FSSV.O11_L")), mappingConfig.getLoadTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.ACTIVE_POWER_SETPOINT, "HVDC1")), mappingConfig.getHvdcLineTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.OPEN, "FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0")), mappingConfig.getBreakerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.PHASE_TAP_POSITION, "FP.AND1  FTDPRA1  1")), mappingConfig.getPhaseTapChangerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.RATIO_TAP_POSITION, "FP.AND1  FTDPRA1  1")), mappingConfig.getRatioTapChangerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.POWER_FACTOR, "FVALDI1_FVALDI1_HVDC1")), mappingConfig.getLccConverterStationTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.VOLTAGE_SETPOINT, "FSSV.O1_FSSV.O1_HVDC1")), mappingConfig.getVscConverterStationTimeSeries());
    }

    @Test
    void provideTsVariableTest() {

        // mapping script
        String script = String.join(System.lineSeparator(),
                "provideTsGenerators {",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variables targetP, minP, maxP, targetQ, voltageRegulatorOn, targetV, disconnected",
                "}",
                "provideTsLoads {",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "    variables p0, fixedActivePower, variableActivePower, q0, fixedReactivePower, variableReactivePower",
                "}",
                "provideTsHvdcLines {",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "    variables activePowerSetpoint, minP, maxP, nominalV",
                "}",
                "provideTsBreakers {",
                "    filter {",
                "        breaker.id==\"FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0\"",
                "    }",
                "    variables open",
                "}",
                "provideTsTransformers {",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variables ratedU1, ratedU2",
                "}",
                "provideTsPhaseTapChangers {",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variables phaseTapPosition, regulationMode, phaseRegulating, targetDeadband",
                "}",
                "provideTsRatioTapChangers {",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variables ratioTapPosition, loadTapChangingCapabilities, ratioRegulating, targetV",
                "}",
                "provideTsLccConverterStations {",
                "    filter {",
                "        lccConverterStation.id==\"FVALDI1_FVALDI1_HVDC1\"",
                "    }",
                "    variables powerFactor",
                "}",
                "provideTsVscConverterStations {",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "    variables voltageSetpoint, voltageRegulatorOn, reactivePowerSetpoint",
                "}",
                "provideTsLines {",
                "    filter {",
                "        line.id==\"FP.AND1  FVERGE1  1\"",
                "    }",
                "    variables disconnected",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("ts1", index, 10d, 11d),
                TimeSeries.createDouble("ts2", index, -10d, -11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.MIN_P, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.MAX_P, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.TARGET_Q, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.VOLTAGE_REGULATOR_ON, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.TARGET_V, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.DISCONNECTED, "FSSV.O11_G")),
                                     mappingConfig.getGeneratorTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.P0, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.VARIABLE_ACTIVE_POWER, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.FIXED_ACTIVE_POWER, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.Q0, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.VARIABLE_REACTIVE_POWER, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.FIXED_REACTIVE_POWER, "FSSV.O11_L")),
                                     mappingConfig.getLoadTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.ACTIVE_POWER_SETPOINT, "HVDC1"),
                                     new MappingKey(EquipmentVariable.MIN_P, "HVDC1"),
                                     new MappingKey(EquipmentVariable.MAX_P, "HVDC1"),
                                     new MappingKey(EquipmentVariable.NOMINAL_V, "HVDC1")),
                                     mappingConfig.getHvdcLineTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.OPEN, "FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0")),
                                     mappingConfig.getBreakerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.RATED_U1, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.RATED_U2, "FP.AND1  FTDPRA1  1")),
                                     mappingConfig.getTransformerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.PHASE_TAP_POSITION, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.REGULATION_MODE, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.PHASE_REGULATING, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.TARGET_DEADBAND, "FP.AND1  FTDPRA1  1")),
                                     mappingConfig.getPhaseTapChangerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.RATIO_TAP_POSITION, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.TARGET_V, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.LOAD_TAP_CHANGING_CAPABILITIES, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.RATIO_REGULATING, "FP.AND1  FTDPRA1  1")),
                                     mappingConfig.getRatioTapChangerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.POWER_FACTOR, "FVALDI1_FVALDI1_HVDC1")),
                                     mappingConfig.getLccConverterStationTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.VOLTAGE_SETPOINT, "FSSV.O1_FSSV.O1_HVDC1"),
                                     new MappingKey(EquipmentVariable.VOLTAGE_REGULATOR_ON, "FSSV.O1_FSSV.O1_HVDC1"),
                                     new MappingKey(EquipmentVariable.REACTIVE_POWER_SETPOINT, "FSSV.O1_FSSV.O1_HVDC1")),
                                     mappingConfig.getVscConverterStationTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.DISCONNECTED, "FP.AND1  FVERGE1  1")),
                                     mappingConfig.getLineTimeSeries());
    }

    @Test
    void provideTsNotMappedTest() throws Exception {
        // mapping script
        String script = String.join(System.lineSeparator(),
                "provideTsGenerators {",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 10d, 11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        try (StringWriter sw = new StringWriter()) {
            TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), sw, null);
            assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_G")), mappingConfig.getGeneratorTimeSeries());
            assertEquals("WARNING;Mapping script;provideTs - Time series can not be provided for id FSSV.O11_G because id is not mapped on targetP\n", TestUtil.normalizeLineSeparator(sw.toString()));
        }
    }

    @Test
    void provideTsEmptyFilterTest() throws Exception {
        // mapping script
        String script = String.join(System.lineSeparator(),
                "provideTsGenerators {",
                "    filter {",
                "        generator.id==\"XYZ\"",
                "    }",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("ts1", index, 10d, 11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        try (StringWriter sw = new StringWriter()) {
            TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), sw, null);
            assertEquals(Collections.emptySet(), mappingConfig.getGeneratorTimeSeries());
            assertEquals("WARNING;Mapping script;provideTs - Empty filtered list for equipment type GENERATOR and variables [targetP]\n", TestUtil.normalizeLineSeparator(sw.toString()));
        }
    }
}
