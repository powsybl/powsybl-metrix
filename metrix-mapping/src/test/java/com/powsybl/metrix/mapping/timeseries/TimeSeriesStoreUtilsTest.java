/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.google.common.collect.ImmutableSortedSet;
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.timeseries.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class TimeSeriesStoreUtilsTest extends AbstractConverterTest {

    @Test
    public void export() throws IOException {
        Path output = tmpDir.resolve("output.csv");

        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts2", index, 1d, 3d, 5d);
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache(ts1, ts2);

        try (Writer writer = Files.newBufferedWriter(output)) {
            TimeSeriesStoreUtil.writeCsv(store, writer, ';', ZoneId.of(ZoneOffset.UTC.getId()), ImmutableSortedSet.of(1), ImmutableSortedSet.of("ts1", "ts2"));
        } catch (IOException e) {
            Assert.fail();
        }

        try (InputStream expected = getClass().getResourceAsStream("/expected/simpleExport.csv")) {
            try (InputStream actual = Files.newInputStream(output)) {
                compareTxt(expected, actual);
            }
        }
    }

}
