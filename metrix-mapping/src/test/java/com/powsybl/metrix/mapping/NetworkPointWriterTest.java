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
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.commons.observer.TimeSeriesMapperObserver;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        addBattery1();
        Path networkOutputDir = fileSystem.getPath(".");

        // Create NetworkPointWriter
        DataSource dataSource = DataSourceUtil.createDataSource(networkOutputDir.resolve(network.getId()), null);
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource);
        networkPointWriterList = List.of(networkPointWriter);
    }

    private void addBattery1() {
        network.getVoltageLevel("FP.AND1").newBattery()
            .setId("BATTERY_1")
            .setEnsureIdUnicity(true)
            .setNode(6)
            .setTargetP(20.0)
            .setTargetQ(20.0)
            .setMinP(10.0)
            .setMaxP(200.0)
            .add();
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
        String script = """
            mapToPhaseTapChangers {
                timeSeriesName 'regulation_mode_ts'
                filter {
                    twoWindingsTransformer.id=="FP.AND1  FTDPRA1  1"
                }
                variable regulationMode
            }
            """;

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

        try (InputStream scriptStream = Objects.requireNonNull(getClass().getResourceAsStream("/network_point_writer_mapping_script.groovy"))) {
            // Mapping script
            String script = new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);

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
