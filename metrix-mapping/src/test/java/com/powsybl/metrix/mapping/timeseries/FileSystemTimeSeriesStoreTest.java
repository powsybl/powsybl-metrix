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
    void testTsStore() throws IOException {
        Path resDir = Files.createDirectory(fileSystem.getPath("/tmp"));
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
        tsStore.importTimeSeries(List.of(ts1), 1, false, true);
        tsStore.importTimeSeries(List.of(ts1), 2, false, true);

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
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3), 1, false, true);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3), 2, false, true);

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
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3), 1, false, true);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3), 2, false, true);

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
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3, ts4), 1, false, true);
        tsStore.importTimeSeries(List.of(ts1, ts2, ts3, ts4), 2, false, true);

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
}
