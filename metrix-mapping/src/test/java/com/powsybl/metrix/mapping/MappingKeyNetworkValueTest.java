/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.serde.NetworkSerDe;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.EPSILON_COMPARISON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
class MappingKeyNetworkValueTest {

    private final Offset<Double> offset = Offset.offset(EPSILON_COMPARISON);
    private final Network network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));
    private final MappingKeyNetworkValue key = new MappingKeyNetworkValue(network);

    private void checkValue(double actual, double expected) {
        assertThat(actual).isCloseTo(expected, offset);
    }

    @Test
    void getGeneratorValueTest() {
        final String id = "FSSV.O11_G";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.TARGET_P, id)), 480);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.TARGET_Q, id)), 2.3523099422454834);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.MIN_P, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.MAX_P, id)), 500);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.VOLTAGE_REGULATOR_ON, id)), 1);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.TARGET_V, id)), 406.45004272460938);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.DISCONNECTED, id)), 0);
    }

    @Test
    void getLoadValueTest() {
        final String id = "FSSV.O11_L";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.P0, id)), 480);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.Q0, id)), 4.8000001907348633);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.FIXED_ACTIVE_POWER, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.VARIABLE_ACTIVE_POWER, id)), 480);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.FIXED_REACTIVE_POWER, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.VARIABLE_REACTIVE_POWER, id)), 4.8000001907348633);
    }

    @Test
    void getLoadDetailValueTest() {
        final String id = "FVALDI11_L";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.P0, id)), 470);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.Q0, id)), 4.8000001907348633);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.FIXED_ACTIVE_POWER, id)), 400);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.VARIABLE_ACTIVE_POWER, id)), 70);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.FIXED_REACTIVE_POWER, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.VARIABLE_REACTIVE_POWER, id)), 0);
    }

    @Test
    void getHvdcLineValueTest() {
        final String id = "HVDC1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.ACTIVE_POWER_SETPOINT, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.MAX_P, id)), 1011);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.MIN_P, id)), -1011);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.NOMINAL_V, id)), 320);
    }

    @Test
    void getSwitchValueTest() {
        final String id = "FSSV.O1_FSSV.O1_DJ_OMN";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.OPEN, id)), 0);
    }

    @Test
    void getTwoWindingsTransformerValueTest() {
        final String id = "FP.AND1  FTDPRA1  1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.RATED_U1, id)), 380);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.RATED_U2, id)), 380);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.DISCONNECTED, id)), 0);
        // phaseTapChanger
        checkValue(key.getValue(new MappingKey(EquipmentVariable.PHASE_TAP_POSITION, id)), 16);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.PHASE_REGULATING, id)), 1);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.PHASE_REGULATING, id)), 1);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.TARGET_DEADBAND, id)), 0);
        // ratioTapChanger
        checkValue(key.getValue(new MappingKey(EquipmentVariable.RATIO_TAP_POSITION, id)), 14);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.TARGET_V, id)), Double.NaN);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.LOAD_TAP_CHANGING_CAPABILITIES, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.RATIO_REGULATING, id)), 0);
    }

    @Test
    void getRegulationModeValueTest() {
        final String id = "FP.AND1  FTDPRA1  1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.REGULATION_MODE, id)), 0);

        // Regulation mode changed
        network.getTwoWindingsTransformer(id).getPhaseTapChanger().setRegulationMode(PhaseTapChanger.RegulationMode.ACTIVE_POWER_CONTROL);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.REGULATION_MODE, id)), 1);

        // Regulation disabled
        network.getTwoWindingsTransformer(id).getPhaseTapChanger().setRegulating(false);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.REGULATION_MODE, id)), 2);
    }

    @Test
    void getLccConverterStationValueTest() {
        final String id = "FVALDI1_FVALDI1_HVDC1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.POWER_FACTOR, id)), 0.5);
    }

    @Test
    void getVscConverterStationValueTest() {
        final String id = "FSSV.O1_FSSV.O1_HVDC2";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.VOLTAGE_REGULATOR_ON, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.VOLTAGE_SETPOINT, id)), 406);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.REACTIVE_POWER_SETPOINT, id)), 0);
    }

    @Test
    void getLineValueTest() {
        final String id = "FSSV.O1  FP.AND1  1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.DISCONNECTED, id)), 0);
    }

    @Test
    void getWrongIdentifiableTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.TARGET_P, "wrongId")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown identifiable wrongId");
    }

    @Test
    void getWrongEquipmentTypeTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.P0, "FSSV.O1_1")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown equipment type com.powsybl.iidm.network.impl.BusbarSectionImpl");
    }

    @Test
    void getWrongVariableGeneratorTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.P0, "FSSV.O11_G")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable p0 for generator FSSV.O11_G");
    }

    @Test
    void getWrongVariableLoadTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableHvdcLineTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableSwitchTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableTwoWindingTransformerTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableLccConverterStationTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableVscConverterStationTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableLineTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.TARGET_P, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }
}
