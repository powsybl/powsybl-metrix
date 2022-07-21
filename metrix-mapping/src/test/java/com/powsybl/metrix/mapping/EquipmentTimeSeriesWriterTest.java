/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.TreeSet;

import static com.powsybl.metrix.mapping.AbstractCompareTxt.compareStreamTxt;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EquipmentTimeSeriesWriterTest {

    private Network network;

    ReadOnlyTimeSeriesStore store;

    TimeSeriesMapper mapper;

    private final boolean ignoreLimits = false;

    private final boolean ignoreEmptyFilter = false;

    private final boolean identifyConstantTimeSeries = true;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    public void setUp() throws IOException {
        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));

        // Mapping script
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'constant_ts1'",
                "    filter {generator.id == 'FSSV.O11_G'}",
                "    variable maxP",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'constant_ts2'",
                "    filter {generator.id == 'FSSV.O12_G'}",
                "}",
                "mapToGenerators {",
                "    timeSeriesName 'variable_ts1'",
                "    filter {generator.id == 'FSSV.O11_G'}",
                "    variable minP",
                "}");

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));

        store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("constant_ts1", index, 100d, 100d),
                TimeSeries.createDouble("constant_ts2", index, 0d, 0d),
                TimeSeries.createDouble("variable_ts1", index, 10d, 11d)
        );

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        mapper = new TimeSeriesMapper(mappingConfig, network, new TimeSeriesMappingLogger());
    }

    @Test
    void equipmentTimeSeriesConstantVariantTest() throws Exception {

        String directoryName = "/expected/EquipmentTimeSeriesWriter/";

        // Create EquipmentTimeSeriesWriter
        StringWriter equipmentTimeSeriesWriter = new StringWriter();
        EquipmentTimeSeriesWriter equipmentTimeSeriesBufferedWriter = new EquipmentTimeSeriesWriter(new BufferedWriter(equipmentTimeSeriesWriter));

        // Create parameters
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 1), ignoreLimits, ignoreEmptyFilter, identifyConstantTimeSeries, mappingParameters.getToleranceThreshold());
        // Launch mapper
        mapper.mapToNetwork(store, parameters, ImmutableList.of(equipmentTimeSeriesBufferedWriter));

        // Check equipment time series output
        try (InputStream expected = getClass().getResourceAsStream(directoryName + "version_1.csv")) {
            try (InputStream actual = new ByteArrayInputStream(equipmentTimeSeriesWriter.toString().getBytes(StandardCharsets.UTF_8))) {
                assertNotNull(compareStreamTxt(expected, actual));
            }
        }
    }
}
