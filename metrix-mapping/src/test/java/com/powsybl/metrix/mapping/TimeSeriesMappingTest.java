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
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.commons.observer.TimeSeriesMapperObserver;
import com.powsybl.metrix.mapping.config.ScriptLogConfig;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigCsvWriter;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigEquipmentCsvWriter;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigStatusCsvWriter;
import com.powsybl.metrix.mapping.writers.TimeSeriesMappingConfigSynthesisCsvWriter;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

import static com.powsybl.commons.test.ComparisonUtils.assertTxtEquals;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesMappingTest {

    private Network network;

    private MappingParameters mappingParameters;

    private void checkMappingConfigOutput(TimeSeriesMappingConfig mappingConfig, String directoryName,
                                          ReadOnlyTimeSeriesStore store, ComputationRange computationRange) throws Exception {

        TimeSeriesMappingConfigSynthesisCsvWriter synthesisCsvWriter = new TimeSeriesMappingConfigSynthesisCsvWriter(mappingConfig);
        TimeSeriesMappingConfigEquipmentCsvWriter equipmentCsvWriter = new TimeSeriesMappingConfigEquipmentCsvWriter(mappingConfig, network);
        TimeSeriesMappingConfigCsvWriter mappingCsvWriter = new TimeSeriesMappingConfigCsvWriter(mappingConfig, network, store, computationRange, true);

        StringWriter timeSeriesMappingSynthesisTxt = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingSynthesisTxt)) {
            synthesisCsvWriter.writeMappingSynthesis(bufferedWriter);
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "mappingSynthesis.txt")),
                new ByteArrayInputStream(timeSeriesMappingSynthesisTxt.toString().getBytes(StandardCharsets.UTF_8)));
        }

        assertScript(synthesisCsvWriter::writeMappingSynthesisCsv, directoryName, "mappingSynthesis.csv");
        assertScript(mappingCsvWriter::writeTimeSeriesToGeneratorsMapping, directoryName, "timeSeriesToGeneratorsMapping.csv");
        assertScript(mappingCsvWriter::writeTimeSeriesToLoadsMapping, directoryName, "timeSeriesToLoadsMapping.csv");
        assertScript(mappingCsvWriter::writeTimeSeriesToBoundaryLinesMapping, directoryName, "timeSeriesToBoundaryLinesMapping.csv");
        assertScript(mappingCsvWriter::writeTimeSeriesToHvdcLinesMapping, directoryName, "timeSeriesToHvdcLinesMapping.csv");
        assertScript(mappingCsvWriter::writeTimeSeriesToPstMapping, directoryName, "timeSeriesToPstMapping.csv");
        assertScript(mappingCsvWriter::writeTimeSeriesToBreakersMapping, directoryName, "timeSeriesToBreakersMapping.csv");
        assertScript(mappingCsvWriter::writeGeneratorToTimeSeriesMapping, directoryName, "generatorToTimeSeriesMapping.csv");
        assertScript(mappingCsvWriter::writeLoadToTimeSeriesMapping, directoryName, "loadToTimeSeriesMapping.csv");
        assertScript(mappingCsvWriter::writeBoundaryLineToTimeSeriesMapping, directoryName, "boundaryLineToTimeSeriesMapping.csv");
        assertScript(mappingCsvWriter::writeHvdcLineToTimeSeriesMapping, directoryName, "hvdcLineToTimeSeriesMapping.csv");
        assertScript(mappingCsvWriter::writePstToTimeSeriesMapping, directoryName, "pstToTimeSeriesMapping.csv");
        assertScript(mappingCsvWriter::writeBreakerToTimeSeriesMapping, directoryName, "breakerToTimeSeriesMapping.csv");
        assertScript(equipmentCsvWriter::writeUnmappedGenerators, directoryName, "unmappedGenerators.csv");
        assertScript(equipmentCsvWriter::writeUnmappedLoads, directoryName, "unmappedLoads.csv");
        assertScript(equipmentCsvWriter::writeUnmappedBoundaryLines, directoryName, "unmappedBoundaryLines.csv");
        assertScript(equipmentCsvWriter::writeUnmappedHvdcLines, directoryName, "unmappedHvdcLines.csv");
        assertScript(equipmentCsvWriter::writeUnmappedPst, directoryName, "unmappedPsts.csv");
        assertScript(equipmentCsvWriter::writeDisconnectedGenerators, directoryName, "disconnectedGenerators.csv");
        assertScript(equipmentCsvWriter::writeDisconnectedLoads, directoryName, "disconnectedLoads.csv");
        assertScript(equipmentCsvWriter::writeDisconnectedBoundaryLines, directoryName, "disconnectedBoundaryLines.csv");
        assertScript(equipmentCsvWriter::writeIgnoredUnmappedGenerators, directoryName, "ignoredUnmappedGenerators.csv");
        assertScript(equipmentCsvWriter::writeIgnoredUnmappedLoads, directoryName, "ignoredUnmappedLoads.csv");
        assertScript(equipmentCsvWriter::writeIgnoredUnmappedBoundaryLines, directoryName, "ignoredUnmappedBoundaryLines.csv");
        assertScript(equipmentCsvWriter::writeIgnoredUnmappedHvdcLines, directoryName, "ignoredUnmappedHvdcLines.csv");
        assertScript(equipmentCsvWriter::writeIgnoredUnmappedPst, directoryName, "ignoredUnmappedPsts.csv");
        assertScript(equipmentCsvWriter::writeOutOfMainCCGenerators, directoryName, "outOfMainCcGenerators.csv");
        assertScript(equipmentCsvWriter::writeOutOfMainCCLoads, directoryName, "outOfMainCcLoads.csv");
        assertScript(equipmentCsvWriter::writeOutOfMainCCBoundaryLines, directoryName, "outOfMainCcBoundaryLines.csv");
        assertScript(mappingCsvWriter::writeMappedTimeSeries, directoryName, "mappedTimeSeries.csv");
    }

    private void assertScript(Consumer<BufferedWriter> consumer, String directoryName, String fileName) throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(stringWriter)) {
            consumer.accept(bufferedWriter);
            bufferedWriter.flush();
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + fileName)),
                new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8)));
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
            assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "status.csv")),
                new ByteArrayInputStream(timeSeriesMappingStatusWriter.toString().getBytes(StandardCharsets.UTF_8)));
        }

        // Check mapping output
        assertTxtEquals(Objects.requireNonNull(getClass().getResourceAsStream(directoryName + "output.txt")),
            new ByteArrayInputStream(output.toString().getBytes(StandardCharsets.UTF_8)));

        // Check mapping config output
        checkMappingConfigOutput(mappingConfig, directoryName, store, computationRange);
    }
}
