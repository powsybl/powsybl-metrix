/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.serde.NetworkSerDe;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.EPSILON_COMPARISON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author marifunf {@literal <marianne.funfrock at rte-france.com>}
 */
class MappingKeyNetworkValueTest {

    private final Offset<Double> offset = Offset.offset(EPSILON_COMPARISON);
    private final MappingKeyNetworkValue key = new MappingKeyNetworkValue(NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml"))));

    private void checkValue(double actual, double expected) {
        assertThat(actual).isCloseTo(expected, offset);
    }

    @Test
    void getGeneratorValueTest() {
        final String id = "FSSV.O11_G";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.targetP, id)), 480);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.targetQ, id)), 2.3523099422454834);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.minP, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.maxP, id)), 500);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.voltageRegulatorOn, id)), 1);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.targetV, id)), 406.45004272460938);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.disconnected, id)), 0);
    }

    @Test
    void getLoadValueTest() {
        final String id = "FSSV.O11_L";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.p0, id)), 480);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.q0, id)), 4.8000001907348633);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.fixedActivePower, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.variableActivePower, id)), 480);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.fixedReactivePower, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.variableReactivePower, id)), 4.8000001907348633);
    }

    @Test
    void getLoadDetailValueTest() {
        final String id = "FVALDI11_L";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.p0, id)), 470);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.q0, id)), 4.8000001907348633);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.fixedActivePower, id)), 400);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.variableActivePower, id)), 70);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.fixedReactivePower, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.variableReactivePower, id)), 0);
    }

    @Test
    void getHvdcLineValueTest() {
        final String id = "HVDC1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.activePowerSetpoint, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.maxP, id)), 1011);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.minP, id)), -1011);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.nominalV, id)), 320);
    }

    @Test
    void getSwitchValueTest() {
        final String id = "FSSV.O1_FSSV.O1_DJ_OMN";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.open, id)), 0);
    }

    @Test
    void getTwoWindingsTransformerValueTest() {
        final String id = "FP.AND1  FTDPRA1  1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.ratedU1, id)), 380);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.ratedU2, id)), 380);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.disconnected, id)), 0);
        // phaseTapChanger
        checkValue(key.getValue(new MappingKey(EquipmentVariable.phaseTapPosition, id)), 16);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.phaseRegulating, id)), 1);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.phaseRegulating, id)), 1);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.targetDeadband, id)), 0);
        // ratioTapChanger
        checkValue(key.getValue(new MappingKey(EquipmentVariable.ratioTapPosition, id)), 14);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.targetV, id)), Double.NaN);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.loadTapChangingCapabilities, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.ratioRegulating, id)), 0);
    }

    @Test
    void getLccConverterStationValueTest() {
        final String id = "FVALDI1_FVALDI1_HVDC1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.powerFactor, id)), 0.5);
    }

    @Test
    void getVscConverterStationValueTest() {
        final String id = "FSSV.O1_FSSV.O1_HVDC2";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.voltageRegulatorOn, id)), 0);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.voltageSetpoint, id)), 406);
        checkValue(key.getValue(new MappingKey(EquipmentVariable.reactivePowerSetpoint, id)), 0);
    }

    @Test
    void getLineValueTest() {
        final String id = "FSSV.O1  FP.AND1  1";
        checkValue(key.getValue(new MappingKey(EquipmentVariable.disconnected, id)), 0);
    }

    @Test
    void getWrongIdentifiableTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.targetP, "wrongId")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown identifiable wrongId");
    }

    @Test
    void getWrongEquipmentTypeTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.p0, "FSSV.O1_1")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown equipment type com.powsybl.iidm.network.impl.BusbarSectionImpl");
    }

    @Test
    void getWrongVariableGeneratorTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.p0, "FSSV.O11_G")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable p0 for generator FSSV.O11_G");
    }

    @Test
    void getWrongVariableLoadTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableHvdcLineTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableSwitchTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableTwoWindingTransformerTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableLccConverterStationTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableVscConverterStationTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }

    @Test
    void getWrongVariableLineTest() {
        assertThatThrownBy(() -> key.getValue(new MappingKey(EquipmentVariable.targetP, "FSSV.O11_L")))
                .isInstanceOf(TimeSeriesMappingException.class)
                .hasMessage("Unknown variable targetP for load FSSV.O11_L");
    }
}
