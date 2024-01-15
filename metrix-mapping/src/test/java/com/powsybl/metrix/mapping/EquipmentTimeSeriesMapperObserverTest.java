/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class EquipmentTimeSeriesMapperObserverTest {

    private FileSystem fileSystem;
    private Network network;

    private final int chunkSize = 2;
    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    public void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
        network = NetworkXml.read(getClass().getResourceAsStream("/simpleNetwork.xml"));
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fileSystem.close();
    }

    void checkMappedEquipment(Range<Integer> pointRange, double[] values, Map<String, String> tags) {
        assertThat(pointRange).isEqualTo(Range.closed(0, chunkSize - 1));
        assertThat(values).isEqualTo(new double[]{10, 11});
        assertTrue(tags.containsKey("equipment"));
        assertThat(tags.get("equipment")).isEqualTo("FSSV.O11_G");
        assertTrue(tags.containsKey("variable"));
        assertThat(tags.get("variable")).isEqualTo("targetP");
    }

    void checkNotMappedEquipment(Range<Integer> pointRange, double[] values, Map<String, String> tags) {
        assertThat(pointRange).isEqualTo(Range.closed(0, 2)); // constant time series on full index
        assertThat(values).isEqualTo(new double[]{480});
        assertTrue(tags.containsKey("equipment"));
        assertThat(tags.get("equipment")).isEqualTo("FSSV.O12_G");
        assertTrue(tags.containsKey("variable"));
        assertThat(tags.get("variable")).isEqualTo("targetP");
    }

    @Test
    void equipmentTimeSeriesTest() {
        // Mapping script
        // 2 generators in FSSV.O1 voltageLevel : FSSV.O11_G is mapped, FSSV.O12_G is not
        String script = String.join(System.lineSeparator(),
                "mapToGenerators {",
                "    timeSeriesName 'ts'",
                "    filter { generator.id==\"FSSV.O11_G\" }",
                "}",
                "provideTsGenerators {",
                "    filter { generator.terminal.voltageLevel.id == 'FSSV.O1' }",
                "}");

        // Create time series space
        TimeSeriesIndex regularIndex = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(
                TimeSeries.createDouble("ts", regularIndex, 10d, 11d)
        );

        // Load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // Expected results
        final String mappedGeneratorTsName = "FSSV.O11_G_targetP";
        final String unmappedGeneratorTsName = "FSSV.O12_G_targetP";
        Set<String> expectedTimeSeriesNames = ImmutableSet.of(mappedGeneratorTsName, unmappedGeneratorTsName);

        // Create observer
        EquipmentTimeSeriesMapperObserver observer = new EquipmentTimeSeriesMapperObserver(network, mappingConfig, chunkSize, Range.closed(0, chunkSize)) {
            int nbTs = 0;

            @Override
            public void addTimeSeries(String timeSeriesName, int version, Range<Integer> pointRange, double[] values, Map<String, String> tags, TimeSeriesIndex index) {
                assertTrue(expectedTimeSeriesNames.contains(timeSeriesName));
                assertThat(version).isEqualTo(1);
                assertThat(index).isEqualTo(regularIndex);
                switch (timeSeriesName) {
                    case mappedGeneratorTsName -> checkMappedEquipment(pointRange, values, tags);
                    case unmappedGeneratorTsName -> checkNotMappedEquipment(pointRange, values, tags);
                    default -> fail();
                }
                nbTs++;
            }

            @Override
            public void versionEnd(int version) {
                assertThat(nbTs).isEqualTo(expectedTimeSeriesNames.size());
            }
        };

        // Launch mapper with observer
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, network, new TimeSeriesMappingLogger());
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 1), true, false, false, mappingParameters.getToleranceThreshold());
        mapper.mapToNetwork(store, parameters, ImmutableList.of(observer));
    }
}
