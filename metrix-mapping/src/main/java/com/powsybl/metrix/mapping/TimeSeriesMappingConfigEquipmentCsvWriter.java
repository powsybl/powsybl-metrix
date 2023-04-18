/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.Lists;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.network.extensions.LoadDetail;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.powsybl.metrix.mapping.TimeSeriesConstants.*;
import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigCsvWriter.formatDouble;
import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigCsvWriter.getNotSignificantValue;

public class TimeSeriesMappingConfigEquipmentCsvWriter {

    private static final String VOLTAGE_LEVEL = "voltageLevel";
    private static final String VOLTAGE_LEVEL1 = "voltageLevel1";
    private static final String VOLTAGE_LEVEL2 = "voltageLevel2";
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
    private static final String GENERATOR_TYPE = "generator";
    private static final String LOAD_TYPE = "load";
    private static final String BOUNDARY_LINE_TYPE = "boundary";
    private static final String HVDC_LINE_TYPE = "hvdc";
    private static final String PHASE_TAP_CHANGER_TYPE = "phaseTapChanger";
    private static final String RATIO_TAP_CHANGER_TYPE = "ratioTapChanger";
    private static final String TRANSFORMER_TYPE = "twoWindingsTransformer";
    private static final String LINE_TYPE = "line";
    private static final String LCC_CONVERTER_STATION_TYPE = "lccConverterStation";
    private static final String VSC_CONVERTER_STATION_TYPE = "vscConverterStation";
    private static final String BREAKER_TYPE = "breaker";
    private static final String EMPTY_TYPE = "-";

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

    private final TimeSeriesMappingConfig config;
    private final Network network;

    public TimeSeriesMappingConfigEquipmentCsvWriter(TimeSeriesMappingConfig config, Network network) {
        this.config = Objects.requireNonNull(config);
        this.network = Objects.requireNonNull(network);
    }

    public int writeEquipmentHeader(BufferedWriter writer, String equipmentType) {
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
        VoltageLevel voltageLevel = generator.getTerminal().getVoltageLevel();

        writer.write(voltageLevel.getSubstation().map(Identifiable::getId).orElse(StringUtils.EMPTY));
        writer.write(CSV_SEPARATOR);
        writer.write(voltageLevel.getId());
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

        int currentTap = pst.getPhaseTapChanger().getTapPosition();

        writer.write(formatDouble(currentTap));
        writer.write(CSV_SEPARATOR);
    }

    protected List<String> getLoadHeader() {
        return LOAD_HEADER;
    }

    protected void writeLoad(BufferedWriter writer, String id) throws IOException {
        Load load = network.getLoad(id);
        VoltageLevel voltageLevel = load.getTerminal().getVoltageLevel();

        LoadDetail loadDetail = load.getExtension(LoadDetail.class);
        String fixedActivePower = StringUtils.EMPTY;
        String variableActivePower = StringUtils.EMPTY;
        if (loadDetail != null) {
            fixedActivePower = formatDouble(loadDetail.getFixedActivePower());
            variableActivePower = formatDouble(loadDetail.getVariableActivePower());
        }

        writer.write(voltageLevel.getSubstation().map(Identifiable::getId).orElse(StringUtils.EMPTY));
        writer.write(CSV_SEPARATOR);
        writer.write(voltageLevel.getId());
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

        writer.write(sw.getVoltageLevel().getId());
        writer.write(CSV_SEPARATOR);
    }

    public void writeEquipment(BufferedWriter writer, String equipmentType, String id) {
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
                case PHASE_TAP_CHANGER_TYPE:
                case RATIO_TAP_CHANGER_TYPE:
                case TRANSFORMER_TYPE:
                case LINE_TYPE:
                case LCC_CONVERTER_STATION_TYPE:
                case VSC_CONVERTER_STATION_TYPE:
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

    public void writeEquipmentSet(Path dir, String fileName, String equipmentLabel, String status, Set<String> equipmentSet, Set<String> equipmentSetToIgnore) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(fileName))) {
            writeEquipmentSet(writer, equipmentLabel, status, equipmentSet, equipmentSetToIgnore);
        }
    }

    public void writeEquipmentSet(BufferedWriter writer, String equipmentLabel, String status, Set<String> equipmentSet, Set<String> equipmentSetToIgnore) throws IOException {
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
                writer.write(getNotSignificantValue()); // Sum
                writer.write(CSV_SEPARATOR);
                writer.write(getNotSignificantValue()); // Variable
                writer.write(CSV_SEPARATOR);
                writer.write(getNotSignificantValue()); // Applied time series
                writer.write(CSV_SEPARATOR);
                writer.write(getNotSignificantValue()); // Non applied time series
                writer.newLine();
            }
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
            writeEquipmentSet(writer, PST, UNMAPPED, config.getUnmappedPhaseTapChangers(), config.getIgnoredUnmappedPhaseTapChangers());
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
            writeEquipmentSet(writer, PST, IGNORED_UNMAPPED, config.getIgnoredUnmappedPhaseTapChangers(), new HashSet<>());
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
}
