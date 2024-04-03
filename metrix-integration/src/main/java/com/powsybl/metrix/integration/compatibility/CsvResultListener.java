/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.compatibility;

import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.metrix.mapping.timeseries.FileSystemTimeseriesStore;
import com.powsybl.metrix.mapping.timeseries.TimeSeriesStoreUtil;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.tools.ToolRunningContext;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import com.google.common.base.Stopwatch;

public class CsvResultListener implements ResultListener {

    private final Path csvResultFilePath;
    private final FileSystemTimeseriesStore resultStore;
    private final Stopwatch stopwatch;
    private final ToolRunningContext context;

    public CsvResultListener(Path csvResultFilePath, FileSystemTimeseriesStore resultStore, Stopwatch stopwatch, ToolRunningContext context) {
        this.csvResultFilePath = csvResultFilePath;
        this.resultStore = resultStore;
        this.stopwatch = stopwatch;
        this.context = context;
    }

    @Override
    public void onChunkResult(int version, int chunk, List<TimeSeries> timeSeriesList, Network networkPoint) {
        resultStore.importTimeSeries(timeSeriesList, version, false, true);
    }

    @Override
    public void onEnd() {
        // csv export
        if (csvResultFilePath != null) {
            stopwatch.reset();
            stopwatch.start();

            context.getOutputStream().println("Writing results to CSV file...");

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new GZIPOutputStream(Files.newOutputStream(csvResultFilePath)), StandardCharsets.UTF_8))) {
                TimeSeriesStoreUtil.writeCsv(resultStore, writer, ';', ZoneId.systemDefault());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            stopwatch.stop();
            context.getOutputStream().println("Results written to CSV file in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }
}
