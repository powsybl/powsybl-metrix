/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.commons.AbstractConverterTest;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class FileSystemTimeseriesStoreTest extends AbstractConverterTest {

    @Test
    public void testTsStore() throws IOException {
        Path resDir = Files.createDirectory(fileSystem.getPath("tmp/res"));
        FileSystemTimeseriesStore tsStore = new FileSystemTimeseriesStore(resDir);
        Set<String> emptyTimeSeriesNames = tsStore.getTimeSeriesNames(null);
        assertThat(emptyTimeSeriesNames).isEmpty();

        try (InputStream resourceAsStream = FileSystemTimeseriesStoreTest.class.getResourceAsStream("/testStore.csv");
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceAsStream))
        ) {
            tsStore.importTimeSeries(bufferedReader, true, false);
        }

        assertThat(tsStore.getTimeSeriesNames(null)).isNotEmpty();
        assertThat(tsStore.getTimeSeriesNames(null)).containsExactlyInAnyOrder("BALANCE", "tsX");

        assertTrue(tsStore.timeSeriesExists("BALANCE"));
        assertFalse(tsStore.timeSeriesExists("tsY"));
    }
}
