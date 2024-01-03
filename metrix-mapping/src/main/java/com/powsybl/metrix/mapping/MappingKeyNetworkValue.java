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
        if (variable instanceof EquipmentVariable equipmentVariable) {
            return getValue(identifiable, equipmentVariable);
        }
        throw new TimeSeriesMappingException("Unknown variable type " + variable.getClass().getName() + " for identifiable " + identifiable.getId());
    }

    private double getValue(Identifiable<?> identifiable, EquipmentVariable variable) {
        if (identifiable instanceof Generator generator) {
            return getGeneratorValue(generator, variable);
        }
        if (identifiable instanceof Load load) {
            return getLoadValue(load, variable);
        }
        if (identifiable instanceof HvdcLine hvdcLine) {
            return getHvdcLineValue(hvdcLine, variable);
        }
        if (identifiable instanceof Switch sw) {
            return getSwitchValue(sw, variable);
        }
        if (identifiable instanceof TwoWindingsTransformer twoWindingsTransformer) {
            return getTwoWindingsTransformerValue(twoWindingsTransformer, variable);
        }
        if (identifiable instanceof LccConverterStation lccConverterStation) {
            return getLccConverterStationValue(lccConverterStation, variable);
        }
        if (identifiable instanceof VscConverterStation vscConverterStation) {
            return getVscConverterStationValue(vscConverterStation, variable);
        }
        if (identifiable instanceof Line line) {
            return getLineValue(line, variable);
        }
        throw new TimeSeriesMappingException("Unknown equipment type " + identifiable.getClass().getName());
    }

    private double getGeneratorValue(Generator generator, EquipmentVariable variable) {
        return switch (variable) {
            case targetP -> generator.getTargetP();
            case targetQ -> generator.getTargetQ();
            case minP -> generator.getMinP();
            case maxP -> generator.getMaxP();
            case voltageRegulatorOn -> generator.isVoltageRegulatorOn() ? ON_VALUE : OFF_VALUE;
            case targetV -> generator.getTargetV();
            case disconnected -> generator.getTerminal().isConnected() ? OFF_VALUE : ON_VALUE;
            default ->
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for generator " + generator.getId());
        };
    }

    private double getLoadValue(Load load, EquipmentVariable variable) {
        LoadDetail loadDetail = load.getExtension(LoadDetail.class);
        return switch (variable) {
            case p0 -> load.getP0();
            case q0 -> load.getQ0();
            case fixedActivePower -> loadDetail != null ? loadDetail.getFixedActivePower() : 0;
            case variableActivePower -> loadDetail != null ? loadDetail.getVariableActivePower() : load.getP0();
            case fixedReactivePower -> loadDetail != null ? loadDetail.getFixedReactivePower() : 0;
            case variableReactivePower -> loadDetail != null ? loadDetail.getVariableReactivePower() : load.getQ0();
            default ->
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for load " + load.getId());
        };
    }

    private double getHvdcLineValue(HvdcLine hvdcLine, EquipmentVariable variable) {
        return switch (variable) {
            case activePowerSetpoint -> TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine);
            case maxP -> TimeSeriesMapper.getMax(hvdcLine);
            case minP -> TimeSeriesMapper.getMin(hvdcLine);
            case nominalV -> hvdcLine.getNominalV();
            default ->
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for hvdcLine " + hvdcLine.getId());
        };
    }

    private double getSwitchValue(Switch sw, EquipmentVariable variable) {
        if (variable == EquipmentVariable.open) {
            return sw.isOpen() ? ON_VALUE : OFF_VALUE;
        }
        throw new TimeSeriesMappingException("Unknown variable " + variable + " for switch " + sw.getId());
    }

    private int getRegulationModeValue(PhaseTapChanger.RegulationMode mode) {
        return switch (mode) {
            case CURRENT_LIMITER -> 0;
            case ACTIVE_POWER_CONTROL -> 1;
            case FIXED_TAP -> 2;
        };
    }

    private double getTwoWindingsTransformerValue(TwoWindingsTransformer twoWindingsTransformer, EquipmentVariable variable) {
        return switch (variable) {
            case ratedU1 -> twoWindingsTransformer.getRatedU1();
            case ratedU2 -> twoWindingsTransformer.getRatedU2();
            case disconnected ->
                !twoWindingsTransformer.getTerminal1().isConnected() || !twoWindingsTransformer.getTerminal2().isConnected() ? ON_VALUE : OFF_VALUE;
            // mapToPhaseTapChangers variables
            case phaseTapPosition -> twoWindingsTransformer.getPhaseTapChanger().getTapPosition();
            case phaseRegulating -> twoWindingsTransformer.getPhaseTapChanger().isRegulating() ? ON_VALUE : OFF_VALUE;
            case targetDeadband -> twoWindingsTransformer.getPhaseTapChanger().getTargetDeadband();
            case regulationMode ->
                getRegulationModeValue(twoWindingsTransformer.getPhaseTapChanger().getRegulationMode());
            // mapToRatioTapChanger variables
            case ratioTapPosition -> twoWindingsTransformer.getRatioTapChanger().getTapPosition();
            case targetV -> twoWindingsTransformer.getRatioTapChanger().getTargetV();
            case loadTapChangingCapabilities ->
                twoWindingsTransformer.getRatioTapChanger().hasLoadTapChangingCapabilities() ? ON_VALUE : OFF_VALUE;
            case ratioRegulating -> twoWindingsTransformer.getRatioTapChanger().isRegulating() ? ON_VALUE : OFF_VALUE;
            default ->
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for twoWindingsTransformer " + twoWindingsTransformer.getId());
        };
    }

    private double getLccConverterStationValue(LccConverterStation lccConverterStation, EquipmentVariable variable) {
        if (variable == EquipmentVariable.powerFactor) {
            return lccConverterStation.getPowerFactor();
        }
        throw new TimeSeriesMappingException("Unknown variable " + variable + " for lccConverterStation " + lccConverterStation.getId());
    }

    private double getVscConverterStationValue(VscConverterStation vscConverterStation, EquipmentVariable variable) {
        return switch (variable) {
            case voltageRegulatorOn -> vscConverterStation.isVoltageRegulatorOn() ? ON_VALUE : OFF_VALUE;
            case voltageSetpoint -> vscConverterStation.getVoltageSetpoint();
            case reactivePowerSetpoint -> vscConverterStation.getReactivePowerSetpoint();
            default ->
                throw new TimeSeriesMappingException("Unknown variable " + variable + " for vscConverterStation " + vscConverterStation.getId());
        };
    }

    private double getLineValue(Line line, EquipmentVariable variable) {
        if (variable == EquipmentVariable.disconnected) {
            return !line.getTerminal1().isConnected() || !line.getTerminal2().isConnected() ? ON_VALUE : OFF_VALUE;
        }
        throw new TimeSeriesMappingException("Unknown variable " + variable + " for line " + line.getId());
    }
}
