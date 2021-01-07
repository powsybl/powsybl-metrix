package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.Before;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TimeSeriesProvideTsTest {

    private Network network;

    private MappingParameters mappingParameters = MappingParameters.load();

    @Before
    public void setUp() throws Exception {
        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/reseau_test_6noeuds.xml"));
    }

    @Test
    public void provideTsDefaultVariableTest() throws Exception {

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
                "provideTsPsts {",
                "    filter {",
                "        pst.id==\"FP.AND1  FTDPRA1  1\"",
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
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.currentTap, "FP.AND1  FTDPRA1  1")), mappingConfig.getPstTimeSeries());
    }

    @Test
    public void provideTsVariableTest() throws Exception {

        Map<String, List<MappingVariable>> results = new HashMap<>();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "provideTsGenerators {",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variables minP",
                "}",
                "provideTsGenerators {",
                "    filter {",
                "        generator.id==\"FVALDI11_G\"",
                "    }",
                "    variables maxP",
                "}",
                "provideTsLoads {",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "    variables p0",
                "}",
                "provideTsLoads {",
                "    filter {",
                "        load.id==\"FVALDI11_L\"",
                "    }",
                "    variables fixedActivePower, variableActivePower",
                "}",
                "provideTsLoads {",
                "    filter {",
                "        load.id==\"FVALDI11_L2\"",
                "    }",
                "    variables variableActivePower",
                "}",
                "provideTsHvdcLines {",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "    variables minP",
                "}",
                "provideTsHvdcLines {",
                "    filter {",
                "        hvdcLine.id==\"HVDC2\"",
                "    }",
                "    variables maxP",
                "}",
                "provideTsBreakers {",
                "    filter {",
                "        breaker.id==\"FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0\"",
                "    }",
                "    variables open",
                "}",
                "provideTsPsts {",
                "    filter {",
                "        pst.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variables currentTap",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("ts1", index, 10d, 11d),
                TimeSeries.createDouble("ts2", index, -10d, -11d));

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.minP, "FSSV.O11_G"), new MappingKey(EquipmentVariable.maxP, "FVALDI11_G")), mappingConfig.getGeneratorTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.p0, "FSSV.O11_L"), new MappingKey(EquipmentVariable.variableActivePower, "FVALDI11_L2"), new MappingKey(EquipmentVariable.fixedActivePower, "FVALDI11_L"), new MappingKey(EquipmentVariable.variableActivePower, "FVALDI11_L")), mappingConfig.getLoadTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.minP, "HVDC1"), new MappingKey(EquipmentVariable.maxP, "HVDC2")), mappingConfig.getHvdcLineTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.open, "FTDPRA1_FTDPRA1  FVERGE1  1_SC5_0")), mappingConfig.getBreakerTimeSeries());
        assertEquals(ImmutableSet.of(new MappingKey(EquipmentVariable.currentTap, "FP.AND1  FTDPRA1  1")), mappingConfig.getPstTimeSeries());
    }

    @Test
    public void provideTsNotMappedTest() throws Exception {
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
            assertEquals("WARNING - provideTs - Time series can not be provided for id FSSV.O11_G because id is not mapped on targetP\n", sw.toString());
        }
    }

    @Test
    public void provideTsEmptyFilterTest() throws Exception {
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
            assertEquals("WARNING - provideTs - Empty filtered list for equipment type GENERATOR and variables [targetP]\n", sw.toString());
        }
    }
}
