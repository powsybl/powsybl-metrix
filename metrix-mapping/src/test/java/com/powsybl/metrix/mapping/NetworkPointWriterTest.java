/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.commons.observer.TimeSeriesMapperObserver;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import static com.powsybl.commons.test.ComparisonUtils.assertXmlEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class NetworkPointWriterTest {

    private FileSystem fileSystem;
    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();
    private final TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));
    private final String expectedDirectoryNameBase = "/expected/NetworkPointWriter/";
    private List<TimeSeriesMapperObserver> networkPointWriterList;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        Path networkOutputDir = fileSystem.getPath(".");

        // Create NetworkPointWriter
        DataSource dataSource = DataSourceUtil.createDataSource(networkOutputDir.resolve(network.getId()), null);
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);
        networkPointWriterList = List.of(networkPointWriter);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void regulationModeTest() throws Exception {
        // Resource directory
        String expectedDirectoryName = expectedDirectoryNameBase + "regulationMode/";

        // Mapping script
        String script = String.join(System.lineSeparator(),
            "mapToPhaseTapChangers {",
            "    timeSeriesName 'regulation_mode_ts'",
            "    filter {",
            "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
            "    }",
            "    variable regulationMode",
            "}");

        // Timeseries
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("regulation_mode_ts", index, 0d, 2d)
        );

        assertMapping(script, store, expectedDirectoryName);
    }

    @Test
    void regulationModeExceptionTest() {
        // Mapping script
        String script = String.join(System.lineSeparator(),
            "mapToPhaseTapChangers {",
            "    timeSeriesName 'regulation_mode_ts'",
            "    filter {",
            "        twoWindingsTransformer.id==\"FP.AND1  FTDPRA1  1\"",
            "    }",
            "    variable regulationMode",
            "}");

        // Timeseries
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("regulation_mode_ts", index, 3d, 3d)
        );

        // Create Mapper
        TimeSeriesMapper mapper = prepareMapper(script, store);

        // Launch mapper should throw an exception
        AssertionError exception = assertThrows(AssertionError.class, () -> mapper.mapToNetwork(store, networkPointWriterList));
        assertEquals("Unsupported regulation mode 3.0", exception.getMessage());
    }

    @Test
    void networkPointConstantVariantTest() throws Exception {
        // Resource directory
        String expectedDirectoryName = expectedDirectoryNameBase + "full/";

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

        assertMapping(script, store, expectedDirectoryName);
    }

    private TimeSeriesMapper prepareMapper(String script, ReadOnlyTimeSeriesStore store) {
        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // Create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
            Range.closed(0, 1), true, false, false, mappingParameters.getToleranceThreshold());
        return new TimeSeriesMapper(mappingConfig, parameters, network, logger);
    }

    private void assertMapping(String script, ReadOnlyTimeSeriesStore store, String expectedDirectoryName) throws Exception {
        // Create Mapper
        TimeSeriesMapper mapper = prepareMapper(script, store);

        // Launch mapper
        mapper.mapToNetwork(store, networkPointWriterList);

        for (int point = 0; point < index.getPointCount(); point++) {
            String fileName = NetworkPointWriter.getFileName(network, 1, point, index) + ".xiidm";
            Path actualFilePath = fileSystem.getPath(fileName);
            compareTxt(actualFilePath, expectedDirectoryName, fileName);
        }
    }

    private void compareTxt(Path actualPath, String directoryName, String fileName) throws Exception {
        try (InputStream expected = getClass().getResourceAsStream(directoryName + fileName)) {
            try (InputStream actual = Files.newInputStream(actualPath)) {
                assertXmlEquals(expected, actual);
            }
        }
    }
}
