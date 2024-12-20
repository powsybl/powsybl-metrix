/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.commons.io.table.AsciiTableFormatter;
import com.powsybl.commons.io.table.Column;
import com.powsybl.commons.io.table.TableFormatter;
import com.powsybl.commons.io.table.TableFormatterConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.powsybl.metrix.mapping.TimeSeriesConstants.*;
import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigChecker.*;
import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigCsvWriter.getNotSignificantValue;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class TimeSeriesMappingConfigSynthesisCsvWriter {

    private static final boolean IS_WITH_BOUNDARY_LINES = false;

    private static final String MAPPING_SYNTHESIS = "Mapping synthesis";
    private static final String VARIABLE_SYNTHESIS = "Variable synthesis";
    private static final String MAPPING_SYNTHESIS_FILE_NAME = "mappingSynthesis";
    private static final String MAPPING_SYNTHESIS_CSV_FILE_NAME = MAPPING_SYNTHESIS_FILE_NAME + ".csv";
    private static final String MAPPING_SYNTHESIS_TXT_FILE_NAME = MAPPING_SYNTHESIS_FILE_NAME + ".txt";

    private static final String SYNTHESIS = "Synthesis";

    protected final TimeSeriesMappingConfig config;

    public TimeSeriesMappingConfigSynthesisCsvWriter(TimeSeriesMappingConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public void writeMappingSynthesisCsv(Path dir) {
        try {
            Files.createDirectories(dir);
            BufferedWriter writer = Files.newBufferedWriter(dir.resolve(MAPPING_SYNTHESIS_CSV_FILE_NAME));
            writeMappingSynthesisCsv(writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeValue(BufferedWriter writer, String value) throws IOException {
        writer.write(CSV_SEPARATOR);
        writer.write(value);
    }

    private void writeHeader(BufferedWriter writer) throws IOException {
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
        writer.write(PSTS);
        writer.write(CSV_SEPARATOR);
        writer.write(BREAKERS);
    }

    private void writeSynthesisLine(BufferedWriter writer, String name, String nbGenerator, String nbLoad, String nbBoundaryLine, String nbHvdcLine, String nbPhaseTapChanger, String nbBreaker) throws IOException {
        writer.newLine();
        writer.write(name);
        writeValue(writer, nbGenerator);
        writeValue(writer, nbLoad);
        if (IS_WITH_BOUNDARY_LINES) {
            writeValue(writer, nbBoundaryLine);
        }
        writeValue(writer, nbHvdcLine);
        writeValue(writer, nbPhaseTapChanger);
        writeValue(writer, nbBreaker);
    }

    private void writeMapped(BufferedWriter writer) throws IOException {
        writeSynthesisLine(
            writer,
            MAPPED,
            Integer.toString(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.getByDefaultVariable(MappableEquipmentType.GENERATOR))),
            Integer.toString(getNbMapped(config.getLoadToTimeSeriesMapping(), EquipmentVariable.getByDefaultVariable(MappableEquipmentType.LOAD))),
            Integer.toString(getNbMapped(config.getDanglingLineToTimeSeriesMapping(), EquipmentVariable.getByDefaultVariable(MappableEquipmentType.BOUNDARY_LINE))),
            Integer.toString(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.getByDefaultVariable(MappableEquipmentType.HVDC_LINE))),
            Integer.toString(getNbMapped(config.getPhaseTapChangerToTimeSeriesMapping(), EquipmentVariable.getByDefaultVariable(MappableEquipmentType.PHASE_TAP_CHANGER))),
            Integer.toString(getNbMapped(config.getBreakerToTimeSeriesMapping(), EquipmentVariable.getByDefaultVariable(MappableEquipmentType.SWITCH))));
    }

    private void writeUnmapped(BufferedWriter writer) throws IOException {
        writeSynthesisLine(
            writer,
            UNMAPPED,
            Integer.toString(getNbUnmapped(config.getUnmappedGenerators(), config.getIgnoredUnmappedGenerators())),
            Integer.toString(getNbUnmapped(config.getUnmappedLoads(), config.getIgnoredUnmappedLoads())),
            Integer.toString(getNbUnmapped(config.getUnmappedDanglingLines(), config.getIgnoredUnmappedDanglingLines())),
            Integer.toString(getNbUnmapped(config.getUnmappedHvdcLines(), config.getIgnoredUnmappedHvdcLines())),
            Integer.toString(getNbUnmapped(config.getUnmappedPhaseTapChangers(), config.getIgnoredUnmappedPhaseTapChangers())),
            getNotSignificantValue());
    }

    private void writeMultiMapped(BufferedWriter writer) throws IOException {
        writeSynthesisLine(
            writer,
            MULTI_MAPPED,
            Integer.toString(getNbMultiMapped(config.getGeneratorToTimeSeriesMapping())),
            Integer.toString(getNbMultiMapped(config.getLoadToTimeSeriesMapping())),
            Integer.toString(getNbMultiMapped(config.getDanglingLineToTimeSeriesMapping())),
            Integer.toString(getNbMultiMapped(config.getHvdcLineToTimeSeriesMapping())),
            Integer.toString(getNbMultiMapped(config.getPhaseTapChangerToTimeSeriesMapping())),
            Integer.toString(getNbMultiMapped(config.getBreakerToTimeSeriesMapping())));
    }

    private void writeIgnoredUnmapped(BufferedWriter writer) throws IOException {
        writeSynthesisLine(
            writer,
            IGNORED_UNMAPPED,
            Integer.toString(config.getIgnoredUnmappedGenerators().size()),
            Integer.toString(config.getIgnoredUnmappedLoads().size()),
            Integer.toString(config.getIgnoredUnmappedDanglingLines().size()),
            Integer.toString(config.getIgnoredUnmappedHvdcLines().size()),
            Integer.toString(config.getIgnoredUnmappedPhaseTapChangers().size()),
            getNotSignificantValue());
    }

    private void writeDisconnected(BufferedWriter writer) throws IOException {
        writeSynthesisLine(
            writer,
            DISCONNECTED,
            Integer.toString(config.getDisconnectedGenerators().size()),
            Integer.toString(config.getDisconnectedLoads().size()),
            Integer.toString(config.getDisconnectedDanglingLines().size()),
            getNotSignificantValue(),
            getNotSignificantValue(),
            getNotSignificantValue());
    }

    private void writeOutOfMainCc(BufferedWriter writer) throws IOException {
        writeSynthesisLine(
            writer,
            OUT_OF_MAIN_CC,
            Integer.toString(config.getOutOfMainCcGenerators().size()),
            Integer.toString(config.getOutOfMainCcLoads().size()),
            Integer.toString(config.getOutOfMainCcDanglingLines().size()),
            getNotSignificantValue(),
            getNotSignificantValue(),
            getNotSignificantValue());
    }

    private void writeVariableSynthesis(BufferedWriter writer, EquipmentVariable variable) throws IOException {
        writeSynthesisLine(
            writer,
            variable.getVariableName(),
            getNbMapped(MappableEquipmentType.GENERATOR, variable, config.getGeneratorToTimeSeriesMapping()),
            getNbMapped(MappableEquipmentType.LOAD, variable, config.getLoadToTimeSeriesMapping()),
            getNbMapped(MappableEquipmentType.BOUNDARY_LINE, variable, config.getDanglingLineToTimeSeriesMapping()),
            getNbMapped(MappableEquipmentType.HVDC_LINE, variable, config.getHvdcLineToTimeSeriesMapping()),
            getNbMapped(MappableEquipmentType.PHASE_TAP_CHANGER, variable, config.getPhaseTapChangerToTimeSeriesMapping()),
            getNbMapped(MappableEquipmentType.SWITCH, variable, config.getBreakerToTimeSeriesMapping()));
    }

    public void writeMappingSynthesisCsv(BufferedWriter writer) {
        try {
            writeHeader(writer);
            writeMapped(writer);
            writeUnmapped(writer);
            writeMultiMapped(writer);
            writeIgnoredUnmapped(writer);
            writeDisconnected(writer);
            writeOutOfMainCc(writer);
            writeVariableSynthesis(writer, EquipmentVariable.TARGET_P);
            writeVariableSynthesis(writer, EquipmentVariable.MIN_P);
            writeVariableSynthesis(writer, EquipmentVariable.MAX_P);
            writeVariableSynthesis(writer, EquipmentVariable.P0);
            writeVariableSynthesis(writer, EquipmentVariable.FIXED_ACTIVE_POWER);
            writeVariableSynthesis(writer, EquipmentVariable.VARIABLE_ACTIVE_POWER);
            writeVariableSynthesis(writer, EquipmentVariable.ACTIVE_POWER_SETPOINT);
            writeVariableSynthesis(writer, EquipmentVariable.PHASE_TAP_POSITION);
            writeVariableSynthesis(writer, EquipmentVariable.OPEN);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(MAPPING_SYNTHESIS_TXT_FILE_NAME))) {
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
            return bos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
                new Column(StringUtils.EMPTY),
                new Column(GENERATORS),
                new Column(LOADS),
                new Column(BOUNDARY_LINES),
                new Column(HVDC_LINES),
                new Column(PSTS),
                new Column(BREAKERS))) {
            tableFormatter.writeCell(MAPPED)
                    .writeCell(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.TARGET_P))
                    .writeCell(getNbMapped(config.getLoadToTimeSeriesMapping()))
                    .writeCell(getNbMapped(config.getDanglingLineToTimeSeriesMapping(), EquipmentVariable.P0))
                    .writeCell(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.ACTIVE_POWER_SETPOINT))
                    .writeCell(getNbMapped(config.getPhaseTapChangerToTimeSeriesMapping(), EquipmentVariable.PHASE_TAP_POSITION))
                    .writeCell(getNbMapped(config.getBreakerToTimeSeriesMapping(), EquipmentVariable.OPEN))
                    .writeCell(UNMAPPED)
                    .writeCell(getNbUnmapped(config.getUnmappedGenerators(), config.getIgnoredUnmappedGenerators()))
                    .writeCell(getNbUnmapped(config.getUnmappedLoads(), config.getIgnoredUnmappedLoads()))
                    .writeCell(getNbUnmapped(config.getUnmappedDanglingLines(), config.getIgnoredUnmappedDanglingLines()))
                    .writeCell(getNbUnmapped(config.getUnmappedHvdcLines(), config.getIgnoredUnmappedHvdcLines()))
                    .writeCell(getNbUnmapped(config.getUnmappedPhaseTapChangers(), config.getIgnoredUnmappedPhaseTapChangers()))
                    .writeCell(getNotSignificantValue())
                    .writeCell(MULTI_MAPPED)
                    .writeCell(getNbMultiMapped(config.getGeneratorToTimeSeriesMapping()))
                    .writeCell(getNbMultiMapped(config.getLoadToTimeSeriesMapping()))
                    .writeCell(getNbMultiMapped(config.getDanglingLineToTimeSeriesMapping()))
                    .writeCell(getNbMultiMapped(config.getHvdcLineToTimeSeriesMapping()))
                    .writeCell(getNbMultiMapped(config.getPhaseTapChangerToTimeSeriesMapping()))
                    .writeCell(getNbMultiMapped(config.getBreakerToTimeSeriesMapping()))
                    .writeCell(IGNORED_UNMAPPED)
                    .writeCell(config.getIgnoredUnmappedGenerators().size())
                    .writeCell(config.getIgnoredUnmappedLoads().size())
                    .writeCell(config.getIgnoredUnmappedDanglingLines().size())
                    .writeCell(config.getIgnoredUnmappedHvdcLines().size())
                    .writeCell(config.getIgnoredUnmappedPhaseTapChangers().size())
                    .writeCell(getNotSignificantValue())
                    .writeCell(DISCONNECTED)
                    .writeCell(config.getDisconnectedGenerators().size())
                    .writeCell(config.getDisconnectedLoads().size())
                    .writeCell(config.getDisconnectedDanglingLines().size())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(OUT_OF_MAIN_CC)
                    .writeCell(config.getOutOfMainCcGenerators().size())
                    .writeCell(config.getOutOfMainCcLoads().size())
                    .writeCell(config.getOutOfMainCcDanglingLines().size())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (TableFormatter tableFormatter = new AsciiTableFormatter(
                writer,
                VARIABLE_SYNTHESIS,
                tableFormatterConfig,
                new Column(StringUtils.EMPTY),
                new Column(GENERATORS),
                new Column(LOADS),
                new Column(BOUNDARY_LINES),
                new Column(HVDC_LINES),
                new Column(PSTS),
                new Column(BREAKERS))) {
            tableFormatter.writeCell(EquipmentVariable.TARGET_P.getVariableName())
                    .writeCell(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.TARGET_P))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(EquipmentVariable.MIN_P.getVariableName())
                    .writeCell(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.MIN_P))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.MIN_P))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(EquipmentVariable.MAX_P.getVariableName())
                    .writeCell(getNbMapped(config.getGeneratorToTimeSeriesMapping(), EquipmentVariable.MAX_P))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.MAX_P))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(EquipmentVariable.P0.getVariableName())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNbMapped(config.getLoadToTimeSeriesMapping(), EquipmentVariable.P0))
                    .writeCell(getNbMapped(config.getDanglingLineToTimeSeriesMapping(), EquipmentVariable.P0))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(EquipmentVariable.FIXED_ACTIVE_POWER.getVariableName())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNbMapped(config.getLoadToTimeSeriesMapping(), EquipmentVariable.FIXED_ACTIVE_POWER))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(EquipmentVariable.VARIABLE_ACTIVE_POWER.getVariableName())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNbMapped(config.getLoadToTimeSeriesMapping(), EquipmentVariable.VARIABLE_ACTIVE_POWER))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(EquipmentVariable.ACTIVE_POWER_SETPOINT.getVariableName())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNbMapped(config.getHvdcLineToTimeSeriesMapping(), EquipmentVariable.ACTIVE_POWER_SETPOINT))
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(EquipmentVariable.PHASE_TAP_POSITION.getVariableName())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNbMapped(config.getPhaseTapChangerToTimeSeriesMapping(), EquipmentVariable.PHASE_TAP_POSITION))
                    .writeCell(getNotSignificantValue())
                    .writeCell(EquipmentVariable.OPEN.getVariableName())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNotSignificantValue())
                    .writeCell(getNbMapped(config.getBreakerToTimeSeriesMapping(), EquipmentVariable.OPEN));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
