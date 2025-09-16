/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.balance;

import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.TimeSeriesMapper;
import com.powsybl.metrix.mapping.TimeSeriesMapperParameters;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.TimeSeriesMappingLogger;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import static com.powsybl.metrix.mapping.utils.AbstractCompareTxt.compareStreamTxt;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
@Disabled
class BalanceSummaryTest {

    private static final char SEPARATOR = ';';

    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    void setUp() {
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
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 1),
                false, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        // Create BalanceSummary
        ByteArrayOutputStream balanceSummaryOutput = new ByteArrayOutputStream();
        BalanceSummary balanceSummary = new BalanceSummary(new PrintStream(balanceSummaryOutput));

        // Launch mapper
        mapper.mapToNetwork(store, List.of(balanceSummary));

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
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 1),
                false, true, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);
        // Create BalanceSummary
        ByteArrayOutputStream balanceSummaryOutput = new ByteArrayOutputStream();
        BalanceSummary balanceSummary = new BalanceSummary(new PrintStream(balanceSummaryOutput));

        // Launch mapper
        mapper.mapToNetwork(store, List.of(balanceSummary));

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
