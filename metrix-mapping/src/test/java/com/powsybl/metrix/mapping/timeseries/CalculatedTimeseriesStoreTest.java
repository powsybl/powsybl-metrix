/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.commons.PowsyblException;
import com.powsybl.timeseries.DoubleTimeSeries;
import com.powsybl.timeseries.TimeSeriesException;
import com.powsybl.timeseries.TimeSeriesMetadata;
import com.powsybl.timeseries.ast.BinaryOperation;
import com.powsybl.timeseries.ast.DoubleNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class CalculatedTimeseriesStoreTest {

    @Test
    void test() throws IOException {
        InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(InMemoryTimeSeriesStore.class.getResourceAsStream("/expected/simpleExport.csv"))))) {
            store.importTimeSeries(reader);
        }

        Map<String, NodeCalc> tsNodes = new HashMap<>();

        BinaryOperation ts3 = BinaryOperation.plus(new TimeSeriesNameNodeCalc("ts1"), new TimeSeriesNameNodeCalc("ts2"));
        BinaryOperation ts4 = BinaryOperation.greaterThan(ts3, new DoubleNodeCalc(3d));
        tsNodes.put("ts3", ts3);
        tsNodes.put("ts4", ts4);
        CalculatedTimeSeriesStore timeSeriesStore = new CalculatedTimeSeriesStore(tsNodes, store);

        assertThat(timeSeriesStore.timeSeriesExists("ts3")).isTrue();
        assertThat(timeSeriesStore.timeSeriesExists("ts5")).isFalse();
        assertThat(timeSeriesStore.getTimeSeriesNodeCalc("ts3")).isEqualTo(ts3);
        assertThat(timeSeriesStore.getTimeSeriesNames(null)).containsExactlyInAnyOrder("ts3", "ts4");
        assertThat(timeSeriesStore.getTimeSeriesDataVersions("ts3")).containsExactly(1);
        assertThat(timeSeriesStore.getTimeSeriesDataVersions()).containsExactly(1);
        List<TimeSeriesMetadata> metadatas = timeSeriesStore.getTimeSeriesMetadata(Collections.singleton("ts3"));
        assertThat(metadatas).hasSize(1);
        TimeSeriesMetadata timeSeriesMetadata = metadatas.get(0);
        assertThat(timeSeriesMetadata.getIndex().getType()).isEqualTo("regularIndex");
        List<DoubleTimeSeries> ts31 = timeSeriesStore.getDoubleTimeSeries(Collections.singleton("ts3"), 1);
        assertThat(ts31).hasSize(1);
        assertThat(ts31.get(0).toArray()).isEqualTo(new double[]{2d, 5d, 8d});
        ts31 = timeSeriesStore.getDoubleTimeSeries(Collections.singleton("ts4"), 1);
        assertThat(ts31).hasSize(1);
        assertThat(ts31.get(0).toArray()).isEqualTo(new double[]{0d, 1d, 1d});

        // Second call to getTimeSeriesMetadata in order to check that index computing is not done twice but results are still correct
        List<TimeSeriesMetadata> metadatasSecondCall = timeSeriesStore.getTimeSeriesMetadata(Collections.singleton("ts3"));
        assertThat(metadatasSecondCall).hasSize(1);
        TimeSeriesMetadata timeSeriesMetadataSecondCall = metadatasSecondCall.get(0);
        assertThat(timeSeriesMetadataSecondCall.getIndex().getType()).isEqualTo("regularIndex");
    }

    @Test
    void testWithTags() throws IOException {
        InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(InMemoryTimeSeriesStore.class.getResourceAsStream("/expected/simpleExport.csv"))))) {
            store.importTimeSeries(reader);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(InMemoryTimeSeriesStore.class.getResourceAsStream("/additionalTimeSeries.csv"))))) {
            store.importTimeSeries(reader);
        }
        CalculatedTimeSeriesStore timeSeriesStore = getCalculatedTimeSeriesStore(store);

        // Calculated TimeSeries based on two other calculated TimeSeries
        List<TimeSeriesMetadata> metadatasCalculatedOfCalculated = timeSeriesStore.getTimeSeriesMetadata(Collections.singleton("ts7"));
        assertThat(metadatasCalculatedOfCalculated).hasSize(1);
        TimeSeriesMetadata timeSeriesMetadataCalculatedOfCalculated = metadatasCalculatedOfCalculated.get(0);
        assertThat(timeSeriesMetadataCalculatedOfCalculated.getIndex().getType()).isEqualTo("regularIndex");

        // Unsynchronized index
        Set<String> ts5Name = Collections.singleton("ts5");
        PowsyblException e0 = assertThrows(TimeSeriesException.class, () -> timeSeriesStore.getTimeSeriesMetadata(ts5Name));
        assertEquals("A calculated time series must depend on synchronized time series", e0.getMessage());
    }

    private static CalculatedTimeSeriesStore getCalculatedTimeSeriesStore(InMemoryTimeSeriesStore store) {
        Map<String, NodeCalc> tsNodes = new HashMap<>();

        // Calculated Time Series
        BinaryOperation ts3 = BinaryOperation.plus(new TimeSeriesNameNodeCalc("ts1"), new TimeSeriesNameNodeCalc("ts2"));
        BinaryOperation ts5 = BinaryOperation.plus(new TimeSeriesNameNodeCalc("ts1"), new TimeSeriesNameNodeCalc("ts8"));
        BinaryOperation ts7 = BinaryOperation.plus(new TimeSeriesNameNodeCalc("ts3"), new TimeSeriesNameNodeCalc("ts5"));
        tsNodes.put("ts3", ts3);
        tsNodes.put("ts5", ts5);
        tsNodes.put("ts7", ts7);

        // Tags
        Map<String, String> newInsideTags = new HashMap<>();
        newInsideTags.put("testTag", "testParam");
        Map<String, Map<String, String>> tags = new HashMap<>();
        tags.put("test", newInsideTags);

        return new CalculatedTimeSeriesStore(tsNodes, tags, store);
    }
}
