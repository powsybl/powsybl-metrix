/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.LoadDetail;

import java.util.Objects;

import static com.powsybl.metrix.mapping.NetworkPointWriter.OFF_VALUE;

public class MappingKeyNetworkValue {

    private static final int ON_VALUE = 1;

    private final Network network;

    public MappingKeyNetworkValue(Network network) {
        Objects.requireNonNull(network);
        this.network = network;
    }

    public double getValue(MappingKey key) {
        Objects.requireNonNull(key);
        Identifiable<?> identifiable = network.getIdentifiable(key.getId());
        boolean isExistingIdentifiable = identifiable != null;
        if (!isExistingIdentifiable) {
            throw new TimeSeriesMappingException("Unknown identifiable " + key.getId());
        }
        MappingVariable variable = key.getMappingVariable();
        if (variable instanceof EquipmentVariable) {
            return getValue(identifiable, (EquipmentVariable) variable);
        }
        throw new TimeSeriesMappingException("Unknown variable type " + variable.getClass().getName() + " for identifiable " + identifiable.getId());
    }

    private double getValue(Identifiable<?> identifiable, EquipmentVariable variable) {
        if (identifiable instanceof Generator) {
            return getGeneratorValue((Generator) identifiable, variable);
        }
        if (identifiable instanceof Load) {
            return getLoadValue((Load) identifiable, variable);
        }
        if (identifiable instanceof HvdcLine) {
            return getHvdcLineValue((HvdcLine) identifiable, variable);
        }
        if (identifiable instanceof Switch) {
            return getSwitchValue((Switch) identifiable, variable);
        }
        if (identifiable instanceof TwoWindingsTransformer) {
            return getTwoWindingsTransformerValue((TwoWindingsTransformer) identifiable, variable);
        }
        if (identifiable instanceof LccConverterStation) {
            return getLccConverterStationValue((LccConverterStation) identifiable, variable);
        }
        if (identifiable instanceof VscConverterStation) {
            return getVscConverterStationValue((VscConverterStation) identifiable, variable);
        }
        if (identifiable instanceof Line) {
            return getLineValue((Line) identifiable, variable);
        }
        throw new TimeSeriesMappingException("Unknown equipment type " + identifiable.getClass().getName());
    }

    private double getGeneratorValue(Generator generator, EquipmentVariable variable) {
        double value;
        switch (variable) {
            case targetP:
                value = generator.getTargetP();
                break;
            case targetQ:
                value = generator.getTargetQ();
                break;
            case minP:
                value = generator.getMinP();
                break;
            case maxP:
                value = generator.getMaxP();
                break;
            case voltageRegulatorOn:
                value = generator.isVoltageRegulatorOn() ? ON_VALUE : OFF_VALUE;
                break;
            case targetV:
                value = generator.getTargetV();
                break;
            case disconnected:
                value = generator.getTerminal().isConnected() ? OFF_VALUE : ON_VALUE;
                break;
            default:
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for generator " + generator.getId());
        }
        return value;
    }

    private double getLoadValue(Load load, EquipmentVariable variable) {
        double value;
        LoadDetail loadDetail = load.getExtension(LoadDetail.class);
        switch (variable) {
            case p0:
                value = load.getP0();
                break;
            case q0:
                value = load.getQ0();
                break;
            case fixedActivePower:
                value = loadDetail != null ? loadDetail.getFixedActivePower() : 0;
                break;
            case variableActivePower:
                value = loadDetail != null ? loadDetail.getVariableActivePower() : load.getP0();
                break;
            case fixedReactivePower:
                value = loadDetail != null ? loadDetail.getFixedReactivePower() : 0;
                break;
            case variableReactivePower:
                value = loadDetail != null ? loadDetail.getVariableReactivePower() : load.getQ0();
                break;
            default:
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for load " + load.getId());
        }
        return value;
    }

    private double getHvdcLineValue(HvdcLine hvdcLine, EquipmentVariable variable) {
        double value;
        switch (variable) {
            case activePowerSetpoint:
                value = TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine);
                break;
            case maxP:
                value = TimeSeriesMapper.getMax(hvdcLine);
                break;
            case minP:
                value = TimeSeriesMapper.getMin(hvdcLine);
                break;
            case nominalV:
                value = hvdcLine.getNominalV();
                break;
            default:
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for hvdcLine " + hvdcLine.getId());
        }
        return value;
    }

    private double getSwitchValue(Switch sw, EquipmentVariable variable) {
        if (variable == EquipmentVariable.open) {
            return sw.isOpen() ? ON_VALUE : OFF_VALUE;
        }
        throw new TimeSeriesMappingException("Unknown variable " + variable + " for switch " + sw.getId());
    }

    private int getRegulationModeValue(PhaseTapChanger.RegulationMode mode) {
        int value;
        switch (mode) {
            case CURRENT_LIMITER:
                value = 0;
                break;
            case ACTIVE_POWER_CONTROL:
                value = 1;
                break;
            case FIXED_TAP:
                value = 2;
                break;
            default:
                throw new TimeSeriesMappingException("Unsupported regulation mode " + mode);
        }
        return value;
    }

    private double getTwoWindingsTransformerValue(TwoWindingsTransformer twoWindingsTransformer, EquipmentVariable variable) {
        double value;
        switch (variable) {
            case ratedU1:
                value = twoWindingsTransformer.getRatedU1();
                break;
            case ratedU2:
                value = twoWindingsTransformer.getRatedU2();
                break;
            case disconnected:
                value = !twoWindingsTransformer.getTerminal1().isConnected() || !twoWindingsTransformer.getTerminal2().isConnected() ? ON_VALUE : OFF_VALUE;
                break;
            // mapToPhaseTapChangers variables
            case phaseTapPosition:
                value = twoWindingsTransformer.getPhaseTapChanger().getTapPosition();
                break;
            case phaseRegulating:
                value = twoWindingsTransformer.getPhaseTapChanger().isRegulating() ? ON_VALUE : OFF_VALUE;
                break;
            case targetDeadband:
                value = twoWindingsTransformer.getPhaseTapChanger().getTargetDeadband();
                break;
            case regulationMode:
                value = getRegulationModeValue(twoWindingsTransformer.getPhaseTapChanger().getRegulationMode());
                break;
            // mapToRatioTapChanger variables
            case ratioTapPosition:
                value = twoWindingsTransformer.getRatioTapChanger().getTapPosition();
                break;
            case targetV:
                value = twoWindingsTransformer.getRatioTapChanger().getTargetV();
                break;
            case loadTapChangingCapabilities:
                value = twoWindingsTransformer.getRatioTapChanger().hasLoadTapChangingCapabilities() ? ON_VALUE : OFF_VALUE;
                break;
            case ratioRegulating:
                value = twoWindingsTransformer.getRatioTapChanger().isRegulating() ? ON_VALUE : OFF_VALUE;
                break;
            default:
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for twoWindingsTransformer " + twoWindingsTransformer.getId());
        }
        return value;
    }

    private double getLccConverterStationValue(LccConverterStation lccConverterStation, EquipmentVariable variable) {
        if (variable == EquipmentVariable.powerFactor) {
            return lccConverterStation.getPowerFactor();
        }
        throw new TimeSeriesMappingException("Unknown variable " + variable + " for lccConverterStation " + lccConverterStation.getId());
    }

    private double getVscConverterStationValue(VscConverterStation vscConverterStation, EquipmentVariable variable) {
        double value;
        switch (variable) {
            case voltageRegulatorOn:
                value = vscConverterStation.isVoltageRegulatorOn() ? ON_VALUE : OFF_VALUE;
                break;
            case voltageSetpoint:
                value = vscConverterStation.getVoltageSetpoint();
                break;
            case reactivePowerSetpoint:
                value = vscConverterStation.getReactivePowerSetpoint();
                break;
            default:
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for vscConverterStation " + vscConverterStation.getId());
        }
        return value;
    }

    private double getLineValue(Line line, EquipmentVariable variable) {
        if (variable == EquipmentVariable.disconnected) {
            return !line.getTerminal1().isConnected() || !line.getTerminal2().isConnected() ? ON_VALUE : OFF_VALUE;
        }
        throw new TimeSeriesMappingException("Unknown variable " + variable + " for line " + line.getId());
    }
}