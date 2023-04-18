/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.base.Strings;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.iidm.network.extensions.LoadDetailAdder;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNames;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Function;

import static com.powsybl.metrix.mapping.TimeSeriesConstants.*;

public class TimeSeriesMappingConfigCsvWriter {

    private static final int N = 1;
    private static final DecimalFormat FORMATTER = new DecimalFormat("0." + Strings.repeat("#", N), new DecimalFormatSymbols(Locale.US));
    private static final String NOT_SIGNIFICANT_VALUE = "-";

    private static final String TIME_SERIES = "TimeSeries";
    private static final String SRC_TS = "SourceTimeSeries";
    private static final String MAPPED_TS = "MappedTimeSeries";
    private static final String TYPE = "Type";
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
    private static final String MIN_POWER = "MinPower";
    private static final String MAX_POWER = "MaxPower";
    private static final String AVERAGE_POWER = "AveragePower";

    private static final String UNMAPPED_GENERATORS_FILE_NAME = "unmappedGenerators.csv";
    private static final String UNMAPPED_LOADS_FILE_NAME = "unmappedLoads.csv";
    private static final String UNMAPPED_BOUNDARY_LINES_FILE_NAME = "unmappedBoundaryLines.csv";
    private static final String UNMAPPED_HVDC_LINES_FILE_NAME = "unmappedHvdcLines.csv";
    private static final String UNMAPPED_PSTS_FILE_NAME = "unmappedPst.csv";
    private static final String DISCONNECTED_GENERATORS_FILE_NAME = "disconnectedGenerators.csv";
    private static final String DISCONNECTED_LOADS_FILE_NAME = "disconnectedLoads.csv";
    private static final String DISCONNECTED_BOUNDARY_LINES_FILE_NAME = "disconnectedBoundaryLines.csv";
    private static final String IGNORED_UNMAPPED_GENERATORS_FILE_NAME = "ignoredUnmappedGenerators.csv";
    private static final String IGNORED_UNMAPPED_LOADS_FILE_NAME = "ignoredUnmappedLoads.csv";
    private static final String IGNORED_UNMAPPED_BOUNDARY_LINES_FILE_NAME = "ignoredUnmappedBoundaryLines.csv";
    private static final String IGNORED_UNMAPPED_HVDC_LINES_FILE_NAME = "ignoredUnmappedHvdcLines.csv";
    private static final String IGNORED_UNMAPPED_PSTS_FILE_NAME = "ignoredUnmappedPst.csv";
    private static final String OUT_OF_MAIN_CC_GENERATORS_FILE_NAME = "outOfMainCcGenerators.csv";
    private static final String OUT_OF_MAIN_CC_LOADS_FILE_NAME = "outOfMainCcLoads.csv";
    private static final String OUT_OF_MAIN_CC_BOUNDARY_LINES_FILE_NAME = "outOfMainCcBoundaryLines.csv";
    private static final String TIME_SERIES_TO_GENERATORS_MAPPING = "timeSeriesToGeneratorsMapping.csv";
    private static final String TIME_SERIES_TO_LOADS_MAPPING = "timeSeriesToLoadsMapping.csv";
    private static final String TIME_SERIES_TO_BOUNDARY_LINES_MAPPING = "timeSeriesToBoundaryLinesMapping.csv";
    private static final String TIME_SERIES_TO_HVDC_LINES_MAPPING = "timeSeriesToHvdcLinesMapping.csv";
    private static final String TIME_SERIES_TO_PSTS_MAPPING = "timeSeriesToPstMapping.csv";
    private static final String TIME_SERIES_TO_BREAKERS_MAPPING = "timeSeriesToBreakersMapping.csv";
    private static final String GENERATOR_TO_TIME_SERIES_MAPPING = "generatorToTimeSeriesMapping.csv";
    private static final String LOAD_TO_TIME_SERIES_MAPPING = "loadToTimeSeriesMapping.csv";
    private static final String BOUNDARY_LINE_TO_TIME_SERIES_MAPPING = "boundaryLineToTimeSeriesMapping.csv";
    private static final String HVDC_LINE_TO_TIME_SERIES_MAPPING = "hvdcLineToTimeSeriesMapping.csv";
    private static final String PST_TO_TIME_SERIES_MAPPING = "pstToTimeSeriesMapping.csv";
    private static final String BREAKER_TO_TIME_SERIES_MAPPING = "breakerToTimeSeriesMapping.csv";
    private static final String TIME_SERIES_FILE_NAME = "timeSeries.csv";

    private final TimeSeriesMappingConfig config;
    private final Network network;
    private final boolean withTimeSeriesStats;
    private final TimeSeriesMappingConfigEquipmentCsvWriter equipmentWriter;
    private final TimeSeriesMappingConfigStats stats;

    public TimeSeriesMappingConfigCsvWriter(TimeSeriesMappingConfig config, Network network, ReadOnlyTimeSeriesStore store, ComputationRange computationRange, boolean withTimeSeriesStats) {
        this.config = Objects.requireNonNull(config);
        this.network = Objects.requireNonNull(network);
        this.withTimeSeriesStats = withTimeSeriesStats;
        Objects.requireNonNull(config);
        Objects.requireNonNull(network);
        this.equipmentWriter = new TimeSeriesMappingConfigEquipmentCsvWriter(config, network);
        this.stats = new TimeSeriesMappingConfigStats(store, Objects.requireNonNull(computationRange));
    }

    public void writeMappingCsv(Path dir) {
        try {
            Files.createDirectories(dir);
            writeAllTimeSerieToEquipmentsMapping(dir);
            writeAllEquipmentToTimeSeriesMapping(dir);
            writeAllEquipmentSet(dir);
            writeMappedTimeSeries(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String formatDouble(double value) {
        return FORMATTER.format(value);
    }

    public static String getNotSignificantValue() {
        return NOT_SIGNIFICANT_VALUE;
    }

    private void writeStatus(BufferedWriter writer, MappingVariable variable, Collection<String> values) {
        try {
            if (values.size() > 1) {
                writer.write(MULTI_MAPPED);
            } else if (variable == EquipmentVariable.targetP ||
                       variable == EquipmentVariable.p0 || variable == EquipmentVariable.fixedActivePower || variable == EquipmentVariable.variableActivePower ||
                       variable == EquipmentVariable.activePowerSetpoint ||
                       variable == EquipmentVariable.phaseTapPosition ||
                       variable == EquipmentVariable.open) {
                writer.write(MAPPED);
            } else {
                writer.write(StringUtils.EMPTY);
            }
            writer.write(CSV_SEPARATOR);
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

    private static LoadDetail getLoadDetail(Load load, double fixedActivePower, double variableActivePower) {
        load.newExtension(LoadDetailAdder.class)
                .withFixedActivePower(fixedActivePower)
                .withFixedReactivePower(0)
                .withVariableActivePower(variableActivePower)
                .withVariableReactivePower(0)
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
            writer.write(getNotSignificantValue());
        } else {
            writer.write(formatDouble(value));
        }
    }

    private void writeStats(BufferedWriter writer, String key) throws IOException {
        writeValue(writer, stats.getTimeSeriesMin(key));
        writer.write(CSV_SEPARATOR);
        writeValue(writer, stats.getTimeSeriesMax(key));
        writer.write(CSV_SEPARATOR);
        writeValue(writer, stats.getTimeSeriesAvg(key));
        writer.write(CSV_SEPARATOR);
    }

    private int computeNbCol(int nbEquipmentValues, boolean isEqToTS, boolean withNetworkPower) {
        int nbCol = nbEquipmentValues + 2; // +2 for name (equipment or time series), variable
        if (isEqToTS) {
            nbCol++; // +1 for status
            nbCol++; // +1 for 2 separated applied/non applied columns
        }
        if (withNetworkPower) {
            nbCol++; // +1 for networkPower
        }
        if (withTimeSeriesStats && !isEqToTS) {
            nbCol++; // +1 for min
            nbCol++; // +1 for max
            nbCol++; // +1 for average
        }
        return nbCol;
    }

    private void writeMultimap(BufferedWriter writer, String equipmentsLabel, MappingVariable variable, String key, Collection<String> values) {
        writeMultimap(writer, equipmentsLabel, variable, key, values, false, null, 1, false);
    }

    private void writeMultimap(BufferedWriter writer, String equipmentsLabel, MappingVariable variable, String key, Collection<String> values, boolean withTimeSeriesStats, Map<MappingKey, Double> networkPowerMap, int nbEquipmentValues, boolean isEqToTS) {
        try {
            double power;

            boolean withNetworkPower = networkPowerMap != null;
            int nbCol = computeNbCol(nbEquipmentValues, isEqToTS, withNetworkPower);

            writer.write(key);
            writer.write(CSV_SEPARATOR);
            equipmentWriter.writeEquipment(writer, equipmentsLabel, key);
            if (isEqToTS) {
                writeStatus(writer, variable, values);
            }
            if (withTimeSeriesStats && !isEqToTS) {
                writeStats(writer, key);
            }
            if (withNetworkPower) {
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
                            writer.write(StringUtils.EMPTY);
                            writer.write(CSV_SEPARATOR);
                        }
                    }
                }
            } else {
                writer.write(getNotSignificantValue());
                writer.newLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeTimeSerieToEquipmentsMapping(BufferedWriter writer, String equipmentsLabel, Map<MappingKey, List<String>> timeSerieToEquipmentsMapping) throws IOException {
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
        timeSerieToEquipmentsMapping.forEach((timeSerie, ids) -> writeMultimap(writer, equipmentsLabel, timeSerie.getMappingVariable(), timeSerie.getId(), ids, withTimeSeriesStats, networkPowerMap, 0, false));
    }

    private void writeTimeSerieToEquipmentsMapping(Path dir, String fileName, String equipmentsLabel, Map<MappingKey, List<String>> timeSerieToEquipmentsMapping) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(fileName))) {
            writeTimeSerieToEquipmentsMapping(writer, equipmentsLabel, timeSerieToEquipmentsMapping);
        }
    }

    private void writeEquipmentToTimeSeriesMapping(BufferedWriter writer, String equipmentLabel, Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, Map<MappingKey, List<String>> timeSeriesToEquipmentsMapping) throws IOException {
        writer.write(equipmentLabel);
        writer.write(CSV_SEPARATOR);
        int nbEquipmentValues = equipmentWriter.writeEquipmentHeader(writer, equipmentLabel);
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
        equipmentToTimeSeriesMapping.forEach((equipmentId, timeSeries) -> writeMultimap(writer, equipmentLabel, equipmentId.getMappingVariable(), equipmentId.getId(), timeSeries, withTimeSeriesStats, networkPowerMap, nbEquipmentValues, true));
    }

    private void writeEquipmentToTimeSeriesMapping(Path dir, String fileName, String equipmentLabel, Map<MappingKey, List<String>> equipmentToTimeSeriesMapping, Map<MappingKey, List<String>> timeSeriesToEquipmentsMapping) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(fileName))) {
            writeEquipmentToTimeSeriesMapping(writer, equipmentLabel, equipmentToTimeSeriesMapping, timeSeriesToEquipmentsMapping);
        }
    }

    public void writeTimeSeriesToGeneratorsMapping(BufferedWriter writer) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, GENERATORS, config.getTimeSeriesToGeneratorsMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToLoadsMapping(BufferedWriter writer) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, LOADS, config.getTimeSeriesToLoadsMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToBoundaryLinesMapping(BufferedWriter writer) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, BOUNDARY_LINES, config.getTimeSeriesToDanglingLinesMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToHvdcLinesMapping(BufferedWriter writer) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, HVDC_LINES, config.getTimeSeriesToHvdcLinesMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToPstMapping(BufferedWriter writer) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, PSTS, config.getTimeSeriesToPhaseTapChangersMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeTimeSeriesToBreakersMapping(BufferedWriter writer) {
        try {
            writeTimeSerieToEquipmentsMapping(writer, BREAKERS, config.getTimeSeriesToBreakersMapping());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeMappedTimeSeries(Path dir) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(TIME_SERIES_FILE_NAME))) {
            writeMappedTimeSeries(writer);
        }
    }

    public Map<MappingKey, Set<String>> findMappedTimeSeries(Map<MappingKey, List<String>> timeSeriesToEquipments) {
        Map<MappingKey, Set<String>> mappedTimeSeries = new LinkedHashMap<>();
        timeSeriesToEquipments.keySet().forEach(mappingKey -> {
            NodeCalc nodeCalc = config.getTimeSeriesNodes().get(mappingKey.getId());
            mappedTimeSeries.put(mappingKey, nodeCalc == null ? Collections.emptySet() : TimeSeriesNames.list(nodeCalc));
        });
        return mappedTimeSeries;
    }

    public Map<MappingKey, Set<String>> findDistributionKeyTimeSeries(Map<MappingKey, DistributionKey> distributionKeys) {
        Map<MappingKey, Set<String>> mappedTimeSeries = new LinkedHashMap<>();
        distributionKeys.entrySet().stream()
                .filter(e -> e.getValue() instanceof TimeSeriesDistributionKey)
                .forEach(e -> {
                    String timeSeriesName = ((TimeSeriesDistributionKey) e.getValue()).getTimeSeriesName();
                    MappingVariable mappingVariable = e.getKey().getMappingVariable();
                    NodeCalc nodeCalc = config.getTimeSeriesNodes().get(timeSeriesName);
                    mappedTimeSeries.put(new MappingKey(mappingVariable, timeSeriesName), nodeCalc == null ? Collections.emptySet() : TimeSeriesNames.list(nodeCalc));
                });
        return mappedTimeSeries;
    }

    private void writeMappedTimeSeries(BufferedWriter writer, Map<MappingKey, List<String>> timeSeriesToEquipments, String equipmentsLabel) {
        Map<MappingKey, Set<String>> mappedTimeSeries = findMappedTimeSeries(timeSeriesToEquipments);
        mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, equipmentsLabel, timeSerie.getMappingVariable(), timeSerie.getId(), ids));
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

            writeMappedTimeSeries(writer, config.getTimeSeriesToLoadsMapping(), LOAD_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToGeneratorsMapping(), GENERATOR_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToHvdcLinesMapping(), HVDC_LINE_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToPhaseTapChangersMapping(), PHASE_TAP_CHANGER_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToDanglingLinesMapping(), BOUNDARY_LINE_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToBreakersMapping(), BREAKER_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToTransformersMapping(), TRANSFORMER_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToLinesMapping(), LINE_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToRatioTapChangersMapping(), RATIO_TAP_CHANGER_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToLccConverterStationsMapping(), LCC_CONVERTER_STATION_TYPE);
            writeMappedTimeSeries(writer, config.getTimeSeriesToVscConverterStationsMapping(), VSC_CONVERTER_STATION_TYPE);

            Map<MappingKey, Set<String>> mappedTimeSeries = findDistributionKeyTimeSeries(config.getDistributionKeys());
            mappedTimeSeries.forEach((timeSerie, ids) -> writeMultimap(writer, EMPTY_TYPE, timeSerie.getMappingVariable(), timeSerie.getId(), ids,  false,  null, 1, false));
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
            writeEquipmentToTimeSeriesMapping(writer, PST, config.getPhaseTapChangerToTimeSeriesMapping(), config.getTimeSeriesToPhaseTapChangersMapping());
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

    public void writeAllEquipmentSet(Path dir) throws IOException {
        equipmentWriter.writeEquipmentSet(dir, UNMAPPED_GENERATORS_FILE_NAME, GENERATOR, UNMAPPED, config.getUnmappedGenerators(), config.getIgnoredUnmappedGenerators());
        equipmentWriter.writeEquipmentSet(dir, UNMAPPED_LOADS_FILE_NAME, LOAD, UNMAPPED, config.getUnmappedLoads(), config.getIgnoredUnmappedLoads());
        equipmentWriter.writeEquipmentSet(dir, UNMAPPED_BOUNDARY_LINES_FILE_NAME, BOUNDARY_LINE, UNMAPPED, config.getUnmappedDanglingLines(), config.getIgnoredUnmappedDanglingLines());
        equipmentWriter.writeEquipmentSet(dir, UNMAPPED_HVDC_LINES_FILE_NAME, HVDC_LINE, UNMAPPED, config.getUnmappedHvdcLines(), config.getIgnoredUnmappedHvdcLines());
        equipmentWriter.writeEquipmentSet(dir, UNMAPPED_PSTS_FILE_NAME, PST, UNMAPPED, config.getUnmappedPhaseTapChangers(), config.getIgnoredUnmappedPhaseTapChangers());
        equipmentWriter.writeEquipmentSet(dir, DISCONNECTED_GENERATORS_FILE_NAME, GENERATOR, DISCONNECTED, config.getDisconnectedGenerators(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, DISCONNECTED_LOADS_FILE_NAME, LOAD, DISCONNECTED, config.getDisconnectedLoads(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, DISCONNECTED_BOUNDARY_LINES_FILE_NAME, BOUNDARY_LINE, DISCONNECTED, config.getDisconnectedDanglingLines(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, IGNORED_UNMAPPED_GENERATORS_FILE_NAME, GENERATOR, IGNORED_UNMAPPED, config.getIgnoredUnmappedGenerators(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, IGNORED_UNMAPPED_LOADS_FILE_NAME, LOAD, IGNORED_UNMAPPED, config.getIgnoredUnmappedLoads(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, IGNORED_UNMAPPED_BOUNDARY_LINES_FILE_NAME, BOUNDARY_LINE, IGNORED_UNMAPPED, config.getIgnoredUnmappedDanglingLines(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, IGNORED_UNMAPPED_HVDC_LINES_FILE_NAME, HVDC_LINE, IGNORED_UNMAPPED, config.getIgnoredUnmappedHvdcLines(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, IGNORED_UNMAPPED_PSTS_FILE_NAME, PST, IGNORED_UNMAPPED, config.getIgnoredUnmappedPhaseTapChangers(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, OUT_OF_MAIN_CC_GENERATORS_FILE_NAME, GENERATOR, OUT_OF_MAIN_CC, config.getOutOfMainCcGenerators(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, OUT_OF_MAIN_CC_LOADS_FILE_NAME, LOAD, OUT_OF_MAIN_CC, config.getOutOfMainCcLoads(), new HashSet<>());
        equipmentWriter.writeEquipmentSet(dir, OUT_OF_MAIN_CC_BOUNDARY_LINES_FILE_NAME, BOUNDARY_LINE, OUT_OF_MAIN_CC, config.getOutOfMainCcDanglingLines(), new HashSet<>());
    }

    public void writeAllTimeSerieToEquipmentsMapping(Path dir) throws IOException {
        writeTimeSerieToEquipmentsMapping(dir, TIME_SERIES_TO_GENERATORS_MAPPING, GENERATORS, config.getTimeSeriesToGeneratorsMapping());
        writeTimeSerieToEquipmentsMapping(dir, TIME_SERIES_TO_LOADS_MAPPING, LOADS, config.getTimeSeriesToLoadsMapping());
        writeTimeSerieToEquipmentsMapping(dir, TIME_SERIES_TO_BOUNDARY_LINES_MAPPING, BOUNDARY_LINES, config.getTimeSeriesToDanglingLinesMapping());
        writeTimeSerieToEquipmentsMapping(dir, TIME_SERIES_TO_HVDC_LINES_MAPPING, HVDC_LINES, config.getTimeSeriesToHvdcLinesMapping());
        writeTimeSerieToEquipmentsMapping(dir, TIME_SERIES_TO_PSTS_MAPPING, PSTS, config.getTimeSeriesToPhaseTapChangersMapping());
        writeTimeSerieToEquipmentsMapping(dir, TIME_SERIES_TO_BREAKERS_MAPPING, BREAKERS, config.getTimeSeriesToBreakersMapping());
    }

    public void writeAllEquipmentToTimeSeriesMapping(Path dir) throws IOException {
        writeEquipmentToTimeSeriesMapping(dir, GENERATOR_TO_TIME_SERIES_MAPPING, GENERATOR, config.getGeneratorToTimeSeriesMapping(), config.getTimeSeriesToGeneratorsMapping());
        writeEquipmentToTimeSeriesMapping(dir, LOAD_TO_TIME_SERIES_MAPPING, LOAD, config.getLoadToTimeSeriesMapping(), config.getTimeSeriesToLoadsMapping());
        writeEquipmentToTimeSeriesMapping(dir, BOUNDARY_LINE_TO_TIME_SERIES_MAPPING, BOUNDARY_LINE, config.getDanglingLineToTimeSeriesMapping(), config.getTimeSeriesToDanglingLinesMapping());
        writeEquipmentToTimeSeriesMapping(dir, HVDC_LINE_TO_TIME_SERIES_MAPPING, HVDC_LINE, config.getHvdcLineToTimeSeriesMapping(), config.getTimeSeriesToHvdcLinesMapping());
        writeEquipmentToTimeSeriesMapping(dir, PST_TO_TIME_SERIES_MAPPING, PST, config.getPhaseTapChangerToTimeSeriesMapping(), config.getTimeSeriesToPhaseTapChangersMapping());
        writeEquipmentToTimeSeriesMapping(dir, BREAKER_TO_TIME_SERIES_MAPPING, BREAKER, config.getBreakerToTimeSeriesMapping(), config.getTimeSeriesToBreakersMapping());
    }
}
