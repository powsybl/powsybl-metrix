/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.commons.MappingVariable;
import com.powsybl.metrix.commons.data.datatable.DataTableStore;
import com.powsybl.metrix.commons.data.timeseries.InMemoryTimeSeriesStore;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.observer.DefaultEquipmentTimeSeriesMapperObserver;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreAggregator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigTableLoader.plannedOutagesEquipmentTsName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class TimeSeriesMapPlannedOutagesTest {

    private FileSystem fileSystem;
    private ReadOnlyTimeSeriesStore store;
    private Network network;

    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());

        // TimeSeriesStore
        InMemoryTimeSeriesStore tsStore = new InMemoryTimeSeriesStore();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(InMemoryTimeSeriesStore.class.getResourceAsStream("/additionalTimeSeries.csv"))))) {
            tsStore.importTimeSeries(reader);
        }
        InMemoryTimeSeriesStore plannedOutageStore = new InMemoryTimeSeriesStore();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(InMemoryTimeSeriesStore.class.getResourceAsStream("/plannedOutagesTimeSeries.csv"))))) {
            plannedOutageStore.importTimeSeries(reader);
        }
        store = new ReadOnlyTimeSeriesStoreAggregator(tsStore, plannedOutageStore);

        // create test network
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.fileSystem.close();
    }

    private void checkEquipment(String expectedTimeSeriesName, double expectedValue, String timeSeriesName, MappingVariable variable, double value) {
        assertEquals(expectedTimeSeriesName, timeSeriesName);
        assertEquals(EquipmentVariable.DISCONNECTED, variable);
        assertEquals(expectedValue, value, 0);
    }

    @Test
    void mapPlannedOutagesWithMapToTest() {
        final String disconnectedIdsTimeSeriesName = "disconnected_ids_time_series";
        final String id1 = "FVALDI11_G";
        final String id2 = "FVALDI12_G";

        // Expected results
        // step index -> disconnected id list
        // 0 : id1
        // 1 : id1, id2
        // 2 : none
        Map<String, double[]> expectedResults = Map.of(
            id1, new double[]{0, 0, 1},
            id2, new double[]{1, 0, 1});

        // mapping script
        String script = String.join(System.lineSeparator(),
                "mapPlannedOutages {",
                "'" + disconnectedIdsTimeSeriesName + "'",
                "}");

        // load mapping script
        TimeSeriesDslLoader dsl = new TimeSeriesDslLoader(script);
        TimeSeriesMappingConfig mappingConfig = dsl.load(network, mappingParameters, store, new DataTableStore(), null);

        // create mapper
        TimeSeriesMappingLogger logger = new TimeSeriesMappingLogger();
        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(1)),
                Range.closed(0, 2), false, false, true, mappingParameters.getToleranceThreshold());
        TimeSeriesMapper mapper = new TimeSeriesMapper(mappingConfig, parameters, network, logger);

        // launch TimeSeriesMapper test
        final Map<Integer, List<String>> actualPointIds = new HashMap<>();
        DefaultEquipmentTimeSeriesMapperObserver observer = new DefaultEquipmentTimeSeriesMapperObserver() {
            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
                String actualId = identifiable.getId();
                actualPointIds.computeIfAbsent(point, k -> new ArrayList<>());
                actualPointIds.get(point).add(actualId);
                String expectedTimeSeriesName = plannedOutagesEquipmentTsName(disconnectedIdsTimeSeriesName, actualId);
                double expectedValue = expectedResults.get(actualId)[point];
                checkEquipment(expectedTimeSeriesName, expectedValue, timeSeriesName, variable, equipmentValue);
            }
        };
        mapper.mapToNetwork(store, List.of(observer));
        assertThat(actualPointIds).containsOnlyKeys(0, 1, 2);
        actualPointIds.values().forEach(ids -> assertThat(ids).containsExactlyInAnyOrder(id1, id2));
    }
}
