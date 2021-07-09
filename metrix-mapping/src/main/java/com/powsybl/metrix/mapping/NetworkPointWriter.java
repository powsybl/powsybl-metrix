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
import com.powsybl.iidm.export.ExportOptions;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.iidm.network.extensions.LoadDetailAdder;
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

import static com.powsybl.metrix.mapping.TimeSeriesMapper.addActivePowerRangeExtension;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class NetworkPointWriter extends DefaultTimeSeriesMapperObserver {

    private static final int ON_VALUE = 1;

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

    private void mapToEquipmentVariable(Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        if (!Double.isNaN(equipmentValue)) {
            if (identifiable instanceof Generator) {
                Generator generator = network.getGenerator(identifiable.getId());
                if (variable == EquipmentVariable.targetP) {
                    generator.setTargetP((float) equipmentValue);
                } else if (variable == EquipmentVariable.targetQ) {
                    generator.setTargetQ((float) equipmentValue);
                } else if (variable == EquipmentVariable.minP) {
                    generator.setMinP((float) equipmentValue);
                } else if (variable == EquipmentVariable.maxP) {
                    generator.setMaxP((float) equipmentValue);
                } else if (variable == EquipmentVariable.voltageRegulatorOn) {
                    generator.setVoltageRegulatorOn(Math.round(equipmentValue) == ON_VALUE);
                } else if (variable == EquipmentVariable.targetV) {
                    generator.setTargetV((float) equipmentValue);
                }
            } else if (identifiable instanceof Load) {
                Load load = network.getLoad(identifiable.getId());
                if (variable == EquipmentVariable.p0) {
                    LoadDetail loadDetail = load.getExtension(LoadDetail.class);
                    if (loadDetail != null) {
                        loadDetail.setFixedActivePower(0f);
                        loadDetail.setVariableActivePower(0f);
                    }
                    load.setP0((float) equipmentValue);
                } else if (variable == EquipmentVariable.q0) {
                    LoadDetail loadDetail = load.getExtension(LoadDetail.class);
                    if (loadDetail != null) {
                        loadDetail.setFixedReactivePower(0f);
                        loadDetail.setVariableReactivePower(0f);
                    }
                    load.setQ0((float) equipmentValue);
                } else if (variable == EquipmentVariable.fixedActivePower || variable == EquipmentVariable.variableActivePower) {
                    LoadDetail loadDetail = load.getExtension(LoadDetail.class);
                    if (loadDetail == null) {
                        load.newExtension(LoadDetailAdder.class)
                                .withFixedActivePower(0f)
                                .withFixedReactivePower(0f)
                                .withVariableActivePower(0f)
                                .withVariableReactivePower(0f)
                                .add();
                        loadDetail = load.getExtension(LoadDetail.class);
                    }
                    if (variable == EquipmentVariable.fixedActivePower) {
                        loadDetail.setFixedActivePower((float) equipmentValue);
                    } else {
                        loadDetail.setVariableActivePower((float) equipmentValue);
                    }
                    load.setP0(loadDetail.getFixedActivePower() + loadDetail.getVariableActivePower());
                } else if (variable == EquipmentVariable.fixedReactivePower || variable == EquipmentVariable.variableReactivePower) {
                    LoadDetail loadDetail = load.getExtension(LoadDetail.class);
                    if (loadDetail == null) {
                        load.newExtension(LoadDetailAdder.class)
                                .withFixedActivePower(0f)
                                .withFixedReactivePower(0f)
                                .withVariableActivePower(0f)
                                .withVariableReactivePower(0f)
                                .add();
                        loadDetail = load.getExtension(LoadDetail.class);
                    }
                    if (variable == EquipmentVariable.fixedReactivePower) {
                        loadDetail.setFixedReactivePower((float) equipmentValue);
                    } else {
                        loadDetail.setVariableReactivePower((float) equipmentValue);
                    }
                    load.setQ0(loadDetail.getFixedReactivePower() + loadDetail.getVariableReactivePower());
                }
            } else if (identifiable instanceof HvdcLine) {
                HvdcLine hvdcLine = network.getHvdcLine(identifiable.getId());
                HvdcOperatorActivePowerRange hvdcRange = addActivePowerRangeExtension(hvdcLine);
                if (variable == EquipmentVariable.activePowerSetpoint) {
                    HvdcAngleDroopActivePowerControl activePowerControl = TimeSeriesMapper.getActivePowerControl(hvdcLine);
                    if (activePowerControl != null) {
                        activePowerControl.setP0((float) equipmentValue);
                    } else {
                        if (equipmentValue >= 0) {
                            hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_RECTIFIER_SIDE_2_INVERTER);
                            hvdcLine.setActivePowerSetpoint((float) equipmentValue);
                        } else {
                            hvdcLine.setConvertersMode(HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER);
                            hvdcLine.setActivePowerSetpoint((float) -equipmentValue);
                        }
                    }
                } else if (variable == EquipmentVariable.minP || variable == EquipmentVariable.maxP) {
                    if (variable == EquipmentVariable.minP) {
                        hvdcRange.setOprFromCS2toCS1((float) Math.abs(equipmentValue));
                        double maxP = Math.max(Math.abs(equipmentValue), hvdcRange.getOprFromCS1toCS2());
                        if (hvdcLine.getMaxP() < maxP) {
                            hvdcLine.setMaxP(maxP);
                        }
                    } else {
                        hvdcRange.setOprFromCS1toCS2((float) Math.abs(equipmentValue));
                        double maxP = Math.max(Math.abs(equipmentValue), hvdcRange.getOprFromCS2toCS1());
                        if (hvdcLine.getMaxP() < maxP) {
                            hvdcLine.setMaxP(maxP);
                        }
                    }
                } else if (variable == EquipmentVariable.nominalV) {
                    hvdcLine.setNominalV(equipmentValue);
                }
            } else if (identifiable instanceof Switch) {
                Switch breaker = network.getSwitch(identifiable.getId());
                breaker.setOpen(equipmentValue == TimeSeriesMapper.SWITCH_OPEN);
            } else if (identifiable instanceof TwoWindingsTransformer) {
                TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(identifiable.getId());
                // mapToTransformers variables
                if (variable == EquipmentVariable.ratedU1) {
                    transformer.setRatedU1(equipmentValue);
                } else if (variable == EquipmentVariable.ratedU2) {
                    transformer.setRatedU2(equipmentValue);
                // mapToPhaseTapChangers variables
                } else if (variable == EquipmentVariable.phaseTapPosition) {
                    transformer.getPhaseTapChanger().setTapPosition((int) equipmentValue);
                } else if (variable == EquipmentVariable.phaseRegulating) {
                    transformer.getPhaseTapChanger().setRegulating(Math.round(equipmentValue) == ON_VALUE);
                } else if (variable == EquipmentVariable.targetDeadband) {
                    transformer.getPhaseTapChanger().setTargetDeadband(equipmentValue);
                } else if (variable == EquipmentVariable.regulationMode) {
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
                // mapToRatioTapChanger variables
                } else if (variable == EquipmentVariable.ratioTapPosition) {
                    transformer.getRatioTapChanger().setTapPosition((int) equipmentValue);
                } else if (variable == EquipmentVariable.targetV) {
                    transformer.getRatioTapChanger().setTargetV(equipmentValue);
                } else if (variable == EquipmentVariable.loadTapChangingCapabilities) {
                    transformer.getRatioTapChanger().setLoadTapChangingCapabilities(Math.round(equipmentValue) == ON_VALUE);
                } else if (variable == EquipmentVariable.ratioRegulating) {
                    transformer.getRatioTapChanger().setRegulating(Math.round(equipmentValue) == ON_VALUE);
                }
            } else if (identifiable instanceof LccConverterStation) {
                LccConverterStation converter = network.getLccConverterStation(identifiable.getId());
                if (variable == EquipmentVariable.powerFactor) {
                    converter.setPowerFactor((float) equipmentValue);
                }
            } else if (identifiable instanceof VscConverterStation) {
                VscConverterStation converter = network.getVscConverterStation(identifiable.getId());
                if (variable == EquipmentVariable.voltageRegulatorOn) {
                    converter.setVoltageRegulatorOn(Math.round(equipmentValue) == ON_VALUE);
                } else if (variable == EquipmentVariable.voltageSetpoint) {
                    converter.setVoltageSetpoint(equipmentValue);
                } else if (variable == EquipmentVariable.reactivePowerSetpoint) {
                    converter.setReactivePowerSetpoint(equipmentValue);
                }
            } else {
                throw new AssertionError(String.format("Unknown equipment type %s", identifiable.getClass().getName()));
            }
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
        } else {
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

            // Return to initial values for attributes not depending on the variant
            restoreInitialStateValues();
        }
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        mapToEquipmentVariable(identifiable, variable, equipmentValue);
    }

    @Override
    public void versionEnd(int version) {
        this.version = -1;
    }
}
