/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.mapping.log.Log;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.util.Collections;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkTopographyChangeNotifierTest {

    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    private TimeSeriesMappingLogger logger;

    private final String minimalScript = String.join(System.lineSeparator(),
            "mapToGenerators {",
            "    timeSeriesName 'constant_ts1'",
            "    filter {generator.id == 'FSSV.O11_G'}",
            "}");

    @BeforeEach
    public void setUp() {
        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

        logger = Mockito.mock(TimeSeriesMappingLogger.class);
    }

    private void runTest(String script) {

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("constant_ts1", index, 100d, 100d)
        );

        // Create NetworkTopographyChangeNotifier
        NetworkTopographyChangeNotifier networkTopographyChangeNotifier = new NetworkTopographyChangeNotifier("id", logger);
        // Add NetworkTopographyChangeNotifier listener
        network.addListener(networkTopographyChangeNotifier);

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        // Create mapper
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 0), true, false, false, mappingParameters.getToleranceThreshold());

        // Launch mapper
        mapper.mapToNetwork(store, parameters, Collections.emptyList());
    }

    @Test
    void checkNetworkCreationNotification() {

        AtomicBoolean hit = new AtomicBoolean(false);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            if (args.length > 0 && args[0] instanceof Log) {
                Log log = (Log) args[0];
                String actualLabel = log.getLabel();
                String expectedLabel = "network update";
                String actualMessage = log.getMessage();
                String expectedMessage = "Network update not applied : Creation of item FP.AND1_1_NEW";
                hit.set(hit.get() || (actualLabel.compareTo(expectedLabel) == 0 && actualMessage.compareTo(expectedMessage) == 0));
            }
            return null;
        }).when(logger).addLog(Mockito.any(Log.class));

        String script = String.join(System.lineSeparator(),
                minimalScript,
                "vl = 'FP.AND1'",
                "bbs = network.getVoltageLevel(vl).nodeBreakerView.busbarSections[0]",
                "loadId = bbs.id + '_NEW'",
                "nextNode = network.getVoltageLevel(vl).nodeBreakerView.nodeCount + 1 ",
                "network.getVoltageLevel(vl).newLoad()",
                "        .setId(loadId)",
                "        .setNode(nextNode)",
                "        .setP0(1)",
                "        .setQ0(0)",
                ".add()"
        );
        runTest(script);
        assertTrue(hit.get());
    }

    @Test
    void checkNetworkUpdateNotification() {

        AtomicBoolean hit = new AtomicBoolean(false);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            if (args.length > 0 && args[0] instanceof Log) {
                Log log = (Log) args[0];
                String actualLabel = log.getLabel();
                String expectedLabel = "network update";
                String actualMessage = log.getMessage();
                String expectedMessage = "Network update not applied : Update of item Disj FSSV.O11_L";
                hit.set(hit.get() || (actualLabel.compareTo(expectedLabel) == 0 && actualMessage.compareTo(expectedMessage) == 0));
            }
            return null;
        }).when(logger).addLog(Mockito.any(Log.class));

        String script = String.join(System.lineSeparator(),
                minimalScript,
                "network.getLoad('FSSV.O11_L').getTerminal().disconnect()"
        );
        runTest(script);
        assertTrue(hit.get());
    }

    @Test
    void checkNetworkRemovalNotification() {

        AtomicBoolean hit = new AtomicBoolean(false);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            if (args.length > 0 && args[0] instanceof Log) {
                Log log = (Log) args[0];
                String actualLabel = log.getLabel();
                String expectedLabel = "network update";
                String actualMessage = log.getMessage();
                String expectedMessage = "Network update not applied : Remove of item FP.AND1  FVERGE1  1";
                hit.set(hit.get() || (actualLabel.compareTo(expectedLabel) == 0 && actualMessage.compareTo(expectedMessage) == 0));
            }
            return null;
        }).when(logger).addLog(Mockito.any(Log.class));

        String script = String.join(System.lineSeparator(),
                minimalScript,
                "network.getLine('FP.AND1  FVERGE1  1').remove()"
        );
        runTest(script);
        assertTrue(hit.get());
    }
}

