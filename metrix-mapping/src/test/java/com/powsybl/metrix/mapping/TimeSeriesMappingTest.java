package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.Before;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeSeriesMappingTest extends AbstractConverterTest {

    private static final char SEPARATOR = ';';

    private Network network;

    private MappingParameters mappingParameters;

    private void compareTxt(StringWriter writer, String directoryName, String fileName) throws Exception {
        try (InputStream expected = getClass().getResourceAsStream(directoryName + fileName)) {
            try (InputStream actual = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8))) {
                compareTxt(expected, actual);
            }
        }
    }

    private void checkMappingConfigOutput(TimeSeriesMappingConfig mappingConfig, String directoryName, ReadOnlyTimeSeriesStore store, ComputationRange computationRange) throws Exception {

        TimeSeriesMappingConfigCsvWriter mappingConfigCsvWriter = new TimeSeriesMappingConfigCsvWriter(mappingConfig, network);

        Path resultDir = tmpDir.resolve("csvmapping");
        mappingConfigCsvWriter.writeMappingCsv(resultDir, store, computationRange, mappingParameters);
        List<String> outputFiles = Files.list(resultDir).map(Path::toString).collect(Collectors.toList());
        assertThat(outputFiles).containsExactlyInAnyOrder(
            "tmp/csvmapping/boundaryLineToTimeSeriesMapping.csv",
            "tmp/csvmapping/breakerToTimeSeriesMapping.csv",
            "tmp/csvmapping/disconnectedBoundaryLines.csv",
            "tmp/csvmapping/disconnectedGenerators.csv",
            "tmp/csvmapping/disconnectedLoads.csv",
            "tmp/csvmapping/generatorToTimeSeriesMapping.csv",
            "tmp/csvmapping/hvdcLineToTimeSeriesMapping.csv",
            "tmp/csvmapping/ignoredUnmappedBoundaryLines.csv",
            "tmp/csvmapping/ignoredUnmappedGenerators.csv",
            "tmp/csvmapping/ignoredUnmappedHvdcLines.csv",
            "tmp/csvmapping/ignoredUnmappedLoads.csv",
            "tmp/csvmapping/ignoredUnmappedPst.csv",
            "tmp/csvmapping/loadToTimeSeriesMapping.csv",
            "tmp/csvmapping/outOfMainCcBoundaryLines.csv",
            "tmp/csvmapping/outOfMainCcGenerators.csv",
            "tmp/csvmapping/outOfMainCcLoads.csv",
            "tmp/csvmapping/pstToTimeSeriesMapping.csv",
            "tmp/csvmapping/timeSeries.csv",
            "tmp/csvmapping/timeSeriesToBoundaryLinesMapping.csv",
            "tmp/csvmapping/timeSeriesToBreakersMapping.csv",
            "tmp/csvmapping/timeSeriesToGeneratorsMapping.csv",
            "tmp/csvmapping/timeSeriesToHvdcLinesMapping.csv",
            "tmp/csvmapping/timeSeriesToLoadsMapping.csv",
            "tmp/csvmapping/timeSeriesToPstMapping.csv",
            "tmp/csvmapping/unmappedBoundaryLines.csv",
            "tmp/csvmapping/unmappedGenerators.csv",
            "tmp/csvmapping/unmappedHvdcLines.csv",
            "tmp/csvmapping/unmappedLoads.csv",
            "tmp/csvmapping/unmappedPst.csv");

        StringWriter timeSeriesMappingSynthesisTxt = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingSynthesisTxt)) {
            mappingConfigCsvWriter.writeMappingSynthesis(bufferedWriter);
            compareTxt(timeSeriesMappingSynthesisTxt, directoryName, "mappingSynthesis.txt");
        }

        StringWriter timeSeriesMappingSynthesis = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingSynthesis)) {
            mappingConfigCsvWriter.writeMappingSynthesisCsv(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(timeSeriesMappingSynthesis, directoryName, "mappingSynthesis.csv");
        }

        StringWriter timeSeriesToGeneratorsMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToGeneratorsMapping)) {
            mappingConfigCsvWriter.writeTimeSeriesToGeneratorsMapping(bufferedWriter, store, computationRange, mappingParameters.getWithTimeSeriesStats());
            bufferedWriter.flush();
            compareTxt(timeSeriesToGeneratorsMapping, directoryName, "timeSeriesToGeneratorsMapping.csv");
        }

        StringWriter timeSeriesToLoadsMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToLoadsMapping)) {
            mappingConfigCsvWriter.writeTimeSeriesToLoadsMapping(bufferedWriter, store, computationRange, mappingParameters.getWithTimeSeriesStats());
            bufferedWriter.flush();
            compareTxt(timeSeriesToLoadsMapping, directoryName, "timeSeriesToLoadsMapping.csv");
        }

        StringWriter timeSeriesToBoundaryLinesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToBoundaryLinesMapping)) {
            mappingConfigCsvWriter.writeTimeSeriesToBoundaryLinesMapping(bufferedWriter, store, computationRange, mappingParameters.getWithTimeSeriesStats());
            bufferedWriter.flush();
            compareTxt(timeSeriesToBoundaryLinesMapping, directoryName, "timeSeriesToBoundaryLinesMapping.csv");
        }

        StringWriter timeSeriesToHvdcLinesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToHvdcLinesMapping)) {
            mappingConfigCsvWriter.writeTimeSeriesToHvdcLinesMapping(bufferedWriter, store, computationRange, mappingParameters.getWithTimeSeriesStats());
            bufferedWriter.flush();
            compareTxt(timeSeriesToHvdcLinesMapping, directoryName, "timeSeriesToHvdcLinesMapping.csv");
        }

        StringWriter timeSeriesToPstMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToPstMapping)) {
            mappingConfigCsvWriter.writeTimeSeriesToPstMapping(bufferedWriter, store, computationRange, mappingParameters.getWithTimeSeriesStats());
            bufferedWriter.flush();
            compareTxt(timeSeriesToPstMapping, directoryName, "timeSeriesToPstMapping.csv");
        }

        StringWriter timeSeriesToBreakersMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesToBreakersMapping)) {
            mappingConfigCsvWriter.writeTimeSeriesToBreakersMapping(bufferedWriter, store, computationRange, mappingParameters.getWithTimeSeriesStats());
            bufferedWriter.flush();
            compareTxt(timeSeriesToBreakersMapping, directoryName, "timeSeriesToBreakersMapping.csv");
        }

        StringWriter generatorToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(generatorToTimeSeriesMapping)) {
            mappingConfigCsvWriter.writeGeneratorToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(generatorToTimeSeriesMapping, directoryName, "generatorToTimeSeriesMapping.csv");
        }

        StringWriter loadToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(loadToTimeSeriesMapping)) {
            mappingConfigCsvWriter.writeLoadToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(loadToTimeSeriesMapping, directoryName, "loadToTimeSeriesMapping.csv");
        }

        StringWriter boundaryLineToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(boundaryLineToTimeSeriesMapping)) {
            mappingConfigCsvWriter.writeBoundaryLineToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(boundaryLineToTimeSeriesMapping, directoryName, "boundaryLineToTimeSeriesMapping.csv");
        }

        StringWriter hvdcLineToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(hvdcLineToTimeSeriesMapping)) {
            mappingConfigCsvWriter.writeHvdcLineToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(hvdcLineToTimeSeriesMapping, directoryName, "hvdcLineToTimeSeriesMapping.csv");
        }

        StringWriter pstToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(pstToTimeSeriesMapping)) {
            mappingConfigCsvWriter.writePstToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(pstToTimeSeriesMapping, directoryName, "pstToTimeSeriesMapping.csv");
        }

        StringWriter breakerToTimeSeriesMapping = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(breakerToTimeSeriesMapping)) {
            mappingConfigCsvWriter.writeBreakerToTimeSeriesMapping(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(breakerToTimeSeriesMapping, directoryName, "breakerToTimeSeriesMapping.csv");
        }

        StringWriter unmappedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedGenerators)) {
            mappingConfigCsvWriter.writeUnmappedGenerators(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(unmappedGenerators, directoryName, "unmappedGenerators.csv");
        }

        StringWriter unmappedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedLoads)) {
            mappingConfigCsvWriter.writeUnmappedLoads(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(unmappedLoads, directoryName, "unmappedLoads.csv");
        }

        StringWriter unmappedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedBoundaryLines)) {
            mappingConfigCsvWriter.writeUnmappedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(unmappedBoundaryLines, directoryName, "unmappedBoundaryLines.csv");
        }

        StringWriter unmappedHvdcLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedHvdcLines)) {
            mappingConfigCsvWriter.writeUnmappedHvdcLines(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(unmappedHvdcLines, directoryName, "unmappedHvdcLines.csv");
        }

        StringWriter unmappedPst = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(unmappedPst)) {
            mappingConfigCsvWriter.writeUnmappedPst(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(unmappedPst, directoryName, "unmappedPsts.csv");
        }

        StringWriter disconnectedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedGenerators)) {
            mappingConfigCsvWriter.writeDisconnectedGenerators(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(disconnectedGenerators, directoryName, "disconnectedGenerators.csv");
        }

        StringWriter disconnectedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedLoads)) {
            mappingConfigCsvWriter.writeDisconnectedLoads(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(disconnectedLoads, directoryName, "disconnectedLoads.csv");
        }

        StringWriter disconnectedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(disconnectedBoundaryLines)) {
            mappingConfigCsvWriter.writeDisconnectedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(disconnectedBoundaryLines, directoryName, "disconnectedBoundaryLines.csv");
        }

        StringWriter ignoredUnmappedGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedGenerators)) {
            mappingConfigCsvWriter.writeIgnoredUnmappedGenerators(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(ignoredUnmappedGenerators, directoryName, "ignoredUnmappedGenerators.csv");
        }

        StringWriter ignoredUnmappedLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedLoads)) {
            mappingConfigCsvWriter.writeIgnoredUnmappedLoads(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(ignoredUnmappedLoads, directoryName, "ignoredUnmappedLoads.csv");
        }

        StringWriter ignoredUnmappedBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedBoundaryLines)) {
            mappingConfigCsvWriter.writeIgnoredUnmappedBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(ignoredUnmappedBoundaryLines, directoryName, "ignoredUnmappedBoundaryLines.csv");
        }

        StringWriter ignoredUnmappedHvdcLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedHvdcLines)) {
            mappingConfigCsvWriter.writeIgnoredUnmappedHvdcLines(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(ignoredUnmappedHvdcLines, directoryName, "ignoredUnmappedHvdcLines.csv");
        }

        StringWriter ignoredUnmappedPst = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(ignoredUnmappedPst)) {
            mappingConfigCsvWriter.writeIgnoredUnmappedPst(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(ignoredUnmappedPst, directoryName, "ignoredUnmappedPsts.csv");
        }

        StringWriter outOfMainCcGenerators = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcGenerators)) {
            mappingConfigCsvWriter.writeOutOfMainCCGenerators(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(outOfMainCcGenerators, directoryName, "outOfMainCcGenerators.csv");
        }

        StringWriter outOfMainCcLoads = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcLoads)) {
            mappingConfigCsvWriter.writeOutOfMainCCLoads(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(outOfMainCcLoads, directoryName, "outOfMainCcLoads.csv");
        }

        StringWriter outOfMainCcBoundaryLines = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(outOfMainCcBoundaryLines)) {
            mappingConfigCsvWriter.writeOutOfMainCCBoundaryLines(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(outOfMainCcBoundaryLines, directoryName, "outOfMainCcBoundaryLines.csv");
        }

        StringWriter mappedTimeSeries = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(mappedTimeSeries)) {
            mappingConfigCsvWriter.writeMappedTimeSeries(bufferedWriter);
            bufferedWriter.flush();
            compareTxt(mappedTimeSeries, directoryName, "mappedTimeSeries.csv");
        }
    }

    @Before
    public void setUp() throws IOException {
        super.setUp();

        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

        // create mapping parameters
        mappingParameters = mappingParameters.load();
        mappingParameters.setWithTimeSeriesStats(true);
    }

    @Test
    public void mapToGeneratorTest() throws Exception {

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
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 1), false, false, mappingParameters.getToleranceThreshold());

        // Create observers
        List<TimeSeriesMapperObserver> observers = new ArrayList<>(2);
        /*
        MemDataSource dataSource = new MemDataSource();
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource) {

            @Override
            public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
                super.timeSeriesMappingEnd(point, index, balance);
                String suffix = getSuffix(point, index);
                String ext = "xiidm";
                String fileName = DataSourceUtil.getFileName(network.getId(), suffix, ext);
                byte[] data = dataSource.getData(suffix, "xiidm");

                // Check network output file
                try {
                    try (InputStream expected = getClass().getResourceAsStream(directoryName + fileName)) {
                        try (InputStream actual = new ByteArrayInputStream(data)) {
                            compareTxt(expected, actual);
                        }
                    }
                } catch (Exception e) {
                    throw new AssertionError("Impossible to check " + fileName);
                }
            }
        };
        observers.add(networkPointWriter);
        */

        StringWriter equipmentTimeSeriesWriter = new StringWriter();
        EquipmentTimeSeriesWriter equipmentTimeSeriesBufferedWriter = new EquipmentTimeSeriesWriter(new BufferedWriter(equipmentTimeSeriesWriter));
        observers.add(equipmentTimeSeriesBufferedWriter);

        // Launch TimeSeriesMapper test
        mapper.mapToNetwork(store, parameters, observers);

        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        // Check time series mapping status
        StringWriter timeSeriesMappingStatusWriter = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(timeSeriesMappingStatusWriter)) {
            new TimeSeriesMappingConfigCsvWriter(mappingConfig, network).writeTimeSeriesMappingStatus(store, bufferedWriter);
            bufferedWriter.flush();
            compareTxt(timeSeriesMappingStatusWriter, directoryName, "status.csv");
        }

        // Check equipment time series output
        compareTxt(equipmentTimeSeriesWriter, directoryName, "version_1.csv");

        // Check mapping output
        compareTxt(output, directoryName, "output.txt");

        // Check mapping config output
        checkMappingConfigOutput(mappingConfig, directoryName, store, new ComputationRange(ImmutableSet.of(1), 0, 1));
    }
}
