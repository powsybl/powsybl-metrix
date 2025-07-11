/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.google.common.collect.Sets;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreAggregator;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesDataType;
import com.powsybl.timeseries.TimeSeriesMetadata;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class InMemoryTimeSeriesStoreTest {

    @Test
    void test() throws URISyntaxException {
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));
        InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        Path storeFile = Paths.get(Objects.requireNonNull(getClass().getResource("/expected/simpleExport.csv")).toURI());
        store.importTimeSeries(List.of(storeFile));

        assertEquals(Sets.newHashSet("ts2", "ts1"), store.getTimeSeriesNames(null));
        assertTrue(store.timeSeriesExists("ts1"));
        assertFalse(store.timeSeriesExists("ts3"));
        assertEquals(new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, index), store.getTimeSeriesMetadata("ts1").orElseThrow(AssertionError::new));
        assertThat(store.getTimeSeriesMetadata(Sets.newHashSet("ts1", "ts2"))).containsExactlyInAnyOrder(new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, index),
                new TimeSeriesMetadata("ts2", TimeSeriesDataType.DOUBLE, index));
        assertThat(store.getTimeSeriesDataVersions()).containsExactly(1);
        assertThat(store.getTimeSeriesDataVersions("ts1")).containsExactly(1);
        assertThat(store.getDoubleTimeSeries("ts1", 1).orElseThrow(AssertionError::new).toArray()).isEqualTo(new double[]{1d, 2d, 3d});
        assertFalse(store.getDoubleTimeSeries("ts3", 1).isPresent());
        assertFalse(store.getStringTimeSeries("ts3", 1).isPresent());
        assertTrue(store.getStringTimeSeries(Collections.singleton("ts3"), 1).isEmpty());
    }

    @Test
    void notVersionedTest() throws URISyntaxException {
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));
        InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        Path storeFile = Paths.get(Objects.requireNonNull(getClass().getResource("/expected/simpleExport.csv")).toURI());
        Path notVersionedStoreFile = Paths.get(Objects.requireNonNull(getClass().getResource("/notVersionedStore.csv")).toURI());
        store.importTimeSeries(List.of(storeFile, notVersionedStoreFile));

        assertEquals(Sets.newHashSet("ts2", "ts1", "notVersionedTs"), store.getTimeSeriesNames(null));
        assertTrue(store.timeSeriesExists("notVersionedTs"));
        assertEquals(new TimeSeriesMetadata("notVersionedTs", TimeSeriesDataType.DOUBLE, index), store.getTimeSeriesMetadata("notVersionedTs").orElseThrow(AssertionError::new));
        assertThat(store.getTimeSeriesMetadata(Sets.newHashSet("ts1", "ts2", "notVersionedTs"))).containsExactlyInAnyOrder(
                new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, index),
                new TimeSeriesMetadata("ts2", TimeSeriesDataType.DOUBLE, index),
                new TimeSeriesMetadata("notVersionedTs", TimeSeriesDataType.DOUBLE, index));
        assertEquals(Set.of(-1, 1), store.getTimeSeriesDataVersions());
        assertThat(store.getTimeSeriesDataVersions("notVersionedTs")).containsExactly(-1);
        assertThat(store.getDoubleTimeSeries("notVersionedTs", -1).orElseThrow(AssertionError::new).toArray()).isEqualTo(new double[]{10d, 20d, 30d});
        assertThat(store.getDoubleTimeSeries("notVersionedTs", 0).orElseThrow(AssertionError::new).toArray()).isEqualTo(new double[]{10d, 20d, 30d});
        assertThat(store.getDoubleTimeSeries("notVersionedTs", 1).orElseThrow(AssertionError::new).toArray()).isEqualTo(new double[]{10d, 20d, 30d});
    }

    @Test
    void aggregatedStoreTest() throws URISyntaxException {
        InMemoryTimeSeriesStore initialStore = new InMemoryTimeSeriesStore();
        Path storeFile = Paths.get(Objects.requireNonNull(getClass().getResource("/expected/simpleExport.csv")).toURI());
        initialStore.importTimeSeries(List.of(storeFile));

        InMemoryTimeSeriesStore additionalStore = new InMemoryTimeSeriesStore();
        Path additionalStoreFile = Paths.get(Objects.requireNonNull(getClass().getResource("/additionalTimeSeries.csv")).toURI());
        additionalStore.importTimeSeries(List.of(additionalStoreFile));

        ReadOnlyTimeSeriesStore aggregatedStore = new ReadOnlyTimeSeriesStoreAggregator(initialStore, additionalStore);
        assertThat(aggregatedStore.getTimeSeriesNames(null)).isNotEmpty();
        assertThat(aggregatedStore.getTimeSeriesNames(null)).containsExactlyInAnyOrder("ts1", "ts2", "ts8");
        assertTrue(aggregatedStore.timeSeriesExists("ts1"));
        assertTrue(aggregatedStore.timeSeriesExists("ts2"));
        assertTrue(aggregatedStore.timeSeriesExists("ts8"));
        assertEquals(Set.of(1), aggregatedStore.getTimeSeriesDataVersions());
        assertEquals(Set.of(1), aggregatedStore.getTimeSeriesDataVersions("ts1"));
        assertEquals(Set.of(1), aggregatedStore.getTimeSeriesDataVersions("ts2"));
        assertEquals(Set.of(1), aggregatedStore.getTimeSeriesDataVersions("ts8"));

        TimeSeriesMetadata ts1Metadata = initialStore.getTimeSeriesMetadata("ts1").orElseThrow();
        TimeSeriesMetadata ts2Metadata = initialStore.getTimeSeriesMetadata("ts2").orElseThrow();
        TimeSeriesMetadata ts8Metadata = additionalStore.getTimeSeriesMetadata("ts8").orElseThrow();
        List<TimeSeriesMetadata> aggregatedStoreTimeSeriesMetadata = aggregatedStore.getTimeSeriesMetadata(aggregatedStore.getTimeSeriesNames(null));
        assertThat(aggregatedStoreTimeSeriesMetadata).isNotEmpty().containsExactlyInAnyOrder(ts1Metadata, ts2Metadata, ts8Metadata);
    }
}
