/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.IntFunction;

import static com.powsybl.metrix.mapping.AbstractCompareTxt.compareStreamTxt;
import static com.powsybl.metrix.mapping.timeseries.FileSystemTimeSeriesStore.ExistingFilePolicy.APPEND;
import static com.powsybl.metrix.mapping.timeseries.TimeSeriesStoreUtil.isNotVersioned;
import static com.powsybl.metrix.mapping.timeseries.TimeSeriesStoreUtil.toTable;
import static com.powsybl.timeseries.TimeSeries.DEFAULT_VERSION_NUMBER_FOR_UNVERSIONED_TIMESERIES;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
class TimeSeriesStoreUtilsTest {
    private FileSystem fileSystem;

    @BeforeEach
    public void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void export() throws IOException {
        Path output = fileSystem.getPath("output.csv");

        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts2", index, 1d, 3d, 5d);
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(ts1, ts2);

        try (Writer writer = Files.newBufferedWriter(output)) {
            TimeSeriesStoreUtil.writeCsv(store, writer, ';', ZoneId.of(ZoneOffset.UTC.getId()), ImmutableSortedSet.of(1), ImmutableSortedSet.of("ts1", "ts2"));
        } catch (IOException e) {
            fail();
        }

        try (InputStream expected = getClass().getResourceAsStream("/expected/simpleExport.csv")) {
            try (InputStream actual = Files.newInputStream(output)) {
                compareStreamTxt(expected, actual);
            } catch (UncheckedIOException ex) {
                fail();
            }
        }
    }

    @Test
    void notVersionedSingleNumberTest() {
        assertTrue(isNotVersioned(Set.of(DEFAULT_VERSION_NUMBER_FOR_UNVERSIONED_TIMESERIES)));
        assertFalse(isNotVersioned(Set.of(1)));
        assertFalse(isNotVersioned(Set.of(DEFAULT_VERSION_NUMBER_FOR_UNVERSIONED_TIMESERIES, 1)));
    }

    @Test
    void testToTable() throws IOException {
        // TimeSeries index
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StringTimeSeries ts3 = TimeSeries.createString("ts3", index, "a", "b", "c");

        // TimeSeries metadata
        TimeSeriesMetadata ts1Metadata = new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, index);
        TimeSeriesMetadata ts2Metadata = new TimeSeriesMetadata("ts2", TimeSeriesDataType.STRING, index);

        // TimeSeriesStore
        Path resDir = Files.createDirectory(fileSystem.getPath("/tmp"));
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);
        tsStore.importTimeSeries(List.of(ts1, ts3), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts1, ts3), 2, APPEND);

        // Versions
        NavigableSet<Integer> versions = new TreeSet<>(List.of(1, 2));

        // Useful variables
        List<String> fileNames = new ArrayList<>();
        TimeSeriesTable table;
        IntFunction<ByteBuffer> byteBufferAllocator = size -> {
            String fileName = "csv_export_" + UUID.randomUUID();
            ByteBuffer buffer = MmapByteBufferService.INSTANCE.create(fileName, size);
            fileNames.add(fileName);
            return buffer;
        };

        // Case 1: one DoubleTimeSeries
        table = toTable(tsStore, byteBufferAllocator, versions, List.of(ts1Metadata), Set.of("ts1"), new HashSet<>());
        assertEquals(index, table.getTableIndex());
        assertIterableEquals(List.of("ts1"), table.getTimeSeriesNames());
        assertEquals(0, table.getDoubleTimeSeriesIndex("ts1"));
        assertEquals(1d, table.getDoubleValue(1, 0, 0));
        assertEquals(2d, table.getDoubleValue(1, 0, 1));
        assertEquals(3d, table.getDoubleValue(1, 0, 2));
        assertEquals(1, fileNames.size());

        // Case 2: one StringTimeSeries
        table = toTable(tsStore, byteBufferAllocator, versions, List.of(ts2Metadata), new HashSet<>(), Set.of("ts3"));
        assertEquals(index, table.getTableIndex());
        assertIterableEquals(List.of("ts3"), table.getTimeSeriesNames());
        assertEquals(0, table.getStringTimeSeriesIndex("ts3"));
        assertEquals("a", table.getStringValue(1, 0, 0));
        assertEquals("b", table.getStringValue(1, 0, 1));
        assertEquals("c", table.getStringValue(1, 0, 2));
        assertEquals(2, fileNames.size());
    }

    @Test
    void testToTableExceptions() throws IOException {
        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));
        RegularTimeSeriesIndex otherIndex = RegularTimeSeriesIndex.create(now, now.plus(1, ChronoUnit.HOURS), Duration.ofMinutes(30));
        InfiniteTimeSeriesIndex infiniteIndex = new InfiniteTimeSeriesIndex();

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts2", infiniteIndex, 1d, 5d);
        StoredDoubleTimeSeries ts4 = TimeSeries.createDouble("ts4", otherIndex, 1d, 3d, 5d);

        // TimeSeries metadata
        TimeSeriesMetadata ts1Metadata = new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, index);
        TimeSeriesMetadata ts2Metadata = new TimeSeriesMetadata("ts3", TimeSeriesDataType.DOUBLE, infiniteIndex);
        TimeSeriesMetadata ts4Metadata = new TimeSeriesMetadata("ts4", TimeSeriesDataType.DOUBLE, otherIndex);

        // TimeSeriesStore
        Path resDir = Files.createDirectory(fileSystem.getPath("/tmp"));
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts4), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts4), 2, APPEND);

        // Versions
        NavigableSet<Integer> versions = new TreeSet<>(List.of(1, 2));

        // Useful variable
        List<String> fileNames = new ArrayList<>();

        IntFunction<ByteBuffer> byteBufferAllocator = size -> {
            String fileName = "csv_export_" + UUID.randomUUID();
            ByteBuffer buffer = MmapByteBufferService.INSTANCE.create(fileName, size);
            fileNames.add(fileName);
            return buffer;
        };

        // Case 1: different indexes
        List<TimeSeriesMetadata> timeSeriesMetadataListCase1 = List.of(ts1Metadata, ts4Metadata);
        Set<String> doubleTimeSeriesNamesCase1 = Set.of("ts1", "ts4");
        Set<String> stringTimeSeriesNamesCase1 = new HashSet<>();
        TimeSeriesException timeSeriesException = assertThrows(TimeSeriesException.class,
            () -> toTable(tsStore, byteBufferAllocator, versions, timeSeriesMetadataListCase1, doubleTimeSeriesNamesCase1, stringTimeSeriesNamesCase1));
        assertEquals("Impossible to write CSV because index is not unique", timeSeriesException.getMessage());

        // Case 2: Infinite index
        List<TimeSeriesMetadata> timeSeriesMetadataListCase2 = List.of(ts1Metadata, ts2Metadata);
        Set<String> doubleTimeSeriesNamesCase2 = Set.of("ts1", "ts2");
        Set<String> stringTimeSeriesNamesCase2 = new HashSet<>();
        UnsupportedOperationException unsupportedOperationException = assertThrows(UnsupportedOperationException.class,
            () -> toTable(tsStore, byteBufferAllocator, versions, timeSeriesMetadataListCase2, doubleTimeSeriesNamesCase2, stringTimeSeriesNamesCase2));
        assertEquals("Not yet implemented", unsupportedOperationException.getMessage());
        assertEquals(1, fileNames.size()); // a file name was created since the exception comes late in the process
    }
}
