/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.network.MetrixNetworkPoint;
import com.powsybl.timeseries.*;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.EPSILON_COMPARISON;
import static org.junit.Assert.*;

public class MetrixRunResultTest extends AbstractConverterTest {

    @Test
    public void metrixResultTest() throws IOException, URISyntaxException {

        MetrixOutputData results = new MetrixOutputData(10, 5);
        Path workingDir = Paths.get(getClass().getResource("/").toURI());
        results.readFile(workingDir, 10); // 0 n-k result
        results.readFile(workingDir, 11); // 1 n-k result, no remedial action, with marginal costs
        results.readFile(workingDir, 12); // 5 n-k results
        results.readFile(workingDir, 13); // missing
        results.readFile(workingDir, 14); // 5 n-k results with remedial actions

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-17T00:00:00Z"), Duration.ofDays(1));

        // Results file writing
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        results.createTimeSeries(index, timeSeriesList);

        TimeSeriesTable table = new TimeSeriesTable(1, 1, index);
        table.load(1, timeSeriesList);

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            table.writeCsv(writer, new TimeSeriesCsvConfig(ZoneId.of("UTC")));
            bufferedWriter.flush();

            String actual = writer.toString();
            compareTxt(getClass().getResourceAsStream("/metrixResults.csv"), new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    public void metrixNetworkPointResultTest() throws IOException, URISyntaxException {

        MetrixOutputData results = new MetrixOutputData(10, 5);
        Path workingDir = Paths.get(getClass().getResource("/").toURI());
        results.readFile(workingDir, 10); // 0 n-k result
        results.readFile(workingDir, 11); // 1 n-k result, no remedial action, with marginal costs
        results.readFile(workingDir, 12); // 5 n-k results
        results.readFile(workingDir, 13); // missing
        results.readFile(workingDir, 14); // 5 n-k results with remedial actions

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-17T00:00:00Z"), Duration.ofDays(1));

        // Results file writing
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        results.createTimeSeries(index, timeSeriesList);

        // Preventive time series
        Network network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
        MetrixNetworkPoint.addTimeSeriesValues(1, 14, false, "NIORTL41SSFLO", timeSeriesList, network);
        assertEquals(480 + 4.9, network.getGenerator("FSSV.O11_G").getTargetP(), EPSILON_COMPARISON);
        assertEquals(480 - 45.155, network.getLoad("FSSV.O11_L").getP0(), EPSILON_COMPARISON);
        assertEquals(129.67, network.getHvdcLine("HVDC1").getActivePowerSetpoint(), EPSILON_COMPARISON);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER, network.getHvdcLine("HVDC1").getConvertersMode());
        assertEquals(9, network.getTwoWindingsTransformer("FP.AND1  FTDPRA1  1").getPhaseTapChanger().getTapPosition());
        assertFalse(network.getSwitch("FVERGE1_FP.AND1  FVERGE1  2_DJ7").isOpen());

        // Generator curative time series
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
        MetrixNetworkPoint.addTimeSeriesValues(1, 12, true, "MELLEL41ZMAGD", timeSeriesList, network);
        assertEquals(480 - 150, network.getGenerator("FSSV.O12_G").getTargetP(), EPSILON_COMPARISON);

        // Load curative time series
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
        MetrixNetworkPoint.addTimeSeriesValues(1, 12, true, "MELLEL41ZMAGD", timeSeriesList, network);
        assertEquals(480 + 50, network.getLoad("FSSV.O11_L").getP0(), EPSILON_COMPARISON);

        // Hvdc curative time series
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
        MetrixNetworkPoint.addTimeSeriesValues(1, 14, true, "I.JOUL41LAITI", timeSeriesList, network);
        assertEquals(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER, network.getHvdcLine("HVDC1").getConvertersMode());
        assertEquals(500.0, network.getHvdcLine("HVDC1").getActivePowerSetpoint(), EPSILON_COMPARISON);

        // PhaseTapChanger curative timme series
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
        MetrixNetworkPoint.addTimeSeriesValues(1, 14, true, "MELLEL41ZMAGD", timeSeriesList, network);
        assertEquals(12, network.getTwoWindingsTransformer("FP.AND1  FTDPRA1  1").getPhaseTapChanger().getTapPosition());

        // Topology curative time series
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
        MetrixNetworkPoint.addTimeSeriesValues(1, 14, true, "NIORTL41SSFLO", timeSeriesList, network);
        assertTrue(network.getSwitch("FVERGE1_FP.AND1  FVERGE1  2_DJ7").isOpen()); // Topology time series
    }

}
