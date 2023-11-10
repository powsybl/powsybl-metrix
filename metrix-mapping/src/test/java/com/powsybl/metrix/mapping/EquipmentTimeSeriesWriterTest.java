/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import static com.powsybl.metrix.mapping.AbstractCompareTxt.compareStreamTxt;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EquipmentTimeSeriesWriterTest {

    private FileSystem fileSystem;
    private Network network;
    private ReadOnlyTimeSeriesStore store;
    private TimeSeriesMappingConfig mappingConfig;
    private TimeSeriesMapper mapper;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());

        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T05:00:00Z"), Duration.ofHours(1));

        store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("constant_ts", index, 100d, 100d, 100d, 100d, 100d),
                TimeSeries.createDouble("variable_ts", index, 10d, 11d, 12d, 13d, 14d)
        );

        // Mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'constant_ts'",
                "    filter {generator.id == 'FSSV.O11_G'}",
                "    variable maxP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'variable_ts'",
                "    filter {generator.id == 'FSSV.O11_G'}",
                "    variable minP",
                "}",
                "provideTsGenerators {",
                "    variables minP, maxP",
                "    filter {generator.id == 'FSSV.O11_G'}",
                "}",
                "provideGroupTsGenerators {",
                "    group VOLTAGE_LEVEL",
                "    withPowerType false",
                "}");

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        mapper = new TimeSeriesMapper(mappingConfig, network, new TimeSeriesMappingLogger());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void equipmentTimeSeriesTest() throws Exception {

        String directoryName = "/expected/EquipmentTimeSeriesWriter/";
        Path outputDir = fileSystem.getPath(".");

        // Create EquipmentTimeSeriesWriter
        TimeSeriesMapperObserver equipmentTimeSeriesObserver = new EquipmentTimeSeriesWriterObserver(network, mappingConfig, 2, Range.closed(0, 4), outputDir);
        TimeSeriesMapperObserver equipmentGroupTimeSeriesObserver = new EquipmentGroupTimeSeriesWriterObserver(network, mappingConfig, 2, Range.closed(0, 4), outputDir);

        // Create parameters
        final boolean ignoreLimits = false;
        final boolean ignoreEmptyFilter = false;
        final boolean identifyConstantTimeSeries = true;
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 4), ignoreLimits, ignoreEmptyFilter, identifyConstantTimeSeries, mappingParameters.getToleranceThreshold());
        // Launch mapper
        mapper.mapToNetwork(store, parameters, List.of(equipmentTimeSeriesObserver, equipmentGroupTimeSeriesObserver));

        Path expectedFile = Paths.get(Objects.requireNonNull(getClass().getResource(directoryName + "version_1.csv")).toURI());
        Path actualFile = fileSystem.getPath("version_1.csv");
        assertNotNull(compareStreamTxt(Files.newInputStream(expectedFile), Files.newInputStream(actualFile)));

        Path expectedGroupFile = Paths.get(Objects.requireNonNull(getClass().getResource(directoryName + "group_version_1.csv")).toURI());
        Path actualGroupFile = fileSystem.getPath("group_version_1.csv");
        assertNotNull(compareStreamTxt(Files.newInputStream(expectedGroupFile), Files.newInputStream(actualGroupFile)));
    }
}
