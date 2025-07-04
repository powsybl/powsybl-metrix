/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.timeseries.InitOptimizedTimeSeriesWriter;
import com.powsybl.metrix.mapping.DataTableStore;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.InputStream;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import static com.powsybl.metrix.integration.AbstractCompareTxt.compareStreamTxt;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
class InitOptimizedTimeSeriesWriterTest {

    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    void setUp() {
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @Test
    void initOptimizedTimeSeriesWriterTest() {
        // Mapping script
        String script = String.join(System.lineSeparator(),
                "mapToPhaseTapChangers {",
                "    timeSeriesName 'ts1'",
                "    filter { twoWindingsTransformer.id == 'FP.AND1  FTDPRA1  1' }",
                "}",
                "mapToHvdcLines {",
                "    timeSeriesName 'ts2'",
                "    filter {hvdcLine.id == 'HVDC1'}",
                "}");

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("ts1", index, 10d, 11d),
                TimeSeries.createDouble("ts2", index, 100d, 110d));

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // Create initOptimizedTimeSeriesWriter
        MetrixDslData metrixDslData = new MetrixDslData();
        metrixDslData.addHvdcFlowResults("HVDC1");
        metrixDslData.addHvdcFlowResults("HVDC2");
        metrixDslData.addPstAngleTapResults("FP.AND1  FTDPRA1  1");
        StringWriter writer = new StringWriter();
        InitOptimizedTimeSeriesWriter initOptimizedTimeSeriesWriter = new InitOptimizedTimeSeriesWriter(network, metrixDslData, Range.closed(0, 1), writer);

        // Create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 1), true, false, false, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        // Launch mapper
        mapper.mapToNetwork(store, List.of(initOptimizedTimeSeriesWriter));

        // Check
        InputStream expected = getClass().getResourceAsStream("/inputs_optimized_time_series.json");
        Assertions.assertNotNull(compareStreamTxt(expected, writer.toString()));
    }
}
