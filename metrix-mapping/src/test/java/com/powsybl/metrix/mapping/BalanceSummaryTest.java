/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.*;

import static com.powsybl.metrix.mapping.AbstractCompareTxt.compareStreamTxt;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Disabled
class BalanceSummaryTest {

    private static final char SEPARATOR = ';';

    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    public void setUp() throws IOException {
        // create test network
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @Test
    void balanceSummaryTest() throws Exception {

        String directoryName = "/expected/BalanceSummary/";

        // Mapping script
        String script = String.join(System.lineSeparator(),
                "mapToLoads {",
                "    timeSeriesName 'FVALDI11_L'",
                "    filter {load.id == 'FVALDI11_L'}",
                "}",
                "mapToLoads {",
                "    timeSeriesName 'FSSV.O11_L'",
                "    filter {load.id == 'FSSV.O11_L'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'FSSV.O11_G'",
                "    filter {generator.id == 'FSSV.O11_G'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'FVALDI11_G'",
                "    filter {generator.id == 'FVALDI11_G'}",
                "}",
               "mapToGenerators {",
                "    timeSeriesName 'FVALDI12_G'",
                "    filter {generator.id == 'FVALDI12_G'}",
                "}");

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("FVALDI11_L", index, 475d, 500d),
                TimeSeries.createDouble("FSSV.O11_L", index, 400d, 400d),
                TimeSeries.createDouble("FSSV.O11_G", index, 470d, 510d),
                TimeSeries.createDouble("FVALDI11_G", index, 1000d, 1000d),
                TimeSeries.createDouble("FVALDI12_G", index, 2000d, 2100d)
        );

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // Create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 1),
                false, false, true, mappingParameters.getToleranceThreshold());

        // Create BalanceSummary
        ByteArrayOutputStream balanceSummaryOutput = new ByteArrayOutputStream();
        BalanceSummary balanceSummary = new BalanceSummary(new PrintStream(balanceSummaryOutput));

        // Launch mapper
        mapper.mapToNetwork(store, parameters, ImmutableList.of(balanceSummary));

        // Check balance summary file
        StringWriter balanceSummaryCsvOutput = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(balanceSummaryCsvOutput)) {
            balanceSummary.writeCsv(bufferedWriter, SEPARATOR, ZoneId.of("UTC"));
            bufferedWriter.flush();
            try (InputStream expected = getClass().getResourceAsStream(directoryName + "balanceSummary.csv")) {
                try (InputStream actual = new ByteArrayInputStream(balanceSummaryCsvOutput.toString().getBytes(StandardCharsets.UTF_8))) {
                    assertNotNull(compareStreamTxt(expected, actual));
                }
            }
        }

        // Check balance summary output
        try (InputStream expected = getClass().getResourceAsStream(directoryName + "balanceSummary.txt")) {
            try (InputStream actual = new ByteArrayInputStream(balanceSummaryOutput.toString().getBytes(StandardCharsets.UTF_8))) {
                assertNotNull(compareStreamTxt(expected, actual));
            }
        }
    }

    @Test
    void balanceMappingNokTest() throws Exception {

        String directoryName = "/expected/BalanceSummary/";

        // Mapping script
        String script = String.join(System.lineSeparator(),
                "mapToLoads {",
                "    timeSeriesName 'FSSV.011_L'",
                "    variable p0",
                "    filter {load.id == 'EMPTY'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'FSSV.O11_G'",
                "    filter {generator.id == 'EMPTY'}",
                "}");

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("FSSV.011_L", index, 400d, 600d),
                TimeSeries.createDouble("FSSV.O11_G", index, 5000d, 5000d)
        );

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // Create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 1),
                false, true, true, mappingParameters.getToleranceThreshold());
        // Create BalanceSummary
        ByteArrayOutputStream balanceSummaryOutput = new ByteArrayOutputStream();
        BalanceSummary balanceSummary = new BalanceSummary(new PrintStream(balanceSummaryOutput));

        // Launch mapper
        mapper.mapToNetwork(store, parameters, ImmutableList.of(balanceSummary));

        // Check balance summary file
        StringWriter balanceSummaryCsvOutput = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(balanceSummaryCsvOutput)) {
            balanceSummary.writeCsv(bufferedWriter, SEPARATOR, ZoneId.of("UTC"));
            bufferedWriter.flush();
            try (InputStream expected = getClass().getResourceAsStream(directoryName + "balanceMappingNok.csv")) {
                try (InputStream actual = new ByteArrayInputStream(balanceSummaryCsvOutput.toString().getBytes(StandardCharsets.UTF_8))) {
                    assertNotNull(compareStreamTxt(expected, actual));
                }
            }
        }

        // Check balance summary output
        try (InputStream expected = getClass().getResourceAsStream(directoryName + "balanceMappingNok.txt")) {
            try (InputStream actual = new ByteArrayInputStream(balanceSummaryOutput.toString().getBytes(StandardCharsets.UTF_8))) {
                assertNotNull(compareStreamTxt(expected, actual));
            }
        }
    }
}
