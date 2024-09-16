/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Sets;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.commons.test.TestUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesDslLoaderTest {

    private final MappingParameters parameters = MappingParameters.load();
    private final Network network = MappingTestNetwork.create();
    private final FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());

    private final String tagScript = String.join(System.lineSeparator(),
            "ts['one'] = 1",
            "ts['test'] = %s",
            "tag(ts['test'], 'calculatedTag', 'calculatedParam')",
            "metadata_value = getMetadataTags(ts['test'])",
            "println metadata_value"
    );

    private final String statScript = String.join(System.lineSeparator(),
            "allVersions = %s",
            "res_sum = sum(ts['test'], allVersions)",
            "res_avg = avg(ts['test'], allVersions)",
            "res_min = min(ts['test'], allVersions)",
            "res_max = max(ts['test'], allVersions)",
            "res_median = median(ts['test'], allVersions)",
            "println res_sum",
            "println res_avg",
            "println res_min",
            "println res_max",
            "println res_median"
    );

    @Test
    void mappingFileTest() throws URISyntaxException {
        File mappingFile = new File(Objects.requireNonNull(getClass().getResource("/emptyScript.groovy")).toURI());
        TimeSeriesMappingConfig config = new TimeSeriesDslLoader(mappingFile).load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), null);
        assertTrue(config.getMappedTimeSeriesNames().isEmpty());
    }

    @Test
    void mappingScriptTest() {
        TimeSeriesMappingConfig config = new TimeSeriesDslLoader("").load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), null);
        assertTrue(config.getMappedTimeSeriesNames().isEmpty());
    }

    @Test
    void mappingReaderTest() throws IOException {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(TimeSeriesDslLoaderTest.class.getResourceAsStream("/emptyScript.groovy")), StandardCharsets.UTF_8)) {
            TimeSeriesMappingConfig config = new TimeSeriesDslLoader(reader).load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), null, null);
            assertTrue(config.getMappedTimeSeriesNames().isEmpty());
        }
    }

    @Test
    void mappingPathTest() throws IOException {
        Path mappingFile = fileSystem.getPath("/emptyScript.groovy");
        try (Writer writer = Files.newBufferedWriter(mappingFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), ""));
        }
        TimeSeriesMappingConfig config = new TimeSeriesDslLoader(mappingFile).load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), null);
        assertTrue(config.getMappedTimeSeriesNames().isEmpty());
    }

    @Test
    void mappingTest() {
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
        TimeSeriesMappingConfig config = dsl.load(network, parameters, store, new DataTableStore(), null);
        TimeSeriesMappingConfigSynthesisCsvWriter csvWriter = new TimeSeriesMappingConfigSynthesisCsvWriter(config);
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
        assertTrue(new TimeSeriesMappingConfigChecker(config).isMappingComplete());
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
        MappingKey keyZero = new MappingKey(EquipmentVariable.TARGET_P, "zero");
        MappingKey keyG4 = new MappingKey(EquipmentVariable.TARGET_P, "G4");
        assertEquals(1, config.getTimeSeriesToGeneratorsMapping().get(keyZero).size());
        assertEquals("G4", config.getTimeSeriesToGeneratorsMapping().get(keyZero).iterator().next());
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keyG4));

        // 2 generators have been mapped to time serie 'nucl_ts': G1 and G2
        // repartition key is based on maxP
        MappingKey keyNuclTs = new MappingKey(EquipmentVariable.TARGET_P, "nucl_ts");
        MappingKey keyG1 = new MappingKey(EquipmentVariable.TARGET_P, "G1");
        MappingKey keyG2 = new MappingKey(EquipmentVariable.TARGET_P, "G2");
        assertEquals(2, config.getTimeSeriesToGeneratorsMapping().get(keyNuclTs).size());
        assertEquals(Sets.newHashSet("G1", "G2"), Sets.newHashSet(config.getTimeSeriesToGeneratorsMapping().get(keyNuclTs)));
        assertEquals(Sets.newHashSet(new NumberDistributionKey(500d), new NumberDistributionKey(1000d)),
                Sets.newHashSet(config.getDistributionKey(keyG1), config.getDistributionKey(keyG2)));

        // 1 generator has been mapped to time serie 'hydro_ts': G3
        MappingKey keyHydroTs = new MappingKey(EquipmentVariable.TARGET_P, "hydro_ts");
        assertEquals(1, config.getTimeSeriesToGeneratorsMapping().get(keyHydroTs).size());
        assertEquals("G3", config.getTimeSeriesToGeneratorsMapping().get(keyHydroTs).iterator().next());
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(new MappingKey(EquipmentVariable.TARGET_P, "G3")));

        // 1 load has been mapped to time serie 'load1_ts': LD1
        MappingKey keyLoad1Ts = new MappingKey(EquipmentVariable.P0, "load1_ts");
        assertEquals(1, config.getTimeSeriesToLoadsMapping().get(keyLoad1Ts).size());
        assertEquals("LD1", config.getTimeSeriesToLoadsMapping().get(keyLoad1Ts).iterator().next());
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(new MappingKey(EquipmentVariable.P0, "LD1")));

        // 2 loads have been mapped to time serie 'load2_ts': LD2 and LD3
        MappingKey keyLoad2Ts = new MappingKey(EquipmentVariable.P0, "load2_ts");
        MappingKey keyLd2 = new MappingKey(EquipmentVariable.P0, "LD2");
        MappingKey keyLd3 = new MappingKey(EquipmentVariable.P0, "LD3");
        assertEquals(2, config.getTimeSeriesToLoadsMapping().get(keyLoad2Ts).size());
        assertEquals(Sets.newHashSet("LD2", "LD3"), Sets.newHashSet(config.getTimeSeriesToLoadsMapping().get(keyLoad2Ts)));
        // by default distribution key is 1
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keyLd2));
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keyLd3));

        // no dangling line mapping
        assertTrue(config.getTimeSeriesToDanglingLinesMapping().isEmpty());

        // 2 breakers mapped to time-series 'switch_ts'
        MappingKey keySwitchTs = new MappingKey(EquipmentVariable.OPEN, "switch_ts");
        MappingKey keySw1 = new MappingKey(EquipmentVariable.OPEN, "SW1");
        MappingKey keySw2 = new MappingKey(EquipmentVariable.OPEN, "SW2");
        assertEquals(2, config.getTimeSeriesToBreakersMapping().get(keySwitchTs).size());
        assertEquals(Sets.newHashSet("SW1", "SW2"), Sets.newHashSet(config.getTimeSeriesToBreakersMapping().get(keySwitchTs)));
        // by default distribution key is 1
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keySw1));
        assertEquals(NumberDistributionKey.ONE, config.getDistributionKey(keySw2));
    }

    @Test
    void loadMappingErrorTest() {
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
            dsl.load(network, parameters, store, new DataTableStore(), null);
            fail();
        } catch (TimeSeriesMappingException e) {
            assertEquals("Load 'LD1' is mapped on p0 and on one of the detailed variables (fixedActivePower/variableActivePower)", e.getMessage());
        }
    }

    @Test
    void switchMappingErrorTest() {
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
            dsl.load(network, parameters, store, new DataTableStore(), null);
            fail();
        } catch (groovy.lang.MissingMethodException ignored) {
        }
    }

    @Test
    void tsStatsFunctions() throws IOException {
        final String expectedWithAllVersions = "1.0\n0.2\n-5.0\n3.0\n1.0\n";
        final String expectedWithoutAllVersions = "6.0\n2.0\n1.0\n3.0\n2.0\n";

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(50));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("test", index, 1d, 2d, 3d, -5d, 0d)
        );

        TimeSeriesDslLoader dslWithoutAllVersions = new TimeSeriesDslLoader(String.format(statScript, false));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dslWithoutAllVersions.load(network, parameters, store, new DataTableStore(), out, null);
        }

        String output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals(expectedWithAllVersions, output);

        outputStream.reset();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dslWithoutAllVersions.load(network, parameters, store, new DataTableStore(), out, new ComputationRange(Collections.singleton(1), 0, 3));
        }

        output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals(expectedWithoutAllVersions, output);

        TimeSeriesDslLoader dslWithAllVersions = new TimeSeriesDslLoader(String.format(statScript, true));
        outputStream.reset();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dslWithAllVersions.load(network, parameters, store, new DataTableStore(), out, new ComputationRange(Collections.singleton(1), 0, 3));
        }

        output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals(expectedWithAllVersions, output);
    }

    @Test
    void testParameters() {
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
        dsl.load(network, parameters, store, new DataTableStore(), null);

        assertEquals(0.5f, parameters.getToleranceThreshold(), 0f);
        assertTrue(parameters.getWithTimeSeriesStats());
    }

    @Test
    void writeLogTest() throws IOException {
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        String script = "writeLog(\"LOG_TYPE\", \"LOG_SECTION\", \"LOG_MESSAGE\")";

        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dsl.load(network, parameters, store, new DataTableStore(), out, null);
        }

        String output = outputStream.toString();
        String expectedMessage = "LOG_TYPE;LOG_SECTION;LOG_MESSAGE" + System.lineSeparator();
        assertEquals(expectedMessage, output);
    }

    @Test
    void metadataTest() throws IOException {
        // mapping script
        String script = String.join(System.lineSeparator(),
                "ts['one'] = 1",
                "metadata_ts = getMetadataTags(ts['test'])",
                "metadata_int = getMetadataTags(ts['one'])",
                "string_metadatas = stringMetadatas()",
                "double_metadatas = doubleMetadatas()",
                "int_metadatas = intMetadatas()",
                "boolean_metadatas = booleanMetadatas()",
                "println metadata_ts",
                "println metadata_int",
                "println string_metadatas",
                "println double_metadatas",
                "println int_metadatas",
                "println boolean_metadatas"
        );

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(50));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                new StoredDoubleTimeSeries(
                        new TimeSeriesMetadata("test", TimeSeriesDataType.DOUBLE, Map.of("tag", "value"), index),
                        new UncompressedDoubleDataChunk(0, new double[]{1d})));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            new TimeSeriesDslLoader(script).load(network, parameters, store, new DataTableStore(), out, null);
        }

        String output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals("[tag:value]\n[:]\n[:]\n[:]\n[:]\n[:]\n", output);
    }

    void simpleCalculatedTagTest(String expression) throws IOException {
        tagTest(String.format(tagScript, expression), "[calculatedTag:calculatedParam]");
    }

    void tagTest(String script, String expectedTag) throws IOException {
        Network network = MappingTestNetwork.create();

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(50));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                new StoredDoubleTimeSeries(
                        new TimeSeriesMetadata("test", TimeSeriesDataType.DOUBLE, Map.of("storedTag", "storedParam"), index),
                        new UncompressedDoubleDataChunk(0, new double[]{1d})));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            new TimeSeriesDslLoader(script).load(network, parameters, store, new DataTableStore(), out, null);
        }

        String output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals(TestUtil.normalizeLineSeparator(expectedTag + "\n"), output);
    }

    @Test
    void simpleCalculatedTagTest() throws IOException {
        // IntegerNodeCalc
        simpleCalculatedTagTest("new Integer(1)");

        // FloatNodeCalc
        simpleCalculatedTagTest("new Float(0.1)");

        // DoubleNodeCalc
        simpleCalculatedTagTest("new Double(0.1)");

        // BigDecimal
        simpleCalculatedTagTest("new BigDecimal(0.1)");

        // BinaryOperation
        simpleCalculatedTagTest("ts['test'] + 1");

        // UnaryOperation
        simpleCalculatedTagTest("- ts['test']");

        // MinNodeCalc
        simpleCalculatedTagTest("ts['one'].min(1)");

        // MaxNodeCalc
        simpleCalculatedTagTest("ts['one'].max(1)");

        // TimeNodeCalc
        simpleCalculatedTagTest("ts['one'].time()");
    }

    @Test
    void storedTagTest() throws IOException {
        String script = String.join(System.lineSeparator(),
                "metadata_test = getMetadataTags(ts['test'])",
                "println metadata_test",
                "ts['test'] = ts['test']",
                "metadata_test = getMetadataTags(ts['test'])",
                "println metadata_test",
                "tag(ts['test'], 'calculatedTag', 'calculatedParam')",
                "metadata_test = getMetadataTags(ts['test'])",
                "println metadata_test"
        );
        tagTest(script, "[storedTag:storedParam]\n[storedTag:storedParam]\n[calculatedTag:calculatedParam, storedTag:storedParam]");
    }

    @Test
    void calculatedTimeSeriesTagTest() throws IOException {
        // mapping script
        String script = String.join(System.lineSeparator(),
                "ts['calculated'] = ts['test']",
                "ts['calculated_same_as_previous_one'] = ts['test']",
                "tag(ts['calculated'], 'tag', 'param')",
                "tag(ts['calculated_same_as_previous_one'], 'tag_same_as_previous_one', 'param_same_as_previous_one')",
                "metadata_calculated = getMetadataTags(ts['calculated'])",
                "metadata_calculated_same_as_previous_one = getMetadataTags(ts['calculated_same_as_previous_one'])",
                "println metadata_calculated",
                "println metadata_calculated_same_as_previous_one"
        );
        tagTest(script, "[tag:param, storedTag:storedParam]\n[tag_same_as_previous_one:param_same_as_previous_one, storedTag:storedParam]");
    }

    @Test
    void tagOnAbsentTimeSeries() throws IOException {
        String expression = "new Integer(1)";
        String tagScriptError = String.join(System.lineSeparator(),
            "ts['one'] = 1",
            "ts['test'] = %s"
        );
        Map<String, String> newInsideTags = new HashMap<>();
        newInsideTags.put("testTag", "testParam");
        Map<String, Map<String, String>> newTags = new HashMap<>();
        newTags.put("test", newInsideTags);

        Network network = MappingTestNetwork.create();

        // mapping script
        String script = String.format(tagScriptError, expression);

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(50));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            new StoredDoubleTimeSeries(
                new TimeSeriesMetadata("test", TimeSeriesDataType.DOUBLE, Map.of("storedTag", "storedParam"), index),
                new UncompressedDoubleDataChunk(0, new double[]{1d})));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TimeSeriesMappingConfig timeSeriesMappingConfig;
        Map<String, Map<String, String>> tags;
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            timeSeriesMappingConfig = new TimeSeriesDslLoader(script).load(network, parameters, store, new DataTableStore(), out, null);

            timeSeriesMappingConfig.setTimeSeriesNodeTags(newTags);
            timeSeriesMappingConfig.addTag("testError", "calculatedTagError", "calculatedParamError");
            tags = timeSeriesMappingConfig.getTimeSeriesNodeTags();
        }

        assertTrue(tags.containsKey("test"));
        assertTrue(tags.get("test").containsKey("testTag"));
        assertEquals("testParam", tags.get("test").get("testTag"));
        assertFalse(tags.containsKey("testError"));
        assertFalse(tags.get("test").containsKey("calculatedTagError"));

    }
}
