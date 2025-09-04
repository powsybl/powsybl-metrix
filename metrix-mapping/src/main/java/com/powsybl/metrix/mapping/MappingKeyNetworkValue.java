/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.LoadDetail;
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException;

import java.util.Objects;

import static com.powsybl.metrix.mapping.NetworkPointWriter.OFF_VALUE;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
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
            case TARGET_P -> generator.getTargetP();
            case TARGET_Q -> generator.getTargetQ();
            case MIN_P -> generator.getMinP();
            case MAX_P -> generator.getMaxP();
            case VOLTAGE_REGULATOR_ON -> generator.isVoltageRegulatorOn() ? ON_VALUE : OFF_VALUE;
            case TARGET_V -> generator.getTargetV();
            case DISCONNECTED -> generator.getTerminal().isConnected() ? OFF_VALUE : ON_VALUE;
            default ->
                throw new TimeSeriesMappingException(String.format("Unknown variable %s for generator %s", variable, generator.getId()));
        };
    }

    private double getLoadValue(Load load, EquipmentVariable variable) {
        LoadDetail loadDetail = load.getExtension(LoadDetail.class);
        return switch (variable) {
            case P0 -> load.getP0();
            case Q0 -> load.getQ0();
            case FIXED_ACTIVE_POWER -> loadDetail != null ? loadDetail.getFixedActivePower() : 0;
            case VARIABLE_ACTIVE_POWER -> loadDetail != null ? loadDetail.getVariableActivePower() : load.getP0();
            case FIXED_REACTIVE_POWER -> loadDetail != null ? loadDetail.getFixedReactivePower() : 0;
            case VARIABLE_REACTIVE_POWER -> loadDetail != null ? loadDetail.getVariableReactivePower() : load.getQ0();
            default ->
                throw new TimeSeriesMappingException(String.format("Unknown variable %s for load %s", variable, load.getId()));
        };
    }

    private double getHvdcLineValue(HvdcLine hvdcLine, EquipmentVariable variable) {
        return switch (variable) {
            case ACTIVE_POWER_SETPOINT -> TimeSeriesMapper.getHvdcLineSetPoint(hvdcLine);
            case MAX_P -> TimeSeriesMapper.getMax(hvdcLine);
            case MIN_P -> TimeSeriesMapper.getMin(hvdcLine);
            case NOMINAL_V -> hvdcLine.getNominalV();
            default ->
                throw new TimeSeriesMappingException(String.format("Unknown variable %s for hvdcLine %s", variable, hvdcLine.getId()));
        };
    }

    private double getSwitchValue(Switch sw, EquipmentVariable variable) {
        if (variable == EquipmentVariable.OPEN) {
            return sw.isOpen() ? ON_VALUE : OFF_VALUE;
        }
        throw new TimeSeriesMappingException(String.format("Unknown variable %s for switch %s", variable, sw.getId()));
    }

    private int getRegulationModeValue(PhaseTapChanger.RegulationMode mode, boolean isRegulating) {
        return isRegulating ? switch (mode) {
            case CURRENT_LIMITER -> 0;
            case ACTIVE_POWER_CONTROL -> 1;
        } : 2;
    }

    private double getTwoWindingsTransformerValue(TwoWindingsTransformer twoWindingsTransformer, EquipmentVariable variable) {
        return switch (variable) {
            case RATED_U1 -> twoWindingsTransformer.getRatedU1();
            case RATED_U2 -> twoWindingsTransformer.getRatedU2();
            case DISCONNECTED ->
                !twoWindingsTransformer.getTerminal1().isConnected() || !twoWindingsTransformer.getTerminal2().isConnected() ? ON_VALUE : OFF_VALUE;
            // mapToPhaseTapChangers variables
            case PHASE_TAP_POSITION -> twoWindingsTransformer.getPhaseTapChanger().getTapPosition();
            case PHASE_REGULATING -> twoWindingsTransformer.getPhaseTapChanger().isRegulating() ? ON_VALUE : OFF_VALUE;
            case TARGET_DEADBAND -> twoWindingsTransformer.getPhaseTapChanger().getTargetDeadband();
            case REGULATION_MODE ->
                getRegulationModeValue(twoWindingsTransformer.getPhaseTapChanger().getRegulationMode(),
                    twoWindingsTransformer.getPhaseTapChanger().isRegulating());
            // mapToRatioTapChanger variables
            case RATIO_TAP_POSITION -> twoWindingsTransformer.getRatioTapChanger().getTapPosition();
            case TARGET_V -> twoWindingsTransformer.getRatioTapChanger().getTargetV();
            case LOAD_TAP_CHANGING_CAPABILITIES ->
                twoWindingsTransformer.getRatioTapChanger().hasLoadTapChangingCapabilities() ? ON_VALUE : OFF_VALUE;
            case RATIO_REGULATING -> twoWindingsTransformer.getRatioTapChanger().isRegulating() ? ON_VALUE : OFF_VALUE;
            default ->
                throw new TimeSeriesMappingException(String.format("Unknown variable %s for twoWindingsTransformer %s", variable, twoWindingsTransformer.getId()));
        };
    }

    private double getLccConverterStationValue(LccConverterStation lccConverterStation, EquipmentVariable variable) {
        if (variable == EquipmentVariable.POWER_FACTOR) {
            return lccConverterStation.getPowerFactor();
        }
        throw new TimeSeriesMappingException(String.format("Unknown variable %s for lccConverterStation %s", variable, lccConverterStation.getId()));
    }

    private double getVscConverterStationValue(VscConverterStation vscConverterStation, EquipmentVariable variable) {
        return switch (variable) {
            case VOLTAGE_REGULATOR_ON -> vscConverterStation.isVoltageRegulatorOn() ? ON_VALUE : OFF_VALUE;
            case VOLTAGE_SETPOINT -> vscConverterStation.getVoltageSetpoint();
            case REACTIVE_POWER_SETPOINT -> vscConverterStation.getReactivePowerSetpoint();
            default ->
                throw new TimeSeriesMappingException(String.format("Unknown variable %s for vscConverterStation %s", variable, vscConverterStation.getId()));
        };
    }

    private double getLineValue(Line line, EquipmentVariable variable) {
        if (variable == EquipmentVariable.DISCONNECTED) {
            return !line.getTerminal1().isConnected() || !line.getTerminal2().isConnected() ? ON_VALUE : OFF_VALUE;
        }
        throw new TimeSeriesMappingException(String.format("Unknown variable %s for line %s", variable, line.getId()));
    }
}
