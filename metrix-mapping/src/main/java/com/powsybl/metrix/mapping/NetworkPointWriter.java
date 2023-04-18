/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.iidm.network.extensions.LoadDetailAdder;
import com.powsybl.iidm.xml.ExportOptions;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.mapping.common.MetrixIidmConfiguration;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.*;

public class NetworkPointWriter extends DefaultTimeSeriesMapperObserver {

    private static final int OFF_VALUE = 0;

    private static final class GeneratorInitialValues {

        private final double minP;

        private final double maxP;

        private GeneratorInitialValues(double minP, double maxP) {
            this.minP = minP;
            this.maxP = maxP;
        }
    }

    static final class HvdcLineInitialValues {

        private final double maxP;

        private final boolean isActivePowerRange;

        private final float oprFromCS1toCS2;

        private final float oprFromCS2toCS1;

        private HvdcLineInitialValues(double maxP, boolean isActivePowerRange, float oprFromCS1toCS2, float oprFromCS2toCS1) {
            this.maxP = maxP;
            this.isActivePowerRange = isActivePowerRange;
            this.oprFromCS1toCS2 = oprFromCS1toCS2;
            this.oprFromCS2toCS1 = oprFromCS2toCS1;
        }
    }

    private final Map<String, GeneratorInitialValues> generatorToInitialValues = new HashMap<>();

    private final Map<String, HvdcLineInitialValues> hvdcLineToInitialValues = new HashMap<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final Network network;

    private final DataSource dataSource;

    private int version = -1;

    public NetworkPointWriter(Network network, DataSource dataSource) {
        this.network = Objects.requireNonNull(network);
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    private static String getStateId(int point, TimeSeriesIndex index) {
        return "point-" + index.getInstantAt(point);
    }

    private void mapToEquipmentVariable(Identifiable<?> identifiable, EquipmentVariable variable, double equipmentValue) {
        if (Double.isNaN(equipmentValue)) {
            return;
        }

        if (identifiable instanceof Generator) {
            mapToGeneratorVariable(identifiable, variable, equipmentValue);
        } else if (identifiable instanceof Load) {
            mapToLoadVariable(identifiable, variable, equipmentValue);
        } else if (identifiable instanceof HvdcLine) {
            mapToHvdcLineVariable(identifiable, variable, equipmentValue);
        } else if (identifiable instanceof Switch) {
            mapToSwitchVariable(identifiable, equipmentValue);
        } else if (identifiable instanceof TwoWindingsTransformer) {
            mapToTwoWindingsTransformerVariable(identifiable, variable, equipmentValue);
        } else if (identifiable instanceof LccConverterStation) {
            mapToLccConverterStationVariable(identifiable, variable, (float) equipmentValue);
        } else if (identifiable instanceof VscConverterStation) {
            mapToVscConverterStationVariable(identifiable, variable, equipmentValue);
        } else if (identifiable instanceof Line) {
            mapToLineVariable(identifiable, variable, equipmentValue);
        } else {
            throw new AssertionError(String.format("Unknown equipment type %s", identifiable.getClass().getName()));
        }
    }

    private void mapToLineVariable(Identifiable<?> identifiable, EquipmentVariable variable, double equipmentValue) {
        Line line = network.getLine(identifiable.getId());
        if (variable == EquipmentVariable.disconnected && Math.abs(equipmentValue - DISCONNECTED_VALUE) > EPSILON_COMPARISON) {
            line.getTerminal1().disconnect();
            line.getTerminal2().disconnect();
        }
    }

    private void mapToSwitchVariable(Identifiable<?> identifiable, double equipmentValue) {
        Switch breaker = network.getSwitch(identifiable.getId());
        breaker.setOpen(Math.abs(equipmentValue - TimeSeriesMapper.SWITCH_OPEN) < EPSILON_COMPARISON);
    }

    private void mapToLccConverterStationVariable(Identifiable<?> identifiable, EquipmentVariable variable, float equipmentValue) {
        LccConverterStation converter = network.getLccConverterStation(identifiable.getId());
        if (variable == EquipmentVariable.powerFactor) {
            converter.setPowerFactor(equipmentValue);
        }
    }

    private void mapToVscConverterStationVariable(Identifiable<?> identifiable, EquipmentVariable variable, double equipmentValue) {
        VscConverterStation converter = network.getVscConverterStation(identifiable.getId());
        switch (variable) {
            case voltageRegulatorOn:
                converter.setVoltageRegulatorOn(Math.abs(equipmentValue - OFF_VALUE) > EPSILON_COMPARISON);
                break;
            case voltageSetpoint:
                converter.setVoltageSetpoint(equipmentValue);
                break;
            case reactivePowerSetpoint:
                converter.setReactivePowerSetpoint(equipmentValue);
                break;
            default:
                break;
        }
    }

    private void mapToTwoWindingsTransformerVariable(Identifiable<?> identifiable, EquipmentVariable variable, double equipmentValue) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(identifiable.getId());
        // mapToTransformers variables
        switch (variable) {
            case ratedU1:
                transformer.setRatedU1(equipmentValue);
                break;
            case ratedU2:
                transformer.setRatedU2(equipmentValue);
                break;
            // mapToPhaseTapChangers variables
            case phaseTapPosition:
                transformer.getPhaseTapChanger().setTapPosition((int) equipmentValue);
                break;
            case phaseRegulating:
                transformer.getPhaseTapChanger().setRegulating(Math.abs(equipmentValue - OFF_VALUE) > EPSILON_COMPARISON);
                break;
            case targetDeadband:
                transformer.getPhaseTapChanger().setTargetDeadband(equipmentValue);
                break;
            case regulationMode:
                selectRegulationMode(equipmentValue, transformer);
                break;
            // mapToRatioTapChanger variables
            case ratioTapPosition:
                transformer.getRatioTapChanger().setTapPosition((int) equipmentValue);
                break;
            case targetV:
                transformer.getRatioTapChanger().setTargetV(equipmentValue);
                break;
            case loadTapChangingCapabilities:
                transformer.getRatioTapChanger().setLoadTapChangingCapabilities(Math.abs(equipmentValue - OFF_VALUE) > EPSILON_COMPARISON);
                break;
            case ratioRegulating:
                transformer.getRatioTapChanger().setRegulating(Math.abs(equipmentValue - OFF_VALUE) > EPSILON_COMPARISON);
                break;
            case disconnected:
                if (Math.abs(equipmentValue - DISCONNECTED_VALUE) > EPSILON_COMPARISON) {
                    transformer.getTerminal1().disconnect();
                    transformer.getTerminal2().disconnect();
                }
                break;
            default:
                break;
        }
    }

    private void selectRegulationMode(double equipmentValue, TwoWindingsTransformer transformer) {
        PhaseTapChanger.RegulationMode mode;
        switch ((int) equipmentValue) {
            case 0 :
                mode = PhaseTapChanger.RegulationMode.CURRENT_LIMITER;
                break;
            case 1 :
                mode = PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL;
                break;
            case 2 :
                mode = PhaseTapChanger.RegulationMode.FIXED_TAP;
                break;
            default :
                throw new AssertionError("Unsupported regulation mode " + equipmentValue);
        }
        transformer.getPhaseTapChanger().setRegulationMode(mode);
    }

    private void mapToHvdcLineVariable(Identifiable<?> identifiable, EquipmentVariable variable, double equipmentValue) {
        HvdcLine hvdcLine = network.getHvdcLine(identifiable.getId());
        HvdcOperatorActivePowerRange hvdcRange = addActivePowerRangeExtension(hvdcLine);
        switch (variable) {
            case activePowerSetpoint:
                HvdcAngleDroopActivePowerControl activePowerControl = TimeSeriesMapper.getActivePowerControl(hvdcLine);
                if (activePowerControl != null) {
                    activePowerControl.setP0((float) equipmentValue);
                } else if (equipmentValue >= 0) {
                    hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
                    hvdcLine.setActivePowerSetpoint((float) equipmentValue);
                } else {
                    hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
                    hvdcLine.setActivePowerSetpoint((float) -equipmentValue);
                }
                break;
            case minP:
                hvdcRange.setOprFromCS2toCS1((float) Math.abs(equipmentValue));
                calculateMaxP(equipmentValue, hvdcLine, hvdcRange.getOprFromCS1toCS2());
                break;
            case maxP:
                hvdcRange.setOprFromCS1toCS2((float) Math.abs(equipmentValue));
                calculateMaxP(equipmentValue, hvdcLine, hvdcRange.getOprFromCS2toCS1());
                break;
            case nominalV:
                hvdcLine.setNominalV(equipmentValue);
                break;
            default:
                break;
        }
    }

    private void calculateMaxP(double equipmentValue, HvdcLine hvdcLine, float hvdcRange) {
        double maxP = Math.max(Math.abs(equipmentValue), hvdcRange);
        if (hvdcLine.getMaxP() < maxP) {
            hvdcLine.setMaxP(maxP);
        }
    }

    private void mapToLoadVariable(Identifiable<?> identifiable, EquipmentVariable variable, double equipmentValue) {
        Load load = network.getLoad(identifiable.getId());
        LoadDetail loadDetail = load.getExtension(LoadDetail.class);
        switch (variable) {
            case p0:
                if (loadDetail != null) {
                    loadDetail.setFixedActivePower(0);
                    loadDetail.setVariableActivePower(0);
                }
                load.setP0(equipmentValue);
                break;
            case q0:
                if (loadDetail != null) {
                    loadDetail.setFixedReactivePower(0);
                    loadDetail.setVariableReactivePower(0);
                }
                load.setQ0(equipmentValue);
                break;
            case fixedActivePower:
                loadDetail = newLoadDetailExtension(load, loadDetail);
                loadDetail.setFixedActivePower(equipmentValue);
                load.setP0(loadDetail.getFixedActivePower() + loadDetail.getVariableActivePower());
                break;
            case variableActivePower:
                loadDetail = newLoadDetailExtension(load, loadDetail);
                loadDetail.setVariableActivePower(equipmentValue);
                load.setP0(loadDetail.getFixedActivePower() + loadDetail.getVariableActivePower());
                break;
            case fixedReactivePower:
                loadDetail = newLoadDetailExtension(load, loadDetail);
                loadDetail.setFixedReactivePower(equipmentValue);
                load.setQ0(loadDetail.getFixedReactivePower() + loadDetail.getVariableReactivePower());
                break;
            case variableReactivePower:
                loadDetail = newLoadDetailExtension(load, loadDetail);
                loadDetail.setVariableReactivePower(equipmentValue);
                load.setQ0(loadDetail.getFixedReactivePower() + loadDetail.getVariableReactivePower());
                break;
            default:
                break;
        }
    }

    private LoadDetail newLoadDetailExtension(Load load, LoadDetail loadDetail) {
        if (loadDetail == null) {
            load.newExtension(LoadDetailAdder.class)
                    .withFixedActivePower(0d)
                    .withFixedReactivePower(0d)
                    .withVariableActivePower(0d)
                    .withVariableReactivePower(0d)
                    .add();
            return load.getExtension(LoadDetail.class);
        }
        return loadDetail;
    }

    private void mapToGeneratorVariable(Identifiable<?> identifiable, EquipmentVariable variable, double equipmentValue) {
        Generator generator = network.getGenerator(identifiable.getId());
        switch (variable) {
            case targetP:
                generator.setTargetP(equipmentValue);
                break;
            case targetQ:
                generator.setTargetQ(equipmentValue);
                break;
            case minP:
                generator.setMinP(equipmentValue);
                break;
            case maxP:
                generator.setMaxP(equipmentValue);
                break;
            case voltageRegulatorOn:
                generator.setVoltageRegulatorOn(Math.abs(equipmentValue - OFF_VALUE) > EPSILON_COMPARISON);
                break;
            case targetV:
                generator.setTargetV(equipmentValue);
                break;
            case disconnected:
                if (Math.abs(equipmentValue - OFF_VALUE) > EPSILON_COMPARISON) {
                    generator.getTerminal().disconnect();
                }
                break;
            default:
                break;
        }
    }

    public static String getFileName(Network network, int version, int point, TimeSeriesIndex index) {
        return network.getId() + "_" + version + "_" + FMT.format(index.getInstantAt(point).atZone(ZoneId.of("UTC")));
    }

    private String getSuffix(int point, TimeSeriesIndex index) {
        return getSuffix(version, point, index);
    }

    public static String getSuffix(int version, int point, TimeSeriesIndex index) {
        return "_" + version + "_" + FMT.format(index.getInstantAt(point).atZone(ZoneId.of("UTC")));
    }

    private void storeInitialStateValues() {
        network.getGenerators().forEach(g -> generatorToInitialValues.put(g.getId(),
                new GeneratorInitialValues(
                        g.getMinP(),
                        g.getMaxP())));
        network.getHvdcLines().forEach(l -> hvdcLineToInitialValues.put(l.getId(),
                new HvdcLineInitialValues(
                        l.getMaxP(),
                        l.getExtension(HvdcOperatorActivePowerRange.class) != null,
                        l.getExtension(HvdcOperatorActivePowerRange.class) != null ? l.getExtension(HvdcOperatorActivePowerRange.class).getOprFromCS1toCS2() : (float) l.getMaxP(),
                        l.getExtension(HvdcOperatorActivePowerRange.class) != null ? l.getExtension(HvdcOperatorActivePowerRange.class).getOprFromCS2toCS1() : (float) l.getMaxP()
                        )));
    }

    private void restoreInitialStateValues() {
        for (Map.Entry<String, GeneratorInitialValues> e : generatorToInitialValues.entrySet()) {
            Generator g = network.getGenerator(e.getKey());
            GeneratorInitialValues initialValues = e.getValue();
            g.setMinP(initialValues.minP);
            g.setMaxP(initialValues.maxP);
        }
        for (Map.Entry<String, HvdcLineInitialValues> e : hvdcLineToInitialValues.entrySet()) {
            HvdcLine l = network.getHvdcLine(e.getKey());
            HvdcLineInitialValues initialValues = e.getValue();
            l.setMaxP(initialValues.maxP);
            if (initialValues.isActivePowerRange) {
                HvdcOperatorActivePowerRange activePowerRange = l.getExtension(HvdcOperatorActivePowerRange.class);
                activePowerRange.setOprFromCS1toCS2(initialValues.oprFromCS1toCS2);
                activePowerRange.setOprFromCS2toCS1(initialValues.oprFromCS2toCS1);
            } else {
                l.removeExtension(HvdcOperatorActivePowerRange.class);
            }
        }
    }

    @Override
    public void start() {
        //Nothing to do
    }

    @Override
    public void versionStart(int version) {
        this.version = version;
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        if (point != TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            String stateId = getStateId(point, index);
            network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, stateId);
            network.getVariantManager().setWorkingVariant(stateId);
        }
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        if (point == TimeSeriesMapper.CONSTANT_VARIANT_ID) {
            storeInitialStateValues();
            return;
        }

        // Write variant
        String suffix = getSuffix(point, index);
        if (dataSource instanceof MemDataSource) {
            // for the moment, it is not possible with PowSyBl to import with a suffix different from null ...
            suffix = null;
        }
        try (OutputStream os = dataSource.newOutputStream(suffix, "xiidm", false)) {
            ExportOptions exportOptions = new ExportOptions();
            exportOptions.setVersion(MetrixIidmConfiguration.load().getNetworkExportVersion());
            NetworkXml.write(network, exportOptions, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Remove variant
        network.getVariantManager().removeVariant(getStateId(point, index));
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        // Return to initial values for attributes not depending on the variant
        restoreInitialStateValues();
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        if (variable instanceof EquipmentVariable) {
            mapToEquipmentVariable(identifiable, (EquipmentVariable) variable, equipmentValue);
        }
    }

    @Override
    public void versionEnd(int version) {
        this.version = -1;
    }
}
