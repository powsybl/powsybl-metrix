/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.iidm.network.extensions.LoadDetailAdder;
import com.powsybl.metrix.integration.contingency.Probability;
import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.metrix.mapping.MappingVariable;
import com.powsybl.metrix.mapping.TimeSeriesMapper;
import com.powsybl.timeseries.TimeSeriesTable;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixVariantsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixVariantsWriter.class);

    private static final char SEPARATOR = ';';

    private static final int N = 3;
    private static final double EPSILON = Math.pow(10, -N);

    private final MetrixVariantProvider variantProvider;
    private final MetrixNetwork metrixNetwork;

    private static String getMetrixKey(MappingVariable variable, String equipmentType) {
        String key = null;
        switch (equipmentType) {
            case "generator":
                if (variable == EquipmentVariable.targetP) {
                    key = "PRODIM";
                } else if (variable == EquipmentVariable.minP) {
                    key = "TRPUIMIN";
                } else if (variable == EquipmentVariable.maxP) {
                    key = "TRVALPMD";
                }
                break;
            case "load":
                if (variable == EquipmentVariable.p0) {
                    key = "CONELE";
                }
                break;
            case "hvdcLine":
                if (variable == EquipmentVariable.activePowerSetpoint) {
                    key = "DCIMPPUI";
                } else if (variable == EquipmentVariable.minP) {
                    key = "DCMINPUI";
                } else if (variable == EquipmentVariable.maxP) {
                    key = "DCMAXPUI";
                }
                break;
            case "pst":
                if (variable == EquipmentVariable.currentTap) {
                    key = "DTVALDEP";
                }
                break;
            default:
                if (variable == MetrixVariable.offGridCostDown) {
                    key = "COUBHR";
                } else if (variable == MetrixVariable.offGridCostUp) {
                    key = "CTORDR";
                } else if (variable == MetrixVariable.onGridCostDown) {
                    key = "COUBAR";
                } else if (variable == MetrixVariable.onGridCostUp) {
                    key = "COUHAR";
                } else if (variable == MetrixVariable.thresholdN) {
                    key = "QATI00MN";
                } else if (variable == MetrixVariable.thresholdN1) {
                    key = "QATI5MNS";
                } else if (variable == MetrixVariable.thresholdNk) {
                    key = "QATI20MN";
                } else if (variable == MetrixVariable.thresholdITAM) {
                    key = "QATITAMN";
                } else if (variable == MetrixVariable.thresholdITAMNk) {
                    key = "QATITAMK";
                } else if (variable == MetrixVariable.thresholdNEndOr) {
                    key = "QATI00MN2";
                } else if (variable == MetrixVariable.thresholdN1EndOr) {
                    key = "QATI5MNS2";
                } else if (variable == MetrixVariable.thresholdNkEndOr) {
                    key = "QATI20MN2";
                } else if (variable == MetrixVariable.thresholdITAMEndOr) {
                    key = "QATITAMN2";
                } else if (variable == MetrixVariable.thresholdITAMNkEndOr) {
                    key = "QATITAMK2";
                } else if (variable == MetrixVariable.curativeCostDown) {
                    key = "COUEFF";
                } else {
                    LOGGER.warn("Unhandled variable {}", variable);
                }
        }
        return key;
    }

    public MetrixVariantsWriter(MetrixVariantProvider variantProvider, MetrixNetwork metrixNetwork) {
        this.variantProvider = variantProvider;
        this.metrixNetwork = metrixNetwork;
    }

    public void write(Range<Integer> variantRange, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            write(variantRange, writer);
        }
    }

    public void writeGzip(Range<Integer> variantRange, Path fileGz) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(fileGz)), StandardCharsets.UTF_8))) {
            write(variantRange, writer);
        }
    }

    public void write(Range<Integer> variantRange, BufferedWriter writer) throws IOException {
        writer.write("NT");
        writer.write(SEPARATOR);
        if (variantProvider == null) {
            writer.write("1");
            writer.write(SEPARATOR);
            writer.newLine();
            writer.write("0");
            writer.write(SEPARATOR);
            writer.newLine();
        } else {
            int variantCount = variantRange.upperEndpoint() - variantRange.lowerEndpoint() + 1;
            writer.write(Integer.toString(variantCount));
            writer.write(SEPARATOR);
            writer.newLine();
            variantProvider.readVariants(variantRange, new MetrixVariantReader() {

                private final DecimalFormat formatter = new DecimalFormat("0." + Strings.repeat("#", N), new DecimalFormatSymbols(Locale.US));

                private final Map<Load, LoadDetail> loadDetails = new LinkedHashMap<>();

                private final Map<String, List<String>> generatorIds = new HashMap<>();
                private final Map<String, TDoubleArrayList> generatorValues = new HashMap<>();

                private final Map<String, List<String>> hvdcLineIds = new HashMap<>();
                private final Map<String, TDoubleArrayList> hvdcLineValues = new HashMap<>();

                private final Map<String, List<String>> loadIds = new HashMap<>();
                private final Map<String, TDoubleArrayList> loadValues = new HashMap<>();

                private final Map<String, List<String>> pstIds = new HashMap<>();
                private final Map<String, TDoubleArrayList> pstValues = new HashMap<>();

                private final List<String> openBranchList = new ArrayList<>();

                private final Map<String, List<String>> metrixVariableIds = new HashMap<>();
                private final Map<String, TDoubleArrayList> metrixVariableValues = new HashMap<>();

                private final List<String> contingencyIds = new ArrayList<>();
                private final TDoubleArrayList contingencyProbabilityValues = new TDoubleArrayList();

                private final Map<String, List<Contingency>> contingencyVariableProbabilities = metrixNetwork
                        .getContingencyList()
                        .stream()
                        .filter(contingency -> contingency.getExtension(Probability.class) != null && contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef() != null)
                        .collect(Collectors.groupingBy(contingency -> ((Contingency) contingency).getExtension(Probability.class).getProbabilityTimeSeriesRef(), Collectors.toList()));

                private final Map<Double, List<Contingency>> contingencyConstantProbabilities = metrixNetwork
                        .getContingencyList()
                        .stream()
                        .filter(contingency -> contingency.getExtension(Probability.class) != null && contingency.getExtension(Probability.class).getProbabilityBase() != null && contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef() == null)
                        .collect(Collectors.groupingBy(contingency -> ((Contingency) contingency).getExtension(Probability.class).getProbabilityBase(), Collectors.toList()));

                private void addValue(String id, MappingVariable variable, double value, Map<String, List<String>> ids, Map<String, TDoubleArrayList> values, String equipmentType) {
                    String key = getMetrixKey(variable, equipmentType);
                    if (key == null) {
                        LOGGER.warn("Unrecognized key {}", variable);
                        return;
                    }
                    if (ids.containsKey(key)) {
                        ids.get(key).add(id);
                        values.get(key).add(value);
                    } else {
                        List<String> idList = new ArrayList<>();
                        idList.add(id);
                        ids.put(key, idList);
                        TDoubleArrayList valueList = new TDoubleArrayList();
                        valueList.add(value);
                        values.put(key, valueList);
                    }
                }

                private void addLoadValues() {
                    loadDetails.forEach((load, loadDetail) -> {
                        double activePower = loadDetail.getFixedActivePower() + loadDetail.getVariableActivePower();
                        if (isDifferent(activePower, load.getP0())) {
                            addValue(load.getId(), EquipmentVariable.p0, activePower, loadIds, loadValues, "load");
                        }
                    });
                }

                @Override
                public void onVariantStart(int variantNum) {
                    // nothing to do
                }

                @Override
                public void onVariant(int version, int point, TimeSeriesTable table) {
                    if (point != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
                        addContingencyProbabilityValue(contingencyVariableProbabilities, tsName -> {
                            int tsIndex = table.getDoubleTimeSeriesIndex(tsName);
                            return table.getDoubleValue(version, tsIndex, point);
                        });
                        addContingencyProbabilityValue(contingencyConstantProbabilities, value -> value);
                    }
                }

                private <T> void addContingencyProbabilityValue(Map<T, List<Contingency>> contingencyProbabilities, Function<T, Double> getValue) {
                    contingencyProbabilities.forEach((key, value) -> {
                        double[] probabilityValues = new double[value.size()];
                        Arrays.fill(probabilityValues, getValue.apply(key));
                        contingencyProbabilityValues.add(probabilityValues);
                        contingencyIds.addAll(value.stream().map(Contingency::getId).collect(Collectors.toList()));
                    });
                }

                private String formatDouble(double value) {
                    return formatter.format(value);
                }

                private boolean writeVariant(int num, Map<String, List<String>> ids, Map<String, TDoubleArrayList> values) throws IOException {
                    boolean atLeastOneChange = false;
                    for (Map.Entry<String, List<String>> e : ids.entrySet()) {
                        String key = e.getKey();
                        atLeastOneChange |= writeVariant(num, key, e.getValue(), values.get(key));
                    }
                    return atLeastOneChange;
                }

                private boolean writeVariant(int num, String key, List<String> ids, TDoubleArrayList values) throws IOException {
                    if (ids.isEmpty()) {
                        return false;
                    }
                    writer.write(Integer.toString(num));
                    writer.write(SEPARATOR);
                    writer.write(key);
                    writer.write(SEPARATOR);
                    writer.write(Integer.toString(ids.size()));
                    for (int i = 0; i < ids.size(); i++) {
                        writer.write(SEPARATOR);
                        writer.write(ids.get(i));
                        writer.write(SEPARATOR);
                        writer.write(formatDouble(values.get(i)));
                    }
                    writer.write(SEPARATOR);
                    writer.newLine();
                    return true;
                }

                private boolean writeVariant(int num, String key, List<String> ids) throws IOException {
                    if (ids.isEmpty()) {
                        return false;
                    }
                    writer.write(Integer.toString(num));
                    writer.write(SEPARATOR);
                    writer.write(key);
                    writer.write(SEPARATOR);
                    writer.write(Integer.toString(ids.size()));
                    for (int i = 0; i < ids.size(); i++) {
                        writer.write(SEPARATOR);
                        writer.write(ids.get(i));
                    }
                    writer.write(SEPARATOR);
                    writer.newLine();
                    return true;
                }

                @Override
                public void onVariantEnd(int variantNum) {
                    addLoadValues();
                    try {
                        boolean atLeastOneChange = writeVariant(variantNum, "QUADIN", openBranchList);
                        atLeastOneChange |= writeVariant(variantNum, generatorIds, generatorValues);
                        atLeastOneChange |= writeVariant(variantNum, hvdcLineIds, hvdcLineValues);
                        atLeastOneChange |= writeVariant(variantNum, loadIds, loadValues);
                        atLeastOneChange |= writeVariant(variantNum, pstIds, pstValues);
                        atLeastOneChange |= writeVariant(variantNum, metrixVariableIds, metrixVariableValues);
                        atLeastOneChange |= writeVariant(variantNum, "PROBABINC", contingencyIds, contingencyProbabilityValues);

                        if (!atLeastOneChange && variantNum != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
                            writer.write(Integer.toString(variantNum));
                            writer.write(SEPARATOR);
                            writer.newLine();
                        }

                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    loadDetails.clear();
                    loadIds.clear();
                    loadValues.clear();
                    generatorIds.clear();
                    generatorValues.clear();
                    hvdcLineIds.clear();
                    hvdcLineValues.clear();
                    pstIds.clear();
                    pstValues.clear();
                    openBranchList.clear();
                    metrixVariableIds.clear();
                    metrixVariableValues.clear();
                    contingencyIds.clear();
                    contingencyProbabilityValues.clear();
                }

                private boolean isDifferent(double value1, double value2) {
                    return Math.abs(value1 - value2) > EPSILON;
                }

                void onGeneratorVariant(Generator generator, MappingVariable variable, double value) {
                    boolean isDifferent = true;
                    if (variable == EquipmentVariable.targetP) {
                        isDifferent = isDifferent(value, generator.getTargetP());
                    } else if (variable == EquipmentVariable.minP) {
                        isDifferent = isDifferent(value, generator.getMinP());
                    } else if (variable == EquipmentVariable.maxP) {
                        isDifferent = isDifferent(value, generator.getMaxP());
                    }
                    if (isDifferent) {
                        addValue(generator.getId(), variable, value, generatorIds, generatorValues, "generator");
                    }
                }

                private LoadDetail getLoadDetail(Load load) {
                    LoadDetail ld = load.getExtension(LoadDetail.class);
                    if (ld != null) {
                        return getLoadDetail(load, ld.getFixedActivePower(), ld.getVariableActivePower());
                    } else {
                        return getLoadDetail(load, 0, 0);
                    }
                }

                private LoadDetail getLoadDetail(Load load, float fixedActivePower, float variableActivePower) {
                    load.newExtension(LoadDetailAdder.class)
                            .withFixedActivePower(fixedActivePower)
                            .withFixedReactivePower(0f)
                            .withVariableActivePower(variableActivePower)
                            .withVariableReactivePower(0f)
                            .add();
                    return load.getExtension(LoadDetail.class);
                }

                private void updateLoadDetail(LoadDetail loadDetail, MappingVariable variable, double newValue) {
                    if (variable == EquipmentVariable.fixedActivePower) {
                        loadDetail.setFixedActivePower((float) newValue);
                    } else if (variable == EquipmentVariable.variableActivePower) {
                        loadDetail.setVariableActivePower((float) newValue);
                    }
                }

                void onLoadVariant(Load load, MappingVariable variable, double newActivePower) {
                    if (variable != EquipmentVariable.fixedActivePower && variable != EquipmentVariable.variableActivePower) {
                        if (isDifferent(newActivePower, load.getP0())) {
                            addValue(load.getId(), EquipmentVariable.p0, newActivePower, loadIds, loadValues, "load");
                        }
                    } else {
                        LoadDetail loadDetail = loadDetails.computeIfAbsent(load, this::getLoadDetail);
                        updateLoadDetail(loadDetail, variable, newActivePower);
                    }
                }

                void onHvdcVariant(HvdcLine hvdcLine, MappingVariable variable, double value) {
                    boolean isDifferent = true;
                    if (variable == EquipmentVariable.activePowerSetpoint) {
                        isDifferent = isDifferent(value, MetrixInputData.getHvdcLineSetPoint(hvdcLine));
                    } else if (variable == EquipmentVariable.maxP) {
                        isDifferent = isDifferent(value, MetrixInputData.getHvdcLineMax(hvdcLine));
                    } else if (variable == EquipmentVariable.minP) {
                        isDifferent = isDifferent(value, MetrixInputData.getHvdcLineMin(hvdcLine));
                    }
                    if (isDifferent) {
                        addValue(hvdcLine.getId(), variable, value, hvdcLineIds, hvdcLineValues, "hvdcLine");
                    }
                }

                void onPstVariant(TwoWindingsTransformer twc, MappingVariable variable, double value) {
                    boolean isDifferent = true;
                    if (variable == EquipmentVariable.currentTap) {
                        isDifferent = isDifferent(value, twc.getPhaseTapChanger().getTapPosition());
                    }
                    if (isDifferent) {
                        addValue(twc.getId(), variable, value, pstIds, pstValues, "pst");
                    }
                }

                void onSwitchVariant(Switch sw, double value) {
                    if (value == TimeSeriesMapper.SWITCH_OPEN) {
                        metrixNetwork.getMappedBranch(sw).ifPresent(openBranchList::add);
                    }
                }

                @Override
                public void onEquipmentVariant(Identifiable identifiable, MappingVariable variable, double value) {
                    if (variable instanceof MetrixVariable) {
                        String id = identifiable.getId();
                        addValue(id, variable, value, metrixVariableIds, metrixVariableValues, "");
                    } else if (variable instanceof EquipmentVariable) {
                        if (identifiable instanceof Load) {
                            onLoadVariant((Load) identifiable, variable, value);
                        } else if (identifiable instanceof HvdcLine) {
                            onHvdcVariant((HvdcLine) identifiable, variable, value);
                        } else if (identifiable instanceof Generator) {
                            onGeneratorVariant((Generator) identifiable, variable, value);
                        } else if (identifiable instanceof TwoWindingsTransformer) {
                            onPstVariant((TwoWindingsTransformer) identifiable, variable, value);
                        } else if (identifiable instanceof Switch) {
                            onSwitchVariant((Switch) identifiable, value);
                        }
                    } else {
                        throw new AssertionError("Unsupported variable type " + variable.getClass().getName());
                    }
                }

            });
        }

    }
}
