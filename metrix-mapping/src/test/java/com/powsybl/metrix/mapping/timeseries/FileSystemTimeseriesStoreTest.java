/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
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
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FileSystemTimeseriesStoreTest {
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
    void testTsStore() throws IOException {
        Path resDir = Files.createDirectory(fileSystem.getPath("/tmp"));
        FileSystemTimeseriesStore tsStore = new FileSystemTimeseriesStore(resDir);
        Set<String> emptyTimeSeriesNames = tsStore.getTimeSeriesNames(null);
        assertThat(emptyTimeSeriesNames).isEmpty();

        try (InputStream resourceAsStream = Objects.requireNonNull(FileSystemTimeseriesStoreTest.class.getResourceAsStream("/testStore.csv"));
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
}
