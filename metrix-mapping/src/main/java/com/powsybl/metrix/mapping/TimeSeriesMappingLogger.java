/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.metrix.mapping.log.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.powsybl.metrix.mapping.TimeSeriesConstants.CSV_SEPARATOR;

public class TimeSeriesMappingLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMappingLogger.class);

    private final List<Log> logs = new ArrayList<>();

    public void addLog(Log log) {
        logs.add(log);
    }

    public void printLogSynthesis() {
        Map<String, AtomicInteger> labelCount = new HashMap<>();
        for (Log log : logs) {
            AtomicInteger count = labelCount.computeIfAbsent(log.getLabel(), k -> new AtomicInteger(0));
            count.incrementAndGet();
        }
        labelCount.forEach((label, count) -> LOGGER.error("{} {}", count, label));
    }

    public void writeJson(Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeJson(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeJson(Writer writer) {
        ObjectMapper mapper = JsonUtil.createObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, logs);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeCsv(Path file) {
        writeCsv(file, CSV_SEPARATOR);
    }

    private void writeCsv(Path file, char separator) {
        writeCsv(file, separator, ZoneId.systemDefault());
    }

    public void writeCsv(Path file, ZoneId zoneId) {
        writeCsv(file, CSV_SEPARATOR, zoneId);
    }

    private void writeCsv(Path file, char separator, ZoneId zoneId) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeCsv(writer, separator, zoneId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeCsv(BufferedWriter writer) {
        writeCsv(writer, CSV_SEPARATOR);
    }

    private void writeCsv(BufferedWriter writer, char separator) {
        writeCsv(writer, separator, ZoneId.systemDefault());
    }

    public void writeCsv(BufferedWriter writer, ZoneId zoneId) {
        writeCsv(writer, CSV_SEPARATOR, zoneId);
    }

    private void writeCsv(BufferedWriter writer, char separator, ZoneId zoneId) {
        try {
            TimeSeriesLoggerConfig config = new TimeSeriesLoggerConfig(separator, DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId));
            writer.write("Type");
            writer.write(config.separator);
            writer.write("Label");
            writer.write(config.separator);
            writer.write("Time");
            writer.write(config.separator);
            writer.write("Variant");
            writer.write(config.separator);
            writer.write("Version");
            writer.write(config.separator);
            writer.write("Message");
            writer.newLine();
            for (Log log : logs) {
                int point = log.getPoint();
                String pointLabel = "";
                String dateLabel = "";
                if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
                    pointLabel = "all";
                } else if (point != Integer.MAX_VALUE) {
                    pointLabel = Integer.toString(point + 1);
                    ZonedDateTime dateTime = ZonedDateTime.ofInstant(log.getIndex().getInstantAt(point), zoneId);
                    dateLabel = dateTime.format(config.dateTimeFormatter);
                }
                writer.write(log.getLevel().name());
                writer.write(config.separator);
                writer.write(log.getLabel());
                writer.write(config.separator);
                writer.write(dateLabel);
                writer.write(config.separator);
                writer.write(pointLabel);
                writer.write(config.separator);
                writer.write(Integer.toString(log.getVersion()));
                writer.write(config.separator);
                writer.write(log.getMessage());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
