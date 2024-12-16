/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.mapping.*;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MetrixDslDataLoaderDoctrineCostTest {

    private FileSystem fileSystem;

    private Path dslFile;

    private Path mappingFile;

    private Network network;

    private final MetrixParameters parameters = new MetrixParameters();

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    public void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        dslFile = fileSystem.getPath("/test.dsl");
        mappingFile = fileSystem.getPath("/mapping.dsl");
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        // Create mapping file for use in all tests
        try (Writer writer = Files.newBufferedWriter(mappingFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "timeSeries['ts1'] = 100",
                "timeSeries['ts2'] = 200"
            ));
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void testLoadShedding() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "def contingenciesList = network.branches.collect()",
                "load('FSSV.O11_L') {",
                "   preventiveSheddingPercentage 30",
                "   preventiveSheddingDoctrineCost 12000",
                "}",
                "load('FVALDI11_L') {",
                "   curativeSheddingPercentage 10",
                "   curativeSheddingCost 15000",
                "   curativeSheddingDoctrineCost 10000",
                "   onContingencies contingenciesList",
                "}",
                "load('FVALDI11_L2') {",
                "   preventiveSheddingPercentage 30",
                "   preventiveSheddingDoctrineCost 'ts1'",
                "}",
                "load('FVERGE11_L') {",
                "   curativeSheddingPercentage 10",
                "   curativeSheddingCost 15000",
                "   curativeSheddingDoctrineCost 'ts2'",
                "   onContingencies contingenciesList",
                "}"));
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig mappingConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslDataLoader.load(dslFile, network, parameters, store, mappingConfig);

        assertEquals("12000", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.PREVENTIVE_DOCTRINE_COST_DOWN, "FSSV.O11_L")));
        assertEquals("ts1", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.PREVENTIVE_DOCTRINE_COST_DOWN, "FVALDI11_L2")));

        assertEquals(Set.of(new MappingKey(MetrixVariable.PREVENTIVE_DOCTRINE_COST_DOWN, "FSSV.O11_L")), mappingConfig.getTimeSeriesToEquipment().get("12000"));
        assertEquals(Set.of(new MappingKey(MetrixVariable.PREVENTIVE_DOCTRINE_COST_DOWN, "FVALDI11_L2")), mappingConfig.getTimeSeriesToEquipment().get("ts1"));

        assertEquals("10000", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.CURATIVE_DOCTRINE_COST_DOWN, "FVALDI11_L")));
        assertEquals("ts2", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.CURATIVE_DOCTRINE_COST_DOWN, "FVERGE11_L")));

        assertEquals(Set.of(new MappingKey(MetrixVariable.CURATIVE_DOCTRINE_COST_DOWN, "FVALDI11_L")), mappingConfig.getTimeSeriesToEquipment().get("10000"));
        assertEquals(Set.of(new MappingKey(MetrixVariable.CURATIVE_DOCTRINE_COST_DOWN, "FVERGE11_L")), mappingConfig.getTimeSeriesToEquipment().get("ts2"));
    }

    @Test
    void testGeneratorRedispatching() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "generator('FSSV.O11_G') {",
                "    redispatchingDownCosts (-10)",
                "    redispatchingUpCosts 100",
                "    redispatchingUpDoctrineCosts 50",
                "    redispatchingDownDoctrineCosts 200",
                "}",
                "generator('FSSV.O12_G') {",
                "    redispatchingDownCosts (-10)",
                "    redispatchingUpCosts 100",
                "    redispatchingUpDoctrineCosts 'ts1'",
                "    redispatchingDownDoctrineCosts 'ts2'",
                "}"));
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig mappingConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslDataLoader.load(dslFile, network, parameters, store, mappingConfig);

        assertEquals("50", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, "FSSV.O11_G")));
        assertEquals("200", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN, "FSSV.O11_G")));
        assertEquals("ts1", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, "FSSV.O12_G")));
        assertEquals("ts2", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN, "FSSV.O12_G")));

        assertEquals(Set.of(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, "FSSV.O11_G")), mappingConfig.getTimeSeriesToEquipment().get("50"));
        assertEquals(Set.of(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN, "FSSV.O11_G")), mappingConfig.getTimeSeriesToEquipment().get("200"));
        assertEquals(Set.of(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_UP, "FSSV.O12_G")), mappingConfig.getTimeSeriesToEquipment().get("ts1"));
        assertEquals(Set.of(new MappingKey(MetrixVariable.ON_GRID_DOCTRINE_COST_DOWN, "FSSV.O12_G")), mappingConfig.getTimeSeriesToEquipment().get("ts2"));
    }

    @Test
    void testLosses() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                    "losses() {",
                    "    costs 'ts1'",
                    "}"));
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig mappingConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslDataLoader.load(dslFile, network, parameters, store, mappingConfig);

        assertEquals("ts1", mappingConfig.getEquipmentToTimeSeries().get(new MappingKey(MetrixVariable.LOSSES_DOCTRINE_COST, "SimpleNetworkWithReducedThresholds")));
        assertEquals(Set.of(new MappingKey(MetrixVariable.LOSSES_DOCTRINE_COST, "SimpleNetworkWithReducedThresholds")), mappingConfig.getTimeSeriesToEquipment().get("ts1"));
    }

    @Test
    void testDoctrineCostIsMissing() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "def contingenciesList = network.branches.collect()",
                "load('FSSV.O11_L') {",
                "   preventiveSheddingPercentage 30",
                "}",
                "load('FVALDI11_L') {",
                "   curativeSheddingPercentage 10",
                "   curativeSheddingCost 15000",
                "   onContingencies contingenciesList",
                "}",
                "generator('FSSV.O11_G') {",
                "    redispatchingDownCosts 10",
                "    redispatchingUpCosts 20",
                "}",
                "generator('FSSV.O12_G') {",
                "    redispatchingDownCosts 10",
                "    redispatchingUpCosts 20",
                "    redispatchingDownDoctrineCosts 40",
                "}",
                "generator('FVALDI11_G') {",
                "    redispatchingDownCosts 10",
                "    redispatchingUpCosts 20",
                "    redispatchingUpDoctrineCosts 30",
                "}"));
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            MetrixDslDataLoader.load(dslFile, network, parameters, store, new TimeSeriesMappingConfig(), out);
        }

        String output = outputStream.toString();
        String expectedMessage = String.join(System.lineSeparator(),
            "WARNING;Metrix script;load FSSV.O11_L is missing preventive shedding doctrine cost to be properly configured",
            "WARNING;Metrix script;load FVALDI11_L is missing curative shedding doctrine cost to be properly configured",
            "WARNING;Metrix script;generator FSSV.O11_G is missing redispatching doctrine cost to be properly configured",
            "WARNING;Metrix script;generator FSSV.O12_G is missing redispatching doctrine up cost to be properly configured",
            "WARNING;Metrix script;generator FVALDI11_G is missing redispatching doctrine down cost to be properly configured",
            "");
        assertEquals(expectedMessage, output);
    }
}
