/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import com.powsybl.commons.TestUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeSeriesProviderTsTest {

    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    public void setUp() {
        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
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
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_G")), mappingConfig.getGeneratorTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.p0, "FSSV.O11_L"), new MappingKey(EquipmentVariable.variableActivePower, "FSSV.O11_L"), new MappingKey(EquipmentVariable.fixedActivePower, "FSSV.O11_L")), mappingConfig.getLoadTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.activePowerSetpoint, "HVDC1")), mappingConfig.getHvdcLineTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.open, "FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0")), mappingConfig.getBreakerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.phaseTapPosition, "FP.AND1  FTDPRA1  1")), mappingConfig.getPhaseTapChangerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.ratioTapPosition, "FP.AND1  FTDPRA1  1")), mappingConfig.getRatioTapChangerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.powerFactor, "FVALDI1_FVALDI1_HVDC1")), mappingConfig.getLccConverterStationTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.voltageSetpoint, "FSSV.O1_FSSV.O1_HVDC1")), mappingConfig.getVscConverterStationTimeSeries());
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
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.minP, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.maxP, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.targetQ, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.voltageRegulatorOn, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.targetV, "FSSV.O11_G"),
                                     new MappingKey(EquipmentVariable.disconnected, "FSSV.O11_G")),
                                     mappingConfig.getGeneratorTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.p0, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.variableActivePower, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.fixedActivePower, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.q0, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.variableReactivePower, "FSSV.O11_L"),
                                     new MappingKey(EquipmentVariable.fixedReactivePower, "FSSV.O11_L")),
                                     mappingConfig.getLoadTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.activePowerSetpoint, "HVDC1"),
                                     new MappingKey(EquipmentVariable.minP, "HVDC1"),
                                     new MappingKey(EquipmentVariable.maxP, "HVDC1"),
                                     new MappingKey(EquipmentVariable.nominalV, "HVDC1")),
                                     mappingConfig.getHvdcLineTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.open, "FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0")),
                                     mappingConfig.getBreakerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.ratedU1, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.ratedU2, "FP.AND1  FTDPRA1  1")),
                                     mappingConfig.getTransformerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.phaseTapPosition, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.regulationMode, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.phaseRegulating, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.targetDeadband, "FP.AND1  FTDPRA1  1")),
                                     mappingConfig.getPhaseTapChangerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.ratioTapPosition, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.targetV, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.loadTapChangingCapabilities, "FP.AND1  FTDPRA1  1"),
                                     new MappingKey(EquipmentVariable.ratioRegulating, "FP.AND1  FTDPRA1  1")),
                                     mappingConfig.getRatioTapChangerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.powerFactor, "FVALDI1_FVALDI1_HVDC1")),
                                     mappingConfig.getLccConverterStationTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.voltageSetpoint, "FSSV.O1_FSSV.O1_HVDC1"),
                                     new MappingKey(EquipmentVariable.voltageRegulatorOn, "FSSV.O1_FSSV.O1_HVDC1"),
                                     new MappingKey(EquipmentVariable.reactivePowerSetpoint, "FSSV.O1_FSSV.O1_HVDC1")),
                                     mappingConfig.getVscConverterStationTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.disconnected, "FP.AND1  FVERGE1  1")),
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
            TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, sw, null);
            assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_G")), mappingConfig.getGeneratorTimeSeries());
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
            TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, sw, null);
            assertEquals(Collections.emptySet(), mappingConfig.getGeneratorTimeSeries());
            assertEquals("WARNING;Mapping script;provideTs - Empty filtered list for equipment type GENERATOR and variables [targetP]\n", TestUtil.normalizeLineSeparator(sw.toString()));
        }
    }
}
