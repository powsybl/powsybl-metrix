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
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.DataSourceUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.TreeSet;

@Ignore
public class NetworkPointWriterTest extends AbstractConverterTest {

    private Network network;

    private MappingParameters mappingParameters = MappingParameters.load();

    private void compareTxt(ByteArrayOutputStream stream, String directoryName, String fileName) throws Exception {
        try (InputStream expected = getClass().getResourceAsStream(directoryName + fileName)) {
            try (InputStream actual = new ByteArrayInputStream(stream.toByteArray())) {
                compareTxt(expected, actual);
            }
        }
    }

    @Before
    public void setUp() throws IOException {
        super.setUp();

        // create test network
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
    }

    @Test
    public void networkPointConstantVariantTest() throws Exception {

        String directoryName = "/expected/NetworkPointWriter/";

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
                "}",
                "mapToBreakers {",
                "    timeSeriesName 'switch_ts'",
                "    filter {breaker.id == 'FP.AND1_FP.AND1_DJ_OMN'}",
                "}");

        // Create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));

        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("constant_ts1", index, 100d, 100d),
                TimeSeries.createDouble("constant_ts2", index, 3000d, 3000d),
                TimeSeries.createDouble("variable_ts1", index, 10d, 11d),
                TimeSeries.createDouble("switch_ts", index, 0d, 1d)
        );

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, null);

        // Create NetworkPointWriter
        ByteArrayOutputStream networkPointWriterOutput = new ByteArrayOutputStream();
        DataSource dataSource = DataSourceUtil.createDataSource(Paths.get(getClass().getResource("/").toURI()), network.getId(), null);
        NetworkPointWriter networkPointWriter = new NetworkPointWriter(network, dataSource) {

            @Override
            public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
                if (point != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
                    NetworkXml.write(network, networkPointWriterOutput);

                    // Check network output file
                    String fileName = NetworkPointWriter.getFileName(network, 1, point, index);
                    try {
                        compareTxt(networkPointWriterOutput, directoryName, fileName);
                    } catch (Exception e) {
                        throw new AssertionError("Impossible to check " + fileName);
                    }

                    networkPointWriterOutput.reset();
                    network.getVariantManager().removeVariant("point-" + index.getInstantAt(point));
                }
            }
        };

        // Create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, logger);
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)), Range.closed(0, 1), true, false, mappingParameters.getToleranceThreshold());

        // Launch mapper
        mapper.mapToNetwork(store, parameters, ImmutableList.of(networkPointWriter));
    }
}
