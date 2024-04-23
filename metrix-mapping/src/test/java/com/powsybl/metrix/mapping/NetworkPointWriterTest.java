/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.timeseries.*;
import org.apache.commons.io.input.ReaderInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeSet;

import static com.powsybl.metrix.mapping.AbstractCompareTxt.compareStreamTxt;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NetworkPointWriterTest {

    private FileSystem fileSystem;
    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    public void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void networkPointConstantVariantTest() throws Exception {

        Path networkOutputDir = fileSystem.getPath(".");

        String expectedDirectoryName = "/expected/NetworkPointWriter/";

        // Mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'constant_ts1'",
                "    filter {generator.id == 'FSSV.O11_G'}",
                "    variable maxP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'constant_ts2'",
                "    filter {generator.id == 'FSSV.O12_G'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'variable_ts1'",
                "    filter {generator.id == 'FSSV.O11_G'}",
                "    variable minP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FVALDI11_G\"",
                "    }",
                "    variable voltageRegulatorOn",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable targetQ",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable targetV",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable voltageRegulatorOn",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        generator.id==\"FSSV.O11_G\"",
                "    }",
                "    variable disconnected",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "    variable p0",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FSSV.O11_L\"",
                "    }",
                "    variable q0",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L\"",
                "    }",
                "    variable fixedActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L2\"",
                "    }",
                "    variable variableActivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L\"",
                "    }",
                "    variable fixedReactivePower",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        load.id==\"FVALDI11_L2\"",
                "    }",
                "    variable variableReactivePower",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "    variable activePowerSetpoint",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts2'",
                "    filter {",
                "        hvdcLine.id==\"HVDC2\"",
                "    }",
                "    variable minP",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC2\"",
                "    }",
                "    variable maxP",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        hvdcLine.id==\"HVDC1\"",
                "    }",
                "    variable nominalV",
                "}",
                "mapToBreakers {",
                "    timeSeriesName 'switch_ts'",
                "    filter {breaker.id == 'FP.AND1_FP.AND1_DJ_OMN'}",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable phaseTapPosition",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'regulation_mode_ts'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable regulationMode",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable phaseRegulating",
                "}",
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable targetDeadband",
                "}",
                "mapToTransformers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable ratedU1",
                "}",
                "mapToTransformers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable ratedU2",
                "}",
                "mapToTransformers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable disconnected",
                "}",
                "mapToRatioTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable targetV",
                "}",
                "mapToRatioTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable ratioTapPosition",
                "}",
                "mapToRatioTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
                "    }",
                "    variable loadTapChangingCapabilities",
                "}",
                "mapToLccConverterStations {",
                "    timeSeriesName 'power_factor_ts'",
                "    filter {",
                "        lccConverterStation.id==\"FVALDI1_FVALDI1_HVDC1\"",
                "    }",
                "    variable powerFactor",
                "}",
                "mapToVscConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "    variable voltageRegulatorOn",
                "}",
                "mapToVscConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "    variable voltageSetpoint",
                "}",
                "mapToVscConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "    variable reactivePowerSetpoint",
                "}",
                "mapToVscConverterStations {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        vscConverterStation.id==\"FSSV.O1_FSSV.O1_HVDC1\"",
                "    }",
                "    variable voltageRegulatorOn",
                "}",
                "mapToLines {",
                "    timeSeriesName 'ts1'",
                "    filter {",
                "        line.id==\"FP.AND1  FVERGE1  1\"",
                "    }",
                "    variable disconnected",
                "}");

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("constant_ts1", index, 100d, 100d),
                TimeSeries.createDouble("constant_ts2", index, 3000d, 3000d),
                TimeSeries.createDouble("variable_ts1", index, 10d, 11d),
                TimeSeries.createDouble("switch_ts", index, 0d, 1d),
                TimeSeries.createDouble("ts1", index, 10d, 11d),
                TimeSeries.createDouble("ts2", index, -10d, -11d),
                TimeSeries.createDouble("power_factor_ts", index, 0d, 1d),
                TimeSeries.createDouble("regulation_mode_ts", index, 0d, 1d)
        );

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // Create NetworkPointWriter
        DataSource dataSource = DataSourceUtil.createDataSource(networkOutputDir, network.getId(), null);
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);

        // Create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 1), true, false, false, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);
        // Launch mapper
        mapper.mapToNetwork(store, ImmutableList.of(networkPointWriter));

        for (int point = 0; point < index.getPointCount(); point++) {
            String fileName = NetworkPointWriter.getFileName(network, 1, point, index);
            Path actualFilePath = fileSystem.getPath(fileName + ".xiidm");
            compareTxt(actualFilePath, expectedDirectoryName, fileName);
        }
    }

    private void compareTxt(Path actualPath, String directoryName, String fileName) throws Exception {
        try (InputStream expected = getClass().getResourceAsStream(directoryName + fileName)) {
            try (InputStream actual = Files.newInputStream(actualPath)) {
                // skip the two first lines : xml version line and network line (containing extensions)
                // because extensions are not ordered in the same way for each test launching
                assertNotNull(expected);
                BufferedReader expectedReader = new BufferedReader(new InputStreamReader(expected));
                expectedReader.readLine();
                expectedReader.readLine();
                BufferedReader actualReader = new BufferedReader(new InputStreamReader(actual));
                actualReader.readLine();
                actualReader.readLine();
                InputStream expectedStream = ReaderInputStream.builder()
                    .setReader(expectedReader)
                    .setCharset(StandardCharsets.UTF_8)
                    .get();
                InputStream actualStream = ReaderInputStream.builder()
                    .setReader(actualReader)
                    .setCharset(StandardCharsets.UTF_8)
                    .get();
                assertNotNull(compareStreamTxt(expectedStream, actualStream));
            }
        }
    }
}
