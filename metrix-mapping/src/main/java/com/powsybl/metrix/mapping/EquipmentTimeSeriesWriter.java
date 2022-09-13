/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EquipmentTimeSeriesWriter extends DefaultTimeSeriesMapperObserver {

    private static final char SEPARATOR = ';';

    private final Path dir;

    private BufferedWriter writer;

    private final Map<String, Double> values = new HashMap<>();
    private final Map<String, Double> constantValues = new HashMap<>();

    private List<String> columnNames;

    private boolean header;

    public EquipmentTimeSeriesWriter(Path dir) {
        this.dir = Objects.requireNonNull(dir);
    }

    public EquipmentTimeSeriesWriter(BufferedWriter writer) {
        this.dir = null;
        this.writer = Objects.requireNonNull(writer);
    }

    @Override
    public void versionStart(int version) {
        try {
            if (dir != null) {
                writer = Files.newBufferedWriter(dir.resolve("version_" + version + ".csv"), StandardCharsets.UTF_8);
            }
            header = true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        values.clear();
        if (point != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            values.putAll(constantValues);
        }
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        if (!Double.isNaN(equipmentValue) && !timeSeriesName.isEmpty()) {
            if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
                constantValues.put(identifiable.getId() + "_" + variable.getVariableName(), equipmentValue);
            } else {
                values.put(identifiable.getId() + "_" + variable.getVariableName(), equipmentValue);
            }
        }
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        try {
            if (point != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
                if (header) {
                    writer.write("time");
                    columnNames = values.keySet().stream().sorted().collect(Collectors.toList());
                    for (String columnName : columnNames) {
                        writer.write(SEPARATOR);
                        writer.write(columnName);
                    }
                    header = false;
                }

                writer.newLine();

                writer.write(index.getInstantAt(point).toString());

                for (String columnName : columnNames) {
                    double value = values.get(columnName);
                    writer.write(SEPARATOR);
                    writer.write(Double.isNaN(value) ? "" : Double.toString(value));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void versionEnd(int version) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        constantValues.clear();
    }
}
