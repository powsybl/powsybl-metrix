/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.Sets;
import com.powsybl.commons.TestUtil;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesDslLoaderTest {

    private final MappingParameters parameters = MappingParameters.load();

    @Test
    void mappingTest() {
        // create test network
        Network network = MappingTestNetwork.create();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapPlannedOutages {",
                "   'multiple_ouverture_id'",
                "}",
                "timeSeries['zero'] = 0",
                "mapToGenerators {",
                "    timeSeriesName 'zero'",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'nucl_ts'",
                "    filter {",
                "        generator.energySource == NUCLEAR",
                "    }",
                "    distributionKey {",
                "        generator.maxP",
                "    }",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'hydro_ts'",
                "    filter {",
                "        generator.energySource == HYDRO",
                "    }",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'load1_ts'",
                "    filter {",
                "        load.terminal.voltageLevel.id == 'VL1'",
                "    }",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'load2_ts'",
                "    filter {",
                "        load.terminal.voltageLevel.id == 'VL2' && load.terminal.voltageLevel.substation.country == FR",
                "    }",
                "}",
                "mapToBreakers {",
                "    timeSeriesName 'switch_ts'",
                "    filter {",
                "        breaker.voltageLevel.id == 'VL1' && breaker.kind == com.powsybl.iidm.network.SwitchKind.BREAKER",
                "    }",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'switch_ts'",
                "}");

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(200));
        ReadOnlyTimeSeriesStoreCache store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("nucl_ts", index, 1d, 1d),
                TimeSeries.createDouble("hydro_ts", index, 1d, 1d),
                TimeSeries.createDouble("load1_ts", index, 1d, 1d),
                TimeSeries.createDouble("load2_ts", index, 1d, 1d),
                TimeSeries.createDouble("switch_ts", index, 0d, 1d),
                TimeSeries.createDouble("multiple_ouverture_id", index, 1d, 1d)
        ) {
            public Optional<StringTimeSeries> getStringTimeSeries(String timeSeriesName, int version) {
                return Optional.of(TimeSeries.createString("multiple_ouverture_id", index, "1", "G1,twt,L1"));
            }
        };

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig config = dsl.load(network, parameters, store, null);
        TimeSeriesMappingConfigCsvWriter csvWriter = new TimeSeriesMappingConfigCsvWriter(config, network);
        csvWriter.printMappingSynthesis(System.out, new TableFormatterConfig());

        Map<String, Set<String>> timeSeriesToPlannedOutagesMappingExpected = new HashMap<>();
        Set<String> outages = new HashSet<>();
        outages.add("1");
        outages.add("G1");
        outages.add("twt");
        outages.add("L1");
        timeSeriesToPlannedOutagesMappingExpected.put("multiple_ouverture_id", outages);
        Map<String, Set<String>> timeSeriesToPlannedOutagesMapping = config.getTimeSeriesToPlannedOutagesMapping();
        assertEquals(timeSeriesToPlannedOutagesMappingExpected, timeSeriesToPlannedOutagesMapping);

        // assertions
        assertTrue(config.isMappingComplete());
        assertTrue(config.getUnmappedLoads().isEmpty());
        assertTrue(config.getUnmappedGenerators().isEmpty());
        assertTrue(config.getUnmappedDanglingLines().isEmpty());
        assertTrue(config.getUnmappedHvdcLines().isEmpty());
        assertTrue(config.getUnmappedPhaseTapChangers().isEmpty());
        assertTrue(config.getGeneratorTimeSeries().isEmpty());
        assertTrue(config.getLoadTimeSeries().isEmpty());
        assertTrue(config.getDanglingLineTimeSeries().isEmpty());
        assertTrue(config.getHvdcLineTimeSeries().isEmpty());
        assertTrue(config.getPhaseTapChangerTimeSeries().isEmpty());
        assertTrue(config.getBreakerTimeSeries().isEmpty());

        // 1 generator has been mapped to 'zero': G4
        MappingKey keyZero = new MappingKey(EquipmentVariable.targetP, "zero");
        MappingKey keyG4 = new MappingKey(EquipmentVariable.targetP, "G4");
        assertEquals(1, config.getTimeSeriesToGeneratorsMapping().get(keyZero).size());
        assertEquals("G4", config.getTimeSeriesToGeneratorsMapping().get(keyZero).iterator().next());
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keyG4));

        // 2 generators have been mapped to time serie 'nucl_ts': G1 and G2
        // repartition key is based on maxP
        MappingKey keyNuclTs = new MappingKey(EquipmentVariable.targetP, "nucl_ts");
        MappingKey keyG1 = new MappingKey(EquipmentVariable.targetP, "G1");
        MappingKey keyG2 = new MappingKey(EquipmentVariable.targetP, "G2");
        assertEquals(2, config.getTimeSeriesToGeneratorsMapping().get(keyNuclTs).size());
        assertEquals(Sets.newHashSet("G1", "G2"), Sets.newHashSet(config.getTimeSeriesToGeneratorsMapping().get(keyNuclTs)));
        assertEquals(Sets.newHashSet(new NumberDistributionKey(500d), new NumberDistributionKey(1000d)),
                Sets.newHashSet(config.getDistributionKey(keyG1), config.getDistributionKey(keyG2)));

        // 1 generator has been mapped to time serie 'hydro_ts': G3
        MappingKey keyHydroTs = new MappingKey(EquipmentVariable.targetP, "hydro_ts");
        assertEquals(1, config.getTimeSeriesToGeneratorsMapping().get(keyHydroTs).size());
        assertEquals("G3", config.getTimeSeriesToGeneratorsMapping().get(keyHydroTs).iterator().next());
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(new MappingKey(EquipmentVariable.targetP, "G3")));

        // 1 load has been mapped to time serie 'load1_ts': LD1
        MappingKey keyLoad1Ts = new MappingKey(EquipmentVariable.p0, "load1_ts");
        assertEquals(1, config.getTimeSeriesToLoadsMapping().get(keyLoad1Ts).size());
        assertEquals("LD1", config.getTimeSeriesToLoadsMapping().get(keyLoad1Ts).iterator().next());
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(new MappingKey(EquipmentVariable.p0, "LD1")));

        // 2 loads have been mapped to time serie 'load2_ts': LD2 and LD3
        MappingKey keyLoad2Ts = new MappingKey(EquipmentVariable.p0, "load2_ts");
        MappingKey keyLd2 = new MappingKey(EquipmentVariable.p0, "LD2");
        MappingKey keyLd3 = new MappingKey(EquipmentVariable.p0, "LD3");
        assertEquals(2, config.getTimeSeriesToLoadsMapping().get(keyLoad2Ts).size());
        assertEquals(Sets.newHashSet("LD2", "LD3"), Sets.newHashSet(config.getTimeSeriesToLoadsMapping().get(keyLoad2Ts)));
        // by default distribution key is 1
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keyLd2));
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keyLd3));

        // no dangling line mapping
        assertTrue(config.getTimeSeriesToDanglingLinesMapping().isEmpty());

        // 2 breakers mapped to time-series 'switch_ts'
        MappingKey keySwitchTs = new MappingKey(EquipmentVariable.open, "switch_ts");
        MappingKey keySw1 = new MappingKey(EquipmentVariable.open, "SW1");
        MappingKey keySw2 = new MappingKey(EquipmentVariable.open, "SW2");
        assertEquals(2, config.getTimeSeriesToBreakersMapping().get(keySwitchTs).size());
        assertEquals(Sets.newHashSet("SW1", "SW2"), Sets.newHashSet(config.getTimeSeriesToBreakersMapping().get(keySwitchTs)));
        // by default distribution key is 1
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keySw1));
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keySw2));
    }

    @Test
    void loadMappingErrorTest() {
        // create test network
        Network network = MappingTestNetwork.create();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "timeSeries['zero'] = 0",
                "mapToLoads {",
                "    timeSeriesName 'zero'",
                "    filter {",
                "        load.id == 'LD1'",
                "    }",
                "    variable p0",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'zero'",
                "    filter {",
                "        load.id == 'LD1'",
                "    }",
                "    variable fixedActivePower",
                "}"
        );

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        try {
            dsl.load(network, parameters, store, null);
            fail();
        } catch (TimeSeriesMappingException e) {
            assertEquals("Load 'LD1' is mapped on p0 and on one of the detailed variables (fixedActivePower/variableActivePower)", e.getMessage());
        }
    }

    @Test
    void switchMappingErrorTest() {
        // create test network
        Network network = MappingTestNetwork.create();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "timeSeries['zero'] = 0",
                "mapToBreakers {",
                "    timeSeriesName 'zero'",
                "    filter {",
                "        breaker.id == 'SW1'",
                "    }",
                "    distributionKey p0",
                "}"
        );

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        try {
            dsl.load(network, parameters, store, null);
            fail();
        } catch (groovy.lang.MissingMethodException ignored) {
        }
    }

    @Test
    void tsStatsFunctions() throws IOException {
        Network network = MappingTestNetwork.create();
        String script = String.join(System.lineSeparator(),
                "res_sum = sum(ts['test'])",
                "res_avg = avg(ts['test'])",
                "res_min = min(ts['test'])",
                "res_max = max(ts['test'])",
                "res_median = median(ts['test'])",
                "println res_sum",
                "println res_avg",
                "println res_min",
                "println res_max",
                "println res_median"
        );

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(50));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("test", index, 1d, 2d, 3d, -5d, 0d)
        );

        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            TimeSeriesMappingConfig config = dsl.load(network, parameters, store, out, null);
            TimeSeriesMappingConfigCsvWriter csvWriter = new TimeSeriesMappingConfigCsvWriter(config, network);
            csvWriter.printMappingSynthesis(System.out, new TableFormatterConfig());
        }

        String output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals("1.0\n0.2\n-5.0\n3.0\n1.0\n", output);

        outputStream.reset();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            TimeSeriesMappingConfig config = dsl.load(network, parameters, store, out, new ComputationRange(Collections.singleton(1), 0, 3));
            TimeSeriesMappingConfigCsvWriter csvWriter = new TimeSeriesMappingConfigCsvWriter(config, network);
            csvWriter.printMappingSynthesis(System.out, new TableFormatterConfig());
        }

        output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals("6.0\n2.0\n1.0\n3.0\n2.0\n", output);
    }

    @Test
    void testParameters() {
        Network network = MappingTestNetwork.create();

        // mapping script
        String script = String.join(System.lineSeparator(),
                "parameters {",
                "    toleranceThreshold 0.5",
                "    withTimeSeriesStats true",
                "}"
        );

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        dsl.load(network, parameters, store, null);

        assertEquals(0.5f, parameters.getToleranceThreshold(), 0f);
        assertTrue(parameters.getWithTimeSeriesStats());
    }

    @Test
    void writeLogTest() throws IOException {
        Network network = MappingTestNetwork.create();
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        String script = "writeLog(\"LOG_TYPE\", \"LOG_SECTION\", \"LOG_MESSAGE\")";

        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dsl.load(network, parameters, store, out, null);
        }

        String output = outputStream.toString();
        assertEquals("LOG_TYPE;LOG_SECTION;LOG_MESSAGE", output);
    }
}
