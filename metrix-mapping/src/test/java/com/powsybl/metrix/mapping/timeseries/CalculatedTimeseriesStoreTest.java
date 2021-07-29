/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.timeseries.DoubleTimeSeries;
import com.powsybl.timeseries.TimeSeriesMetadata;
import com.powsybl.timeseries.ast.BinaryOperation;
import com.powsybl.timeseries.ast.DoubleNodeCalc;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CalculatedTimeseriesStoreTest {

    @Test
    public void test() throws IOException {
        InMemoryTimeSeriesStore store = new InMemoryTimeSeriesStore();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(InMemoryTimeSeriesStore.class.getResourceAsStream("/expected/simpleExport.csv")))) {
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
    }
}
