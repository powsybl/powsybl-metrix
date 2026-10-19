/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.writers;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;
import com.powsybl.metrix.commons.data.timeseries.TimeSeriesStoreUtil;
import com.powsybl.timeseries.CompressedDoubleDataChunk;
import com.powsybl.timeseries.DoubleDataChunk;
import com.powsybl.timeseries.DoubleTimeSeries;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.StoredDoubleTimeSeries;
import com.powsybl.timeseries.TimeSeriesDataType;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesMetadata;
import com.powsybl.timeseries.UncompressedDoubleDataChunk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class EquipmentTimeSeriesWriter {

    private static final char SEPARATOR = ';';
    private static final String OUTPUT_FILE_EXTENSION = ".csv";

    private final Path dir;
    private final String filename;
    private final Map<String, List<DoubleDataChunk>> doubleTimeSeries = new HashMap<>();
    private TimeSeriesIndex index;

    public EquipmentTimeSeriesWriter(Path dir, String filename) {
        this.dir = Objects.requireNonNull(dir);
        this.filename = Objects.requireNonNull(filename);
    }

    public void addTimeSeries(String timeSeriesName, Range<Integer> pointRange, double[] values, TimeSeriesIndex index) {
        this.index = Objects.requireNonNull(index);
        DoubleDataChunk chunk;
        if (values.length == 1 && !pointRange.upperEndpoint().equals(pointRange.lowerEndpoint())) {
            int length = pointRange.upperEndpoint() + 1;
            chunk = new CompressedDoubleDataChunk(pointRange.lowerEndpoint(), length, new double[]{values[0]}, new int[]{length});
        } else {
            chunk = new UncompressedDoubleDataChunk(pointRange.lowerEndpoint(), values).tryToCompress();
        }
        doubleTimeSeries.computeIfAbsent(timeSeriesName, k -> new ArrayList<>());
        doubleTimeSeries.get(timeSeriesName).add(chunk);
    }

    public void versionEnd(int version) {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(filename + version + OUTPUT_FILE_EXTENSION), StandardCharsets.UTF_8)) {
            List<DoubleTimeSeries> timeSeriesList = new ArrayList<>();
            for (Map.Entry<String, List<DoubleDataChunk>> e : doubleTimeSeries.entrySet()) {
                String timeSeriesName = e.getKey();
                List<DoubleDataChunk> chunks = e.getValue();
                TimeSeriesMetadata metadata = new TimeSeriesMetadata(timeSeriesName, TimeSeriesDataType.DOUBLE, index);
                timeSeriesList.add(new StoredDoubleTimeSeries(metadata, chunks));
            }
            if (!timeSeriesList.isEmpty()) {
                ReadOnlyTimeSeriesStoreCache store = new ReadOnlyTimeSeriesStoreCache(timeSeriesList);
                TimeSeriesStoreUtil.writeCsv(store, writer, SEPARATOR, ZoneId.of("UTC"), ImmutableSortedSet.of(version), doubleTimeSeries.keySet());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
