/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.google.common.collect.Sets;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryTimeSeriesStoreTest {

    @Test
    void test() throws IOException {
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));
        InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(InMemoryTimeSeriesStore.class.getResourceAsStream("/expected/simpleExport.csv")))) {
            store.importTimeSeries(reader);
        }

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
}
