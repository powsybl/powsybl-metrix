/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetrixPostProcessingTimeSeriesTest {

    private Network network;

    private final MetrixParameters parameters = new MetrixParameters();

    private final String mappingScript = String.join(System.lineSeparator(),
            "timeSeries['tsN'] = 1000",
            "timeSeries['tsN1'] = 2000",
            "timeSeries['tsITAM'] = 2500",
            "timeSeries['tsAnalysisN'] = 3000",
            "timeSeries['tsAnalysisNk'] = 4000",
            "timeSeries['tsAnalysisITAM'] = 5000");

    private final String metrixConfigurationScript = String.join(System.lineSeparator(),
            "branch('FS.BIS1  FVALDI1  1') {",
            "}",
            "branch('FS.BIS1  FVALDI1  2') {",
            "    baseCaseFlowResults true",
            "    maxThreatFlowResults true",
            "}",
            "branch('FVALDI1  FTDPRA1  1') {",
            "    branchRatingsBaseCase 'tsN'",
            "    branchRatingsOnContingency 'tsN1'",
            "    branchRatingsBeforeCurative 'tsITAM'",
            "}",
            "branch('FP.AND1  FVERGE1  1') {", // this branch is opened in the network
            "    branchRatingsBaseCase 'tsN'",
            "    branchRatingsOnContingency 'tsN1'",
            "    branchRatingsBeforeCurative 'tsITAM'",
            "}",
            "branch('FVALDI1  FTDPRA1  2') {",
            "    branchRatingsBaseCase 'tsN'",
            "    branchRatingsBaseCaseEndOr 'tsNEndOr'",
            "    branchRatingsOnContingency 'tsN1'",
            "    branchRatingsOnContingencyEndOr 'tsN1EndOr'",
            "    branchRatingsBeforeCurative 'tsITAM'",
            "    branchRatingsBeforeCurativeEndOr 'tsITAMEndOr'",
            "}",
            "branch('FTDPRA1  FVERGE1  2') {",
            "    branchRatingsBaseCase 'tsN'",
            "}",
            "branch('FTDPRA1  FVERGE1  2') {",
            "    branchAnalysisRatingsBaseCase 'tsAnalysisN'",
            "}",
            "branch('FTDPRA1  FVERGE1  2') {",
            "    branchAnalysisRatingsOnContingency 'tsAnalysisNk'",
            "}",
            "branch('FTDPRA1  FVERGE1  2') {",
            "    branchRatingsOnContingency 'tsN1'",
            "    branchRatingsBeforeCurative 'tsITAM'",
            "}"
    );

    Map<String, NodeCalc> postProcessingTimeSeries;

    @BeforeEach
    public void setUp() throws IOException {
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
    }

    private void verifyBasecaseLoad(String branchName, NodeCalc flow, NodeCalc ratingN) {
        NodeCalc expectedBasecaseLoad = BinaryOperation.multiply(BinaryOperation.div(flow, ratingN), new FloatNodeCalc(100f));
        assertEquals(expectedBasecaseLoad, postProcessingTimeSeries.get("basecaseLoad_" + branchName));
    }

    private void verifyLoad(String branchName, NodeCalc maxThreat, NodeCalc rating, String loadPrefix) {
        NodeCalc outageLoad1 = BinaryOperation.multiply(BinaryOperation.div(maxThreat, rating), new FloatNodeCalc(100f));
        assertEquals(outageLoad1, postProcessingTimeSeries.get(loadPrefix + branchName));
    }

    private void verifyOutageLoad(String branchName, NodeCalc maxThreat, NodeCalc ratingNk) {
        verifyLoad(branchName, maxThreat, ratingNk, "outageLoad_");
    }

    private void verifyItamLoad(String branchName, NodeCalc maxThreat, NodeCalc ratingItam) {
        verifyLoad(branchName, maxThreat, ratingItam, "itamLoad_");
    }

    private NodeCalc verifyBasecaseOverload(String branchName, NodeCalc flow, NodeCalc ratingN) {
        NodeCalc basecaseOverload = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(flow, ratingN),
                        BinaryOperation.minus(flow, ratingN)),
                BinaryOperation.multiply(BinaryOperation.lessThan(flow, UnaryOperation.negative(ratingN)),
                        BinaryOperation.minus(flow, UnaryOperation.negative(ratingN))));
        assertEquals(basecaseOverload, postProcessingTimeSeries.get("basecaseOverload_" + branchName));
        return basecaseOverload;
    }

    private NodeCalc verifyOverload(String branchName, NodeCalc maxThreat, NodeCalc ratingNkExOr, NodeCalc ratingNkOrEx, String overloadPrefix) {
        NodeCalc outageOverload = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(maxThreat, ratingNkExOr),
                        BinaryOperation.minus(maxThreat, ratingNkExOr)),
                BinaryOperation.multiply(BinaryOperation.lessThan(maxThreat, UnaryOperation.negative(ratingNkOrEx)),
                        BinaryOperation.minus(maxThreat, UnaryOperation.negative(ratingNkOrEx))));
        assertEquals(outageOverload, postProcessingTimeSeries.get(overloadPrefix + branchName));
        return outageOverload;
    }

    private NodeCalc verifyOutageOverload(String branchName, NodeCalc maxThreat, NodeCalc ratingNkOrEx, NodeCalc ratingNkExOr) {
        return verifyOverload(branchName, maxThreat, ratingNkOrEx, ratingNkExOr, "outageOverload_");
    }

    private NodeCalc verifyItamOverload(String branchName, NodeCalc maxThreat, NodeCalc ratingItamOrEx, NodeCalc ratingItamExOr) {
        return verifyOverload(branchName, maxThreat, ratingItamOrEx, ratingItamExOr, "itamOverload_");
    }

    private void verifyOverallOverload(String branchName, NodeCalc basecaseOverload, NodeCalc outageOverload, String overallOverloadPrefix) {
        NodeCalc overallOverload = BinaryOperation.plus(UnaryOperation.abs(basecaseOverload), UnaryOperation.abs(outageOverload));
        assertEquals(overallOverload, postProcessingTimeSeries.get(overallOverloadPrefix + branchName));
    }

    private void verifyOverallOverload(String branchName, NodeCalc basecaseOverload, NodeCalc outageOverload) {
        verifyOverallOverload(branchName, basecaseOverload, outageOverload, "overallOverload_");
    }

    private void verifyOverallItamOverload(String branchName, NodeCalc basecaseOverload, NodeCalc outageOverload) {
        verifyOverallOverload(branchName, basecaseOverload, outageOverload, "overallItamOverload_");
    }

    private void verifySimpleBranchPostProcessing() {
        final String branchName = "FVALDI1  FTDPRA1  1";
        NodeCalc flow = new TimeSeriesNameNodeCalc("FLOW_" + branchName);
        NodeCalc maxThreat = new TimeSeriesNameNodeCalc("MAX_THREAT_1_FLOW_" + branchName);
        NodeCalc maxTmpThreat = new TimeSeriesNameNodeCalc("MAX_TMP_THREAT_FLOW_" + branchName);
        NodeCalc ratingN = new IntegerNodeCalc(1000);
        NodeCalc ratingNk = new IntegerNodeCalc(2000);
        NodeCalc ratingItam = new IntegerNodeCalc(2500);

        verifyBasecaseLoad(branchName, flow, ratingN);
        verifyOutageLoad(branchName, maxThreat, ratingNk);
        verifyItamLoad(branchName, maxTmpThreat, ratingItam);
        NodeCalc basecaseOverload = verifyBasecaseOverload(branchName, flow, ratingN);
        NodeCalc outageOverload = verifyOutageOverload(branchName, maxThreat, ratingNk, ratingNk);
        NodeCalc itamOverload = verifyItamOverload(branchName, maxTmpThreat, ratingItam, ratingItam);
        verifyOverallOverload(branchName, basecaseOverload, outageOverload);
        verifyOverallItamOverload(branchName, basecaseOverload, itamOverload);
    }

    private void verifyExOrBranchPostProcessing() {
        final String branchName = "FVALDI1  FTDPRA1  2";
        NodeCalc flow = new TimeSeriesNameNodeCalc("FLOW_" + branchName);
        NodeCalc maxThreat = new TimeSeriesNameNodeCalc("MAX_THREAT_1_FLOW_" + branchName);
        NodeCalc maxTmpThreat = new TimeSeriesNameNodeCalc("MAX_TMP_THREAT_FLOW_" + branchName);
        NodeCalc ratingNOrEx = new IntegerNodeCalc(1000);
        NodeCalc ratingNExOr = new TimeSeriesNameNodeCalc("tsNEndOr");
        NodeCalc ratingN = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(flow, new IntegerNodeCalc(0)), ratingNOrEx),
                BinaryOperation.multiply(BinaryOperation.lessThan(flow, new IntegerNodeCalc(0)), ratingNExOr));
        NodeCalc ratingNkOrEx = new IntegerNodeCalc(2000);
        NodeCalc ratingNkExOr = new TimeSeriesNameNodeCalc("tsN1EndOr");
        NodeCalc ratingItamOrEx = new IntegerNodeCalc(2500);
        NodeCalc ratingItamExOr = new TimeSeriesNameNodeCalc("tsITAMEndOr");

        verifyBasecaseLoad(branchName, flow, ratingN);
        verifyOutageOverload(branchName, maxThreat, ratingNkOrEx, ratingNkExOr);
        verifyItamOverload(branchName, maxTmpThreat, ratingItamOrEx, ratingItamExOr);
    }

    private void verifySeparatedConfigBranchPostProcessing() {
        // For this branch, ratings are configured in separated branch() instructions
        final String branchName = "FTDPRA1  FVERGE1  2";
        NodeCalc flow = new TimeSeriesNameNodeCalc("FLOW_" + branchName);
        NodeCalc maxThreat = new TimeSeriesNameNodeCalc("MAX_THREAT_1_FLOW_" + branchName);
        NodeCalc maxTmpThreat = new TimeSeriesNameNodeCalc("MAX_TMP_THREAT_FLOW_" + branchName);
        NodeCalc ratingN = new IntegerNodeCalc(3000);
        NodeCalc ratingNk = new IntegerNodeCalc(2000);
        NodeCalc ratingItam = new IntegerNodeCalc(2500);

        verifyBasecaseLoad(branchName, flow, ratingN);
        verifyOutageLoad(branchName, maxThreat, ratingNk);
        verifyItamLoad(branchName, maxTmpThreat, ratingItam);
        NodeCalc basecaseOverload = verifyBasecaseOverload(branchName, flow, ratingN);
        NodeCalc outageOverload = verifyOutageOverload(branchName, maxThreat, ratingNk, ratingNk);
        NodeCalc itamOverload = verifyItamOverload(branchName, maxTmpThreat, ratingItam, ratingItam);
        verifyOverallOverload(branchName, basecaseOverload, outageOverload);
        verifyOverallItamOverload(branchName, basecaseOverload, itamOverload);
    }

    @Test
    void postProcessingTimeSeriesTest() {
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("1970-01-01T00:00:00Z/1970-01-02T00:00:00Z"), Duration.ofDays(1));

        ReadOnlyTimeSeriesStore metrixResultTimeSeries = mock(ReadOnlyTimeSeriesStore.class);
        ReadOnlyTimeSeriesStoreAggregator store = new ReadOnlyTimeSeriesStoreAggregator(new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("tsNEndOr", index, 1500d, 1500d),
                TimeSeries.createDouble("tsN1EndOr", index, 2500d, 2500d),
                TimeSeries.createDouble("tsITAM", index, 3500d, 3500d),
                TimeSeries.createDouble("tsITAMEndOr", index, 4500d, 4500d)
        ), metrixResultTimeSeries);

        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FVALDI1  FTDPRA1  1")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FVALDI1  FTDPRA1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FS.BIS1  FVALDI1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FP.AND1  FVERGE1  1")).thenReturn(false);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.FLOW_NAME + "FTDPRA1  FVERGE1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FVALDI1  FTDPRA1  1")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FVALDI1  FTDPRA1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FS.BIS1  FVALDI1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FP.AND1  FVERGE1  1")).thenReturn(false);
        when(metrixResultTimeSeries.timeSeriesExists(AbstractMetrix.MAX_THREAT_PREFIX + "FTDPRA1  FVERGE1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.MAX_TMP_THREAT_FLOW + "FVALDI1  FTDPRA1  1")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.MAX_TMP_THREAT_FLOW + "FVALDI1  FTDPRA1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.MAX_TMP_THREAT_FLOW + "FS.BIS1  FVALDI1  2")).thenReturn(true);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.MAX_TMP_THREAT_FLOW + "FP.AND1  FVERGE1  1")).thenReturn(false);
        when(metrixResultTimeSeries.timeSeriesExists(MetrixOutputData.MAX_TMP_THREAT_FLOW + "FTDPRA1  FVERGE1  2")).thenReturn(true);

        MappingParameters mappingParameters = MappingParameters.load();
        TimeSeriesDslLoader timeSeriesDslLoader = new TimeSeriesDslLoader(mappingScript);
        TimeSeriesMappingConfig mappingConfig = timeSeriesDslLoader.load(network, mappingParameters, store, null);
        MetrixDslDataLoader metrixDslDataLoader = new MetrixDslDataLoader(metrixConfigurationScript);
        MetrixDslData dslData = metrixDslDataLoader.load(network, parameters, store, mappingConfig, null);

        postProcessingTimeSeries = MetrixPostProcessingTimeSeries.getPostProcessingTimeSeries(dslData, mappingConfig, store, null);
        assertEquals(3 * 8, postProcessingTimeSeries.size());

        verifySimpleBranchPostProcessing();
        verifyExOrBranchPostProcessing();
        verifySeparatedConfigBranchPostProcessing();
    }
}
