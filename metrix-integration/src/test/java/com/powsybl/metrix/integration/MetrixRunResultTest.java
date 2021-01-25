/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.timeseries.*;
import org.junit.Test;
import org.threeten.extra.Interval;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicolhui on 07/09/17.
 */
public class MetrixRunResultTest extends AbstractConverterTest {

    @Test
    public void metrixResultTest() throws IOException, URISyntaxException {

        MetrixOutputData results = new MetrixOutputData(10, 5);
        Path workingDir = Paths.get(getClass().getResource("/").toURI());
        results.readFile(workingDir, 10); // 0 n-k result
        results.readFile(workingDir, 11); // 1 n-k result, no remedial action, with marginal costs
        results.readFile(workingDir, 12); // 5 n-k results
        results.readFile(workingDir, 13); // missing
        results.readFile(workingDir, 14); // 5 n-k results with remedial actions

        // create time series space mock
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T00:00:00Z/2015-01-17T00:00:00Z"), Duration.ofDays(1));

        // Results file writing
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        results.createTimeSeries(index, timeSeriesList);

        TimeSeriesTable table = new TimeSeriesTable(1, 1, index);
        table.load(1, timeSeriesList);

        StringWriter writer = new StringWriter();
        try (BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            table.writeCsv(writer, new TimeSeriesCsvConfig(ZoneId.of("UTC")));
            bufferedWriter.flush();

            String actual = writer.toString();
            compareTxt(getClass().getResourceAsStream("/metrixResults.csv"), new ByteArrayInputStream(actual.getBytes(StandardCharsets.UTF_8)));
        }
    }
}
