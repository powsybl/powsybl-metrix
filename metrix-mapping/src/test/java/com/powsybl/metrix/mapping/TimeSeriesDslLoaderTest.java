/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.commons.test.TestUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.commons.ComputationRange;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.config.ScriptLogConfig;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException;
import com.powsybl.metrix.mapping.references.DistributionKey;
import com.powsybl.metrix.mapping.references.MappingKey;
import com.powsybl.metrix.mapping.references.NumberDistributionKey;
import com.powsybl.metrix.mapping.utils.MappingTestNetwork;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigSynthesisCsvWriter;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.StoredDoubleTimeSeries;
import com.powsybl.timeseries.StringTimeSeries;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesDataType;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesMetadata;
import com.powsybl.timeseries.UncompressedDoubleDataChunk;
import com.powsybl.timeseries.ast.IntegerNodeCalc;
import groovy.lang.MissingMethodException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            TimeSeriesMappingConfig config = new TimeSeriesDslLoader(reader).load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new ScriptLogConfig(), null);
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
            @Override
            public Optional<StringTimeSeries> getStringTimeSeries(String timeSeriesName, int version) {
                return Optional.of(TimeSeries.createString("multiple_ouverture_id", index, "1", "G1,twt,L1"));
            }
        };

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig config = dsl.load(network, parameters, store, new DataTableStore(), null);
        TimeSeriesMappingConfigSynthesisCsvWriter csvWriter = new TimeSeriesMappingConfigSynthesisCsvWriter(config);
        csvWriter.printMappingSynthesis(System.out, new TableFormatterConfig());

        // Compare to the expected TimeSeriesMappingConfig
        TimeSeriesMappingConfig expectedConfig = new TimeSeriesMappingConfig();
        Map<MappingKey, List<String>> timeSeriesToGeneratorsMapping = new HashMap<>();
        timeSeriesToGeneratorsMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "multiple_ouverture_id_G1"), List.of("G1"));
        timeSeriesToGeneratorsMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "zero"), List.of("G4"));
        timeSeriesToGeneratorsMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "nucl_ts"), List.of("G1", "G2"));
        timeSeriesToGeneratorsMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "hydro_ts"), List.of("G3"));
        expectedConfig.setTimeSeriesToGeneratorsMapping(timeSeriesToGeneratorsMapping);
        Map<MappingKey, List<String>> timeSeriesToLoadsMapping = new HashMap<>();
        timeSeriesToLoadsMapping.put(new MappingKey(EquipmentVariable.P0, "load1_ts"), List.of("LD1"));
        timeSeriesToLoadsMapping.put(new MappingKey(EquipmentVariable.P0, "load2_ts"), List.of("LD2", "LD3"));
        expectedConfig.setTimeSeriesToLoadsMapping(timeSeriesToLoadsMapping);
        Map<MappingKey, List<String>> timeSeriesToPhaseTapChangersMapping = new HashMap<>();
        timeSeriesToPhaseTapChangersMapping.put(new MappingKey(EquipmentVariable.PHASE_TAP_POSITION, "switch_ts"), List.of("twt"));
        expectedConfig.setTimeSeriesToPhaseTapChangersMapping(timeSeriesToPhaseTapChangersMapping);
        Map<MappingKey, List<String>> timeSeriesToBreakersMapping = new HashMap<>();
        timeSeriesToBreakersMapping.put(new MappingKey(EquipmentVariable.OPEN, "switch_ts"), List.of("SW1", "SW2"));
        expectedConfig.setTimeSeriesToBreakersMapping(timeSeriesToBreakersMapping);
        Map<MappingKey, List<String>> timeSeriesToTransformersMapping = new HashMap<>();
        timeSeriesToTransformersMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "multiple_ouverture_id_twt"), List.of("twt"));
        expectedConfig.setTimeSeriesToTransformersMapping(timeSeriesToTransformersMapping);
        Map<MappingKey, List<String>> timeSeriesToLinesMapping = new HashMap<>();
        timeSeriesToLinesMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "multiple_ouverture_id_L1"), List.of("L1"));
        expectedConfig.setTimeSeriesToLinesMapping(timeSeriesToLinesMapping);
        Map<MappingKey, List<String>> generatorToTimeSeriesMapping = new HashMap<>();
        generatorToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "G1"), List.of("multiple_ouverture_id_G1"));
        generatorToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "G1"), List.of("nucl_ts", "zero"));
        generatorToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "G2"), List.of("nucl_ts", "zero"));
        generatorToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "G3"), List.of("hydro_ts", "zero"));
        generatorToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "G4"), List.of("zero"));
        expectedConfig.setGeneratorToTimeSeriesMapping(generatorToTimeSeriesMapping);
        Map<MappingKey, List<String>> loadToTimeSeriesMapping = new HashMap<>();
        loadToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.P0, "LD1"), List.of("load1_ts"));
        loadToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.P0, "LD2"), List.of("load2_ts"));
        loadToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.P0, "LD3"), List.of("load2_ts"));
        expectedConfig.setLoadToTimeSeriesMapping(loadToTimeSeriesMapping);
        Map<MappingKey, List<String>> phaseTapChangerToTimeSeriesMapping = new HashMap<>();
        phaseTapChangerToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.PHASE_TAP_POSITION, "twt"), List.of("switch_ts"));
        expectedConfig.setPhaseTapChangerToTimeSeriesMapping(phaseTapChangerToTimeSeriesMapping);
        Map<MappingKey, List<String>> breakerToTimeSeriesMapping = new HashMap<>();
        breakerToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.OPEN, "SW1"), List.of("switch_ts"));
        breakerToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.OPEN, "SW2"), List.of("switch_ts"));
        expectedConfig.setBreakerToTimeSeriesMapping(breakerToTimeSeriesMapping);
        Map<MappingKey, List<String>> transformerToTimeSeriesMapping = new HashMap<>();
        transformerToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "twt"), List.of("multiple_ouverture_id_twt"));
        expectedConfig.setTransformerToTimeSeriesMapping(transformerToTimeSeriesMapping);
        Map<MappingKey, List<String>> lineToTimeSeriesMapping = new HashMap<>();
        lineToTimeSeriesMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "L1"), List.of("multiple_ouverture_id_L1"));
        expectedConfig.setLineToTimeSeriesMapping(lineToTimeSeriesMapping);
        Set<String> generatorSet = ImmutableSet.of("G1", "G2", "G3", "G4");
        expectedConfig.setUnmappedMinPGenerators(generatorSet);
        expectedConfig.setUnmappedMaxPGenerators(generatorSet);
        Map<String, Set<String>> timeSeriesToPlannedOutagesMappingExpected = Map.of("multiple_ouverture_id", Set.of("1", "G1", "twt", "L1"));
        expectedConfig.setTimeSeriesToPlannedOutagesMapping(timeSeriesToPlannedOutagesMappingExpected);
        expectedConfig.setTimeSeriesNodes(Map.of("zero", new IntegerNodeCalc(0)));
        expectedConfig.setMappedTimeSeriesNames(Set.of("zero", "nucl_ts", "hydro_ts", "load1_ts", "load2_ts", "switch_ts", "multiple_ouverture_id_G1", "multiple_ouverture_id_L1", "multiple_ouverture_id_twt"));
        Map<MappingKey, DistributionKey> distributionKeyMapping = getMappingKeyDistributionKeyMap();
        expectedConfig.setDistributionKeys(distributionKeyMapping);
        assertEquals(expectedConfig, config);
    }

    private static Map<MappingKey, DistributionKey> getMappingKeyDistributionKeyMap() {
        DistributionKey distributionKey = new NumberDistributionKey(1.0);
        Map<MappingKey, DistributionKey> distributionKeyMapping = new HashMap<>();
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "twt"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "G1"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.DISCONNECTED, "L1"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.OPEN, "SW1"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.OPEN, "SW2"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "G1"), new NumberDistributionKey(1000.0));
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "G2"), new NumberDistributionKey(500.0));
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "G3"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.TARGET_P, "G4"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.P0, "LD1"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.P0, "LD2"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.P0, "LD3"), distributionKey);
        distributionKeyMapping.put(new MappingKey(EquipmentVariable.PHASE_TAP_POSITION, "twt"), distributionKey);
        return distributionKeyMapping;
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
        DataTableStore dataTableStore = new DataTableStore();
        TimeSeriesMappingException exception = assertThrows(TimeSeriesMappingException.class, () -> dsl.load(network, parameters, store, dataTableStore, null));
        assertEquals("Load 'LD1' is mapped on p0 and on one of the detailed variables (fixedActivePower/variableActivePower)", exception.getMessage());
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
        DataTableStore dataTableStore = new DataTableStore();
        assertThrows(MissingMethodException.class, () -> dsl.load(network, parameters, store, dataTableStore, null));
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
            dslWithoutAllVersions.load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(out), null);
        }

        String output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals(expectedWithAllVersions, output);

        outputStream.reset();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dslWithoutAllVersions.load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(out), new ComputationRange(Collections.singleton(1), 0, 3));
        }

        output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals(expectedWithoutAllVersions, output);

        TimeSeriesDslLoader dslWithAllVersions = new TimeSeriesDslLoader(String.format(statScript, true));
        outputStream.reset();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dslWithAllVersions.load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(out), new ComputationRange(Collections.singleton(1), 0, 3));
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
        String script = """
            writeLog("LOG_TYPE", "LOG_SECTION", "LOG_MESSAGE")
            """;

        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dsl.load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(out), null);
        }

        String output = outputStream.toString();
        String expectedMessage = "LOG_TYPE;LOG_SECTION;LOG_MESSAGE" + System.lineSeparator();
        assertEquals(expectedMessage, output);
    }

    @ParameterizedTest
    @MethodSource("provideMessageByLogLevel")
    void writeLogTestFilterByMaxLogLevel(System.Logger.Level maxLogLevel, String expectedMessage) throws IOException {
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        String script = """
            writeLog("TRACE",   "LOG_SECTION", "LOG_MESSAGE")
            writeLog("DEBUG",   "LOG_SECTION", "LOG_MESSAGE")
            writeLog("INFO",    "LOG_SECTION", "LOG_MESSAGE")
            writeLog("WARNING", "LOG_SECTION", "LOG_MESSAGE")
            writeLog("ERROR",   "LOG_SECTION", "LOG_MESSAGE")
            """;

        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            dsl.load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(maxLogLevel, out), null);
        }
        String output = outputStream.toString();
        String expectedMessageWithLineSeparator = expectedMessage.replaceAll("\n", System.lineSeparator());
        assertEquals(expectedMessageWithLineSeparator, output, "MaxLogLevel : " + maxLogLevel.getName() + " -> expected message : " + expectedMessageWithLineSeparator + " but was : " + output);
    }

    private static Stream<Arguments> provideMessageByLogLevel() {
        return Stream.of(
            Arguments.of(System.Logger.Level.ALL, """
            TRACE;LOG_SECTION;LOG_MESSAGE
            DEBUG;LOG_SECTION;LOG_MESSAGE
            INFO;LOG_SECTION;LOG_MESSAGE
            WARNING;LOG_SECTION;LOG_MESSAGE
            ERROR;LOG_SECTION;LOG_MESSAGE
            """),
            Arguments.of(System.Logger.Level.TRACE, """
            TRACE;LOG_SECTION;LOG_MESSAGE
            DEBUG;LOG_SECTION;LOG_MESSAGE
            INFO;LOG_SECTION;LOG_MESSAGE
            WARNING;LOG_SECTION;LOG_MESSAGE
            ERROR;LOG_SECTION;LOG_MESSAGE
            """),
            Arguments.of(System.Logger.Level.DEBUG, """
            DEBUG;LOG_SECTION;LOG_MESSAGE
            INFO;LOG_SECTION;LOG_MESSAGE
            WARNING;LOG_SECTION;LOG_MESSAGE
            ERROR;LOG_SECTION;LOG_MESSAGE
            """),
            Arguments.of(System.Logger.Level.INFO, """
            INFO;LOG_SECTION;LOG_MESSAGE
            WARNING;LOG_SECTION;LOG_MESSAGE
            ERROR;LOG_SECTION;LOG_MESSAGE
            """),
            Arguments.of(System.Logger.Level.WARNING, """
            WARNING;LOG_SECTION;LOG_MESSAGE
            ERROR;LOG_SECTION;LOG_MESSAGE
            """),
            Arguments.of(System.Logger.Level.ERROR, """
            ERROR;LOG_SECTION;LOG_MESSAGE
            """),
            Arguments.of(System.Logger.Level.OFF, "")
        );
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
            new TimeSeriesDslLoader(script).load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(out), null);
        }

        String output = TestUtil.normalizeLineSeparator(outputStream.toString());
        assertEquals("[tag:value]\n[:]\n[:]\n[:]\n[:]\n[:]\n", output);
    }

    void simpleCalculatedTagTest(String expression) throws IOException {
        tagTest(String.format(tagScript, expression), "[calculatedTag:calculatedParam]");
    }

    void tagTest(String script, String expectedTag) throws IOException {
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-07-20T00:00:00Z"), Duration.ofDays(50));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                new StoredDoubleTimeSeries(
                        new TimeSeriesMetadata("test", TimeSeriesDataType.DOUBLE, Map.of("storedTag", "storedParam"), index),
                        new UncompressedDoubleDataChunk(0, new double[]{1d})));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            new TimeSeriesDslLoader(script).load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(out), null);
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
            timeSeriesMappingConfig = new TimeSeriesDslLoader(script).load(network, parameters, store, new DataTableStore(), new ScriptLogConfig(out), null);

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
