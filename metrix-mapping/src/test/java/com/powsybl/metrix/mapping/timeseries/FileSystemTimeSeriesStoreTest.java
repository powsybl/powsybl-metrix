/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.PowsyblException;
import com.powsybl.timeseries.*;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.metrix.mapping.timeseries.FileSystemTimeSeriesStore.ExistingFilePolicy.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FileSystemTimeSeriesStoreTest {
    private FileSystem fileSystem;
    private Path resDir;

    @BeforeEach
    public void setUp() throws IOException {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
        resDir = Files.createDirectory(fileSystem.getPath("/tmp"));
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    @Deprecated(since = "2.3.0")
    void testTsStoreDeprecatedMethod() throws IOException {
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);
        Set<String> emptyTimeSeriesNames = tsStore.getTimeSeriesNames(null);
        assertThat(emptyTimeSeriesNames).isEmpty();

        try (InputStream resourceAsStream = Objects.requireNonNull(FileSystemTimeSeriesStoreTest.class.getResourceAsStream("/testStore.csv"));
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream))
        ) {
            tsStore.importTimeSeries(bufferedReader, true, false);
        }

        assertThat(tsStore.getTimeSeriesNames(null)).isNotEmpty();
        assertThat(tsStore.getTimeSeriesNames(null)).containsExactlyInAnyOrder("BALANCE", "tsX");

        assertTrue(tsStore.timeSeriesExists("BALANCE"));
        assertFalse(tsStore.timeSeriesExists("tsY"));

        assertEquals(Set.of(1), tsStore.getTimeSeriesDataVersions());
        assertEquals(Set.of(1), tsStore.getTimeSeriesDataVersions("BALANCE"));
    }

    @Test
    void testTsStore() throws IOException {
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);
        Set<String> emptyTimeSeriesNames = tsStore.getTimeSeriesNames(null);
        assertThat(emptyTimeSeriesNames).isEmpty();

        try (InputStream resourceAsStream = Objects.requireNonNull(FileSystemTimeSeriesStoreTest.class.getResourceAsStream("/testStore.csv"));
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream))
        ) {
            tsStore.importTimeSeries(bufferedReader, OVERWRITE);
        }

        assertThat(tsStore.getTimeSeriesNames(null)).isNotEmpty();
        assertThat(tsStore.getTimeSeriesNames(null)).containsExactlyInAnyOrder("BALANCE", "tsX");

        assertTrue(tsStore.timeSeriesExists("BALANCE"));
        assertFalse(tsStore.timeSeriesExists("tsY"));

        assertEquals(Set.of(1), tsStore.getTimeSeriesDataVersions());
        assertEquals(Set.of(1), tsStore.getTimeSeriesDataVersions("BALANCE"));
    }

    @Test
    void testStoreOnFile() throws IOException {
        Path file = Files.createFile(resDir.resolve("foo.bar"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new FileSystemTimeSeriesStore(file));
        assertEquals("Path /tmp/foo.bar is not a directory", exception.getMessage());
    }

    @Test
    void testTimeSeriesMetadata() throws IOException {
        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);

        // TimeSeries metadata
        TimeSeriesMetadata ts1Metadata = new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, index);

        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);
        tsStore.importTimeSeries(List.of(ts1), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts1), 2, APPEND);

        // Case 1: TimeSeries is present
        assertTrue(tsStore.getTimeSeriesMetadata("ts1").isPresent());
        assertEquals(ts1Metadata, tsStore.getTimeSeriesMetadata("ts1").get());

        // Case 2: TimeSeries is not present
        assertTrue(tsStore.getTimeSeriesMetadata("ts2").isEmpty());
    }

    @Test
    void testDoubleTimeSeries() throws IOException {
        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts2", index, 1d, 2d, 5d);
        StoredDoubleTimeSeries ts3 = TimeSeries.createDouble("ts3", index, 1d, 3d, 5d);

        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3), 2, APPEND);

        // Case 1: a specific TimeSeries is asked
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        assertEquals(ts1, tsStore.getDoubleTimeSeries("ts1", 1).get());

        // Case 2: some TimeSeries are asked
        List<DoubleTimeSeries> doubleTimeSeriesList = tsStore.getDoubleTimeSeries(Set.of("ts1", "ts2"), 1);
        assertThat(doubleTimeSeriesList).containsExactlyInAnyOrder(ts1, ts2);

        // Case 3: all TimeSeries are asked
        doubleTimeSeriesList = tsStore.getDoubleTimeSeries(1);
        assertThat(doubleTimeSeriesList).containsExactlyInAnyOrder(ts1, ts2, ts3);
    }

    @Test
    void testStringTimeSeries() throws IOException {
        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StringTimeSeries ts1 = TimeSeries.createString("ts1", index, "a", "b", "c");
        StringTimeSeries ts2 = TimeSeries.createString("ts2", index, "a", "b", "c");
        StringTimeSeries ts3 = TimeSeries.createString("ts3", index, "a", "b", "c");

        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3), 2, APPEND);

        // Case 1: a specific TimeSeries is asked
        assertTrue(tsStore.getStringTimeSeries("ts1", 1).isPresent());
        assertEquals(ts1, tsStore.getStringTimeSeries("ts1", 1).get());

        // Case 2: some TimeSeries are asked
        List<StringTimeSeries> stringTimeSeriesList = tsStore.getStringTimeSeries(Set.of("ts1", "ts2"), 1);
        assertThat(stringTimeSeriesList).containsExactlyInAnyOrder(ts1, ts2);
    }

    @Test
    void testDifferentKindOfTimeSeries() throws IOException {
        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts2", index, 1d, 2d, 5d);
        StringTimeSeries ts3 = TimeSeries.createString("ts3", index, "a", "b", "c");
        StringTimeSeries ts4 = TimeSeries.createString("ts4", index, "a", "b", "c");

        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3, ts4), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3, ts4), 2, APPEND);

        // Case 1: a specific TimeSeries is asked
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        assertEquals(ts1, tsStore.getDoubleTimeSeries("ts1", 1).get());
        assertTrue(tsStore.getStringTimeSeries("ts3", 1).isPresent());
        assertEquals(ts3, tsStore.getStringTimeSeries("ts3", 1).get());

        // Case 2: some TimeSeries are asked
        List<DoubleTimeSeries> doubleTimeSeriesList = tsStore.getDoubleTimeSeries(Set.of("ts1", "ts2"), 1);
        assertThat(doubleTimeSeriesList).containsExactlyInAnyOrder(ts1, ts2);
        List<StringTimeSeries> stringTimeSeriesList = tsStore.getStringTimeSeries(Set.of("ts3", "ts4"), 1);
        assertThat(stringTimeSeriesList).containsExactlyInAnyOrder(ts3, ts4);

        // Case 3: all DoubleTimeSeries are asked
        doubleTimeSeriesList = tsStore.getDoubleTimeSeries(1);
        assertThat(doubleTimeSeriesList).containsExactlyInAnyOrder(ts1, ts2);

        // Case 4: an absent TimeSeries is asked
        assertTrue(tsStore.getDoubleTimeSeries("ts4", 1).isEmpty());
    }

    @Test
    void testMultipleTimeSeries() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // Add a file with multiple TimeSeries
        Files.createDirectory(fileSystem.getPath("/tmp/ts2"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/timeseries.json")),
            fileSystem.getPath("/tmp/ts2/1"));

        // An exception is thrown since there are mutiple TimeSeries in the file
        PowsyblException exception = assertThrows(PowsyblException.class, () -> tsStore.getDoubleTimeSeries("ts2", 1));
        assertEquals("Found more than one timeseries", exception.getMessage());
    }

    @Test
    void testTimeSeriesStoreListener() throws IOException {
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // Add or remove listener
        assertThrows(NotImplementedException.class, () -> tsStore.addListener(null), "Not impletemented");
        assertThrows(NotImplementedException.class, () -> tsStore.removeListener(null), "Not impletemented");
    }

    @Test
    void testExceptionOnImport() throws IOException {
        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);

        // List of TimeSeries
        List<TimeSeries> timeSeriesList = List.of(ts1);

        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // Works the first time
        tsStore.importTimeSeries(timeSeriesList, 1, THROW_EXCEPTION);

        // Fails since it already exists
        PowsyblException exception = assertThrows(PowsyblException.class,
            () -> tsStore.importTimeSeries(timeSeriesList, 1, THROW_EXCEPTION));
        assertEquals("Timeserie ts1 already exist", exception.getMessage());
    }

    @Test
    void testExistingFileWithMultipleTimeSeries() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // Add a file with multiple TimeSeries
        Files.createDirectory(fileSystem.getPath("/tmp/ts1"));
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/timeseries.json")),
            fileSystem.getPath("/tmp/ts1/1"));

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);

        // List of TimeSeries
        List<TimeSeries> timeSeriesList = List.of(ts1);

        // An exception is thrown since there are mutiple TimeSeries in the file
        PowsyblException exception = assertThrows(PowsyblException.class,
            () -> tsStore.importTimeSeries(timeSeriesList, 1, APPEND));
        assertEquals("Existing ts file should contain one and only one ts", exception.getMessage());
    }

    @Test
    void testExistingFileWithInfiniteIndex() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now,
            now.plus(2, ChronoUnit.HOURS),
            Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", new InfiniteTimeSeriesIndex(), 1d, 2d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts2", index, 1d, 2d, 3d);

        // List of TimeSeries
        List<TimeSeries> timeSeriesList = List.of(ts1, ts2);

        // Works the first time
        tsStore.importTimeSeries(timeSeriesList, 1, APPEND);

        // Fails since it already exists
        List<TimeSeries> list1 = List.of(ts1);
        List<TimeSeries> list2 = List.of(TimeSeries.createDouble("ts2", new InfiniteTimeSeriesIndex(), 1d, 2d));
        PowsyblException exception = assertThrows(PowsyblException.class,
            () -> tsStore.importTimeSeries(list1, 1, APPEND));
        assertEquals("Cannot append a TimeSeries with infinite index", exception.getMessage());
        exception = assertThrows(PowsyblException.class,
            () -> tsStore.importTimeSeries(list2, 1, APPEND));
        assertEquals("Cannot append a TimeSeries with infinite index", exception.getMessage());
    }

    @Test
    void testManageVersionFile() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now,
            now.plus(2, ChronoUnit.HOURS),
            Duration.ofHours(1));
        RegularTimeSeriesIndex indexBis = RegularTimeSeriesIndex.create(now.plus(3, ChronoUnit.HOURS),
            now.plus(5, ChronoUnit.HOURS),
            Duration.ofHours(1));
        RegularTimeSeriesIndex indexTer = RegularTimeSeriesIndex.create(now.plus(-3, ChronoUnit.HOURS),
            now.plus(-1, ChronoUnit.HOURS),
            Duration.ofHours(1));
        RegularTimeSeriesIndex indexOther = RegularTimeSeriesIndex.create(now.plus(4, ChronoUnit.HOURS),
            now.plus(6, ChronoUnit.HOURS),
            Duration.ofHours(1));
        RegularTimeSeriesIndex indexOverlap = RegularTimeSeriesIndex.create(now.plus(1, ChronoUnit.HOURS),
            now.plus(3, ChronoUnit.HOURS),
            Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts1bis = TimeSeries.createDouble("ts1", indexBis, 4d, 5d, 6d);
        StoredDoubleTimeSeries ts1ter = TimeSeries.createDouble("ts1", indexTer, 7d, 8d, 9d);
        StringTimeSeries ts2 = TimeSeries.createString("ts2", index, "a", "b", "c");
        StringTimeSeries ts2bis = TimeSeries.createString("ts2", indexOther, "d", "e", "f");
        StoredDoubleTimeSeries ts1Overlap = TimeSeries.createDouble("ts1", indexOverlap, 7d, 8d, 9d);

        // Append the TimeSeries
        tsStore.importTimeSeries(List.of(ts1), 1);
        tsStore.importTimeSeries(List.of(ts1bis), 1);
        tsStore.importTimeSeries(List.of(ts2), 1);
        tsStore.importTimeSeries(List.of(ts2bis), 1);

        // Assertions for Double
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        StoredDoubleTimeSeries storedTs1 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts1", 1).get();
        assertArrayEquals(new double[] {1d, 2d, 3d, 4d, 5d, 6d}, storedTs1.toArray());
        assertEquals(2, storedTs1.getChunks().size());
        assertInstanceOf(RegularTimeSeriesIndex.class, storedTs1.getMetadata().getIndex());
        RegularTimeSeriesIndex storedIndex = (RegularTimeSeriesIndex) storedTs1.getMetadata().getIndex();
        assertEquals(978303600000L, storedIndex.getStartTime());
        assertEquals(978321600000L, storedIndex.getEndTime());
        assertEquals(3600000L, storedIndex.getSpacing());

        // Assertions for String
        assertTrue(tsStore.getStringTimeSeries("ts2", 1).isPresent());
        StringTimeSeries storedTs2 = tsStore.getStringTimeSeries("ts2", 1).get();
        assertArrayEquals(new String[] {"a", "b", "c", "d", "e", "f"}, storedTs2.toArray());
        assertEquals(2, storedTs2.getChunks().size());
        assertInstanceOf(IrregularTimeSeriesIndex.class, storedTs2.getMetadata().getIndex());
        IrregularTimeSeriesIndex storedIndex2 = (IrregularTimeSeriesIndex) storedTs2.getMetadata().getIndex();
        assertEquals(978303600000L, storedIndex2.getTimeAt(0));
        assertEquals(978325200000L, storedIndex2.getTimeAt(storedIndex2.getPointCount() - 1));

        // Add another TimeSeries
        tsStore.importTimeSeries(List.of(ts1ter), 1);
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        storedTs1 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts1", 1).get();
        assertArrayEquals(new double[] {7d, 8d, 9d, 1d, 2d, 3d, 4d, 5d, 6d}, storedTs1.toArray());
        assertEquals(3, storedTs1.getChunks().size());
        assertInstanceOf(RegularTimeSeriesIndex.class, storedTs1.getMetadata().getIndex());
        storedIndex = (RegularTimeSeriesIndex) storedTs1.getMetadata().getIndex();
        assertEquals(978292800000L, storedIndex.getStartTime());
        assertEquals(978321600000L, storedIndex.getEndTime());
        assertEquals(3600000L, storedIndex.getSpacing());

        // Test with overlapping indexes
        List<TimeSeries> list1 = List.of(ts1Overlap);
        PowsyblException exception = assertThrows(PowsyblException.class,
            () -> tsStore.importTimeSeries(list1, 1, APPEND));
        assertEquals("Indexes to concatenate cannot overlap", exception.getMessage());
    }

    @Test
    void testManageVersionFileWithOverwrite() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now,
            now.plus(2, ChronoUnit.HOURS),
            Duration.ofHours(1));
        RegularTimeSeriesIndex indexBis = RegularTimeSeriesIndex.create(now.plus(3, ChronoUnit.HOURS),
            now.plus(5, ChronoUnit.HOURS),
            Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts1bis = TimeSeries.createDouble("ts1", indexBis, 4d, 5d, 6d);

        // Append the TimeSeries
        tsStore.importTimeSeries(List.of(ts1), 1);
        tsStore.importTimeSeries(List.of(ts1bis), 1, OVERWRITE);

        // Assertions for Double
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        StoredDoubleTimeSeries storedTs1 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts1", 1).get();
        assertArrayEquals(new double[] {4d, 5d, 6d}, storedTs1.toArray());
        assertEquals(1, storedTs1.getChunks().size());
        assertInstanceOf(RegularTimeSeriesIndex.class, storedTs1.getMetadata().getIndex());
        RegularTimeSeriesIndex storedIndex = (RegularTimeSeriesIndex) storedTs1.getMetadata().getIndex();
        assertEquals(978314400000L, storedIndex.getStartTime());
        assertEquals(978321600000L, storedIndex.getEndTime());
        assertEquals(3600000L, storedIndex.getSpacing());
    }

    @Test
    void testAppendDifferentTypes() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now,
            now.plus(2, ChronoUnit.HOURS),
            Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StringTimeSeries ts2 = TimeSeries.createString("ts1", index, "a", "b", "c");

        // Append the TimeSeries
        tsStore.importTimeSeries(List.of(ts1), 1);
        List<TimeSeries> list2 = List.of(ts2);
        PowsyblException exception = assertThrows(PowsyblException.class,
            () -> tsStore.importTimeSeries(list2, 1, APPEND));
        assertEquals("Cannot append to a TimeSeries with different data type", exception.getMessage());
    }

    @Test
    void testAppendTimeSeriesIndex() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now,
            now.plus(2, ChronoUnit.HOURS),
            Duration.ofHours(1));
        RegularTimeSeriesIndex indexBefore = RegularTimeSeriesIndex.create(now.plus(-5, ChronoUnit.HOURS),
            now.plus(-3, ChronoUnit.HOURS),
            Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts1Before = TimeSeries.createDouble("ts1", indexBefore, 4d, 5d, 6d);

        // Append the TimeSeries
        tsStore.importTimeSeries(List.of(ts1), 1);

        // Assertions for index before
        tsStore.importTimeSeries(List.of(ts1Before), 1);
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        StoredDoubleTimeSeries storedTs1 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts1", 1).get();
        assertArrayEquals(new double[] {4d, 5d, 6d, 1d, 2d, 3d}, storedTs1.toArray());
        assertEquals(2, storedTs1.getChunks().size());
        assertInstanceOf(IrregularTimeSeriesIndex.class, storedTs1.getMetadata().getIndex());
        IrregularTimeSeriesIndex storedIndexBefore = (IrregularTimeSeriesIndex) storedTs1.getMetadata().getIndex();
        assertEquals(978285600000L, storedIndexBefore.getTimeAt(0));
        assertEquals(978310800000L, storedIndexBefore.getTimeAt(storedIndexBefore.getPointCount() - 1));
    }

    @Test
    void testAppendTimeSeriesIndexIrregularIndexes() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now,
            now.plus(2, ChronoUnit.HOURS),
            Duration.ofHours(1));
        IrregularTimeSeriesIndex indexIrregular = IrregularTimeSeriesIndex.create(
            now.plus(8, ChronoUnit.HOURS),
            now.plus(10, ChronoUnit.HOURS),
            now.plus(11, ChronoUnit.HOURS));

        // TimeSeries
        StoredDoubleTimeSeries ts1Regular = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts1Irregular = TimeSeries.createDouble("ts1", indexIrregular, 4d, 5d, 6d);
        StoredDoubleTimeSeries ts2Irregular = TimeSeries.createDouble("ts2", indexIrregular, 4d, 5d, 6d);
        StoredDoubleTimeSeries ts2Regular = TimeSeries.createDouble("ts2", index, 1d, 2d, 3d);

        // Append the TimeSeries
        tsStore.importTimeSeries(List.of(ts1Regular, ts2Irregular), 1);
        tsStore.importTimeSeries(List.of(ts1Irregular, ts2Regular), 1);

        // Assertions for index before
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        StoredDoubleTimeSeries storedTs1 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts1", 1).get();
        assertArrayEquals(new double[] {1d, 2d, 3d, 4d, 5d, 6d}, storedTs1.toArray());
        assertEquals(2, storedTs1.getChunks().size());
        assertInstanceOf(IrregularTimeSeriesIndex.class, storedTs1.getMetadata().getIndex());
        IrregularTimeSeriesIndex storedIndex1 = (IrregularTimeSeriesIndex) storedTs1.getMetadata().getIndex();
        assertEquals(978303600000L, storedIndex1.getTimeAt(0));
        assertEquals(978343200000L, storedIndex1.getTimeAt(storedIndex1.getPointCount() - 1));

        assertTrue(tsStore.getDoubleTimeSeries("ts2", 1).isPresent());
        StoredDoubleTimeSeries storedTs2 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts2", 1).get();
        assertArrayEquals(new double[] {1d, 2d, 3d, 4d, 5d, 6d}, storedTs2.toArray());
        assertEquals(2, storedTs2.getChunks().size());
        assertInstanceOf(IrregularTimeSeriesIndex.class, storedTs2.getMetadata().getIndex());
        IrregularTimeSeriesIndex storedIndex2 = (IrregularTimeSeriesIndex) storedTs2.getMetadata().getIndex();
        assertEquals(978303600000L, storedIndex2.getTimeAt(0));
        assertEquals(978343200000L, storedIndex2.getTimeAt(storedIndex2.getPointCount() - 1));
    }

    @Test
    void testAppendTimeSeriesIndexDifferentSpacing() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now,
            now.plus(2, ChronoUnit.HOURS),
            Duration.ofHours(1));
        RegularTimeSeriesIndex indexDifferentSpacing = RegularTimeSeriesIndex.create(now.plus(4, ChronoUnit.HOURS),
            now.plus(8, ChronoUnit.HOURS),
            Duration.ofHours(2));

        // TimeSeries
        StoredDoubleTimeSeries ts1Regular = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts1DifferentSpacing = TimeSeries.createDouble("ts1", indexDifferentSpacing, 4d, 5d, 6d);

        // Append the TimeSeries
        tsStore.importTimeSeries(List.of(ts1Regular), 1);
        tsStore.importTimeSeries(List.of(ts1DifferentSpacing), 1);

        // Assertions for index before
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        StoredDoubleTimeSeries storedTs1 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts1", 1).get();
        assertArrayEquals(new double[] {1d, 2d, 3d, 4d, 5d, 6d}, storedTs1.toArray());
        assertEquals(2, storedTs1.getChunks().size());
        assertInstanceOf(IrregularTimeSeriesIndex.class, storedTs1.getMetadata().getIndex());
        IrregularTimeSeriesIndex storedIndex1 = (IrregularTimeSeriesIndex) storedTs1.getMetadata().getIndex();
        assertEquals(978303600000L, storedIndex1.getTimeAt(0));
        assertEquals(978332400000L, storedIndex1.getTimeAt(storedIndex1.getPointCount() - 1));
    }

    @Test
    @Deprecated(since = "2.3.0")
    void testDeprecatedImportTimeSeries() throws IOException {
        // TimeSeriesStore
        FileSystemTimeSeriesStore tsStore = new FileSystemTimeSeriesStore(resDir);

        // TimeSeries indexes
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now,
            now.plus(2, ChronoUnit.HOURS),
            Duration.ofHours(1));
        RegularTimeSeriesIndex indexBis = RegularTimeSeriesIndex.create(now.plus(3, ChronoUnit.HOURS),
            now.plus(5, ChronoUnit.HOURS),
            Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts1", indexBis, 4d, 5d, 6d);

        // Append the TimeSeries
        tsStore.importTimeSeries(List.of(ts1), 1);
        tsStore.importTimeSeries(List.of(ts2), 1, false, true);

        // Assertions for Double
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        StoredDoubleTimeSeries storedTs1 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts1", 1).get();
        assertArrayEquals(new double[] {1d, 2d, 3d, 4d, 5d, 6d}, storedTs1.toArray());
        assertEquals(2, storedTs1.getChunks().size());
        assertInstanceOf(RegularTimeSeriesIndex.class, storedTs1.getMetadata().getIndex());
        RegularTimeSeriesIndex storedIndex = (RegularTimeSeriesIndex) storedTs1.getMetadata().getIndex();
        assertEquals(978303600000L, storedIndex.getStartTime());
        assertEquals(978321600000L, storedIndex.getEndTime());
        assertEquals(3600000L, storedIndex.getSpacing());

        // Append the TimeSeries
        tsStore.importTimeSeries(List.of(ts2), 1, true, false);

        // Assertions for Double
        assertTrue(tsStore.getDoubleTimeSeries("ts1", 1).isPresent());
        storedTs1 = (StoredDoubleTimeSeries) tsStore.getDoubleTimeSeries("ts1", 1).get();
        assertArrayEquals(new double[] {4d, 5d, 6d}, storedTs1.toArray());
        assertEquals(1, storedTs1.getChunks().size());
        assertInstanceOf(RegularTimeSeriesIndex.class, storedTs1.getMetadata().getIndex());
        storedIndex = (RegularTimeSeriesIndex) storedTs1.getMetadata().getIndex();
        assertEquals(978314400000L, storedIndex.getStartTime());
        assertEquals(978321600000L, storedIndex.getEndTime());
        assertEquals(3600000L, storedIndex.getSpacing());

        // Fails since it already exists
        List<TimeSeries> list = List.of(ts2);
        PowsyblException exception = assertThrows(PowsyblException.class,
            () -> tsStore.importTimeSeries(list, 1, false, false));
        assertEquals("Timeserie ts1 already exist", exception.getMessage());

    }
}