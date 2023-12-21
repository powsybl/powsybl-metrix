/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.ImmutableMap;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.integration.network.MetrixNetworkPoint;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.powsybl.metrix.integration.AbstractCompareTxt.compareStreamTxt;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.HVDC_TYPE;
import static com.powsybl.metrix.integration.dataGenerator.MetrixOutputData.PST_TYPE;
import static com.powsybl.metrix.mapping.TimeSeriesMapper.EPSILON_COMPARISON;
import static org.junit.jupiter.api.Assertions.*;

class MetrixRunResultTest {

    private Path workingDir;
    private TimeSeriesIndex index;

    @BeforeEach
    public void setUpRunResultTest() throws URISyntaxException {
        workingDir = Paths.get(getClass().getResource("/").toURI());
        index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-17T00:00:00Z"), Duration.ofDays(1));
    }

    private List<TimeSeries> createResults(List<TimeSeries> initTimeSeries) throws URISyntaxException {
        MetrixOutputData results = new MetrixOutputData(10, 5);
        Path workingDir = Paths.get(getClass().getResource("/").toURI());
        results.readFile(workingDir, 10); // 0 n-k result
        results.readFile(workingDir, 11); // 1 n-k result, no remedial action, with marginal costs
        results.readFile(workingDir, 12); // 5 n-k results
        results.readFile(workingDir, 13); // missing
        results.readFile(workingDir, 14); // 5 n-k results with remedial actions

        // Results file writing
        List<TimeSeries> timeSeries = new ArrayList<>();
        results.createTimeSeries(index, initTimeSeries, timeSeries);

        return timeSeries;
    }

    @Test
    void metrixResultTest() throws IOException, URISyntaxException {
        // Create results
        List<TimeSeries> timeSeriesList = createResults(Collections.emptyList());

        // Results file writing
        TimeSeriesTable table = new TimeSeriesTable(1, 1, index);
        table.load(1, timeSeriesList);

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            table.writeCsv(writer, new TimeSeriesCsvConfig(ZoneId.of("UTC")));
            bufferedWriter.flush();

            String actual = writer.toString();
            assertNotNull(compareStreamTxt(getClass().getResourceAsStream("/metrixResults.csv"),
                    new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
        }
    }

    @Test
    void metrixNetworkPointResultTest() throws URISyntaxException {
        // Create results
        List<TimeSeries> timeSeriesList = createResults(Collections.emptyList());

        // Preventive time series
        Network network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        MetrixNetworkPoint.addTimeSeriesValues(1, 14, false, "NIORTL41SSFLO", timeSeriesList, network);
        assertEquals(480 + 4.9, network.getGenerator("FSSV.O11_G").getTargetP(), EPSILON_COMPARISON);
        assertEquals(480 - 45.155, network.getLoad("FSSV.O11_L").getP0(), EPSILON_COMPARISON);
        assertEquals(129.67, network.getHvdcLine("HVDC1").getActivePowerSetpoint(), EPSILON_COMPARISON);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER, network.getHvdcLine("HVDC1").getConvertersMode());
        assertEquals(9, network.getTwoWindingsTransformer("FP.AND1  FTDPRA1  1").getPhaseTapChanger().getTapPosition());
        assertFalse(network.getSwitch("FVERGE1_FP.AND1  FVERGE1  2_DJ7").isOpen());

        // Generator curative time series
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        MetrixNetworkPoint.addTimeSeriesValues(1, 12, true, "MELLEL41ZMAGD", timeSeriesList, network);
        assertEquals(480 - 150, network.getGenerator("FSSV.O12_G").getTargetP(), EPSILON_COMPARISON);

        // Load curative time series
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        MetrixNetworkPoint.addTimeSeriesValues(1, 12, true, "MELLEL41ZMAGD", timeSeriesList, network);
        assertEquals(480 + 50, network.getLoad("FSSV.O11_L").getP0(), EPSILON_COMPARISON);

        // Hvdc curative time series
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        MetrixNetworkPoint.addTimeSeriesValues(1, 14, true, "I.JOUL41LAITI", timeSeriesList, network);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER, network.getHvdcLine("HVDC1").getConvertersMode());
        assertEquals(500.0, network.getHvdcLine("HVDC1").getActivePowerSetpoint(), EPSILON_COMPARISON);

        // PhaseTapChanger curative timme series
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        MetrixNetworkPoint.addTimeSeriesValues(1, 14, true, "MELLEL41ZMAGD", timeSeriesList, network);
        assertEquals(12, network.getTwoWindingsTransformer("FP.AND1  FTDPRA1  1").getPhaseTapChanger().getTapPosition());

        // Topology curative time series
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
        MetrixNetworkPoint.addTimeSeriesValues(1, 14, true, "NIORTL41SSFLO", timeSeriesList, network);
        assertTrue(network.getSwitch("FVERGE1_FP.AND1  FVERGE1  2_DJ7").isOpen()); // Topology time series
    }

    @Test
    void metrixInitOptimizedResultTest() throws IOException, URISyntaxException {
        // Init time series
        List<TimeSeries> initTimeSeriesList = new ArrayList<>();

        // Init Hvdc time series
        DoubleTimeSeries optimizedHvdc = new StoredDoubleTimeSeries(
                new TimeSeriesMetadata("HVDC_HVDC1", TimeSeriesDataType.DOUBLE, ImmutableMap.of(HVDC_TYPE, "HVDC1"), index),
                new UncompressedDoubleDataChunk(0, new double[] {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 1000d, 2000d, 3000d, 4000d, 5000d, 0d, 0d}));
        DoubleTimeSeries otherHvdc = new StoredDoubleTimeSeries(
                new TimeSeriesMetadata("HVDC_OTHER", TimeSeriesDataType.DOUBLE, ImmutableMap.of(HVDC_TYPE, "OTHER"), index),
                new UncompressedDoubleDataChunk(0, new double[] {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 100d, 200d, 300d, 400d, 500d, 0d, 0d}));
        initTimeSeriesList.add(optimizedHvdc);
        initTimeSeriesList.add(otherHvdc);

        // Init PhaseTapChanger time series
        DoubleTimeSeries optimizedPtc = new StoredDoubleTimeSeries(
                new TimeSeriesMetadata("PST_NIORTL61TDNIO", TimeSeriesDataType.DOUBLE, ImmutableMap.of(PST_TYPE, "NIORTL61TDNIO"), index),
                new UncompressedDoubleDataChunk(0, new double[] {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 10d, 20d, 30d, 40d, 50d, 0d, 0d}));
        DoubleTimeSeries optimizedPtcTap = new StoredDoubleTimeSeries(
                new TimeSeriesMetadata("PST_TAP_NIORTL61TDNIO", TimeSeriesDataType.DOUBLE, ImmutableMap.of(PST_TYPE, "NIORTL61TDNIO"), index),
                new UncompressedDoubleDataChunk(0, new double[] {0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d, 1d, 2d, 3d, 4d, 5d, 0d, 0d}));
        initTimeSeriesList.add(optimizedPtc);
        initTimeSeriesList.add(optimizedPtcTap);

        // Create results
        List<TimeSeries> timeSeriesList = createResults(initTimeSeriesList);

        // Results file writing
        TimeSeriesTable table = new TimeSeriesTable(1, 1, index);
        table.load(1, timeSeriesList);

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            table.writeCsv(writer, new TimeSeriesCsvConfig(ZoneId.of("UTC")));
            bufferedWriter.flush();

            String actual = writer.toString();
            assertNotNull(compareStreamTxt(getClass().getResourceAsStream("/metrixInitOptimizedResults.csv"),
                    new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8))));
        }
    }
}
