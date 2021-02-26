/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.powsybl.commons.io.table.AsciiTableFormatter;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.iidm.network.extensions.LoadDetailAdder;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.metrix.mapping.TimeSeriesMappingConfig.*;

/**
 * /**
 *
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class TimeSeriesMappingConfigCsvWriter implements TimeSeriesConstants {

    private static final boolean IS_WITH_BOUNDARY_LINES = false;

    private static final int N = 1;

    private final DecimalFormat formatter = new DecimalFormat("0." + Strings.repeat("#", N), new DecimalFormatSymbols(Locale.US));

    private static final String MAPPING_SYNTHESIS = "Mapping synthesis";
    private static final String VARIABLE_SYNTHESIS = "Variable synthesis";

    private static final String SYNTHESIS = "Synthesis";
    private static final String GENERATOR = "Generator";
    private static final String LOAD = "Load";
    private static final String BOUNDARY_LINE = "BoundaryLine";
    private static final String HVDC_LINE = "HvdcLine";
    private static final String PST = "Pst";
    private static final String BREAKER = "Breaker";
    private static final String TIME_SERIES = "TimeSeries";
    private static final String SRC_TS = "SourceTimeSeries";
    private static final String MAPPED_TS = "MappedTimeSeries";
    private static final String APPLIED_TIME_SERIES = "AppliedTimeSeries";
    private static final String UNUSED_TIME_SERIES = "UnusedTimeSeries";
    private static final String VARIABLE = "Variable";
    private static final String STATUS = "Status";
    private static final String PAYS_CVG = "paysCvg";
    private static final String PAYS_CVG1 = "paysCvg1";
    private static final String PAYS_CVG2 = "paysCvg2";
    private static final String VOLTAGE_LEVEL = "voltageLevel";
    private static final String VOLTAGE_LEVEL1 = "voltageLevel1";
    private static final String VOLTAGE_LEVEL2 = "voltageLevel2";
    private static final String REGION_DI = "regionDI";
    private static final String GENRE_CVG = "genreCvg";
    private static final String ENTSOE_CATEGORY = "entsoeCategory";
    private static final String TARGET_P = "targetP";
    private static final String MIN_P = "minP";
    private static final String MAX_P = "maxP";
    private static final String CONVERTERS_MODE = "convertersMode";
    private static final String ACTIVE_POWER_SETPOINT = "activePowerSetpoint";
    private static final String P0 = "p0";
    private static final String FIXED_ACTIVE_POWER = "fixedActivePower";
    private static final String VARIABLE_ACTIVE_POWER = "variableActivePower";
    private static final String CURRENT_TAP = "currentTap";
    private static final String SUBSTATION = "substation";
    private static final String NETWORK_POWER = "NetworkPower";
    private static final String TYPE = "Type";
    private static final String GENERATOR_TYPE = "generator";
    private static final String LOAD_TYPE = "load";
    private static final String BOUNDARY_LINE_TYPE = "boundary";
    private static final String HVDC_LINE_TYPE = "hvdc";
    private static final String PST_TYPE = "pst";
    private static final String BREAKER_TYPE = "breaker";
    private static final String EMPTY_TYPE = "-";
    private static final String MIN_POWER = "MinPower";
    private static final String MAX_POWER = "MaxPower";
    private static final String AVERAGE_POWER = "AveragePower";

    private static final List<String> GENERATOR_HEADER = Collections.unmodifiableList(Lists.newArrayList(
            SUBSTATION,
            VOLTAGE_LEVEL,
            TARGET_P,
            MIN_P,
            MAX_P));

    private static final List<String> LOAD_HEADER = Collections.unmodifiableList(Lists.newArrayList(
            SUBSTATION,
            VOLTAGE_LEVEL,
            P0,
            FIXED_ACTIVE_POWER,
            VARIABLE_ACTIVE_POWER));

    private static final List<String> HVDC_LINE_HEADER = Collections.unmodifiableList(Lists.newArrayList(
            VOLTAGE_LEVEL1,
            VOLTAGE_LEVEL2,
            CONVERTERS_MODE,
            ACTIVE_POWER_SETPOINT,
            MIN_P,
            MAX_P));

    private static final List<String> PST_HEADER = Collections.unmodifiableList(Lists.newArrayList(
            CURRENT_TAP));

    private static final List<String> BOUNDARY_LINE_HEADER = Collections.unmodifiableList(Lists.newArrayList(
    ));

    private static final List<String> BREAKER_HEADER = Collections.unmodifiableList(Lists.newArrayList(
            VOLTAGE_LEVEL));

    protected final TimeSeriesMappingConfig config;

    protected final Network network;

    public TimeSeriesMappingConfigCsvWriter(TimeSeriesMappingConfig config, Network network) {
        this.config = Objects.requireNonNull(config);
        this.network = Objects.requireNonNull(network);
    }

    private int writeEquipmentHeader(BufferedWriter writer, String equipmentType) {
        try {
            List<String> header;
            switch (equipmentType) {
                case GENERATOR:
                    header = getGeneratorHeader();
                    break;

                case LOAD:
                    header = getLoadHeader();
                    break;

                case HVDC_LINE:
                    header = getHvdcLineHeader();
                    break;

                case PST:
                    header = getPstHeader();
                    break;

                case BOUNDARY_LINE:
                    header = BOUNDARY_LINE_HEADER;
                    break;

                case BREAKER:
                    header = getBreakerHeader();
                    break;

                default:
                    throw new AssertionError("Unsupported equipment type " + equipmentType);
            }

            for (String col : header) {
                writer.write(col);
                writer.write(CSV_SEPARATOR);
            }
            return header.size();

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected List<String> getGeneratorHeader() {
        return GENERATOR_HEADER;
    }

    protected void writeGenerator(BufferedWriter writer, String id) throws IOException {
        Generator generator = network.getGenerator(id);
        Substation substation = generator.getTerminal().getVoltageLevel().getSubstation();

        writer.write(substation.getId());
        writer.write(CSV_SEPARATOR);
        writer.write(generator.getTerminal().getVoltageLevel().getId());
        writer.write(CSV_SEPARATOR);
        writer.write(formatDouble(generator.getTargetP()));
        writer.write(CSV_SEPARATOR);
        writer.write(formatDouble(generator.getMinP()));
        writer.write(CSV_SEPARATOR);
        writer.write(formatDouble(generator.getMaxP()));
        writer.write(CSV_SEPARATOR);
    }

    protected List<String> getHvdcLineHeader() {
        return HVDC_LINE_HEADER;
    }

    protected void writeHvdcLine(BufferedWriter writer, String id) throws IOException {
        HvdcLine hvdcLine = network.getHvdcLine(id);
        Substation substation1 = hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation();
        Substation substation2 = hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation();

        float min = (float) -hvdcLine.getMaxP();
        float max = (float) hvdcLine.getMaxP();
        HvdcOperatorActivePowerRange activePowerRange = hvdcLine.getExtension(HvdcOperatorActivePowerRange.class);
        if (activePowerRange != null) {
            min = -activePowerRange.getOprFromCS2toCS1();
            max = activePowerRange.getOprFromCS1toCS2();
        }

        writer.write(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getId());
        writer.write(CSV_SEPARATOR);
        writer.write(hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getId());
        writer.write(CSV_SEPARATOR);
        writer.write(hvdcLine.getConvertersMode().name());
        writer.write(CSV_SEPARATOR);
        writer.write(formatDouble(hvdcLine.getActivePowerSetpoint()));
        writer.write(CSV_SEPARATOR);
        writer.write(formatDouble(min));
        writer.write(CSV_SEPARATOR);
        writer.write(formatDouble(max));
        writer.write(CSV_SEPARATOR);
    }

    protected List<String> getPstHeader() {
        return PST_HEADER;
    }

    protected void writePst(BufferedWriter writer, String id) throws IOException {
        TwoWindingsTransformer pst = network.getTwoWindingsTransformer(id);
        Substation substation = pst.getSubstation();

        int currentTap = pst.getPhaseTapChanger().getTapPosition();

        writer.write(formatDouble(currentTap));
        writer.write(CSV_SEPARATOR);
    }

    protected List<String> getLoadHeader() {
        return LOAD_HEADER;
    }

    protected void writeLoad(BufferedWriter writer, String id) throws IOException {
        Load load = network.getLoad(id);
        Substation substation = load.getTerminal().getVoltageLevel().getSubstation();

        LoadDetail loadDetail = load.getExtension(LoadDetail.class);
        String fixedActivePower = "";
        String variableActivePower = "";
        if (loadDetail != null) {
            fixedActivePower = formatDouble(loadDetail.getFixedActivePower());
            variableActivePower = formatDouble(loadDetail.getVariableActivePower());
        }

        writer.write(substation.getId());
        writer.write(CSV_SEPARATOR);
        writer.write(load.getTerminal().getVoltageLevel().getId());
        writer.write(CSV_SEPARATOR);
        writer.write(formatDouble(load.getP0()));
        writer.write(CSV_SEPARATOR);
        writer.write(fixedActivePower);
        writer.write(CSV_SEPARATOR);
        writer.write(variableActivePower);
        writer.write(CSV_SEPARATOR);
    }

    protected List<String> getBreakerHeader() {
        return BREAKER_HEADER;
    }

    protected void writeBreaker(BufferedWriter writer, String id) throws IOException {
        Switch sw = network.getSwitch(id);
        Substation substation = sw.getVoltageLevel().getSubstation();

        String paysCvg = substation.getProperty("paysCvg");
        if (paysCvg == null) {
            paysCvg = "";
        }

        writer.write(paysCvg);
        writer.write(CSV_SEPARATOR);
        writer.write(sw.getVoltageLevel().getId());
        writer.write(CSV_SEPARATOR);
    }

    private void writeEquipment(BufferedWriter writer, String equipmentType, String id) {
        try {
            switch (equipmentType) {
                case GENERATOR:
                    writeGenerator(writer, id);
                    break;

                case HVDC_LINE:
                    writeHvdcLine(writer, id);
                    break;

                case PST:
                    writePst(writer, id);
                    break;

                case LOAD:
                    writeLoad(writer, id);
                    break;

                case BREAKER:
                    writeBreaker(writer, id);
                    break;

                case GENERATOR_TYPE:
                case LOAD_TYPE:
                case BOUNDARY_LINE_TYPE:
                case HVDC_LINE_TYPE:
                case PST_TYPE:
                case BREAKER_TYPE:
                case EMPTY_TYPE:
                case BOUNDARY_LINE:
                    writer.write(equipmentType);
                    writer.write(CSV_SEPARATOR);
                    break;

                case GENERATORS:
                case LOADS:
                case HVDC_LINES:
                case PSTS:
                case BREAKERS:
                    break;

                default:
                    throw new AssertionError("Unsupported equipment type " + equipmentType);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeStatus(BufferedWriter writer, MappingVariable variable, Collection<String> values) {
        try {
            if (values.size() > 1) {
                writer.write(MULTI_MAPPED);
            } else if (variable == EquipmentVariable.targetP ||
                    variable == EquipmentVariable.p0 || variable == EquipmentVariable.fixedActivePower || variable == EquipmentVariable.variableActivePower ||
                    variable == EquipmentVariable.activePowerSetpoint ||
                    variable == EquipmentVariable.currentTap ||
                    variable == EquipmentVariable.open) {
                writer.write(MAPPED);
            } else {
                writer.write("");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static LoadDetail getLoadDetail(Load load) {
        LoadDetail ld = load.getExtension(LoadDetail.class);
        if (ld != null) {
            return getLoadDetail(load, ld.getFixedActivePower(), ld.getVariableActivePower());
        } else {
            return getLoadDetail(load, 0, 0);
        }
    }

    private static LoadDetail getLoadDetail(Load load, float fixedActivePower, float variableActivePower) {
        load.newExtension(LoadDetailAdder.class)
                .withFixedActivePower(fixedActivePower)
                .withFixedReactivePower(0f)
                .withVariableActivePower(variableActivePower)
                .withVariableReactivePower(0f)
                .add();
        return load.getExtension(LoadDetail.class);
    }

    private void computeNetworkPower(String timeSeriesName, MappingVariable variable, Collection<String> ids, Map<MappingKey, Double> timeSeriesEquipmentsSum) {
        double sum = Double.NaN;
        Function<String, Double> getPower = null;
        if (variable == EquipmentVariable.targetP) {
            getPower = id -> network.getGenerator(id).getTargetP();
        } else if (variable == EquipmentVariable.targetQ) {
            getPower = id -> network.getGenerator(id).getTargetQ();
        } else if (variable == EquipmentVariable.minP) {
            getPower = id -> (double) TimeSeriesMapper.getMin(network.getIdentifiable(id));
        } else if (variable == EquipmentVariable.maxP) {
            getPower = id -> (double) TimeSeriesMapper.getMax(network.getIdentifiable(id));
        } else if (variable == EquipmentVariable.p0) {
            getPower = id -> network.getLoad(id).getP0();
        } else if (variable == EquipmentVariable.q0) {
            getPower = id -> network.getLoad(id).getQ0();
        } else if (variable == EquipmentVariable.fixedActivePower) {
            getPower = id -> (double) getLoadDetail(network.getLoad(id)).getFixedActivePower();
        } else if (variable == EquipmentVariable.variableActivePower) {
            getPower = id -> (double) getLoadDetail(network.getLoad(id)).getVariableActivePower();
        } else if (variable == EquipmentVariable.fixedReactivePower) {
            getPower = id -> (double) getLoadDetail(network.getLoad(id)).getFixedReactivePower();
        } else if (variable == EquipmentVariable.variableReactivePower) {
            getPower = id -> (double) getLoadDetail(network.getLoad(id)).getVariableReactivePower();
        } else if (variable == EquipmentVariable.activePowerSetpoint) {
            getPower = id -> (double) network.getHvdcLine(id).getActivePowerSetpoint();
        }
        if (getPower != null) {
            sum = ids.stream().mapToDouble(getPower::apply).sum();
        }
        timeSeriesEquipmentsSum.put(new MappingKey(variable, timeSeriesName), sum);
    }

    private void writeValue(BufferedWriter writer, double value) throws IOException {
        if (Double.isNaN(value)) {
            writer.write("-");
        } else {
            writer.write(formatDouble(value));
        }
    }

    private void writeMultimap(BufferedWriter writer, String equipmentsLabel, MappingVariable variable, String key, Collection<String> values, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats, Map<MappingKey, Double> networkPowerMap, int nbEquipmentValues, boolean isEqToTS) {
        try {
            double power;

            int nbCol = nbEquipmentValues + 2; // +2 for name (equipment or time series), variable
            if (isEqToTS) {
                nbCol++; // +1 for status
                nbCol++; // +1 for 2 separated applied/non applied columns
            }
            if (networkPowerMap != null) {
                nbCol++; // +1 for networkPower
            }
            if (withTimeSeriesStats && !isEqToTS && store != null && computationRange != null) {
                nbCol++; // +1 for min
                nbCol++; // +1 for max
                nbCol++; // +1 for average
            }

            writer.write(key);
            writer.write(CSV_SEPARATOR);
            writeEquipment(writer, equipmentsLabel, key);
            if (isEqToTS) {
                writeStatus(writer, variable, values);
                writer.write(CSV_SEPARATOR);
            }
            if (withTimeSeriesStats && !isEqToTS && store != null && computationRange != null) {
                writeValue(writer, getTimeSeriesMin(key, store, computationRange));
                writer.write(CSV_SEPARATOR);
                writeValue(writer, getTimeSeriesMax(key, store, computationRange));
                writer.write(CSV_SEPARATOR);
                writeValue(writer, getTimeSeriesAvg(key, store, computationRange));
                writer.write(CSV_SEPARATOR);
            }
            if (networkPowerMap != null) {
                if (isEqToTS) {
                    power = networkPowerMap.get(new MappingKey(variable, values.iterator().next()));
                } else {
                    power = networkPowerMap.get(new MappingKey(variable, key));
                }
                writeValue(writer, power);
                writer.write(CSV_SEPARATOR);
            }
            writer.write(variable.getVariableName());
            writer.write(CSV_SEPARATOR);
            Iterator<String> it = values.iterator();
            if (isEqToTS) {
                String value = it.next();
                writer.write(value);
                writer.write(CSV_SEPARATOR);
            }
            if (it.hasNext()) {
                while (it.hasNext()) {
                    String value = it.next();
                    writer.write(value);
                    writer.newLine();
                    if (it.hasNext()) {
                        for (int i = 0; i < nbCol; i++) {
                            writer.write("");
                            writer.write(CSV_SEPARATOR);
                        }
                    }
                }
            } else {
                writer.write("-");
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeTimeSerieToEquipmentsMapping(BufferedWriter writer, String equipmentsLabel, Map<MappingKey, List<String>> timeSerieToEquipmentsMapping, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) throws IOException {
        writer.write(TIME_SERIES);
        writer.write(CSV_SEPARATOR);
        if (withTimeSeriesStats) {
            writer.write(MIN_POWER);
            writer.write(CSV_SEPARATOR);
            writer.write(MAX_POWER);
            writer.write(CSV_SEPARATOR);
            writer.write(AVERAGE_POWER);
            writer.write(CSV_SEPARATOR);
        }
        writer.write(NETWORK_POWER);
        writer.write(CSV_SEPARATOR);
        writer.write(VARIABLE);
        writer.write(CSV_SEPARATOR);
        writer.write(equipmentsLabel);
        writer.newLine();
        Map<MappingKey, Double> networkPowerMap = new LinkedHashMap<>();
        timeSerieToEquipmentsMapping.forEach((timeSerie, ids) -> computeNetworkPower(timeSerie.getId(), timeSerie.getMappingVariable(), ids, networkPowerMap));
        timeSerieToEquipmentsMapping.forEach((timeSerie, ids) -> writeMultimap(writer, equipmentsLabel, timeSerie.getMappingVariable(), timeSerie.getId(), ids, store, computationRange, withTimeSeriesStats, networkPowerMap, 0, false));
    }

    private void writeTimeSerieToEquipmentsMapping(Path dir, String fileName, String equipmentsLabel, Map<MappingKey, List<String>> timeSerieToEquipmentsMapping, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(fileName))) {
            writeTimeSerieToEquipmentsMapping(writer, equipmentsLabel, timeSerieToEquipmentsMapping, store, computationRange, withTimeSeriesStats);
        }
    }

    private void writeEquipmentToTimeSeriesMapping(BufferedWriter writer, String equipmentLabel, Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, Map<MappingKey, List<String>> timeSeriesToEquipmentsMapping) throws IOException {
        writer.write(equipmentLabel);
        writer.write(CSV_SEPARATOR);
        int nbEquipmentValues = writeEquipmentHeader(writer, equipmentLabel);
        writer.write(STATUS);
        writer.write(CSV_SEPARATOR);
        writer.write(NETWORK_POWER);
        writer.write(CSV_SEPARATOR);
        writer.write(VARIABLE);
        writer.write(CSV_SEPARATOR);
        writer.write(APPLIED_TIME_SERIES);
        writer.write(CSV_SEPARATOR);
        writer.write(UNUSED_TIME_SERIES);
        writer.newLine();
        Map<MappingKey, Double> networkPowerMap = new LinkedHashMap<>();
        timeSeriesToEquipmentsMapping.forEach((timeSerie, ids) -> computeNetworkPower(timeSerie.getId(), timeSerie.getMappingVariable(), ids, networkPowerMap));
        equipmentToTimeSeriesMapping.forEach((equipmentId, timeSeries) -> writeMultimap(writer, equipmentLabel, equipmentId.getMappingVariable(), equipmentId.getId(), timeSeries, null, null, false, networkPowerMap, nbEquipmentValues, true));
    }

    private void writeEquipmentToTimeSeriesMapping(Path dir, String fileName, String equipmentLabel, Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, Map<MappingKey, List<String>> timeSeriesToEquipementsMapping) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(fileName))) {
            writeEquipmentToTimeSeriesMapping(writer, equipmentLabel, equipmentToTimeSeriesMapping, timeSeriesToEquipementsMapping);
        }
    }

    private void writeEquipmentSet(BufferedWriter writer, String equipmentLabel, String status, Set<String> equipmentSet, Set<String> equipmentSetToIgnore) throws IOException {
        writer.write(equipmentLabel);
        writer.write(CSV_SEPARATOR);
        writeEquipmentHeader(writer, equipmentLabel);
        writer.write(STATUS);
        writer.write(CSV_SEPARATOR);
        writer.write(NETWORK_POWER);
        writer.write(CSV_SEPARATOR);
        writer.write(VARIABLE);
        writer.write(CSV_SEPARATOR);
        writer.write(APPLIED_TIME_SERIES);
        writer.write(CSV_SEPARATOR);
        writer.write(UNUSED_TIME_SERIES);
        writer.newLine();
        for (String equipment : equipmentSet) {
            if (!equipmentSetToIgnore.contains(equipment)) {
                writer.write(equipment);
                writer.write(CSV_SEPARATOR);
                writeEquipment(writer, equipmentLabel, equipment);
                writer.write(status);
                writer.write(CSV_SEPARATOR);
                writer.write("-"); // Sum
                writer.write(CSV_SEPARATOR);
                writer.write("-"); // Variable
                writer.write(CSV_SEPARATOR);
                writer.write("-"); // Applied time series
                writer.write(CSV_SEPARATOR);
                writer.write("-"); // Non applied time series
                writer.newLine();
            }
        }
    }

    private void writeEquipmentSet(Path dir, String fileName, String equipmentLabel, String status, Set<String> equipmentSet, Set<String> equipmentSetToIgnore) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(fileName))) {
            writeEquipmentSet(writer, equipmentLabel, status, equipmentSet, equipmentSetToIgnore);
        }
    }

    public void writeTimeSeriesToGeneratorsMapping(BufferedWriter writer, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, GENERATORS, config.getTimeSeriesToGeneratorsMapping(), store, computationRange, withTimeSeriesStats);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToLoadsMapping(BufferedWriter writer, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, LOADS, config.getTimeSeriesToLoadsMapping(), store, computationRange, withTimeSeriesStats);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToBoundaryLinesMapping(BufferedWriter writer, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, BOUNDARY_LINES, config.getTimeSeriesToDanglingLinesMapping(), store, computationRange, withTimeSeriesStats);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToHvdcLinesMapping(BufferedWriter writer, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, HVDC_LINES, config.getTimeSeriesToHvdcLinesMapping(), store, computationRange, withTimeSeriesStats);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToPstMapping(BufferedWriter writer, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, PSTS, config.getTimeSeriesToPstMapping(), store, computationRange, withTimeSeriesStats);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToBreakersMapping(BufferedWriter writer, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, BREAKERS, config.getTimeSeriesToBreakersMapping(), store, computationRange, withTimeSeriesStats);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeMappedTimeSeries(Path dir, String fileName) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(fileName))) {
            writeMappedTimeSeries(writer);
        }
    }

    public void writeMappedTimeSeries(BufferedWriter writer) {
        try {
            writer.write(MAPPED_TS);
            writer.write(CSV_SEPARATOR);
            writer.write(TYPE);
            writer.write(CSV_SEPARATOR);
            writer.write(VARIABLE);
            writer.write(CSV_SEPARATOR);
            writer.write(SRC_TS);
            writer.newLine();

            Map<MappingKey, Set<String>> mappedTimeSeries;
            mappedTimeSeries = config.findMappedTimeSeries(config.getTimeSeriesToLoadsMapping());
            mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, LOAD_TYPE, timeSerie.getMappingVariable(), timeSerie.getId(), ids, null, null, false, null, 1, false));
            mappedTimeSeries = config.findMappedTimeSeries(config.getTimeSeriesToGeneratorsMapping());
            mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, GENERATOR_TYPE, timeSerie.getMappingVariable(), timeSerie.getId(), ids, null, null, false, null, 1, false));
            mappedTimeSeries = config.findMappedTimeSeries(config.getTimeSeriesToHvdcLinesMapping());
            mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, HVDC_LINE_TYPE, timeSerie.getMappingVariable(), timeSerie.getId(), ids, null, null, false, null, 1, false));
            mappedTimeSeries = config.findMappedTimeSeries(config.getTimeSeriesToPstMapping());
            mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, PST_TYPE, timeSerie.getMappingVariable(), timeSerie.getId(), ids, null, null, false, null, 1, false));
            mappedTimeSeries = config.findMappedTimeSeries(config.getTimeSeriesToDanglingLinesMapping());
            mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, BOUNDARY_LINE_TYPE, timeSerie.getMappingVariable(), timeSerie.getId(), ids, null, null, false, null, 1, false));
            mappedTimeSeries = config.findMappedTimeSeries(config.getTimeSeriesToBreakersMapping());
            mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, BREAKER_TYPE, timeSerie.getMappingVariable(), timeSerie.getId(), ids, null, null, false, null, 1, false));
            mappedTimeSeries = config.findDistributionKeyTimeSeries();
            mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, EMPTY_TYPE, timeSerie.getMappingVariable(), timeSerie.getId(), ids, null, null, false, null, 1, false));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeGeneratorToTimeSeriesMapping(BufferedWriter writer) {
        try {
            writeEquipmentToTimeSeriesMapping(writer, GENERATOR, config.getGeneratorToTimeSeriesMapping(), config.getTimeSeriesToGeneratorsMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeLoadToTimeSeriesMapping(BufferedWriter writer) {
        try {
            writeEquipmentToTimeSeriesMapping(writer, LOAD, config.getLoadToTimeSeriesMapping(), config.getTimeSeriesToLoadsMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeBoundaryLineToTimeSeriesMapping(BufferedWriter writer) {
        try {
            writeEquipmentToTimeSeriesMapping(writer, BOUNDARY_LINE, config.getDanglingLineToTimeSeriesMapping(), config.getTimeSeriesToDanglingLinesMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeHvdcLineToTimeSeriesMapping(BufferedWriter writer) {
        try {
            writeEquipmentToTimeSeriesMapping(writer, HVDC_LINE, config.getHvdcLineToTimeSeriesMapping(), config.getTimeSeriesToHvdcLinesMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writePstToTimeSeriesMapping(BufferedWriter writer) {
        try {
            writeEquipmentToTimeSeriesMapping(writer, PST, config.getPstToTimeSeriesMapping(), config.getTimeSeriesToPstMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeBreakerToTimeSeriesMapping(BufferedWriter writer) {
        try {
            writeEquipmentToTimeSeriesMapping(writer, BREAKER, config.getBreakerToTimeSeriesMapping(), config.getTimeSeriesToBreakersMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeUnmappedGenerators(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, GENERATOR, UNMAPPED, config.getUnmappedGenerators(), config.getIgnoredUnmappedGenerators());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeUnmappedLoads(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, LOAD, UNMAPPED, config.getUnmappedLoads(), config.getIgnoredUnmappedLoads());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeUnmappedBoundaryLines(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, BOUNDARY_LINE, UNMAPPED, config.getUnmappedDanglingLines(), config.getIgnoredUnmappedDanglingLines());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeUnmappedHvdcLines(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, HVDC_LINE, UNMAPPED, config.getUnmappedHvdcLines(), config.getIgnoredUnmappedHvdcLines());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeUnmappedPst(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, PST, UNMAPPED, config.getUnmappedPst(), config.getIgnoredUnmappedPst());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeDisconnectedGenerators(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, GENERATOR, DISCONNECTED, config.getDisconnectedGenerators(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeDisconnectedLoads(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, LOAD, DISCONNECTED, config.getDisconnectedLoads(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeDisconnectedBoundaryLines(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, BOUNDARY_LINE, DISCONNECTED, config.getDisconnectedDanglingLines(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeIgnoredUnmappedGenerators(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, GENERATOR, IGNORED_UNMAPPED, config.getIgnoredUnmappedGenerators(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeIgnoredUnmappedLoads(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, LOAD, IGNORED_UNMAPPED, config.getIgnoredUnmappedLoads(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeIgnoredUnmappedBoundaryLines(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, BOUNDARY_LINE, IGNORED_UNMAPPED, config.getIgnoredUnmappedDanglingLines(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeIgnoredUnmappedHvdcLines(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, HVDC_LINE, IGNORED_UNMAPPED, config.getIgnoredUnmappedHvdcLines(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeIgnoredUnmappedPst(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, PST, IGNORED_UNMAPPED, config.getIgnoredUnmappedPst(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeOutOfMainCCGenerators(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, GENERATOR, OUT_OF_MAIN_CC, config.getOutOfMainCcGenerators(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeOutOfMainCCLoads(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, LOAD, OUT_OF_MAIN_CC, config.getOutOfMainCcLoads(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeOutOfMainCCBoundaryLines(BufferedWriter writer) {
        try {
            writeEquipmentSet(writer, BOUNDARY_LINE, OUT_OF_MAIN_CC, config.getOutOfMainCcDanglingLines(), new HashSet<>());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeMappingCsv(Path dir, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, MappingParameters mappingParameters) {
        boolean withTimeSeriesStats = mappingParameters.getWithTimeSeriesStats();
        try {
            Files.createDirectories(dir);
            writeTimeSerieToEquipmentsMapping(dir, "timeSeriesToGeneratorsMapping.csv", GENERATORS, config.getTimeSeriesToGeneratorsMapping(), store, computationRange, withTimeSeriesStats);
            writeTimeSerieToEquipmentsMapping(dir, "timeSeriesToLoadsMapping.csv", LOADS, config.getTimeSeriesToLoadsMapping(), store, computationRange, withTimeSeriesStats);
            writeTimeSerieToEquipmentsMapping(dir, "timeSeriesToBoundaryLinesMapping.csv", BOUNDARY_LINES, config.getTimeSeriesToDanglingLinesMapping(), store, computationRange, withTimeSeriesStats);
            writeTimeSerieToEquipmentsMapping(dir, "timeSeriesToHvdcLinesMapping.csv", HVDC_LINES, config.getTimeSeriesToHvdcLinesMapping(), store, computationRange, withTimeSeriesStats);
            writeTimeSerieToEquipmentsMapping(dir, "timeSeriesToPstMapping.csv", PSTS, config.getTimeSeriesToPstMapping(), store, computationRange, withTimeSeriesStats);
            writeTimeSerieToEquipmentsMapping(dir, "timeSeriesToBreakersMapping.csv", BREAKERS, config.getTimeSeriesToBreakersMapping(), store, computationRange, withTimeSeriesStats);
            writeEquipmentToTimeSeriesMapping(dir, "generatorToTimeSeriesMapping.csv", GENERATOR, config.getGeneratorToTimeSeriesMapping(), config.getTimeSeriesToGeneratorsMapping());
            writeEquipmentToTimeSeriesMapping(dir, "loadToTimeSeriesMapping.csv", LOAD, config.getLoadToTimeSeriesMapping(), config.getTimeSeriesToLoadsMapping());
            writeEquipmentToTimeSeriesMapping(dir, "boundaryLineToTimeSeriesMapping.csv", BOUNDARY_LINE, config.getDanglingLineToTimeSeriesMapping(), config.getTimeSeriesToDanglingLinesMapping());
            writeEquipmentToTimeSeriesMapping(dir, "hvdcLineToTimeSeriesMapping.csv", HVDC_LINE, config.getHvdcLineToTimeSeriesMapping(), config.getTimeSeriesToHvdcLinesMapping());
            writeEquipmentToTimeSeriesMapping(dir, "pstToTimeSeriesMapping.csv", PST, config.getPstToTimeSeriesMapping(), config.getTimeSeriesToPstMapping());
            writeEquipmentToTimeSeriesMapping(dir, "breakerToTimeSeriesMapping.csv", BREAKER, config.getBreakerToTimeSeriesMapping(), config.getTimeSeriesToBreakersMapping());
            writeEquipmentSet(dir, "unmappedGenerators.csv", GENERATOR, UNMAPPED, config.getUnmappedGenerators(), config.getIgnoredUnmappedGenerators());
            writeEquipmentSet(dir, "unmappedLoads.csv", LOAD, UNMAPPED, config.getUnmappedLoads(), config.getIgnoredUnmappedLoads());
            writeEquipmentSet(dir, "unmappedBoundaryLines.csv", BOUNDARY_LINE, UNMAPPED, config.getUnmappedDanglingLines(), config.getIgnoredUnmappedDanglingLines());
            writeEquipmentSet(dir, "unmappedHvdcLines.csv", HVDC_LINE, UNMAPPED, config.getUnmappedHvdcLines(), config.getIgnoredUnmappedHvdcLines());
            writeEquipmentSet(dir, "unmappedPst.csv", PST, UNMAPPED, config.getUnmappedPst(), config.getIgnoredUnmappedPst());
            writeEquipmentSet(dir, "disconnectedGenerators.csv", GENERATOR, DISCONNECTED, config.getDisconnectedGenerators(), new HashSet<>());
            writeEquipmentSet(dir, "disconnectedLoads.csv", LOAD, DISCONNECTED, config.getDisconnectedLoads(), new HashSet<>());
            writeEquipmentSet(dir, "disconnectedBoundaryLines.csv", BOUNDARY_LINE, DISCONNECTED, config.getDisconnectedDanglingLines(), new HashSet<>());
            writeEquipmentSet(dir, "ignoredUnmappedGenerators.csv", GENERATOR, IGNORED_UNMAPPED, config.getIgnoredUnmappedGenerators(), new HashSet<>());
            writeEquipmentSet(dir, "ignoredUnmappedLoads.csv", LOAD, IGNORED_UNMAPPED, config.getIgnoredUnmappedLoads(), new HashSet<>());
            writeEquipmentSet(dir, "ignoredUnmappedBoundaryLines.csv", BOUNDARY_LINE, IGNORED_UNMAPPED, config.getIgnoredUnmappedDanglingLines(), new HashSet<>());
            writeEquipmentSet(dir, "ignoredUnmappedHvdcLines.csv", HVDC_LINE, IGNORED_UNMAPPED, config.getIgnoredUnmappedHvdcLines(), new HashSet<>());
            writeEquipmentSet(dir, "ignoredUnmappedPst.csv", PST, IGNORED_UNMAPPED, config.getIgnoredUnmappedPst(), new HashSet<>());
            writeEquipmentSet(dir, "outOfMainCcGenerators.csv", GENERATOR, OUT_OF_MAIN_CC, config.getOutOfMainCcGenerators(), new HashSet<>());
            writeEquipmentSet(dir, "outOfMainCcLoads.csv", LOAD, OUT_OF_MAIN_CC, config.getOutOfMainCcLoads(), new HashSet<>());
            writeEquipmentSet(dir, "outOfMainCcBoundaryLines.csv", BOUNDARY_LINE, OUT_OF_MAIN_CC, config.getOutOfMainCcDanglingLines(), new HashSet<>());
            writeMappedTimeSeries(dir, "timeSeries.csv");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeMappingSynthesisCsv(Path dir) {
        try {
            Files.createDirectories(dir);
            BufferedWriter writer = Files.newBufferedWriter(dir.resolve("mappingSynthesis.csv"));
            writeMappingSynthesisCsv(writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeMappingSynthesisCsv(BufferedWriter writer) {
        try {
            Set<MappingKey> multiMappedGenerators = config.getGeneratorToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedLoads = config.getLoadToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedBoundaryLines = config.getDanglingLineToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedHvdcLines = config.getHvdcLineToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedPst = config.getPstToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedBreakers = config.getBreakerToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());

            writer.write(SYNTHESIS);
            writer.write(CSV_SEPARATOR);
            writer.write(GENERATORS);
            writer.write(CSV_SEPARATOR);
            writer.write(LOADS);
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write(BOUNDARY_LINES);
            }
            writer.write(CSV_SEPARATOR);
            writer.write(HVDC_LINES);
            writer.write(CSV_SEPARATOR);
            writer.write(PST);
            writer.write(CSV_SEPARATOR);
            writer.write(BREAKERS);
            writer.newLine();

            writer.write(MAPPED);
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbGeneratorMapped(EquipmentVariable.targetP)));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbLoadMapped()));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write(Integer.toString(getNbDanglingLineMapped(EquipmentVariable.p0)));
            }
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbHvdcLineMapped(EquipmentVariable.activePowerSetpoint)));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbPstMapped(EquipmentVariable.currentTap)));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbBreakerMapped(EquipmentVariable.open)));
            writer.newLine();

            writer.write(UNMAPPED);
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbGeneratorUnmapped()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbLoadUnmapped()));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write(Integer.toString(getNbDanglingLineUnmapped()));
            }
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbHvdcLineUnmapped()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbPstUnmapped()));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(MULTI_MAPPED);
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(multiMappedGenerators.size()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(multiMappedLoads.size()));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write(Integer.toString(multiMappedBoundaryLines.size()));
            }
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(multiMappedHvdcLines.size()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(multiMappedPst.size()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(multiMappedBreakers.size()));
            writer.newLine();

            writer.write(IGNORED_UNMAPPED);
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(config.getIgnoredUnmappedGenerators().size()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(config.getIgnoredUnmappedLoads().size()));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write(Integer.toString(config.getIgnoredUnmappedDanglingLines().size()));
            }
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(config.getIgnoredUnmappedHvdcLines().size()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(config.getIgnoredUnmappedPst().size()));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(DISCONNECTED);
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(config.getDisconnectedGenerators().size()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(config.getDisconnectedLoads().size()));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write(Integer.toString(config.getDisconnectedDanglingLines().size()));
            }
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(OUT_OF_MAIN_CC);
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(config.getOutOfMainCcGenerators().size()));
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(config.getOutOfMainCcLoads().size()));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write(Integer.toString(config.getOutOfMainCcDanglingLines().size()));
            }
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.targetP.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbGeneratorMapped(EquipmentVariable.targetP)));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write("-");
            }
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.minP.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbGeneratorMapped(EquipmentVariable.minP)));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write("-");
            }
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbHvdcLineMapped(EquipmentVariable.minP)));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.maxP.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbGeneratorMapped(EquipmentVariable.maxP)));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write("-");
            }
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbHvdcLineMapped(EquipmentVariable.maxP)));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.p0.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbLoadMapped(EquipmentVariable.p0)));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write(Integer.toString(getNbDanglingLineMapped(EquipmentVariable.p0)));
            }
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.fixedActivePower.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbLoadMapped(EquipmentVariable.fixedActivePower)));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write("-");
            }
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.variableActivePower.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbLoadMapped(EquipmentVariable.variableActivePower)));
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write("-");
            }
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.activePowerSetpoint.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write("-");
            }
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbHvdcLineMapped(EquipmentVariable.activePowerSetpoint)));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.currentTap.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write("-");
            }
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbPstMapped(EquipmentVariable.currentTap)));
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.newLine();

            writer.write(EquipmentVariable.open.getVariableName());
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            if (IS_WITH_BOUNDARY_LINES) {
                writer.write(CSV_SEPARATOR);
                writer.write("-");
            }
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write("-");
            writer.write(CSV_SEPARATOR);
            writer.write(Integer.toString(getNbBreakerMapped(EquipmentVariable.open)));

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesMappingStatus(ReadOnlyTimeSeriesStore store, BufferedWriter writer) throws IOException {
        Set<String> mappedTimeSeriesNames = config.getMappedTimeSeriesNames();
        writer.write("timeSeries");
        writer.write(CSV_SEPARATOR);
        writer.write("spaces");
        writer.write(CSV_SEPARATOR);
        writer.write("always zero");
        writer.write(CSV_SEPARATOR);
        writer.write("mapped");
        writer.newLine();
        for (String timeSeriesName : store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))) {
            writer.write(timeSeriesName);
            writer.write(CSV_SEPARATOR);
            writer.write("");
            writer.write(CSV_SEPARATOR);
            writer.write(Boolean.toString(mappedTimeSeriesNames.contains(timeSeriesName)));
            writer.newLine();
        }
    }

    public void writeTimeSeriesMappingStatus(ReadOnlyTimeSeriesStore store, Path mappingStatusFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(mappingStatusFile)) {
            writeTimeSeriesMappingStatus(store, writer);
        }
    }

    public void writeMappingSynthesis(BufferedWriter writer) {
        try {
            writer.write(getMappingSynthesis());
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeMappingSynthesis(Path dir) {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve("mappingSynthesis.txt"))) {
            Files.createDirectories(dir);
            writeMappingSynthesis(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String getMappingSynthesis() {
        return getMappingSynthesis(TableFormatterConfig.load());
    }

    private String getMappingSynthesis(TableFormatterConfig tableFormatterConfig) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             PrintStream ps = new PrintStream(bos)) {
            printMappingSynthesis(ps, tableFormatterConfig);
            bos.flush();
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void printMappingSynthesis() {
        printMappingSynthesis(System.out, TableFormatterConfig.load());
    }

    public void printMappingSynthesis(PrintStream out) {
        printMappingSynthesis(out, TableFormatterConfig.load());
    }

    public void printMappingSynthesis(PrintStream out, TableFormatterConfig tableFormatterConfig) {
        Writer writer = new OutputStreamWriter(out) {
            @Override
            public void close() throws IOException {
                flush();
            }
        };
        try (TableFormatter tableFormatter = new AsciiTableFormatter(
                writer,
                MAPPING_SYNTHESIS,
                tableFormatterConfig,
                new Column(""),
                new Column(GENERATORS),
                new Column(LOADS),
                new Column(BOUNDARY_LINES),
                new Column(HVDC_LINES),
                new Column(PSTS),
                new Column(BREAKERS))) {
            Set<MappingKey> multiMappedGenerators = config.getGeneratorToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedLoads = config.getLoadToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedBoundaryLines = config.getDanglingLineToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedHvdcLines = config.getHvdcLineToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedPst = config.getPstToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            Set<MappingKey> multiMappedBreakers = config.getBreakerToTimeSeriesMapping().entrySet().stream().filter(e -> e.getValue().size() > 1)
                    .map(Map.Entry::getKey).collect(Collectors.toSet());
            tableFormatter.writeCell(MAPPED)
                    .writeCell(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.targetP))
                    .writeCell(getNbMapped(config.getLoadToTimeSeriesMapping()))
                    .writeCell(getNbMapped(config.getDanglingLineToTimeSeriesMapping(), EquipmentVariable.p0))
                    .writeCell(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.activePowerSetpoint))
                    .writeCell(getNbMapped(config.getPstToTimeSeriesMapping(), EquipmentVariable.currentTap))
                    .writeCell(getNbMapped(config.getBreakerToTimeSeriesMapping(), EquipmentVariable.open))
                    .writeCell(UNMAPPED)
                    .writeCell(getNbUnmapped(config.getUnmappedGenerators(), config.getIgnoredUnmappedGenerators()))
                    .writeCell(getNbUnmapped(config.getUnmappedLoads(), config.getIgnoredUnmappedLoads()))
                    .writeCell(getNbUnmapped(config.getUnmappedDanglingLines(), config.getIgnoredUnmappedDanglingLines()))
                    .writeCell(getNbUnmapped(config.getUnmappedHvdcLines(), config.getIgnoredUnmappedHvdcLines()))
                    .writeCell(getNbUnmapped(config.getUnmappedPst(), config.getIgnoredUnmappedPst()))
                    .writeCell("-")
                    .writeCell(MULTI_MAPPED)
                    .writeCell(multiMappedGenerators.size())
                    .writeCell(multiMappedLoads.size())
                    .writeCell(multiMappedBoundaryLines.size())
                    .writeCell(multiMappedHvdcLines.size())
                    .writeCell(multiMappedPst.size())
                    .writeCell(multiMappedBreakers.size())
                    .writeCell(IGNORED_UNMAPPED)
                    .writeCell(config.getIgnoredUnmappedGenerators().size())
                    .writeCell(config.getIgnoredUnmappedLoads().size())
                    .writeCell(config.getIgnoredUnmappedDanglingLines().size())
                    .writeCell(config.getIgnoredUnmappedHvdcLines().size())
                    .writeCell(config.getIgnoredUnmappedPst().size())
                    .writeCell("-")
                    .writeCell(DISCONNECTED)
                    .writeCell(config.getDisconnectedGenerators().size())
                    .writeCell(config.getDisconnectedLoads().size())
                    .writeCell(config.getDisconnectedDanglingLines().size())
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(OUT_OF_MAIN_CC)
                    .writeCell(config.getOutOfMainCcGenerators().size())
                    .writeCell(config.getOutOfMainCcLoads().size())
                    .writeCell(config.getOutOfMainCcDanglingLines().size())
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (TableFormatter tableFormatter = new AsciiTableFormatter(
                writer,
                VARIABLE_SYNTHESIS,
                tableFormatterConfig,
                new Column(""),
                new Column(GENERATORS),
                new Column(LOADS),
                new Column(BOUNDARY_LINES),
                new Column(HVDC_LINES),
                new Column(PSTS),
                new Column(BREAKERS))) {
            tableFormatter.writeCell(EquipmentVariable.targetP.getVariableName())
                    .writeCell(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.targetP))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(EquipmentVariable.minP.getVariableName())
                    .writeCell(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.minP))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.minP))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(EquipmentVariable.maxP.getVariableName())
                    .writeCell(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.maxP))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.maxP))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(EquipmentVariable.p0.getVariableName())
                    .writeCell("-")
                    .writeCell(getNbMapped(config.getLoadToTimeSeriesMapping(), EquipmentVariable.p0))
                    .writeCell(getNbMapped(config.getDanglingLineToTimeSeriesMapping(), EquipmentVariable.p0))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(EquipmentVariable.fixedActivePower.getVariableName())
                    .writeCell("-")
                    .writeCell(getNbMapped(config.getLoadToTimeSeriesMapping(), EquipmentVariable.fixedActivePower))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(EquipmentVariable.variableActivePower.getVariableName())
                    .writeCell("-")
                    .writeCell(getNbMapped(config.getLoadToTimeSeriesMapping(), EquipmentVariable.variableActivePower))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(EquipmentVariable.activePowerSetpoint.getVariableName())
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.activePowerSetpoint))
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(EquipmentVariable.currentTap.getVariableName())
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(getNbMapped(config.getPstToTimeSeriesMapping(), EquipmentVariable.currentTap))
                    .writeCell("-")
                    .writeCell(EquipmentVariable.open.getVariableName())
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell("-")
                    .writeCell(getNbMapped(config.getBreakerToTimeSeriesMapping(), EquipmentVariable.open));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int getNbGeneratorUnmapped() {
        return getNbUnmapped(config.getUnmappedGenerators(), config.getIgnoredUnmappedGenerators());
    }

    private int getNbLoadUnmapped() {
        return getNbUnmapped(config.getUnmappedLoads(), config.getIgnoredUnmappedLoads());
    }

    private int getNbDanglingLineUnmapped() {
        return getNbUnmapped(config.getUnmappedDanglingLines(), config.getIgnoredUnmappedDanglingLines());
    }

    private int getNbHvdcLineUnmapped() {
        return getNbUnmapped(config.getUnmappedHvdcLines(), config.getIgnoredUnmappedHvdcLines());
    }

    private int getNbPstUnmapped() {
        return getNbUnmapped(config.getUnmappedPst(), config.getIgnoredUnmappedPst());
    }

    private int getNbGeneratorMapped(EquipmentVariable variable) {
        return getNbMapped(config.getGeneratorToTimeSeriesMapping(), variable);
    }

    private int getNbLoadMapped(EquipmentVariable variable) {
        return getNbMapped(config.getLoadToTimeSeriesMapping(), variable);
    }

    private int getNbDanglingLineMapped(EquipmentVariable variable) {
        return getNbMapped(config.getDanglingLineToTimeSeriesMapping(), variable);
    }

    private int getNbHvdcLineMapped(EquipmentVariable variable) {
        return getNbMapped(config.getHvdcLineToTimeSeriesMapping(), variable);
    }

    private int getNbBreakerMapped(EquipmentVariable variable) {
        return getNbMapped(config.getBreakerToTimeSeriesMapping(), variable);
    }

    private int getNbPstMapped(EquipmentVariable variable) {
        return getNbMapped(config.getPstToTimeSeriesMapping(), variable);
    }

    private static int getNbMapped(Map<MappingKey, List<String>> equipmentToTimeSeriesMapping) {
        return equipmentToTimeSeriesMapping.keySet().stream()
                .map(MappingKey::getId)
                .collect(Collectors.toSet())
                .size();
    }

    private static int getNbMapped(Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, EquipmentVariable variable) {
        return equipmentToTimeSeriesMapping.keySet().stream()
                .filter(key -> key.getMappingVariable() == variable)
                .collect(Collectors.toList())
                .size();
    }

    protected String formatDouble(double value) {
        return formatter.format(value);
    }

    public int getNbGeneratorMapped() {
        return getNbMapped(config.getGeneratorToTimeSeriesMapping());
    }

    private int getNbLoadMapped() {
        return getNbMapped(config.getLoadToTimeSeriesMapping());
    }

    public int getNbDanglingLineMapped() {
        return getNbMapped(config.getDanglingLineToTimeSeriesMapping());
    }

    public int getNbHvdcLineMapped() {
        return getNbMapped(config.getHvdcLineToTimeSeriesMapping());
    }

    private List<String> getMapped(Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, EquipmentVariable variable) {
        return equipmentToTimeSeriesMapping.keySet().stream()
                .filter(key -> key.getMappingVariable() == variable)
                .map(MappingKey::getId)
                .collect(Collectors.toList());
    }

    public List<String> getGeneratorMapped(EquipmentVariable variable) {
        return getMapped(config.getGeneratorToTimeSeriesMapping(), variable);
    }

    public List<String> getHvdcLineMapped(EquipmentVariable variable) {
        return getMapped(config.getHvdcLineToTimeSeriesMapping(), variable);
    }
}
