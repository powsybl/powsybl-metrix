/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static com.powsybl.metrix.mapping.AbstractCompareTxt.compareStreamTxt;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            assertNotNull(compareStreamTxt(timeSeriesMappingSynthesisTxt.toString().getBytes(StandardCharsets.UTF_8), directoryName, "mappingSynthesis.txt"));
        }

        StringWriter timeSeriesMappingSynthesis = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingSynthesis)) {
            synthesisCsvWriter.writeMappingSynthesisCsv(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(timeSeriesMappingSynthesis.toString().getBytes(StandardCharsets.UTF_8), directoryName, "mappingSynthesis.csv"));
        }

        StringWriter timeSeriesToGeneratorsMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToGeneratorsMapping)) {
            mappingCsvWriter.writeTimeSeriesToGeneratorsMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(timeSeriesToGeneratorsMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "timeSeriesToGeneratorsMapping.csv"));
        }

        StringWriter timeSeriesToLoadsMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToLoadsMapping)) {
            mappingCsvWriter.writeTimeSeriesToLoadsMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(timeSeriesToLoadsMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "timeSeriesToLoadsMapping.csv"));
        }

        StringWriter timeSeriesToBoundaryLinesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToBoundaryLinesMapping)) {
            mappingCsvWriter.writeTimeSeriesToBoundaryLinesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(timeSeriesToBoundaryLinesMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "timeSeriesToBoundaryLinesMapping.csv"));

        }

        StringWriter timeSeriesToHvdcLinesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToHvdcLinesMapping)) {
            mappingCsvWriter.writeTimeSeriesToHvdcLinesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(timeSeriesToHvdcLinesMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "timeSeriesToHvdcLinesMapping.csv"));
        }

        StringWriter timeSeriesToPstMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToPstMapping)) {
            mappingCsvWriter.writeTimeSeriesToPstMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(timeSeriesToPstMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "timeSeriesToPstMapping.csv"));
        }

        StringWriter timeSeriesToBreakersMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToBreakersMapping)) {
            mappingCsvWriter.writeTimeSeriesToBreakersMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(timeSeriesToBreakersMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "timeSeriesToBreakersMapping.csv"));

        }

        StringWriter generatorToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(generatorToTimeSeriesMapping)) {
            mappingCsvWriter.writeGeneratorToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(generatorToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "generatorToTimeSeriesMapping.csv"));
        }

        StringWriter loadToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(loadToTimeSeriesMapping)) {
            mappingCsvWriter.writeLoadToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(loadToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "loadToTimeSeriesMapping.csv"));
        }

        StringWriter boundaryLineToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(boundaryLineToTimeSeriesMapping)) {
            mappingCsvWriter.writeBoundaryLineToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(boundaryLineToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "boundaryLineToTimeSeriesMapping.csv"));
        }

        StringWriter hvdcLineToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(hvdcLineToTimeSeriesMapping)) {
            mappingCsvWriter.writeHvdcLineToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(hvdcLineToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "hvdcLineToTimeSeriesMapping.csv"));
        }

        StringWriter pstToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(pstToTimeSeriesMapping)) {
            mappingCsvWriter.writePstToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(pstToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "pstToTimeSeriesMapping.csv"));
        }

        StringWriter breakerToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(breakerToTimeSeriesMapping)) {
            mappingCsvWriter.writeBreakerToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(breakerToTimeSeriesMapping.toString().getBytes(StandardCharsets.UTF_8), directoryName, "breakerToTimeSeriesMapping.csv"));
        }

        StringWriter unmappedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedGenerators)) {
            equipmentCsvWriter.writeUnmappedGenerators(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(unmappedGenerators.toString().getBytes(StandardCharsets.UTF_8), directoryName, "unmappedGenerators.csv"));
        }

        StringWriter unmappedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedLoads)) {
            equipmentCsvWriter.writeUnmappedLoads(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(unmappedLoads.toString().getBytes(StandardCharsets.UTF_8), directoryName, "unmappedLoads.csv"));
        }

        StringWriter unmappedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedBoundaryLines)) {
            equipmentCsvWriter.writeUnmappedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(unmappedBoundaryLines.toString().getBytes(StandardCharsets.UTF_8), directoryName, "unmappedBoundaryLines.csv"));
        }

        StringWriter unmappedHvdcLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedHvdcLines)) {
            equipmentCsvWriter.writeUnmappedHvdcLines(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(unmappedHvdcLines.toString().getBytes(StandardCharsets.UTF_8), directoryName, "unmappedHvdcLines.csv"));
        }

        StringWriter unmappedPst = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedPst)) {
            equipmentCsvWriter.writeUnmappedPst(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(unmappedPst.toString().getBytes(StandardCharsets.UTF_8), directoryName, "unmappedPsts.csv"));
        }

        StringWriter disconnectedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedGenerators)) {
            equipmentCsvWriter.writeDisconnectedGenerators(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(disconnectedGenerators.toString().getBytes(StandardCharsets.UTF_8), directoryName, "disconnectedGenerators.csv"));
        }

        StringWriter disconnectedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedLoads)) {
            equipmentCsvWriter.writeDisconnectedLoads(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(disconnectedLoads.toString().getBytes(StandardCharsets.UTF_8), directoryName, "disconnectedLoads.csv"));
        }

        StringWriter disconnectedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedBoundaryLines)) {
            equipmentCsvWriter.writeDisconnectedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(disconnectedBoundaryLines.toString().getBytes(StandardCharsets.UTF_8), directoryName, "disconnectedBoundaryLines.csv"));
        }

        StringWriter ignoredUnmappedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedGenerators)) {
            equipmentCsvWriter.writeIgnoredUnmappedGenerators(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(ignoredUnmappedGenerators.toString().getBytes(StandardCharsets.UTF_8), directoryName, "ignoredUnmappedGenerators.csv"));
        }

        StringWriter ignoredUnmappedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedLoads)) {
            equipmentCsvWriter.writeIgnoredUnmappedLoads(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(ignoredUnmappedLoads.toString().getBytes(StandardCharsets.UTF_8), directoryName, "ignoredUnmappedLoads.csv"));
        }

        StringWriter ignoredUnmappedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedBoundaryLines)) {
            equipmentCsvWriter.writeIgnoredUnmappedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(ignoredUnmappedBoundaryLines.toString().getBytes(StandardCharsets.UTF_8), directoryName, "ignoredUnmappedBoundaryLines.csv"));
        }

        StringWriter ignoredUnmappedHvdcLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedHvdcLines)) {
            equipmentCsvWriter.writeIgnoredUnmappedHvdcLines(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(ignoredUnmappedHvdcLines.toString().getBytes(StandardCharsets.UTF_8), directoryName, "ignoredUnmappedHvdcLines.csv"));
        }

        StringWriter ignoredUnmappedPst = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedPst)) {
            equipmentCsvWriter.writeIgnoredUnmappedPst(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(ignoredUnmappedPst.toString().getBytes(StandardCharsets.UTF_8), directoryName, "ignoredUnmappedPsts.csv"));
        }

        StringWriter outOfMainCcGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcGenerators)) {
            equipmentCsvWriter.writeOutOfMainCCGenerators(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(outOfMainCcGenerators.toString().getBytes(StandardCharsets.UTF_8), directoryName, "outOfMainCcGenerators.csv"));
        }

        StringWriter outOfMainCcLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcLoads)) {
            equipmentCsvWriter.writeOutOfMainCCLoads(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(outOfMainCcLoads.toString().getBytes(StandardCharsets.UTF_8), directoryName, "outOfMainCcLoads.csv"));
        }

        StringWriter outOfMainCcBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcBoundaryLines)) {
            equipmentCsvWriter.writeOutOfMainCCBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(outOfMainCcBoundaryLines.toString().getBytes(StandardCharsets.UTF_8), directoryName, "outOfMainCcBoundaryLines.csv"));
        }

        StringWriter mappedTimeSeries = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(mappedTimeSeries)) {
            mappingCsvWriter.writeMappedTimeSeries(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(mappedTimeSeries.toString().getBytes(StandardCharsets.UTF_8), directoryName, "mappedTimeSeries.csv"));
        }
    }

    @BeforeEach
    public void setUp() throws IOException {
        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

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
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, output, null);

        // Create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 1), false, false, true, mappingParameters.getToleranceThreshold());
        ComputationRange computationRange = new ComputationRange(ImmutableSet.of(1), 0, 1);

        // Create observers
        List<TimeSeriesMapperObserver> observers = new ArrayList<>(2);

        StringWriter equipmentTimeSeriesWriter = new StringWriter();
        EquipmentTimeSeriesWriter equipmentTimeSeriesBufferedWriter = new EquipmentTimeSeriesWriter(new BufferedWriter(equipmentTimeSeriesWriter));
        observers.add(equipmentTimeSeriesBufferedWriter);

        // Launch TimeSeriesMapper test
        mapper.mapToNetwork(store, parameters, observers);

        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        // Check time series mapping status
        StringWriter timeSeriesMappingStatusWriter = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingStatusWriter)) {
            new TimeSeriesMappingConfigStatusCsvWriter(mappingConfig, store).writeTimeSeriesMappingStatus(bufferedWriter);
            bufferedWriter.flush();
            assertNotNull(compareStreamTxt(timeSeriesMappingStatusWriter.toString().getBytes(StandardCharsets.UTF_8), directoryName, "status.csv"));
        }

        // Check equipment time series output
        assertNotNull(compareStreamTxt(equipmentTimeSeriesWriter.toString().getBytes(StandardCharsets.UTF_8), directoryName, "version_1.csv"));

        // Check mapping output
        assertNotNull(compareStreamTxt(output.toString().getBytes(StandardCharsets.UTF_8), directoryName, "output.txt"));

        // Check mapping config output
        checkMappingConfigOutput(mappingConfig, directoryName, store, computationRange);
    }
}
