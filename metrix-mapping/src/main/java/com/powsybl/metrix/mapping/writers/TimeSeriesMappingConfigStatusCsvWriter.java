/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.writers;

import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesFilter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.metrix.mapping.utils.TimeSeriesConstants.CSV_SEPARATOR;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class TimeSeriesMappingConfigStatusCsvWriter {

    private static final String TIME_SERIES = "timeSeries";
    private static final String MAPPED = "mapped";

    protected final TimeSeriesMappingConfig config;
    protected final ReadOnlyTimeSeriesStore store;

    public TimeSeriesMappingConfigStatusCsvWriter(TimeSeriesMappingConfig config, ReadOnlyTimeSeriesStore store) {
        this.config = Objects.requireNonNull(config);
        this.store = Objects.requireNonNull(store);
    }

    public void writeTimeSeriesMappingStatus(BufferedWriter writer) throws IOException {
        Set<String> mappedTimeSeriesNames = config.getMappedTimeSeriesNames();
        writer.write(TIME_SERIES);
        writer.write(CSV_SEPARATOR);
        writer.write(MAPPED);
        writer.newLine();
        for (String timeSeriesName : store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))) {
            writer.write(timeSeriesName);
            writer.write(CSV_SEPARATOR);
            writer.write(Boolean.toString(mappedTimeSeriesNames.contains(timeSeriesName)));
            writer.newLine();
        }
    }

    public void writeTimeSeriesMappingStatus(Path mappingStatusFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(mappingStatusFile)) {
            writeTimeSeriesMappingStatus(writer);
        }
    }
}
