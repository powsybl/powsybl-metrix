/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.commons.ComputationRange;
import com.powsybl.metrix.commons.observer.TimeSeriesMapperObserver;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.config.ScriptLogConfig;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigCsvWriter;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigEquipmentCsvWriter;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigStatusCsvWriter;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigSynthesisCsvWriter;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import static com.powsybl.commons.test.ComparisonUtils.assertTxtEquals;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesMappingTest {

    private Network network;

    private MappingParameters mappingParameters;

    private void checkMappingConfigOutput(TimeSeriesMappingConfig mappingConfig, String directoryName, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) throws Exception {

        TimeSeriesMappingConfigSynthesisCsvWriter synthesisCsvWriter = new TimeSeriesMappingConfigSynthesisCsvWriter(mappingConfig);
        TimeSeriesMappingConfigEquipmentCsvWriter equipmentCsvWriter = new TimeSeriesMappingConfigEquipmentCsvWriter(mappingConfig, network);
        TimeSeriesMappingConfigCsvWriter mappingCsvWriter = new TimeSeriesMappingConfigCsvWriter(mappingConfig, network, store, computationRange, true);

        StringWriter timeSeriesMappingSynthesisTxt = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingSynthesisTxt)) {
            synthesisCsvWriter.writeMappingSynthesis(bufferedWriter);
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "mappingSynthesis.txt")), new ByteArrayInputStream(timeSeriesMappingSynthesisTxt.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter timeSeriesMappingSynthesis = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingSynthesis)) {
            synthesisCsvWriter.writeMappingSynthesisCsv(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "mappingSynthesis.csv")), new ByteArrayInputStream(timeSeriesMappingSynthesis.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter timeSeriesToGeneratorsMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToGeneratorsMapping)) {
            mappingCsvWriter.writeTimeSeriesToGeneratorsMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "timeSeriesToGeneratorsMapping.csv")), new ByteArrayInputStream(timeSeriesToGeneratorsMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter timeSeriesToLoadsMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToLoadsMapping)) {
            mappingCsvWriter.writeTimeSeriesToLoadsMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "timeSeriesToLoadsMapping.csv")), new ByteArrayInputStream(timeSeriesToLoadsMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter timeSeriesToBoundaryLinesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToBoundaryLinesMapping)) {
            mappingCsvWriter.writeTimeSeriesToBoundaryLinesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "timeSeriesToBoundaryLinesMapping.csv")), new ByteArrayInputStream(timeSeriesToBoundaryLinesMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter timeSeriesToHvdcLinesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToHvdcLinesMapping)) {
            mappingCsvWriter.writeTimeSeriesToHvdcLinesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "timeSeriesToHvdcLinesMapping.csv")), new ByteArrayInputStream(timeSeriesToHvdcLinesMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter timeSeriesToPstMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToPstMapping)) {
            mappingCsvWriter.writeTimeSeriesToPstMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "timeSeriesToPstMapping.csv")), new ByteArrayInputStream(timeSeriesToPstMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter timeSeriesToBreakersMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToBreakersMapping)) {
            mappingCsvWriter.writeTimeSeriesToBreakersMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "timeSeriesToBreakersMapping.csv")), new ByteArrayInputStream(timeSeriesToBreakersMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter generatorToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(generatorToTimeSeriesMapping)) {
            mappingCsvWriter.writeGeneratorToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "generatorToTimeSeriesMapping.csv")), new ByteArrayInputStream(generatorToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter loadToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(loadToTimeSeriesMapping)) {
            mappingCsvWriter.writeLoadToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "loadToTimeSeriesMapping.csv")), new ByteArrayInputStream(loadToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter boundaryLineToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(boundaryLineToTimeSeriesMapping)) {
            mappingCsvWriter.writeBoundaryLineToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "boundaryLineToTimeSeriesMapping.csv")), new ByteArrayInputStream(boundaryLineToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter hvdcLineToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(hvdcLineToTimeSeriesMapping)) {
            mappingCsvWriter.writeHvdcLineToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "hvdcLineToTimeSeriesMapping.csv")), new ByteArrayInputStream(hvdcLineToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter pstToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(pstToTimeSeriesMapping)) {
            mappingCsvWriter.writePstToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "pstToTimeSeriesMapping.csv")), new ByteArrayInputStream(pstToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter breakerToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(breakerToTimeSeriesMapping)) {
            mappingCsvWriter.writeBreakerToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "breakerToTimeSeriesMapping.csv")), new ByteArrayInputStream(breakerToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter unmappedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedGenerators)) {
            equipmentCsvWriter.writeUnmappedGenerators(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "unmappedGenerators.csv")), new ByteArrayInputStream(unmappedGenerators.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter unmappedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedLoads)) {
            equipmentCsvWriter.writeUnmappedLoads(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "unmappedLoads.csv")), new ByteArrayInputStream(unmappedLoads.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter unmappedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedBoundaryLines)) {
            equipmentCsvWriter.writeUnmappedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "unmappedBoundaryLines.csv")), new ByteArrayInputStream(unmappedBoundaryLines.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter unmappedHvdcLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedHvdcLines)) {
            equipmentCsvWriter.writeUnmappedHvdcLines(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "unmappedHvdcLines.csv")), new ByteArrayInputStream(unmappedHvdcLines.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter unmappedPst = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedPst)) {
            equipmentCsvWriter.writeUnmappedPst(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "unmappedPsts.csv")), new ByteArrayInputStream(unmappedPst.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter disconnectedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedGenerators)) {
            equipmentCsvWriter.writeDisconnectedGenerators(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "disconnectedGenerators.csv")), new ByteArrayInputStream(disconnectedGenerators.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter disconnectedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedLoads)) {
            equipmentCsvWriter.writeDisconnectedLoads(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "disconnectedLoads.csv")), new ByteArrayInputStream(disconnectedLoads.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter disconnectedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedBoundaryLines)) {
            equipmentCsvWriter.writeDisconnectedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "disconnectedBoundaryLines.csv")), new ByteArrayInputStream(disconnectedBoundaryLines.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter ignoredUnmappedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedGenerators)) {
            equipmentCsvWriter.writeIgnoredUnmappedGenerators(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "ignoredUnmappedGenerators.csv")), new ByteArrayInputStream(ignoredUnmappedGenerators.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter ignoredUnmappedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedLoads)) {
            equipmentCsvWriter.writeIgnoredUnmappedLoads(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "ignoredUnmappedLoads.csv")), new ByteArrayInputStream(ignoredUnmappedLoads.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter ignoredUnmappedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedBoundaryLines)) {
            equipmentCsvWriter.writeIgnoredUnmappedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "ignoredUnmappedBoundaryLines.csv")), new ByteArrayInputStream(ignoredUnmappedBoundaryLines.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter ignoredUnmappedHvdcLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedHvdcLines)) {
            equipmentCsvWriter.writeIgnoredUnmappedHvdcLines(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "ignoredUnmappedHvdcLines.csv")), new ByteArrayInputStream(ignoredUnmappedHvdcLines.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter ignoredUnmappedPst = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedPst)) {
            equipmentCsvWriter.writeIgnoredUnmappedPst(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "ignoredUnmappedPsts.csv")), new ByteArrayInputStream(ignoredUnmappedPst.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter outOfMainCcGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcGenerators)) {
            equipmentCsvWriter.writeOutOfMainCCGenerators(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "outOfMainCcGenerators.csv")), new ByteArrayInputStream(outOfMainCcGenerators.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter outOfMainCcLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcLoads)) {
            equipmentCsvWriter.writeOutOfMainCCLoads(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "outOfMainCcLoads.csv")), new ByteArrayInputStream(outOfMainCcLoads.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter outOfMainCcBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcBoundaryLines)) {
            equipmentCsvWriter.writeOutOfMainCCBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "outOfMainCcBoundaryLines.csv")), new ByteArrayInputStream(outOfMainCcBoundaryLines.toString().getBytes(StandardCharsets.UTF_8)));
        }

        StringWriter mappedTimeSeries = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(mappedTimeSeries)) {
            mappingCsvWriter.writeMappedTimeSeries(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "mappedTimeSeries.csv")), new ByteArrayInputStream(mappedTimeSeries.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    @BeforeEach
    void setUp() {
        // create test network
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        // create mapping parameters
        mappingParameters = MappingParameters.load();
        mappingParameters.setWithTimeSeriesStats(true);
    }

    @Test
    void mapToGeneratorTest() throws Exception {

        String directoryName = "/expected/";

        // Mapping script
        String script = String.join(System.lineSeparator(),
                "println(\"mappingGenerators FSSV on Pmax\")",
                "mapToGenerators {",
                "    timeSeriesName 'Pmax'",
                "    variable maxP",
                "    filter {generator.id == 'FSSV.O11_G' || generator.id == 'FSSV.O12_G'}",
                "}",
                "println(\"mappingGenerators FSSV on Pmin\")",
                "mapToGenerators {",
                "    timeSeriesName 'Pmin'",
                "    variable minP",
                "    filter {generator.id == 'FSSV.O11_G' || generator.id == 'FSSV.O12_G'}",
                "}",
                "println(\"set FVALDI11_G unmapped\")",
                "unmappedGenerators {",
                "    filter {generator.id == 'FVALDI11_G'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'targetP_12_13'",
                "    filter {generator.id == 'FVALDI12_G' || generator.id == 'FVALDI13_G'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'targetP_13'",
                "    filter {generator.id == 'FVALDI13_G'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'targetP_14_ts1'",
                "    filter {generator.id == 'FVALDI14_G'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'targetP_14_ts2'",
                "    filter {generator.id == 'FVALDI14_G'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'targetP_14_ts3'",
                "    filter {generator.id == 'FVALDI14_G'}",
                "}"
                );

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("Pmin", index, 10d, 20d),
                TimeSeries.createDouble("Pmax", index, 1000d, 2000d),
                TimeSeries.createDouble("targetP_12_13", index, 500d, 600d),
                TimeSeries.createDouble("targetP_13", index, 700d, 800d),
                TimeSeries.createDouble("targetP_14_ts1", index, 0d, 0d),
                TimeSeries.createDouble("targetP_14_ts2", index, 0d, 0d),
                TimeSeries.createDouble("targetP_14_ts3", index, 0d, 0d)
        );

        // Load mapping script
        StringWriter output = new StringWriter();
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), new ScriptLogConfig(output), null);

        // Create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 1), false, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);
        ComputationRange computationRange = new ComputationRange(ImmutableSet.of(1), 0, 1);

        // Create observers
        List<TimeSeriesMapperObserver> observers = new ArrayList<>(2);

        // Launch TimeSeriesMapper test
        mapper.mapToNetwork(store, observers);

        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        // Check time series mapping status
        StringWriter timeSeriesMappingStatusWriter = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingStatusWriter)) {
            new TimeSeriesMappingConfigStatusCsvWriter(mappingConfig, store).writeTimeSeriesMappingStatus(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "status.csv")), new ByteArrayInputStream(timeSeriesMappingStatusWriter.toString().getBytes(StandardCharsets.UTF_8)));
        }

        // Check mapping output
        assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "output.txt")), new ByteArrayInputStream(output.toString().getBytes(StandardCharsets.UTF_8)));

        // Check mapping config output
        checkMappingConfigOutput(mappingConfig, directoryName, store, computationRange);
    }
}
