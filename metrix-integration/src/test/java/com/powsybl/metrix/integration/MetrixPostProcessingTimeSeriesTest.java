/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetrixPostProcessingTimeSeriesTest {

    private FileSystem fileSystem;

    private Path dslFile;

    private Path mappingFile;

    private Network network;

    private final MetrixParameters parameters = new MetrixParameters();

    @BeforeEach
    public void setUp() throws IOException {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        dslFile = fileSystem.getPath("/test.dsl");
        mappingFile = fileSystem.getPath("/mapping.dsl");
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

        // Create mapping file for use in all tests
        try (Writer writer = Files.newBufferedWriter(mappingFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "timeSeries['tsN'] = 1000",
                "timeSeries['tsN1'] = 2000"
            ));
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void postProcessingTimeSeriesTest() throws IOException {

        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore metrixResultTimeSeries = mock(ReadOnlyTimeSeriesStore.class);
        ReadOnlyTimeSeriesStoreAggregator store = new ReadOnlyTimeSeriesStoreAggregator(new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("tsNEndOr", index, 1500d, 1500d),
                TimeSeries.createDouble("tsN1EndOr", index, 2500d, 2500d)
        ), metrixResultTimeSeries);

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(), "branch('FS.BIS1  FVALDI1  1') {",
                "}",
                "branch('FS.BIS1  FVALDI1  2') {",
                "    baseCaseFlowResults true",
                "    maxThreatFlowResults true",
                "}",
                "branch('FVALDI1  FTDPRA1  1') {",
                "    branchRatingsBaseCase 'tsN'",
                "    branchRatingsOnContingency 'tsN1'",
                "}",
                "branch('FP.AND1  FVERGE1  1') {", // this branch is opened in the network
                "    branchRatingsBaseCase 'tsN'",
                "    branchRatingsOnContingency 'tsN1'",
                "}",
                "branch('FVALDI1  FTDPRA1  2') {",
                "    branchRatingsBaseCase 'tsN'",
                "    branchRatingsBaseCaseEndOr 'tsNEndOr'",
                "    branchRatingsOnContingency 'tsN1'",
                "    branchRatingsOnContingencyEndOr 'tsN1EndOr'",
                "}"));
        }

        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FVALDI1  FTDPRA1  1")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FVALDI1  FTDPRA1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FS.BIS1  FVALDI1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FP.AND1  FVERGE1  1")).thenReturn(false);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FVALDI1  FTDPRA1  1")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FVALDI1  FTDPRA1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FS.BIS1  FVALDI1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FP.AND1  FVERGE1  1")).thenReturn(false);

        MappingParameters mappingParameters = MappingParameters.load();
        TimeSeriesMappingConfig tsConfig = TimeSeriesDslLoader.load(mappingFile, network, mappingParameters, store, null);
        MetrixDslData dslData = MetrixDslDataLoader.load(dslFile, network, parameters, store, tsConfig);

        Map<String, NodeCalc> postProcessingTimeSeries = AbstractMetrix.getPostProcessingTimeSeries(dslData, tsConfig, store, null);
        assertEquals(2 * 5, postProcessingTimeSeries.size());

        NodeCalc flow1 = new TimeSeriesNameNodeCalc("FLOW_FVALDI1  FTDPRA1  1");
        NodeCalc maxThreat1 = new TimeSeriesNameNodeCalc("MAX_THREAT_1_FLOW_FVALDI1  FTDPRA1  1");
        NodeCalc ratingN1 = new IntegerNodeCalc(1000);
        NodeCalc ratingNk1 = new IntegerNodeCalc(2000);

        NodeCalc basecaseLoad1 =  BinaryOperation.multiply(BinaryOperation.div(flow1, ratingN1), new FloatNodeCalc(100f));
        assertEquals(basecaseLoad1, postProcessingTimeSeries.get("basecaseLoad_FVALDI1  FTDPRA1  1"));

        NodeCalc outageLoad1 = BinaryOperation.multiply(BinaryOperation.div(maxThreat1, ratingNk1), new FloatNodeCalc(100f));
        assertEquals(outageLoad1, postProcessingTimeSeries.get("outageLoad_FVALDI1  FTDPRA1  1"));

        NodeCalc basecaseOverload1 = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(flow1, ratingN1),
            BinaryOperation.minus(flow1, ratingN1)),
            BinaryOperation.multiply(BinaryOperation.lessThan(flow1, UnaryOperation.negative(ratingN1)),
                BinaryOperation.minus(flow1, UnaryOperation.negative(ratingN1))));
        assertEquals(basecaseOverload1, postProcessingTimeSeries.get("basecaseOverload_FVALDI1  FTDPRA1  1"));

        NodeCalc outageOverload1 = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(maxThreat1, ratingNk1),
            BinaryOperation.minus(maxThreat1, ratingNk1)),
            BinaryOperation.multiply(BinaryOperation.lessThan(maxThreat1, UnaryOperation.negative(ratingNk1)),
                BinaryOperation.minus(maxThreat1, UnaryOperation.negative(ratingNk1))));
        assertEquals(outageOverload1, postProcessingTimeSeries.get("outageOverload_FVALDI1  FTDPRA1  1"));

        NodeCalc overallOverload1 = BinaryOperation.plus(UnaryOperation.abs(basecaseOverload1), UnaryOperation.abs(outageOverload1));
        assertEquals(overallOverload1, postProcessingTimeSeries.get("overallOverload_FVALDI1  FTDPRA1  1"));

        NodeCalc flow2 = new TimeSeriesNameNodeCalc("FLOW_FVALDI1  FTDPRA1  2");
        NodeCalc maxThreat2 = new TimeSeriesNameNodeCalc("MAX_THREAT_1_FLOW_FVALDI1  FTDPRA1  2");
        NodeCalc ratingN2OrEx = new IntegerNodeCalc(1000);
        NodeCalc ratingN2ExOr = new TimeSeriesNameNodeCalc("tsNEndOr");
        NodeCalc ratingN2 = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(flow2, new IntegerNodeCalc(0)), ratingN2OrEx),
            BinaryOperation.multiply(BinaryOperation.lessThan(flow2, new IntegerNodeCalc(0)), ratingN2ExOr));
        NodeCalc ratingNk2OrEx = new IntegerNodeCalc(2000);
        NodeCalc ratingNk2ExOr = new TimeSeriesNameNodeCalc("tsN1EndOr");

        NodeCalc basecaseLoad2 = BinaryOperation.multiply(BinaryOperation.div(flow2, ratingN2), new FloatNodeCalc(100f));
        assertEquals(basecaseLoad2, postProcessingTimeSeries.get("basecaseLoad_FVALDI1  FTDPRA1  2"));

        NodeCalc outageOverload2 = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(maxThreat2, ratingNk2OrEx),
            BinaryOperation.minus(maxThreat2, ratingNk2OrEx)),
            BinaryOperation.multiply(BinaryOperation.lessThan(maxThreat2, UnaryOperation.negative(ratingNk2ExOr)),
                BinaryOperation.minus(maxThreat2, UnaryOperation.negative(ratingNk2ExOr))));
        assertEquals(outageOverload2, postProcessingTimeSeries.get("outageOverload_FVALDI1  FTDPRA1  2"));
    }
}




