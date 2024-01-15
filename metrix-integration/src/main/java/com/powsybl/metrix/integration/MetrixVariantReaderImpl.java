/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.base.Strings;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.iidm.network.extensions.LoadDetailAdder;
import com.powsybl.metrix.integration.contingency.Probability;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputData;
import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.metrix.mapping.MappableEquipmentType;
import com.powsybl.metrix.mapping.MappingVariable;
import com.powsybl.metrix.mapping.TimeSeriesMapper;
import com.powsybl.timeseries.TimeSeriesTable;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.metrix.integration.MetrixVariantsWriter.getMetrixKey;
import static com.powsybl.metrix.integration.MetrixVariantsWriter.getMetrixVariableKey;
import static com.powsybl.metrix.mapping.TimeSeriesMapper.DISCONNECTED_VALUE;

public class MetrixVariantReaderImpl implements MetrixVariantReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixVariantReaderImpl.class);

    private static final int N = 3;

    private final DecimalFormat formatter = new DecimalFormat("0." + Strings.repeat("#", N), new DecimalFormatSymbols(Locale.US));

    private static final double EPSILON = Math.pow(10, -N);

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
    private final List<String> openGeneratorList = new ArrayList<>();

    private final Map<String, List<String>> metrixVariableIds = new HashMap<>();
    private final Map<String, TDoubleArrayList> metrixVariableValues = new HashMap<>();

    private final List<String> contingencyIds = new ArrayList<>();
    private final TDoubleArrayList contingencyProbabilityValues = new TDoubleArrayList();

    private final MetrixNetwork metrixNetwork;

    private final Map<String, List<Contingency>> contingencyVariableProbabilities;

    private final Map<Double, List<Contingency>> contingencyConstantProbabilities;

    private final BufferedWriter writer;

    private final char separator;

    public MetrixVariantReaderImpl(MetrixNetwork metrixNetwork, BufferedWriter writer, char separator) {
        this.metrixNetwork = Objects.requireNonNull(metrixNetwork);
        this.writer = Objects.requireNonNull(writer);
        this.separator = separator;
        this.contingencyVariableProbabilities = metrixNetwork
                .getContingencyList()
                .stream()
                .filter(contingency -> contingency.getExtension(Probability.class) != null && contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef() != null)
                .collect(Collectors.groupingBy(contingency -> contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef(), Collectors.toList()));
        this.contingencyConstantProbabilities = metrixNetwork
                .getContingencyList()
                .stream()
                .filter(contingency -> contingency.getExtension(Probability.class) != null && contingency.getExtension(Probability.class).getProbabilityBase() != null && contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef() == null)
                .collect(Collectors.groupingBy(contingency -> contingency.getExtension(Probability.class).getProbabilityBase(), Collectors.toList()));
    }

    private void addValue(String id, MetrixVariable variable, double value, Map<String, List<String>> ids, Map<String, TDoubleArrayList> values) {
        String key = getMetrixVariableKey(variable);
        if (key != null) {
            addValue(id, variable, value, ids, values, key);
        }
    }

    private void addValue(String id, EquipmentVariable variable, double value, Map<String, List<String>> ids, Map<String, TDoubleArrayList> values, MappableEquipmentType equipmentType) {
        String key = getMetrixKey(variable, equipmentType);
        addValue(id, variable, value, ids, values, key);
    }

    private void addValue(String id, MappingVariable variable, double value, Map<String, List<String>> ids, Map<String, TDoubleArrayList> values, String key) {
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
                addValue(load.getId(), EquipmentVariable.p0, activePower, loadIds, loadValues, MappableEquipmentType.LOAD);
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
            contingencyIds.addAll(value.stream().map(Contingency::getId).toList());
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
        writer.write(separator);
        writer.write(key);
        writer.write(separator);
        writer.write(Integer.toString(ids.size()));
        for (int i = 0; i < ids.size(); i++) {
            writer.write(separator);
            writer.write(ids.get(i));
            writer.write(separator);
            writer.write(formatDouble(values.get(i)));
        }
        writer.write(separator);
        writer.newLine();
        return true;
    }

    private boolean writeVariant(int num, String key, List<String> ids) throws IOException {
        if (ids.isEmpty()) {
            return false;
        }
        writer.write(Integer.toString(num));
        writer.write(separator);
        writer.write(key);
        writer.write(separator);
        writer.write(Integer.toString(ids.size()));
        for (String id : ids) {
            writer.write(separator);
            writer.write(id);
        }
        writer.write(separator);
        writer.newLine();
        return true;
    }

    @Override
    public void onVariantEnd(int variantNum) {
        addLoadValues();
        try {
            boolean atLeastOneChange = writeVariant(variantNum, "QUADIN", openBranchList);
            atLeastOneChange |= writeVariant(variantNum, "PRODIN", openGeneratorList);
            atLeastOneChange |= writeVariant(variantNum, generatorIds, generatorValues);
            atLeastOneChange |= writeVariant(variantNum, hvdcLineIds, hvdcLineValues);
            atLeastOneChange |= writeVariant(variantNum, loadIds, loadValues);
            atLeastOneChange |= writeVariant(variantNum, pstIds, pstValues);
            atLeastOneChange |= writeVariant(variantNum, metrixVariableIds, metrixVariableValues);
            atLeastOneChange |= writeVariant(variantNum, "PROBABINC", contingencyIds, contingencyProbabilityValues);

            if (!atLeastOneChange && variantNum != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
                writer.write(Integer.toString(variantNum));
                writer.write(separator);
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
        openGeneratorList.clear();
        metrixVariableIds.clear();
        metrixVariableValues.clear();
        contingencyIds.clear();
        contingencyProbabilityValues.clear();
    }

    private boolean isDifferent(double value1, double value2) {
        return Math.abs(value1 - value2) > EPSILON;
    }

    void onGeneratorVariant(Generator generator, EquipmentVariable variable, double value) {
        boolean isDifferent = true;
        if (variable == EquipmentVariable.disconnected) {
            Terminal t = generator.getTerminal();
            boolean isConnected = t.isConnected();
            isDifferent = Math.abs(value - DISCONNECTED_VALUE) < EPSILON && isConnected;
            if (isDifferent) {
                openGeneratorList.add(generator.getId());
            }
        } else {
            if (variable == EquipmentVariable.targetP) {
                isDifferent = isDifferent(value, generator.getTargetP());
            } else if (variable == EquipmentVariable.minP) {
                isDifferent = isDifferent(value, generator.getMinP());
            } else if (variable == EquipmentVariable.maxP) {
                isDifferent = isDifferent(value, generator.getMaxP());
            }
            if (isDifferent) {
                addValue(generator.getId(), variable, value, generatorIds, generatorValues, MappableEquipmentType.GENERATOR);
            }
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

    private LoadDetail getLoadDetail(Load load, double fixedActivePower, double variableActivePower) {
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
            loadDetail.setFixedActivePower(newValue);
        } else if (variable == EquipmentVariable.variableActivePower) {
            loadDetail.setVariableActivePower(newValue);
        }
    }

    void onLoadVariant(Load load, EquipmentVariable variable, double newActivePower) {
        if (variable != EquipmentVariable.fixedActivePower && variable != EquipmentVariable.variableActivePower) {
            if (isDifferent(newActivePower, load.getP0())) {
                addValue(load.getId(), EquipmentVariable.p0, newActivePower, loadIds, loadValues, MappableEquipmentType.LOAD);
            }
        } else {
            LoadDetail loadDetail = loadDetails.computeIfAbsent(load, this::getLoadDetail);
            updateLoadDetail(loadDetail, variable, newActivePower);
        }
    }

    void onHvdcVariant(HvdcLine hvdcLine, EquipmentVariable variable, double value) {
        boolean isDifferent = true;
        if (variable == EquipmentVariable.activePowerSetpoint) {
            isDifferent = isDifferent(value, MetrixInputData.getHvdcLineSetPoint(hvdcLine));
        } else if (variable == EquipmentVariable.maxP) {
            isDifferent = isDifferent(value, MetrixInputData.getHvdcLineMax(hvdcLine));
        } else if (variable == EquipmentVariable.minP) {
            isDifferent = isDifferent(value, MetrixInputData.getHvdcLineMin(hvdcLine));
        }
        if (isDifferent) {
            addValue(hvdcLine.getId(), variable, value, hvdcLineIds, hvdcLineValues, MappableEquipmentType.HVDC_LINE);
        }
    }

    void onTransformerVariant(TwoWindingsTransformer twc, EquipmentVariable variable, double value) {
        boolean isDifferent = true;
        if (variable == EquipmentVariable.disconnected) {
            Terminal t1 = twc.getTerminal1();
            Terminal t2 = twc.getTerminal1();
            boolean isConnected = t1.isConnected() && t2.isConnected();
            isDifferent = Math.abs(value - DISCONNECTED_VALUE) < EPSILON && isConnected;
            if (isDifferent) {
                openBranchList.add(twc.getId());
            }
        } else {
            if (variable == EquipmentVariable.phaseTapPosition) {
                isDifferent = isDifferent(value, twc.getPhaseTapChanger().getTapPosition());
            }
            if (isDifferent) {
                addValue(twc.getId(), variable, value, pstIds, pstValues, MappableEquipmentType.PHASE_TAP_CHANGER);
            }
        }
    }

    void onSwitchVariant(Switch sw, double value) {
        if (Math.abs(value - TimeSeriesMapper.SWITCH_OPEN) < EPSILON) {
            metrixNetwork.getMappedBranch(sw).ifPresent(openBranchList::add);
        }
    }

    void onLineVariant(Line line, MappingVariable variable, double value) {
        if (variable == EquipmentVariable.disconnected) {
            Terminal t = line.getTerminal1();
            boolean isConnected = t.isConnected();
            boolean isDifferent = Math.abs(value - DISCONNECTED_VALUE) < EPSILON && isConnected;
            if (isDifferent) {
                openBranchList.add(line.getId());
            }
        }
    }

    @Override
    public void onEquipmentVariant(Identifiable<?> identifiable, MappingVariable variable, double value) {
        if (variable instanceof MetrixVariable metrixVariable) {
            String id = identifiable.getId();
            addValue(id, metrixVariable, value, metrixVariableIds, metrixVariableValues);
        } else if (variable instanceof EquipmentVariable equipmentVariable) {
            if (identifiable instanceof Load load) {
                onLoadVariant(load, equipmentVariable, value);
            } else if (identifiable instanceof HvdcLine hvdcLine) {
                onHvdcVariant(hvdcLine, equipmentVariable, value);
            } else if (identifiable instanceof Generator generator) {
                onGeneratorVariant(generator, equipmentVariable, value);
            } else if (identifiable instanceof TwoWindingsTransformer twoWindingsTransformer) {
                onTransformerVariant(twoWindingsTransformer, equipmentVariable, value);
            } else if (identifiable instanceof Switch switchEquipment) {
                onSwitchVariant(switchEquipment, value);
            } else if (identifiable instanceof Line line) {
                onLineVariant(line, variable, value);
            }
        } else {
            throw new AssertionError("Unsupported variable type " + variable.getClass().getName());
        }
    }

}
