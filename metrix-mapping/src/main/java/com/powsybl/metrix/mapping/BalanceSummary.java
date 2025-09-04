/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.base.Strings;
import com.google.common.collect.TreeBasedTable;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class BalanceSummary extends DefaultTimeSeriesMapperObserver {

    private static final int N = 1;

    private static final DecimalFormat FORMATTER = new DecimalFormat("0." + Strings.repeat("#", N), new DecimalFormatSymbols(Locale.US));

    private static String formatDouble(Double value) {
        if (Math.abs(value) == Double.MAX_VALUE || Double.isNaN(value)) {
            return "?";
        } else {
            return FORMATTER.format(value);
        }
    }

    public static final char SEPARATOR = ';';

    private static final String TIME_COLUMN = "Time";
    private static final String VERSION_COLUMN = "Version";
    private static final String MEAN_COLUMN = "Mean";
    private static final String SUM_COLUMN = "Sum";
    private static final String MIN_COLUMN = "Min";
    private static final String MAX_COLUMN = "Max";

    private final TreeBasedTable<Instant, Integer, Double> table = TreeBasedTable.create();

    private final List<BalanceContext> statsPerVersion = new ArrayList<>();

    private PrintStream out = null;

    private double balanceValue = 0;
    private double constantBalanceValue = 0;

    Set<String> loadIds = new HashSet<>();

    private record BalanceConfig(char separator, DateTimeFormatter dateTimeFormatter) {
    }

    private BalanceContext context;

    public BalanceSummary(PrintStream out) {
        this.out = Objects.requireNonNull(out);
    }

    public BalanceSummary() {
    }

    public static boolean isInjection(Identifiable<?> identifiable, MappingVariable variable) {
        if (identifiable instanceof Injection) {
            return switch (identifiable) {
                case Generator ignored -> variable == EquipmentVariable.TARGET_P;
                case Load ignored -> variable == EquipmentVariable.P0 || variable == EquipmentVariable.FIXED_ACTIVE_POWER || variable == EquipmentVariable.VARIABLE_ACTIVE_POWER;
                case DanglingLine ignored -> variable == EquipmentVariable.P0;
                default -> false;
            };
        }
        return false;
    }

    public double getInjection(Identifiable<?> identifiable, MappingVariable variable) {
        if (identifiable instanceof Injection<?> injection) {
            return switch (injection) {
                case Generator generator -> generator.getTargetP();
                case Load load -> getLoad(variable, load);
                case DanglingLine danglingLine -> -danglingLine.getP0();
                default -> 0;
            };
        }
        return 0;
    }

    private double getLoad(MappingVariable variable, Load load) {
        // in case of scaling down error on fixedActivePower + variableActivePower, don't count p0 twice
        if (variable == EquipmentVariable.P0) {
            return -load.getP0();
        } else if (variable == EquipmentVariable.FIXED_ACTIVE_POWER) {
            LoadDetail loadDetail = load.getExtension(LoadDetail.class);
            if (loadDetail != null) {
                return -loadDetail.getFixedActivePower();
            } else if (!loadIds.contains(load.getId())) {
                loadIds.add(load.getId());
                return -load.getP0();
            }
        } else if (variable == EquipmentVariable.VARIABLE_ACTIVE_POWER) {
            LoadDetail loadDetail = load.getExtension(LoadDetail.class);
            if (loadDetail != null) {
                return -loadDetail.getVariableActivePower();
            } else if (!loadIds.contains(load.getId())) {
                loadIds.add(load.getId());
                return -load.getP0();
            }
        }
        return 0;
    }

    public static boolean isPositiveInjection(Identifiable<?> identifiable) {
        return identifiable instanceof Generator;
    }

    @Override
    public void versionStart(int version) {
        if (out != null) {
            out.println("Version " + version);
        }
        this.context = new BalanceContext(version);
    }

    @Override
    public void versionEnd(int version) {
        statsPerVersion.add(context);
        if (out != null) {
            double average = context.getAverage();
            out.println("Balance summary: min=" + formatDouble(context.getBalanceMin()) + " MWh, max=" + formatDouble(context.getBalanceMax())
                    + " MWh, average=" + formatDouble(average) + " MWh");
        }
        this.context = null;
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        balanceValue = 0;
        loadIds.clear();
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        if (isInjection(identifiable, variable)) {
            if (!Double.isNaN(equipmentValue)) {
                balanceValue += (isPositiveInjection(identifiable) ? 1 : -1) * equipmentValue;
            } else {
                // scaling down is not ok, keep base case values
                balanceValue += getInjection(identifiable, variable);
            }
        }
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        balanceValue += balance; // add base case values for unmapped equipments

        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            constantBalanceValue = balanceValue;
        } else {
            balanceValue += constantBalanceValue;
            context.updateValue(balanceValue);
            table.put(index.getInstantAt(point), context.getVersion(), balanceValue);
            if (out != null) {
                out.println("Balance at " + index.getInstantAt(point) + ": " + String.format(Locale.US, "%.1f", balanceValue));
            }
        }
    }

    public void writeCsv(Path mappingSynthesisDir, char separator) throws IOException {
        writeCsv(mappingSynthesisDir, separator, ZoneId.systemDefault());
    }

    public void writeCsv(Path mappingSynthesisDir, char separator, ZoneId zoneId) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(mappingSynthesisDir.resolve("balanceSummary.csv"))) {
            writeCsv(writer, separator, zoneId);
        }
    }

    public void writeCsv(BufferedWriter writer) throws IOException {
        writeCsvStats(writer, SEPARATOR);
    }

    public void writeCsv(BufferedWriter writer, char separator, ZoneId zoneId) throws IOException {
        BalanceConfig config = new BalanceConfig(separator, DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(zoneId));
        writer.write(TIME_COLUMN);
        for (int version : table.columnKeySet()) {
            writer.write(config.separator);
            writer.write(VERSION_COLUMN + " " + version);
        }
        writer.newLine();
        for (Instant instant : table.rowKeySet()) {
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, zoneId);
            writer.write(dateTime.format(config.dateTimeFormatter));
            for (int version : table.columnKeySet()) {
                writer.write(config.separator);
                writer.write(formatDouble(table.get(instant, version)));
            }
            writer.newLine();
        }
    }

    public void writeCsvStats(BufferedWriter writer, char separator) throws IOException {
        writeCsvStats(writer, separator, statsPerVersion);
    }

    public static void writeCsvStats(BufferedWriter writer, char separator, List<BalanceContext> balanceContexts) throws IOException {
        writer.write(VERSION_COLUMN);
        writer.write(separator);
        writer.write(MIN_COLUMN);
        writer.write(separator);
        writer.write(MAX_COLUMN);
        writer.write(separator);
        writer.write(SUM_COLUMN);
        writer.write(separator);
        writer.write(MEAN_COLUMN);
        writer.newLine();
        for (BalanceContext context : balanceContexts) {
            writer.write(String.valueOf(context.getVersion()));
            writer.write(separator);
            writer.write(formatDouble(context.getBalanceMin()));
            writer.write(separator);
            writer.write(formatDouble(context.getBalanceMax()));
            writer.write(separator);
            writer.write(formatDouble(context.getBalanceSum()));
            writer.write(separator);
            writer.write(formatDouble(context.getAverage()));
            writer.newLine();
        }
    }

    public double[] getValuesSortedByInstantByVersion(int version) {
        return table.column(version).entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .mapToDouble(Double::doubleValue)
                .toArray();
    }
}
