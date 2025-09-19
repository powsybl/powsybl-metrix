/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.binding.MetrixGeneratorsBinding;
import com.powsybl.metrix.integration.binding.MetrixLoadsBinding;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.integration.type.MetrixComputationType;
import com.powsybl.metrix.integration.type.MetrixHvdcControlType;
import com.powsybl.metrix.integration.type.MetrixPtcControlType;
import com.powsybl.metrix.mapping.references.MappingKey;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigTableLoader;
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.ast.IntegerNodeCalc;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class MetrixDslDataLoaderTest {

    private FileSystem fileSystem;

    private Path dslFile;

    private Path mappingFile;

    private Network network;

    private final MetrixParameters parameters = new MetrixParameters();

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        dslFile = fileSystem.getPath("/test.dsl");
        mappingFile = fileSystem.getPath("/mapping.dsl");
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        // Create mapping file for use in all tests
        try (Writer writer = Files.newBufferedWriter(mappingFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "timeSeries['tsN'] = 1000",
                "timeSeries['tsN_1'] = 2000",
                "timeSeries['tsITAM'] = 3000",
                "timeSeries['ts1'] = 100",
                "timeSeries['ts2'] = 200",
                "timeSeries['ts3'] = 300",
                "timeSeries['ts4'] = 400",
                "timeSeries['ts5'] = 500"
            ));
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void testWrongConfigurations() throws IOException {
        try (OutputStream writer = Files.newOutputStream(dslFile);
             InputStream inputStream = Objects.requireNonNull(
                 MetrixDslDataLoaderTest.class.getResourceAsStream("/inputs/badConfiguration.groovy"))) {
            IOUtils.copy(inputStream, writer);
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);

        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        // Compare MetrixDslData
        MetrixDslData expectedData = new MetrixDslData();
        expectedData.addBranchMonitoringN("FVALDI1  FTDPRA1  2");
        expectedData.addBranchMonitoringN("FP.AND1  FVERGE1  2");
        expectedData.addBranchMonitoringNk("FP.AND1  FVERGE1  2");
        expectedData.addBranchMonitoringNk("FVALDI1  FTDPRA1  1");
        expectedData.addBranchMonitoringNk("FS.BIS1  FVALDI1  1");
        expectedData.addSection(new MetrixSection("sectionOk", 2000, Map.of("HVDC1", 0.9f, "FP.AND1  FTDPRA1  1", 1.1f, "FVALDI1  FTDPRA1  1", 2f)));
        expectedData.addGeneratorForRedispatching("FVALDI11_G", null);
        assertEquals(expectedData, data);

        // Compare TimeSeriesMappingConfig
        TimeSeriesMappingConfig expectedTsConfig = new TimeSeriesMappingConfig();
        MappingKey mk1 = new MappingKey(MetrixVariable.THRESHOLD_N1, "FVALDI1  FTDPRA1  1");
        MappingKey mk2 = new MappingKey(MetrixVariable.THRESHOLD_N1, "FS.BIS1  FVALDI1  1");
        MappingKey mk3 = new MappingKey(MetrixVariable.THRESHOLD_N, "FP.AND1  FVERGE1  2");
        MappingKey mk4 = new MappingKey(MetrixVariable.THRESHOLD_N1, "FP.AND1  FVERGE1  2");
        MappingKey mk5 = new MappingKey(MetrixVariable.THRESHOLD_N, "FVALDI1  FTDPRA1  2");
        MappingKey mk6 = new MappingKey(MetrixVariable.THRESHOLD_ITAM, "FS.BIS1  FVALDI1  1");
        MappingKey mk7 = new MappingKey(MetrixVariable.ON_GRID_COST_DOWN, "FVALDI11_G");
        MappingKey mk8 = new MappingKey(MetrixVariable.ON_GRID_COST_UP, "FVALDI11_G");
        expectedTsConfig.setTimeSeriesToEquipment(Map.of(
            "tsN_1", Set.of(mk1),
            "tsN", Set.of(mk2, mk3, mk4, mk5),
            "tsITAM", Set.of(mk6),
            "ts3", Set.of(mk7),
            "ts4", Set.of(mk8)
        ));
        expectedTsConfig.setUnmappedGenerators(Set.of("FSSV.O11_G", "FVALDI11_G", "FSSV.O12_G", "FVERGE11_G"));
        expectedTsConfig.setUnmappedLoads(Set.of("FSSV.O11_L", "FVALDI11_L", "FVALDI11_L2"));
        expectedTsConfig.setUnmappedHvdcLines(Set.of("HVDC1", "HVDC2"));
        expectedTsConfig.setUnmappedPhaseTapChangers(Set.of("FP.AND1  FTDPRA1  1"));
        expectedTsConfig.setUnmappedMinPGenerators(Set.of("FSSV.O11_G", "FVALDI11_G", "FSSV.O12_G", "FVERGE11_G"));
        expectedTsConfig.setUnmappedMaxPGenerators(Set.of("FSSV.O11_G", "FVALDI11_G", "FSSV.O12_G", "FVERGE11_G"));
        expectedTsConfig.setUnmappedMinPHvdcLines(Set.of("HVDC1", "HVDC2"));
        expectedTsConfig.setUnmappedMaxPHvdcLines(Set.of("HVDC1", "HVDC2"));
        expectedTsConfig.setDisconnectedLoads(Set.of("FVERGE11_L"));
        expectedTsConfig.setTimeSeriesNodes(Map.of(
            "ts1",
            new IntegerNodeCalc(100),
            "ts2",
            new IntegerNodeCalc(200),
            "ts3",
            new IntegerNodeCalc(300),
            "ts4",
            new IntegerNodeCalc(400),
            "ts5",
            new IntegerNodeCalc(500),
            "tsN_1",
            new IntegerNodeCalc(2000),
            "tsN",
            new IntegerNodeCalc(1000),
            "tsITAM",
            new IntegerNodeCalc(3000)
        ));
        expectedTsConfig.setEquipmentToTimeSeries(Map.of(
            mk1, "tsN_1",
            mk2, "tsN",
            mk3, "tsN",
            mk4, "tsN",
            mk5, "tsN",
            mk6, "tsITAM",
            mk7, "ts3",
            mk8, "ts4"
        ));
        assertEquals(expectedTsConfig, tsConfig);
    }

    @Test
    void testAutomaticList() throws IOException {

        try (OutputStream os = Files.newOutputStream(dslFile);
             InputStream is = Objects.requireNonNull(
                 MetrixDslDataLoaderTest.class.getResourceAsStream("/inputs/automaticList.groovy"))
        ) {
            IOUtils.copy(is, os);
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);

        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        // Network elements
        Set<String> lineSet = ImmutableSet.of(
            "FP.AND1  FVERGE1  1",
            "FP.AND1  FVERGE1  2",
            "FS.BIS1  FVALDI1  1",
            "FS.BIS1  FVALDI1  2",
            "FS.BIS1 FSSV.O1 1",
            "FS.BIS1 FSSV.O1 2",
            "FSSV.O1  FP.AND1  1",
            "FSSV.O1  FP.AND1  2",
            "FTDPRA1  FVERGE1  1",
            "FTDPRA1  FVERGE1  2",
            "FVALDI1  FTDPRA1  1",
            "FVALDI1  FTDPRA1  2");
        Set<String> generatorSet = ImmutableSet.of("FSSV.O11_G", "FVALDI11_G", "FSSV.O12_G", "FVERGE11_G");
        Set<String> loadSet = ImmutableSet.of("FSSV.O11_L", "FVALDI11_L", "FVALDI11_L2", "FVERGE11_L");
        Set<String> hvdcSet = ImmutableSet.of("HVDC1", "HVDC2");

        // Compare MetrixDslData
        MetrixDslData expectedData = new MetrixDslData();
        for (String line : lineSet) {
            expectedData.addBranchMonitoringN(line);
            expectedData.addBranchResultNk(line);
        }
        expectedData.addBranchResultN("FP.AND1  FTDPRA1  1");
        expectedData.addBranchMonitoringNk("FP.AND1  FTDPRA1  1");
        expectedData.addPtc("FP.AND1  FTDPRA1  1", MetrixPtcControlType.FIXED_POWER_CONTROL, List.of("cty"));
        for (String hvdc : hvdcSet) {
            expectedData.addHvdc(hvdc, MetrixHvdcControlType.OPTIMIZED, List.of("cty1"));
            expectedData.addHvdcFlowResults(hvdc);
        }
        for (String generator : generatorSet) {
            expectedData.addGeneratorForAdequacy(generator);
            expectedData.addGeneratorForRedispatching(generator, List.of("cty2", "cty3"));
        }
        int i = 0;
        for (String load : loadSet) {
            expectedData.addPreventiveLoad(load, 20 + i);
            expectedData.addPreventiveLoadCost(load, 12000f + i);
            expectedData.addCurativeLoad(load, 5 + i, List.of("cty4"));
            i++;
        }
        assertEquals(expectedData, data);

        // Compare TimeSeriesMappingConfig
        TimeSeriesMappingConfig expectedTsConfig = new TimeSeriesMappingConfig();
        expectedTsConfig.setUnmappedGenerators(generatorSet);
        expectedTsConfig.setUnmappedLoads(Set.of("FSSV.O11_L", "FVALDI11_L", "FVALDI11_L2"));
        expectedTsConfig.setUnmappedHvdcLines(hvdcSet);
        expectedTsConfig.setUnmappedPhaseTapChangers(Set.of("FP.AND1  FTDPRA1  1"));
        expectedTsConfig.setUnmappedMinPGenerators(generatorSet);
        expectedTsConfig.setUnmappedMaxPGenerators(generatorSet);
        expectedTsConfig.setUnmappedMinPHvdcLines(hvdcSet);
        expectedTsConfig.setUnmappedMaxPHvdcLines(hvdcSet);
        expectedTsConfig.setDisconnectedLoads(Set.of("FVERGE11_L"));
        expectedTsConfig.setTimeSeriesNodes(Map.of(
            "ts1",
            new IntegerNodeCalc(100),
            "ts2",
            new IntegerNodeCalc(200),
            "ts3",
            new IntegerNodeCalc(300),
            "ts4",
            new IntegerNodeCalc(400),
            "ts5",
            new IntegerNodeCalc(500),
            "tsN_1",
            new IntegerNodeCalc(2000),
            "tsN",
            new IntegerNodeCalc(1000),
            "tsITAM",
            new IntegerNodeCalc(3000)
        ));
        Map<MappingKey, String> equipmentToTimeSeries = new HashMap<>();
        Map<String, Set<MappingKey>> timeSeriesToEquipment = new HashMap<>();
        MappingKey mk;
        for (String id : lineSet) {
            mk = new MappingKey(MetrixVariable.THRESHOLD_N, id);
            equipmentToTimeSeries.put(mk, "tsN");
            timeSeriesToEquipment.computeIfAbsent("tsN", k -> new HashSet<>()).add(mk);
        }
        mk = new MappingKey(MetrixVariable.THRESHOLD_N1, "FP.AND1  FTDPRA1  1");
        equipmentToTimeSeries.put(mk, "tsN_1");
        timeSeriesToEquipment.put("tsN_1", Set.of(mk));
        mk = new MappingKey(MetrixVariable.THRESHOLD_ITAM, "FP.AND1  FTDPRA1  1");
        equipmentToTimeSeries.put(mk, "tsITAM");
        timeSeriesToEquipment.put("tsITAM", Set.of(mk));
        for (String id : generatorSet) {
            mk = new MappingKey(MetrixVariable.OFF_GRID_COST_DOWN, id);
            equipmentToTimeSeries.put(mk, "ts1");
            timeSeriesToEquipment.computeIfAbsent("ts1", k -> new HashSet<>()).add(mk);
            mk = new MappingKey(MetrixVariable.OFF_GRID_COST_UP, id);
            equipmentToTimeSeries.put(mk, "ts2");
            timeSeriesToEquipment.computeIfAbsent("ts2", k -> new HashSet<>()).add(mk);
            mk = new MappingKey(MetrixVariable.ON_GRID_COST_DOWN, id);
            equipmentToTimeSeries.put(mk, "ts3");
            timeSeriesToEquipment.computeIfAbsent("ts3", k -> new HashSet<>()).add(mk);
            mk = new MappingKey(MetrixVariable.ON_GRID_COST_UP, id);
            equipmentToTimeSeries.put(mk, "ts4");
            timeSeriesToEquipment.computeIfAbsent("ts4", k -> new HashSet<>()).add(mk);
        }
        for (String id : loadSet) {
            mk = new MappingKey(MetrixVariable.CURATIVE_COST_DOWN, id);
            equipmentToTimeSeries.put(mk, "ts5");
            timeSeriesToEquipment.computeIfAbsent("ts5", k -> new HashSet<>()).add(mk);
        }
        expectedTsConfig.setTimeSeriesToEquipment(timeSeriesToEquipment);
        expectedTsConfig.setEquipmentToTimeSeries(equipmentToTimeSeries);
        assertEquals(expectedTsConfig, tsConfig);
    }

    @Test
    void testListAsParameter() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "def contingenciesList = network.branches.collect()",
                "branch('FP.AND1  FVERGE1  1') {",
                "    contingencyFlowResults contingenciesList",
                "}",
                "generator('FSSV.O11_G') {",
                "    redispatchingDownCosts (-10)",
                "    redispatchingUpCosts 100",
                "    onContingencies contingenciesList",
                "}",
                "hvdc('HVDC1') {",
                "    controlType OPTIMIZED",
                "    onContingencies contingenciesList",
                "}",
                "phaseShifter('FP.AND1  FTDPRA1  1') {",
                "    controlType OPTIMIZED_ANGLE_CONTROL",
                "    onContingencies contingenciesList",
                "}",
                "load('FVALDI11_L') {",
                "    curativeSheddingPercentage 50",
                "    curativeSheddingCost 12345",
                "    onContingencies contingenciesList",
                "}"));
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        assertEquals(13, data.getContingencyFlowResult("FP.AND1  FVERGE1  1").size());
        assertEquals(13, data.getGeneratorContingencies("FSSV.O11_G").size());
        assertEquals(13, data.getHvdcContingencies("HVDC1").size());
        assertEquals(13, data.getPtcContingencies("FP.AND1  FTDPRA1  1").size());
        assertEquals(13, data.getLoadContingencies("FVALDI11_L").size());
    }

    @Test
    void testLoadDefaultValue() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "load('FVALDI11_L') {",
                "   preventiveSheddingPercentage 20",
                "}",
                "load('FVALDI11_L2') {",
                "   preventiveSheddingPercentage 30",
                "   preventiveSheddingCost 12000",
                "}",
                "load('FVERGE11_L') {",
                "   preventiveSheddingPercentage 0",
                "   preventiveSheddingCost 10000",
                "}",
                "load('FSSV.O11_L') {",
                "   curativeSheddingPercentage 40",
                "}"));
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        assertNull(data.getPreventiveLoadCost("FVALDI11_L"));
        assertEquals(12000f, data.getPreventiveLoadCost("FVALDI11_L2"), 0f);
        assertNull(data.getCurativeLoadPercentage("FSSV.O11_L"));
        assertEquals(0, (long) data.getPreventiveLoadPercentage("FVERGE11_L"));
        assertEquals(10000f, data.getPreventiveLoadCost("FVERGE11_L"), 0f);
    }

    @Test
    void testSpaceTimeSeries() throws IOException {

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("tsN", index, 1d, 1d),
            TimeSeries.createDouble("ts1", index, 1d, 1d),
            TimeSeries.createDouble("ts2", index, 1d, 1d),
            TimeSeries.createDouble("ts3", index, 1d, 1d),
            TimeSeries.createDouble("ts4", index, 1d, 1d)
        );

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "for (l in network.lines) {",
                "    branch(l.id) {",
                "        branchRatingsBaseCase 'tsN'",
                "    }",
                "}",
                "for (g in network.generators) {",
                "    generator(g.id) {",
                "        adequacyUpCosts 'ts2'",
                "        adequacyDownCosts 'ts1'",
                "        redispatchingUpCosts 'ts4'",
                "        redispatchingDownCosts 'ts3'",
                "    }",
                "}"));
        }

        TimeSeriesMappingConfig tsConfig = new TimeSeriesMappingConfig(network);

        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);
        new TimeSeriesMappingConfigTableLoader(tsConfig, store).checkIndexUnicity();

        assertEquals(12, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.MONITORING).count());
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FP.AND1  FVERGE1  1"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FP.AND1  FVERGE1  2"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FS.BIS1  FVALDI1  1"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FS.BIS1  FVALDI1  2"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FS.BIS1 FSSV.O1 1"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FS.BIS1 FSSV.O1 2"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FSSV.O1  FP.AND1  1"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FSSV.O1  FP.AND1  2"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FTDPRA1  FVERGE1  1"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FTDPRA1  FVERGE1  2"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FVALDI1  FTDPRA1  1"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FVALDI1  FTDPRA1  2"));
        assertEquals(4, data.getGeneratorsForRedispatching().size());
        assertEquals(ImmutableSet.of("FSSV.O11_G", "FVALDI11_G", "FSSV.O12_G", "FVERGE11_G"), data.getGeneratorsForRedispatching());
        assertEquals(0, data.getGeneratorContingenciesList().size());
        assertEquals(Collections.emptyList(), data.getGeneratorContingencies("FSSV.O11_G"));
        assertEquals(Collections.emptyList(), data.getGeneratorContingencies("FVALDI11_G"));
        assertEquals(Collections.emptyList(), data.getGeneratorContingencies("FSSV.O12_G"));
        assertEquals(Collections.emptyList(), data.getGeneratorContingencies("FVERGE11_G"));
    }

    @Test
    void testMixedTimeSeries() throws IOException {

        Path mappingFile2 = fileSystem.getPath("/mapping2.dsl");
        try (Writer writer = Files.newBufferedWriter(mappingFile2, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "timeSeries['tsN'] = 1000",
                "timeSeries['ts1'] = 100",
                "timeSeries['ts2'] = 200",
                "timeSeries['ts3'] = 300",
                "timeSeries['ts4'] = 400",
                "timeSeries['tsN1'] = timeSeries['tsNk'] * 1.1"
            ));
        }

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("tsNk", index, 1d, 1d),
            TimeSeries.createDouble("ts5", index, 1d, 1d),
            TimeSeries.createDouble("ts6", index, 1d, 1d),
            TimeSeries.createDouble("ts7", index, 1d, 1d),
            TimeSeries.createDouble("ts8", index, 1d, 1d)
        );

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "for (l in network.lines) {",
                "    branch(l.id) {",
                "        branchRatingsBaseCase 'tsN'",
                "        branchRatingsOnContingency 'tsN1'", // time-series created by mapping
                "        branchRatingsBeforeCurative 500", // constant time-series created on the fly
                "    }",

                "}",
                "for (t in network.twoWindingsTransformers) {",
                "    branch(t.id) {",
                "        baseCaseFlowResults true",
                "        maxThreatFlowResults()",
                "    }",
                "}",
                "generator('FSSV.O11_G') {",
                "    adequacyDownCosts 'ts1'",
                "    adequacyUpCosts 'ts2'",
                "    redispatchingUpCosts 'ts4'",
                "    redispatchingDownCosts 'ts3'",

                "}",
                "generator('FSSV.O12_G') {",
                "    adequacyDownCosts 'ts5'",
                "    adequacyUpCosts 'ts6'",
                "    redispatchingDownCosts 'ts7'",
                "    redispatchingUpCosts 'ts8'",
                "}"));
        }

        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile2).load(network, mappingParameters, store, new DataTableStore(), null);

        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        assertEquals(12, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.MONITORING).count());
        assertEquals(12, data.getBranchMonitoringNkList().stream().filter(a -> data.getBranchMonitoringNk(a) == MetrixInputData.MonitoringType.MONITORING).count());
        assertEquals(1, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.RESULT).count());
        assertEquals(1, data.getBranchMonitoringNkList().stream().filter(a -> data.getBranchMonitoringNk(a) == MetrixInputData.MonitoringType.RESULT).count());
        assertEquals(2, data.getGeneratorsForRedispatching().size());

        Set<String> expectedTS = ImmutableSet.of("ts2", "ts1", "tsN1", "ts4", "ts3", "ts6", "ts5", "ts8", "ts7", "tsN", "500");
        assertEquals(expectedTS, tsConfig.getTimeSeriesToEquipment().keySet());
        new TimeSeriesMappingConfigTableLoader(tsConfig, store).checkIndexUnicity();
    }

    @Test
    void testTimeSeriesNotFound() throws IOException {
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(TimeSeries.createDouble("tsFoo", index, 1d, 1d));

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "for (l in network.lines) {",
                "    branch(l.id) {",
                "        branchRatingsBaseCase 'ts'", // Ts missing
                "        branchRatingsOnContingency 'tsNk'", // TsNk missing
                "        branchRatingsBeforeCurative 'tsITAM'", // , TsIAM created by mapping
                "    }",
                "}",
                "generator('FSSV.O11_G') {",
                "    adequacyUpCosts 'ts2'", // TS created by mapping
                "    adequacyDownCosts 'ts1'", // TS created by mapping
                "    redispatchingDownCosts 'ts3'",
                "    redispatchingUpCosts 'ts4'",
                "}",
                "generator('FSSV.O12_G') {",
                "    redispatchingDownCosts 'ts3'", // ts5 is missing
                "    redispatchingUpCosts 'ts5'", // ts5 is missing
                "}"));
        }

        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);

        DataTableStore dataTableStore = new DataTableStore();
        assertThrows(TimeSeriesMappingException.class,
            () -> MetrixDslDataLoader.load(dslFile, network, parameters, store, dataTableStore, tsConfig),
            "Time Series 'ts' not found");
    }

    @Test
    void contingencyFlowResultTest() throws IOException {
        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "def list = ['FP.AND1  FVERGE1  1', 'FP.AND1  FVERGE1  2', 'FS.BIS1  FVALDI1  1', 'FS.BIS1  FVALDI1  2']",
                "for (line in list) {",
                "  branch(line) {",
                "    contingencyFlowResults 'cty1', 'cty2'",
                "  }",
                "}"));
        }

        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            MetrixDslData data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new TimeSeriesMappingConfig(network), null);

            assertEquals(4, data.getContingencyFlowResultList().size());
            List<String> list = List.of("cty1", "cty2");
            assertEquals(list, data.getContingencyFlowResult("FP.AND1  FVERGE1  1"));
            assertEquals(list, data.getContingencyFlowResult("FP.AND1  FVERGE1  2"));
            assertEquals(list, data.getContingencyFlowResult("FS.BIS1  FVALDI1  1"));
            assertEquals(list, data.getContingencyFlowResult("FS.BIS1  FVALDI1  2"));
        }
    }

    @Test
    void testParameters() throws IOException {

        MetrixParameters expectedParameters = new MetrixParameters();
        expectedParameters.setComputationType(MetrixComputationType.OPF)
            .setLossFactor(5f)
            .setNominalU(103)
            .setWithGridCost(false)
            .setPreCurativeResults(true)
            .setOutagesBreakingConnexity(true)
            .setRemedialActionsBreakingConnexity(true)
            .setAnalogousRemedialActionDetection(true)
            .setPropagateBranchTripping(true)
            .setWithAdequacyResults(false)
            .setWithRedispatchingResults(true)
            .setMarginalVariationsOnBranches(true)
            .setMarginalVariationsOnHvdc(true)
            .setLossDetailPerCountry(true)
            .setOverloadResultsOnly(true)
            .setShowAllTDandHVDCresults(true)
            .setWithLostLoadDetailedResultsOnContingency(true)
            .setMaxSolverTime(-1)
            .setLossNbRelaunch(1)
            .setLossThreshold(504)
            .setPstCostPenality(0.02f)
            .setHvdcCostPenality(0.03f)
            .setLossOfLoadCost(12000f)
            .setCurativeLossOfLoadCost(26000f)
            .setCurativeLossOfGenerationCost(100f)
            .setContingenciesProbability(0.01f)
            .setNbMaxIteration(4)
            .setNbMaxCurativeAction(2)
            .setNbMaxLostLoadDetailedResults(2)
            .setGapVariableCost(9998)
            .setNbThreatResults(3)
            .setRedispatchingCostOffset(333)
            .setAdequacyCostOffset(44)
            .setCurativeRedispatchingLimit(1111);

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                    "parameters {",
                    "    computationType OPF",
                    "    withGridCost false",
                    "    preCurativeResults true",
                    "    outagesBreakingConnexity true",
                    "    remedialActionsBreakingConnexity true",
                    "    analogousRemedialActionDetection true",
                    "    propagateBranchTripping true",
                    "    withAdequacyResults false",
                    "    withRedispatchingResults true",
                    "    marginalVariationsOnBranches true",
                    "    marginalVariationsOnHvdc true",
                    "    lossDetailPerCountry true",
                    "    overloadResultsOnly true",
                    "    showAllTDandHVDCresults true",
                    "    withLostLoadDetailedResultsOnContingency true",
                    "    lossFactor 5",
                    "    lossNbRelaunch 1",
                    "    lossThreshold 504",
                    "    pstCostPenality 0.02",
                    "    hvdcCostPenality 0.03",
                    "    lossOfLoadCost 12000",
                    "    curativeLossOfLoadCost 26000",
                    "    curativeLossOfGenerationCost 100",
                    "    contingenciesProbability 0.01",
                    "    maxSolverTime (-1)",
                    "    nominalU 103",
                    "    nbMaxIteration 4",
                    "    nbMaxCurativeAction 2",
                    "    nbMaxLostLoadDetailedResults 2",
                    "    gapVariableCost 9998",
                    "    nbThreatResults 3",
                    "    redispatchingCostOffset 333",
                    "    adequacyCostOffset 44",
                    "    curativeRedispatchingLimit 1111",
                    "}"
            ));
        }

        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new TimeSeriesMappingConfig(network), null);
            assertEquals(expectedParameters, parameters);
        }
    }

    @Test
    void detailedMarginalVariationsTest() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "def list = ['FP.AND1  FVERGE1  1', 'FP.AND1  FVERGE1  2', 'FS.BIS1  FVALDI1  1', 'FS.BIS1  FVALDI1  2']",
                "for (line in list) {",
                "  branch(line) {",
                "    contingencyDetailedMarginalVariations 'cty1', 'cty2'",
                "  }",
                "}",
                "branch('FVALDI1  FTDPRA1  1') {",
                "  contingencyDetailedMarginalVariations list",
                "}"));
        }

        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            MetrixDslData data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new TimeSeriesMappingConfig(network), null);

            assertEquals(5, data.getContingencyDetailedMarginalVariationsList().size());
            List<String> list = List.of("cty1", "cty2");
            assertEquals(list, data.getContingencyDetailedMarginalVariations("FP.AND1  FVERGE1  1"));
            assertEquals(list, data.getContingencyDetailedMarginalVariations("FP.AND1  FVERGE1  2"));
            assertEquals(list, data.getContingencyDetailedMarginalVariations("FS.BIS1  FVALDI1  1"));
            assertEquals(list, data.getContingencyDetailedMarginalVariations("FS.BIS1  FVALDI1  2"));
            assertEquals(List.of("FP.AND1  FVERGE1  1", "FP.AND1  FVERGE1  2", "FS.BIS1  FVALDI1  1", "FS.BIS1  FVALDI1  2"),
                data.getContingencyDetailedMarginalVariations("FVALDI1  FTDPRA1  1"));
        }
    }

    @Test
    void testWrongId() throws IOException {
        try (OutputStream os = Files.newOutputStream(dslFile);
             InputStream is = Objects.requireNonNull(
                 MetrixDslDataLoaderTest.class.getResourceAsStream("/inputs/wrongId.groovy"))
        ) {
            IOUtils.copy(is, os);
        }

        MetrixDslData data;
        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new TimeSeriesMappingConfig(network), null);

            assertEquals(0, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.MONITORING).count());
            assertEquals(0, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.RESULT).count());
            assertEquals(0, data.getBranchMonitoringNkList().stream().filter(a -> data.getBranchMonitoringNk(a) == MetrixInputData.MonitoringType.MONITORING).count());
            assertEquals(0, data.getBranchMonitoringNkList().stream().filter(a -> data.getBranchMonitoringNk(a) == MetrixInputData.MonitoringType.RESULT).count());
            assertEquals(0, data.getPtcContingenciesList().size());
            assertEquals(MetrixPtcControlType.FIXED_ANGLE_CONTROL, data.getPtcControl("FP.AND1  FTDPRA1  1"));
            assertEquals(0, data.getHvdcContingenciesList().size());
            assertEquals(MetrixHvdcControlType.FIXED, data.getHvdcControl("HVDC1"));
            assertEquals(MetrixHvdcControlType.FIXED, data.getHvdcControl("HVDC2"));
            assertEquals(0, data.getSectionList().size());
            assertEquals(0, data.getGeneratorsForAdequacy().size());
            assertEquals(0, data.getGeneratorsForRedispatching().size());
            assertEquals(0, data.getPreventiveLoadsList().size());
            assertEquals(0, data.getCurativeLoadsList().size());

        }
    }

    @Test
    void testOverloadedConfiguration() throws IOException {

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("ts1", index, 1d, 1d),
            TimeSeries.createDouble("ts2", index, 1d, 1d),
            TimeSeries.createDouble("ts3", index, 1d, 1d),
            TimeSeries.createDouble("ts4", index, 1d, 1d)
        );

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "branch('FVALDI1  FTDPRA1  1') {",
                "    branchRatingsBaseCase 'ts1'",
                "    branchRatingsOnContingency 'ts1'",
                "}",
                "branch('FVALDI1  FTDPRA1  1') {",
                "    branchRatingsBaseCase 'ts2'",
                "}",
                "generator('FSSV.O11_G') {",
                "    adequacyDownCosts 'ts1'",
                "    adequacyUpCosts 'ts1'",
                "    redispatchingDownCosts 'ts1'",
                "    redispatchingUpCosts 'ts1'",
                "}",
                "generator('FSSV.O11_G') {",
                "    adequacyUpCosts 'ts2'",
                "    redispatchingDownCosts 'ts3'",
                "    redispatchingUpCosts 'ts4'",
                "}"
            ));
        }

        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig(network);
        MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), mappingConfig);

        assertEquals(ImmutableSet.of(new MappingKey(MetrixVariable.THRESHOLD_N1, "FVALDI1  FTDPRA1  1"),
            new MappingKey(MetrixVariable.OFF_GRID_COST_DOWN, "FSSV.O11_G"),
            new MappingKey(MetrixVariable.OFF_GRID_COST_UP, "FSSV.O11_G")), mappingConfig.getTimeSeriesToEquipment().get("ts1"));
        assertEquals(ImmutableSet.of(new MappingKey(MetrixVariable.THRESHOLD_N, "FVALDI1  FTDPRA1  1")), mappingConfig.getTimeSeriesToEquipment().get("ts2"));
        assertEquals(ImmutableSet.of(new MappingKey(MetrixVariable.ON_GRID_COST_DOWN, "FSSV.O11_G")), mappingConfig.getTimeSeriesToEquipment().get("ts3"));
        assertEquals(ImmutableSet.of(new MappingKey(MetrixVariable.ON_GRID_COST_UP, "FSSV.O11_G")), mappingConfig.getTimeSeriesToEquipment().get("ts4"));
    }

    @Test
    void testBoundVariables() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), "generatorsGroup('binding1') {}", // missing filter -> ignored
                "generatorsGroup('binding2') {",
                "  filter { true }",
                "}",
                "generatorsGroup('binding2') {",
                "  filter { false }",
                "}",
                "generatorsGroup('binding3') {",
                "  filter { voltageLevel.id == 'FSSV.O1' }",
                "  referenceVariable POBJ",
                "}",
                "generatorsGroup('binding4') {",
                "  filter { generator.id == 'FVALDI11_G' }", // singleton list -> ignored
                "}",
                "generatorsGroup('binding5;') {",
                "  filter { true }", //semi-colon in name -> ignored
                "}",

                "loadsGroup('binding1') {}", // missing filter -> ignored
                "loadsGroup('binding2') {",
                "  filter { true }",
                "}",
                "loadsGroup('binding2') {",
                "  filter { false }",
                "}",
                "loadsGroup('binding3') {",
                "  filter { voltageLevel.id == 'FVALDI1' }",
                "}",
                "loadsGroup('binding4') {",
                "  filter { load.id == 'FVALDI11_L' }", // singleton list -> ignored
                "}",
                "loadsGroup('bin;ding5') {",
                "  filter { 1 }", // semi-colon in name -> ignored
                "}"
            ));
        }

        MetrixDslData data;
        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new TimeSeriesMappingConfig(network), null);
        }

        assertEquals(2, data.getGeneratorsBindings().size());
        Iterator<MetrixGeneratorsBinding> bindings = data.getGeneratorsBindingsValues().iterator();
        assertEquals(new MetrixGeneratorsBinding("binding2", ImmutableSet.of("FSSV.O11_G", "FSSV.O12_G", "FVALDI11_G", "FVERGE11_G")), bindings.next());
        assertEquals(new MetrixGeneratorsBinding("binding3", ImmutableSet.of("FSSV.O11_G", "FSSV.O12_G"), MetrixGeneratorsBinding.ReferenceVariable.POBJ), bindings.next());

        assertEquals(2, data.getLoadsBindings().size());
        Iterator<MetrixLoadsBinding> loadBindings = data.getLoadsBindingsValues().iterator();
        assertEquals(new MetrixLoadsBinding("binding2", ImmutableSet.of("FVALDI11_L", "FSSV.O11_L", "FVERGE11_L", "FVALDI11_L2")), loadBindings.next());
        assertEquals(new MetrixLoadsBinding("binding3", ImmutableSet.of("FVALDI11_L", "FVALDI11_L2")), loadBindings.next());
    }

    @Test
    void specificContingenciesTest() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), "contingencies {",
                "  specificContingencies 'a', 'b', 'c'",
                "}"));
        }

        MetrixDslData data;
        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new TimeSeriesMappingConfig(network), null);
        }

        assertEquals(ImmutableSet.of("a", "b", "c"), data.getSpecificContingenciesList());

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), "contingencies {",
                "  specificContingencies 'a'",
                "}"));
        }

        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new TimeSeriesMappingConfig(network), null);
        }

        assertEquals(ImmutableSet.of("a"), data.getSpecificContingenciesList());

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), "def contingenciesList = ['a', 'b', 'c']",
                "contingencies {",
                "  specificContingencies contingenciesList",
                "}"));
        }

        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new DataTableStore(), new TimeSeriesMappingConfig(network), null);
        }

        assertEquals(ImmutableSet.of("a", "b", "c"), data.getSpecificContingenciesList());
    }

    @Test
    void branchRatingsOnSpecificContingenciesTest() throws IOException {

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("tsNk", index, 1d, 1d),
            TimeSeries.createDouble("tsITAMNk", index, 1d, 1d)
        );

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), "branch('FS.BIS1  FVALDI1  1') {",
                "}",
                "branch('FS.BIS1  FVALDI1  2') {",
                "    branchRatingsBaseCase 'tsN'",
                "    branchRatingsOnContingency 'tsN_1'",
                "    branchRatingsBeforeCurative 'tsITAM'",
                "    branchRatingsBeforeCurativeOnSpecificContingency 'tsITAMNk'",
                "}",
                "branch('FVALDI1  FTDPRA1  1') {",
                "    branchRatingsBaseCase 'tsN'",
                "    branchRatingsOnContingency 'tsN_1'",
                "    branchRatingsOnSpecificContingency 'tsNk'",
                "    branchRatingsBeforeCurative 'tsITAM'",
                "}",
                "branch('FVALDI1  FTDPRA1  2') {",
                "    branchRatingsBaseCase 'tsN'",
                "    branchRatingsOnSpecificContingency 'tsNk'",
                "    branchRatingsBeforeCurative 'tsITAM'",
                "    branchRatingsBeforeCurativeOnSpecificContingency 'tsITAMNk'",
                "}"));
        }

        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        assertEquals(3, tsConfig.getEquipmentIds("tsN").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsN_1").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsNk").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsITAM").size());
        assertEquals(1, tsConfig.getEquipmentIds("tsITAMNk").size());

        Iterator<MappingKey> mappingKeyIterator = tsConfig.getEquipmentIds("tsNk").iterator();
        MappingKey mappingKey = mappingKeyIterator.next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.id());
        assertEquals(MetrixVariable.THRESHOLD_NK, mappingKey.mappingVariable());
        mappingKey = mappingKeyIterator.next();
        assertEquals("FVALDI1  FTDPRA1  2", mappingKey.id());
        assertEquals(MetrixVariable.THRESHOLD_NK, mappingKey.mappingVariable());

        mappingKeyIterator = tsConfig.getEquipmentIds("tsITAMNk").iterator();
        mappingKey = mappingKeyIterator.next();
        assertEquals("FVALDI1  FTDPRA1  2", mappingKey.id());
        assertEquals(MetrixVariable.THRESHOLD_ITAM_NK, mappingKey.mappingVariable());
    }

    @Test
    void branchRatingsEndOrTest() throws IOException {

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
            TimeSeries.createDouble("tsNEndOr", index, 1d, 1d),
            TimeSeries.createDouble("tsN_1EndOr", index, 1d, 1d),
            TimeSeries.createDouble("tsNk", index, 1d, 1d),
            TimeSeries.createDouble("tsNkEndOr", index, 1d, 1d),
            TimeSeries.createDouble("tsITAMEndOr", index, 1d, 1d),
            TimeSeries.createDouble("tsITAMNk", index, 1d, 1d),
            TimeSeries.createDouble("tsITAMNkEndOr", index, 1d, 1d)
        );

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), "branch('FS.BIS1  FVALDI1  1') {",
                "}",
                "branch('FS.BIS1  FVALDI1  2') {",
                "    branchRatingsBaseCase 'tsN'",
                "    branchRatingsOnContingency 'tsN_1'",
                "    branchRatingsOnSpecificContingency 'tsNk'",
                "    branchRatingsBeforeCurative 'tsITAM'",
                "    branchRatingsBeforeCurativeOnSpecificContingency 'tsITAMNk'",
                "}",
                "branch('FVALDI1  FTDPRA1  1') {",
                "    branchRatingsBaseCaseEndOr 'tsNEndOr'",
                "    branchRatingsOnContingency 'tsN_1'",
                "    branchRatingsOnContingencyEndOr 'tsN_1EndOr'",
                "    branchRatingsOnSpecificContingency 'tsNk'",
                "    branchRatingsOnSpecificContingencyEndOr 'tsNkEndOr'",
                "    branchRatingsBeforeCurative 'tsITAM'",
                "    branchRatingsBeforeCurativeEndOr 'tsITAMEndOr'",
                "    branchRatingsBeforeCurativeOnSpecificContingency 'tsITAMNk'",
                "    branchRatingsBeforeCurativeOnSpecificContingencyEndOr 'tsITAMNkEndOr'",
                "}",
                "branch('FVALDI1  FTDPRA1  2') {",
                "    branchRatingsBaseCase 'tsN'",
                "    branchRatingsBaseCaseEndOr 'tsNEndOr'",
                "    branchRatingsOnContingencyEndOr 'tsN_1EndOr'",
                "    branchRatingsOnSpecificContingencyEndOr 'tsNkEndOr'",
                "    branchRatingsBeforeCurativeEndOr 'tsITAMEndOr'",
                "    branchRatingsBeforeCurativeOnSpecificContingencyEndOr 'tsITAMNkEndOr'",
                "}"));
        }

        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        assertEquals(2, tsConfig.getEquipmentIds("tsN").size());
        assertEquals(1, tsConfig.getEquipmentIds("tsNEndOr").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsN_1").size());
        assertEquals(1, tsConfig.getEquipmentIds("tsN_1EndOr").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsNk").size());
        assertEquals(1, tsConfig.getEquipmentIds("tsNkEndOr").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsITAM").size());
        assertEquals(1, tsConfig.getEquipmentIds("tsITAMEndOr").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsITAMNk").size());
        assertEquals(1, tsConfig.getEquipmentIds("tsITAMNkEndOr").size());

        MappingKey mappingKey = tsConfig.getEquipmentIds("tsNEndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  2", mappingKey.id());
        assertEquals(MetrixVariable.THRESHOLD_N_END_OR, mappingKey.mappingVariable());

        mappingKey = tsConfig.getEquipmentIds("tsN_1EndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.id());
        assertEquals(MetrixVariable.THRESHOLD_N1_END_OR, mappingKey.mappingVariable());

        mappingKey = tsConfig.getEquipmentIds("tsNkEndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.id());
        assertEquals(MetrixVariable.THRESHOLD_NK_END_OR, mappingKey.mappingVariable());

        mappingKey = tsConfig.getEquipmentIds("tsITAMEndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.id());
        assertEquals(MetrixVariable.THRESHOLD_ITAM_END_OR, mappingKey.mappingVariable());

        mappingKey = tsConfig.getEquipmentIds("tsITAMNkEndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.id());
        assertEquals(MetrixVariable.THRESHOLD_ITAM_NK_END_OR, mappingKey.mappingVariable());
    }

    @Test
    void testPSTParameters() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                    "def contingenciesList = network.branches.collect()",
                    "branch('FP.AND1  FVERGE1  1') {",
                    "    contingencyFlowResults contingenciesList",
                    "}",
                    "generator('FSSV.O11_G') {",
                    "    redispatchingDownCosts (-10)",
                    "    redispatchingUpCosts 100",
                    "    onContingencies contingenciesList",
                    "}",
                    "hvdc('HVDC1') {",
                    "    controlType OPTIMIZED",
                    "    onContingencies contingenciesList",
                    "    flowResults true",
                    "}",
                    "phaseShifter('FP.AND1  FTDPRA1  1') {",
                    "    controlType OPTIMIZED_ANGLE_CONTROL",
                    "    onContingencies contingenciesList",
                    "    preventiveLowerTapRange 3",
                    "    preventiveUpperTapRange 5",
                    "    angleTapResults true",
                    "}",
                    "load('FVALDI11_L') {",
                    "    curativeSheddingPercentage 50",
                    "    curativeSheddingCost 12345",
                    "    onContingencies contingenciesList",
                    "}"));
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        assertEquals(Integer.valueOf(3), data.getPtcLowerTapChange("FP.AND1  FTDPRA1  1"));
        assertEquals(Integer.valueOf(5), data.getPtcUpperTapChange("FP.AND1  FTDPRA1  1"));
    }

    @Test
    void testAnalysisBranchMonitoring() throws IOException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                    "branch('FVALDI1  FTDPRA1  1') {",
                    "    baseCaseFlowResults true",
                    "    maxThreatFlowResults true",
                    "    branchAnalysisRatingsBaseCase 100",
                    "    branchAnalysisRatingsOnContingency 500",
                    "    branchAnalysisRatingsBaseCaseEndOr 200",
                    "    branchAnalysisRatingsOnContingencyEndOr 200",
                    "}",
                    "branch('FS.BIS1  FVALDI1  1') {",
                    "    branchAnalysisRatingsBaseCase 'ts1'",
                    "}"
            ));
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig);

        assertEquals(2, data.getBranchMonitoringNList().size());
        assertEquals(MetrixInputData.MonitoringType.RESULT, data.getBranchMonitoringN("FVALDI1  FTDPRA1  1"));
        assertEquals(MetrixInputData.MonitoringType.RESULT, data.getBranchMonitoringN("FS.BIS1  FVALDI1  1"));
        assertEquals(4, tsConfig.getTimeSeriesToEquipment().size());
    }

    @Test
    void writeLogTest() throws IOException {
        String script = "writeLog(\"LOG_TYPE\", \"LOG_SECTION\", \"LOG_MESSAGE\")";
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer out = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            MetrixDslDataLoader.load(new StringReader(script), network, parameters, store, new DataTableStore(), new TimeSeriesMappingConfig(), out);
        }

        String output = outputStream.toString();
        String expectedMessage = "LOG_TYPE;LOG_SECTION;LOG_MESSAGE" + System.lineSeparator();
        assertEquals(expectedMessage, output);
    }
}
