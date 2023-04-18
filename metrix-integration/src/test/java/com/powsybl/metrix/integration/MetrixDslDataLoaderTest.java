/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;
import com.powsybl.metrix.mapping.*;
import com.powsybl.timeseries.*;
import org.apache.commons.io.IOUtils;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MetrixDslDataLoaderTest {

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
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

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
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void testWrongConfigurations() throws IOException {
        try (OutputStream writer = Files.newOutputStream(dslFile);
             InputStream inputStream = MetrixDslDataLoaderTest.class.getResourceAsStream("/inputs/badConfiguration.groovy")
        ) {
            IOUtils.copy(inputStream, writer);
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);

        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

        assertEquals(2, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.MONITORING).count());
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN("FVALDI1  FTDPRA1  2"));
        assertEquals(3, data.getBranchMonitoringNkList().stream().filter(a -> data.getBranchMonitoringNk(a) == MetrixInputData.MonitoringType.MONITORING).count());
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringNk("FP.AND1  FVERGE1  2"));

        assertEquals(0, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.RESULT).count());

        assertEquals(0, data.getBranchMonitoringNkList().stream().filter(a -> data.getBranchMonitoringNk(a) == MetrixInputData.MonitoringType.RESULT).count());

        assertEquals(0, data.getPtcContingenciesList().size());

        assertEquals(MetrixPtcControlType.FIXED_ANGLE_CONTROL, data.getPtcControl("FP.AND1  FTDPRA1  1"));

        assertEquals(MetrixHvdcControlType.FIXED, data.getHvdcControl("HVDC1"));

        assertEquals(0, data.getHvdcContingenciesList().size());

        assertEquals(1, data.getSectionList().size());
        assertEquals(ImmutableSet.of(new MetrixSection("sectionOk", 2000f,
                ImmutableMap.of("FP.AND1  FTDPRA1  1", 1.1f, "HVDC1", 0.9f, "FVALDI1  FTDPRA1  1", 2f))),
            data.getSectionList());

        assertEquals(0, data.getGeneratorsForAdequacy().size());

        assertEquals(1, data.getGeneratorsForRedispatching().size());

        assertEquals(0, data.getGeneratorContingenciesList().size());
        assertEquals(Collections.emptyList(), data.getGeneratorContingencies("FVALDI11_G"));

        assertEquals(0, data.getPreventiveLoadsList().size());

        assertEquals(0, data.getCurativeLoadsList().size());

        assertEquals(5, tsConfig.getTimeSeriesToEquipment().size()); // tsN, tsN_1, tsITAM, ts3 and ts4

        assertEquals(4, tsConfig.getEquipmentIds("tsN").size());
        Iterator<MappingKey> mappingKeyIterator = tsConfig.getEquipmentIds("tsN").iterator();
        MappingKey mappingKey = mappingKeyIterator.next();
        assertEquals("FS.BIS1  FVALDI1  1", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdN1, mappingKey.getMappingVariable());
        mappingKey = mappingKeyIterator.next();
        assertEquals("FP.AND1  FVERGE1  2", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdN, mappingKey.getMappingVariable());
        mappingKey = mappingKeyIterator.next();
        assertEquals("FP.AND1  FVERGE1  2", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdN1, mappingKey.getMappingVariable());
        mappingKey = mappingKeyIterator.next();
        assertEquals("FVALDI1  FTDPRA1  2", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdN, mappingKey.getMappingVariable());

        assertEquals(1, tsConfig.getEquipmentIds("ts3").size());
        mappingKey = tsConfig.getEquipmentIds("ts3").iterator().next();
        assertEquals("FVALDI11_G", mappingKey.getId());
        assertEquals(MetrixVariable.onGridCostDown, mappingKey.getMappingVariable());

        assertEquals(1, tsConfig.getEquipmentIds("ts4").size());
        mappingKey = tsConfig.getEquipmentIds("ts4").iterator().next();
        assertEquals("FVALDI11_G", mappingKey.getId());
        assertEquals(MetrixVariable.onGridCostUp, mappingKey.getMappingVariable());

    }

    @Test
    void testAutomaticList() throws IOException {

        try (OutputStream os = Files.newOutputStream(dslFile);
             InputStream is = MetrixDslDataLoaderTest.class.getResourceAsStream("/inputs/automaticList.groovy")
        ) {
            IOUtils.copy(is, os);
        }

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);

        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

        Set<String> lineSet = ImmutableSet.of("FP.AND1  FVERGE1  1",
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

        assertEquals(12, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.MONITORING).count());
        assertEquals(1, data.getBranchMonitoringNList().stream().filter(a -> data.getBranchMonitoringN(a) == MetrixInputData.MonitoringType.RESULT).count());
        assertEquals(1, data.getBranchMonitoringNkList().stream().filter(a -> data.getBranchMonitoringNk(a) == MetrixInputData.MonitoringType.MONITORING).count());
        assertEquals(12, data.getBranchMonitoringNkList().stream().filter(a -> data.getBranchMonitoringNk(a) == MetrixInputData.MonitoringType.RESULT).count());

        for (String line : lineSet) {
            assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringN(line));
            assertEquals(MetrixInputData.MonitoringType.RESULT, data.getBranchMonitoringNk(line));
        }
        assertEquals(MetrixInputData.MonitoringType.RESULT, data.getBranchMonitoringN("FP.AND1  FTDPRA1  1"));
        assertEquals(MetrixInputData.MonitoringType.MONITORING, data.getBranchMonitoringNk("FP.AND1  FTDPRA1  1"));

        assertEquals(network.getTwoWindingsTransformerCount(), data.getPtcContingenciesList().size());
        assertEquals(ImmutableList.of("cty"), data.getPtcContingencies("FP.AND1  FTDPRA1  1"));
        assertEquals(MetrixPtcControlType.FIXED_POWER_CONTROL, data.getPtcControl("FP.AND1  FTDPRA1  1"));

        assertEquals(network.getHvdcLineCount(), data.getHvdcContingenciesList().size());
        assertEquals(ImmutableList.of("cty1"), data.getHvdcContingencies("HVDC1"));
        assertEquals(ImmutableList.of("cty1"), data.getHvdcContingencies("HVDC2"));
        assertEquals(MetrixHvdcControlType.OPTIMIZED, data.getHvdcControl("HVDC1"));
        assertEquals(MetrixHvdcControlType.OPTIMIZED, data.getHvdcControl("HVDC2"));

        assertEquals(network.getGeneratorCount(), data.getGeneratorsForAdequacy().size());
        assertEquals(ImmutableSet.of("FSSV.O11_G", "FVALDI11_G", "FSSV.O12_G", "FVERGE11_G"), data.getGeneratorsForAdequacy());
        assertEquals(network.getGeneratorCount(), data.getGeneratorsForRedispatching().size());
        assertEquals(ImmutableSet.of("FSSV.O11_G", "FVALDI11_G", "FSSV.O12_G", "FVERGE11_G"), data.getGeneratorsForRedispatching());
        assertEquals(network.getGeneratorCount(), data.getGeneratorContingenciesList().size());
        assertEquals(ImmutableList.of("cty2", "cty3"), data.getGeneratorContingencies("FSSV.O11_G"));
        assertEquals(ImmutableList.of("cty2", "cty3"), data.getGeneratorContingencies("FSSV.O12_G"));
        assertEquals(ImmutableList.of("cty2", "cty3"), data.getGeneratorContingencies("FVALDI11_G"));
        assertEquals(ImmutableList.of("cty2", "cty3"), data.getGeneratorContingencies("FVERGE11_G"));

        assertEquals(network.getLoadCount(), data.getPreventiveLoadsList().size());
        assertEquals(20, data.getPreventiveLoadPercentage("FSSV.O11_L").intValue());
        assertEquals(21, data.getPreventiveLoadPercentage("FVALDI11_L").intValue());
        assertEquals(22, data.getPreventiveLoadPercentage("FVALDI11_L2").intValue());

        assertEquals(12000f, data.getPreventiveLoadCost("FSSV.O11_L"), 0);
        assertEquals(12001f, data.getPreventiveLoadCost("FVALDI11_L"), 0);
        assertEquals(12002f, data.getPreventiveLoadCost("FVALDI11_L2"), 0);
        assertEquals(network.getLoadCount(), data.getCurativeLoadsList().size());
        assertEquals(ImmutableList.of("cty4"), data.getLoadContingencies("FSSV.O11_L"));
        assertEquals(ImmutableList.of("cty4"), data.getLoadContingencies("FVALDI11_L"));
        assertEquals(ImmutableList.of("cty4"), data.getLoadContingencies("FVALDI11_L2"));

        assertEquals(5f, data.getCurativeLoadPercentage("FSSV.O11_L").floatValue(), 0);
        assertEquals(6f, data.getCurativeLoadPercentage("FVALDI11_L").floatValue(), 0);
        assertEquals(7f, data.getCurativeLoadPercentage("FVALDI11_L2").floatValue(), 0);
        assertEquals(8f, data.getCurativeLoadPercentage("FVERGE11_L").floatValue(), 0);

        assertEquals(ImmutableSet.of("ts1", "ts2", "ts3", "ts4", "ts5", "tsN_1", "tsN", "tsITAM"), tsConfig.getTimeSeriesToEquipment().keySet());
        assertEquals(network.getGeneratorCount(), tsConfig.getTimeSeriesToEquipment().get("ts1").size());
        assertEquals(network.getGeneratorCount(), tsConfig.getTimeSeriesToEquipment().get("ts2").size());
        assertEquals(network.getGeneratorCount(), tsConfig.getTimeSeriesToEquipment().get("ts3").size());
        assertEquals(network.getGeneratorCount(), tsConfig.getTimeSeriesToEquipment().get("ts4").size());
        assertEquals(network.getLoadCount(), tsConfig.getTimeSeriesToEquipment().get("ts5").size());
        assertEquals(network.getLineCount(), tsConfig.getTimeSeriesToEquipment().get("tsN").size());
        assertEquals(network.getTwoWindingsTransformerCount(), tsConfig.getTimeSeriesToEquipment().get("tsN_1").size());
        assertEquals(network.getTwoWindingsTransformerCount(), tsConfig.getTimeSeriesToEquipment().get("tsITAM").size());

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
        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);
        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

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
        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);
        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

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

        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);
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

        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile2, network, mappingParameters, store, null);

        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

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

        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);

        assertThrows(TimeSeriesMappingException.class,
            () -> MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig),
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
            MetrixDslData data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new TimeSeriesMappingConfig(network), null);

            assertEquals(4, data.getContingencyFlowResultList().size());
            List<String> list = ImmutableList.of("cty1", "cty2");
            assertEquals(list, data.getContingencyFlowResult("FP.AND1  FVERGE1  1"));
            assertEquals(list, data.getContingencyFlowResult("FP.AND1  FVERGE1  2"));
            assertEquals(list, data.getContingencyFlowResult("FS.BIS1  FVALDI1  1"));
            assertEquals(list, data.getContingencyFlowResult("FS.BIS1  FVALDI1  2"));
        }
    }

    @Test
    void testParameters() throws IOException {
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
            dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new TimeSeriesMappingConfig(network), null);

            assertEquals(MetrixComputationType.OPF, parameters.getComputationType());
            assertEquals(5f, parameters.getLossFactor(), 0f);
            assertEquals(103, parameters.getNominalU());
            assertFalse(parameters.isWithGridCost().orElse(true));
            assertTrue(parameters.isPreCurativeResults().orElse(false));
            assertTrue(parameters.isOutagesBreakingConnexity().orElse(false));
            assertTrue(parameters.isRemedialActionsBreakingConnexity().orElse(false));
            assertTrue(parameters.isAnalogousRemedialActionDetection().orElse(false));
            assertTrue(parameters.isPropagateBranchTripping());
            assertFalse(parameters.isWithAdequacyResults().orElse(true));
            assertTrue(parameters.isWithRedispatchingResults().orElse(false));
            assertTrue(parameters.isMarginalVariationsOnBranches().orElse(false));
            assertTrue(parameters.isMarginalVariationsOnHvdc().orElse(false));
            assertTrue(parameters.isLossDetailPerCountry().orElse(false));
            assertTrue(parameters.isOverloadResultsOnly().orElse(false));
            assertTrue(parameters.isShowAllTDandHVDCresults().orElse(false));
            assertTrue(parameters.isWithLostLoadDetailedResultsOnContingency().orElse(true));
            assertEquals(-1, parameters.getOptionalMaxSolverTime().getAsInt());
            assertEquals(1, parameters.getOptionalLossNbRelaunch().getAsInt());
            assertEquals(504, parameters.getOptionalLossThreshold().getAsInt());
            assertEquals(0.02f, parameters.getOptionalPstCostPenality().get(), 0f);
            assertEquals(0.03f, parameters.getOptionalHvdcCostPenality().get(), 0f);
            assertEquals(12000f, parameters.getOptionalLossOfLoadCost().get(), 0f);
            assertEquals(26000f, parameters.getOptionalCurativeLossOfLoadCost().get(), 0f);
            assertEquals(100f, parameters.getOptionalCurativeLossOfGenerationCost().get(), 0f);
            assertEquals(0.01f, parameters.getOptionalContingenciesProbability().get(), 0f);
            assertEquals(4, parameters.getOptionalNbMaxIteration().getAsInt());
            assertEquals(2, parameters.getOptionalNbMaxCurativeAction().getAsInt());
            assertEquals(2, parameters.getOptionalNbMaxLostLoadDetailedResults().getAsInt());
            assertEquals(9998, parameters.getOptionalGapVariableCost().getAsInt());
            assertEquals(3, parameters.getOptionalNbThreatResults().getAsInt());
            assertEquals(333, parameters.getOptionalRedispatchingCostOffset().getAsInt());
            assertEquals(44, parameters.getOptionalAdequacyCostOffset().getAsInt());
            assertEquals(1111, parameters.getOptionalCurativeRedispatchingLimit().getAsInt());
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
            MetrixDslData data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new TimeSeriesMappingConfig(network), null);

            assertEquals(5, data.getContingencyDetailedMarginalVariationsList().size());
            List<String> list = ImmutableList.of("cty1", "cty2");
            assertEquals(list, data.getContingencyDetailedMarginalVariations("FP.AND1  FVERGE1  1"));
            assertEquals(list, data.getContingencyDetailedMarginalVariations("FP.AND1  FVERGE1  2"));
            assertEquals(list, data.getContingencyDetailedMarginalVariations("FS.BIS1  FVALDI1  1"));
            assertEquals(list, data.getContingencyDetailedMarginalVariations("FS.BIS1  FVALDI1  2"));
            assertEquals(ImmutableList.of("FP.AND1  FVERGE1  1", "FP.AND1  FVERGE1  2", "FS.BIS1  FVALDI1  1", "FS.BIS1  FVALDI1  2"),
                data.getContingencyDetailedMarginalVariations("FVALDI1  FTDPRA1  1"));
        }
    }

    @Test
    void testWrongId() throws IOException {
        try (OutputStream os = Files.newOutputStream(dslFile);
             InputStream is = MetrixDslDataLoaderTest.class.getResourceAsStream("/inputs/wrongId.groovy")
        ) {
            IOUtils.copy(is, os);
        }

        MetrixDslData data;
        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new TimeSeriesMappingConfig(network), null);

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
        MetrixDslDataLoader.load(dslFile, network, parameters, store, mappingConfig);

        assertEquals(ImmutableSet.of(new MappingKey(MetrixVariable.thresholdN1, "FVALDI1  FTDPRA1  1"),
            new MappingKey(MetrixVariable.offGridCostDown, "FSSV.O11_G"),
            new MappingKey(MetrixVariable.offGridCostUp, "FSSV.O11_G")), mappingConfig.getTimeSeriesToEquipment().get("ts1"));
        assertEquals(ImmutableSet.of(new MappingKey(MetrixVariable.thresholdN, "FVALDI1  FTDPRA1  1")), mappingConfig.getTimeSeriesToEquipment().get("ts2"));
        assertEquals(ImmutableSet.of(new MappingKey(MetrixVariable.onGridCostDown, "FSSV.O11_G")), mappingConfig.getTimeSeriesToEquipment().get("ts3"));
        assertEquals(ImmutableSet.of(new MappingKey(MetrixVariable.onGridCostUp, "FSSV.O11_G")), mappingConfig.getTimeSeriesToEquipment().get("ts4"));
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
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new TimeSeriesMappingConfig(network), null);
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
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new TimeSeriesMappingConfig(network), null);
        }

        assertEquals(ImmutableSet.of("a", "b", "c"), data.getSpecificContingenciesList());

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), "contingencies {",
                "  specificContingencies 'a'",
                "}"));
        }

        try (Reader reader = Files.newBufferedReader(dslFile, StandardCharsets.UTF_8)) {
            MetrixDslDataLoader dslLoader = new MetrixDslDataLoader(reader, dslFile.getFileName().toString());
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new TimeSeriesMappingConfig(network), null);
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
            data = dslLoader.load(network, parameters, new ReadOnlyTimeSeriesStoreCache(), new TimeSeriesMappingConfig(network), null);
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

        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);
        MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

        assertEquals(3, tsConfig.getEquipmentIds("tsN").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsN_1").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsNk").size());
        assertEquals(2, tsConfig.getEquipmentIds("tsITAM").size());
        assertEquals(1, tsConfig.getEquipmentIds("tsITAMNk").size());

        Iterator<MappingKey> mappingKeyIterator = tsConfig.getEquipmentIds("tsNk").iterator();
        MappingKey mappingKey = mappingKeyIterator.next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdNk, mappingKey.getMappingVariable());
        mappingKey = mappingKeyIterator.next();
        assertEquals("FVALDI1  FTDPRA1  2", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdNk, mappingKey.getMappingVariable());

        mappingKeyIterator = tsConfig.getEquipmentIds("tsITAMNk").iterator();
        mappingKey = mappingKeyIterator.next();
        assertEquals("FVALDI1  FTDPRA1  2", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdITAMNk, mappingKey.getMappingVariable());
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

        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);
        MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

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
        assertEquals("FVALDI1  FTDPRA1  2", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdNEndOr, mappingKey.getMappingVariable());

        mappingKey = tsConfig.getEquipmentIds("tsN_1EndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdN1EndOr, mappingKey.getMappingVariable());

        mappingKey = tsConfig.getEquipmentIds("tsNkEndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdNkEndOr, mappingKey.getMappingVariable());

        mappingKey = tsConfig.getEquipmentIds("tsITAMEndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdITAMEndOr, mappingKey.getMappingVariable());

        mappingKey = tsConfig.getEquipmentIds("tsITAMNkEndOr").iterator().next();
        assertEquals("FVALDI1  FTDPRA1  1", mappingKey.getId());
        assertEquals(MetrixVariable.thresholdITAMNkEndOr, mappingKey.getMappingVariable());
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
        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);
        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

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
        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);
        MetrixDslData data = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

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
            MetrixDslDataLoader.load(new StringReader(script), network, parameters, store, new TimeSeriesMappingConfig(), out);
        }

        String output = outputStream.toString();
        assertEquals("LOG_TYPE;LOG_SECTION;LOG_MESSAGE", output);
    }
}




