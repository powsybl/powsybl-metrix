/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.timeseries.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.time.ZoneId;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static com.powsybl.timeseries.TimeSeries.DEFAULT_VERSION_NUMBER_FOR_UNVERSIONED_TIMESERIES;

public final class TimeSeriesStoreUtil {

    private TimeSeriesStoreUtil() {
    }

    /**
     * Check if a set of version numbers corresponds to set containing single number for not versioned time series
     */
    public static boolean isNotVersioned(Set<Integer> existingVersions) {
        return Set.of(DEFAULT_VERSION_NUMBER_FOR_UNVERSIONED_TIMESERIES).equals(existingVersions);
    }

    private static void writeCsv(ReadOnlyTimeSeriesStore store, Writer writer, char separator, ZoneId zoneId,
                                NavigableSet<Integer> versions, List<TimeSeriesMetadata> metadataList,
                                Set<String> doubleTimeSeriesNames, Set<String> stringTimeSeriesNames) {
        Objects.requireNonNull(writer);
        List<String> fileNames = new ArrayList<>();
        try {
            TimeSeriesTable table = toTable(store, size -> {
                String fileName = "csv_export_" + UUID.randomUUID();
                ByteBuffer buffer = MmapByteBufferService.INSTANCE.create(fileName, size);
                fileNames.add(fileName);
                return buffer;
            }, versions, metadataList, doubleTimeSeriesNames, stringTimeSeriesNames);
            table.writeCsv(writer, new TimeSeriesCsvConfig(zoneId, separator, true, TimeSeries.TimeFormat.DATE_TIME));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            for (String fileName : fileNames) {
                MmapByteBufferService.INSTANCE.closeAndTryToDelete(fileName);
            }
        }
    }

    public static void writeCsv(ReadOnlyTimeSeriesStore store, Writer writer, char separator, ZoneId zoneId) {
        writeCsv(store, writer, separator, zoneId, new TreeSet<>(store.getTimeSeriesDataVersions()), store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(false)));
    }

    public static void writeCsv(ReadOnlyTimeSeriesStore store, Writer writer, char separator, ZoneId zoneId,
                                NavigableSet<Integer> versions, Set<String> doubleTimeSeriesNames, Set<String> stringTimeSeriesNames) {
        Objects.requireNonNull(writer);
        Objects.requireNonNull(store);

        Set<String> storeNames = store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(false));
        if (storeNames.isEmpty()) {
            throw new IllegalArgumentException("Empty store");
        }

        Set<String> tsNames = new HashSet<>(doubleTimeSeriesNames);
        tsNames.addAll(stringTimeSeriesNames);
        List<TimeSeriesMetadata> metadataList = store.getTimeSeriesMetadata(tsNames);
        writeCsv(store, writer, separator, zoneId, versions, metadataList, doubleTimeSeriesNames, stringTimeSeriesNames);
    }

    public static void writeCsv(ReadOnlyTimeSeriesStore store, Writer writer, char separator, ZoneId zoneId,
                                NavigableSet<Integer> versions, Set<String> names) {
        writeCsv(store, writer, separator, zoneId, versions, names, false);
    }

    public static void writeCsv(ReadOnlyTimeSeriesStore store, Writer writer, char separator, ZoneId zoneId,
                                NavigableSet<Integer> versions, Set<String> names, boolean includeDependencies) {
        Objects.requireNonNull(writer);
        Objects.requireNonNull(store);

        Set<String> storeNames = store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(includeDependencies));
        if (storeNames.isEmpty()) {
            throw new IllegalArgumentException("Empty store");
        }

        List<TimeSeriesMetadata> metadataList = store.getTimeSeriesMetadata(names);

        Set<String> doubleTimeSeriesNames = new HashSet<>();
        Set<String> stringTimeSeriesNames = new HashSet<>();
        for (TimeSeriesMetadata metadata : metadataList) {
            switch (metadata.getDataType()) {
                case DOUBLE -> doubleTimeSeriesNames.add(metadata.getName());
                case STRING -> stringTimeSeriesNames.add(metadata.getName());
                default -> throw new AssertionError("Unexpected data type " + metadata.getDataType());
            }
        }
        writeCsv(store, writer, separator, zoneId, versions, metadataList, doubleTimeSeriesNames, stringTimeSeriesNames);
    }

    public static TimeSeriesTable toTable(ReadOnlyTimeSeriesStore store, IntFunction<ByteBuffer> byteBufferAllocator,
                                   NavigableSet<Integer> versions, List<TimeSeriesMetadata> metadataList,
                                   Set<String> doubleTimeSeriesNames, Set<String> stringTimeSeriesNames) {
        Objects.requireNonNull(store);

        Map<String, TimeSeriesMetadata> metadataMap = new HashMap<>();
        for (TimeSeriesMetadata metadata : metadataList) {
            metadataMap.put(metadata.getName(), metadata);
        }

        // check unique index
        Set<TimeSeriesIndex> indexes = metadataList.stream()
                .map(TimeSeriesMetadata::getIndex)
                .filter(index -> !(index instanceof InfiniteTimeSeriesIndex))
                .collect(Collectors.toSet());
        if (indexes.size() != 1) {
            throw new TimeSeriesException("Impossible to write CSV because index is not unique");
        }

        TimeSeriesTable table = new TimeSeriesTable(versions.first(), versions.last(), indexes.iterator().next(), byteBufferAllocator);
        for (int version : versions) {
            // Initialise mutable lists
            List<DoubleTimeSeries> doubleTimeSeries = new ArrayList<>();
            List<StringTimeSeries> stringTimeSeries = new ArrayList<>();

            // Add the DoubleTimeSeries from the store
            if (!doubleTimeSeriesNames.isEmpty()) {
                doubleTimeSeries.addAll(store.getDoubleTimeSeries(doubleTimeSeriesNames, version));
            }

            // complete time series list to have the same list for each version
            Set<String> missingDoubleTimeSeriesNames = new HashSet<>(doubleTimeSeriesNames);
            doubleTimeSeries.stream().map(ts -> ts.getMetadata().getName()).toList().forEach(missingDoubleTimeSeriesNames::remove);
            doubleTimeSeries.addAll(missingDoubleTimeSeriesNames.stream().map(name -> new StoredDoubleTimeSeries(metadataMap.get(name))).toList());

            // Add the StringTimeSeries from the store
            if (!stringTimeSeriesNames.isEmpty()) {
                stringTimeSeries.addAll(store.getStringTimeSeries(stringTimeSeriesNames, version));
            }

            // complete time series list to have the same list for each version
            Set<String> missingStringTimeSeriesNames = new HashSet<>(stringTimeSeriesNames);
            stringTimeSeries.stream().map(ts -> ts.getMetadata().getName()).toList().forEach(missingStringTimeSeriesNames::remove);
            stringTimeSeries.addAll(missingStringTimeSeriesNames.stream().map(name -> new StringTimeSeries(metadataMap.get(name))).toList());

            table.load(version, doubleTimeSeries, stringTimeSeries);
        }

        return table;
    }

}
