/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public enum EquipmentVariable implements MappingVariable {
    TARGET_P("targetP"),
    TARGET_Q("targetQ"),
    MIN_P("minP"),
    MAX_P("maxP"),
    P0("p0"),
    Q0("q0"),
    FIXED_ACTIVE_POWER("fixedActivePower"),
    VARIABLE_ACTIVE_POWER("variableActivePower"),
    FIXED_REACTIVE_POWER("fixedReactivePower"),
    VARIABLE_REACTIVE_POWER("variableReactivePower"),
    ACTIVE_POWER_SETPOINT("activePowerSetpoint"),
    OPEN("open"),
    PHASE_TAP_POSITION("phaseTapPosition"),
    RATIO_TAP_POSITION("ratioTapPosition"),
    VOLTAGE_REGULATOR_ON("voltageRegulatorOn"),
    TARGET_V("targetV"),
    NOMINAL_V("nominalV"),
    REGULATION_MODE("regulationMode"),
    RATED_U1("ratedU1"),
    RATED_U2("ratedU2"),
    LOAD_TAP_CHANGING_CAPABILITIES("loadTapChangingCapabilities"),
    PHASE_REGULATING("phaseRegulating"),
    RATIO_REGULATING("ratioRegulating"),
    VOLTAGE_SETPOINT("voltageSetpoint"),
    REACTIVE_POWER_SETPOINT("reactivePowerSetpoint"),
    POWER_FACTOR("powerFactor"),
    DISCONNECTED("disconnected"),
    TARGET_DEADBAND("targetDeadband");

    protected static final String NAME = "equipment";

    private static final Map<String, EquipmentVariable> NAME_TO_VARIABLE = Arrays.stream(EquipmentVariable.values()).collect(Collectors.toMap(EquipmentVariable::toString, Function.identity()));

    @Override
    public String getFieldName() {
        return NAME;
    }

    static void writeJson(EquipmentVariable variable, JsonGenerator generator) throws IOException {
        Objects.requireNonNull(generator);
        try {
            generator.writeStartObject();
            generator.writeFieldName(variable.getFieldName());
            generator.writeString(variable.getVariableName());
            generator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return getVariableName();
    }

    static MappingVariable parseJson(JsonParser parser) throws IOException {
        JsonToken token = parser.nextToken();
        if (token != null) {
            if (token == JsonToken.VALUE_STRING) {
                return EquipmentVariable.fromString(parser.getValueAsString());
            } else {
                throw new IllegalStateException("Unexpected JSON token: " + token);
            }
        }
        throw new IllegalStateException("Invalid EquipmentVariable JSON");
    }

    private static final Set<EquipmentVariable> GENERATOR_VARIABLES = EnumSet.of(EquipmentVariable.TARGET_P,
                                                                                 EquipmentVariable.TARGET_Q,
                                                                                 EquipmentVariable.MIN_P,
                                                                                 EquipmentVariable.MAX_P,
                                                                                 EquipmentVariable.VOLTAGE_REGULATOR_ON,
                                                                                 EquipmentVariable.TARGET_V,
                                                                                 EquipmentVariable.DISCONNECTED);
    private static final Set<EquipmentVariable> HVDC_LINE_VARIABLES = EnumSet.of(EquipmentVariable.ACTIVE_POWER_SETPOINT,
                                                                                 EquipmentVariable.MIN_P,
                                                                                 EquipmentVariable.MAX_P,
                                                                                 EquipmentVariable.NOMINAL_V);
    private static final Set<EquipmentVariable> LOAD_VARIABLES = EnumSet.of(EquipmentVariable.P0,
                                                                            EquipmentVariable.Q0,
                                                                            EquipmentVariable.FIXED_ACTIVE_POWER,
                                                                            EquipmentVariable.VARIABLE_ACTIVE_POWER,
                                                                            EquipmentVariable.FIXED_REACTIVE_POWER,
                                                                            EquipmentVariable.VARIABLE_REACTIVE_POWER);
    private static final Set<EquipmentVariable> PHASE_TAP_CHANGER_VARIABLES = EnumSet.of(EquipmentVariable.PHASE_TAP_POSITION,
                                                                                         EquipmentVariable.PHASE_REGULATING,
                                                                                         EquipmentVariable.REGULATION_MODE,
                                                                                         EquipmentVariable.TARGET_DEADBAND);
    private static final Set<EquipmentVariable> TWO_WINDINGS_TRANSFORMER_VARIABLES = EnumSet.of(EquipmentVariable.RATED_U1,
                                                                                                EquipmentVariable.RATED_U2,
                                                                                                EquipmentVariable.DISCONNECTED);
    private static final Set<EquipmentVariable> RATIO_TAP_CHANGER_VARIABLES = EnumSet.of(EquipmentVariable.RATIO_TAP_POSITION,
                                                                                         EquipmentVariable.LOAD_TAP_CHANGING_CAPABILITIES,
                                                                                         EquipmentVariable.RATIO_REGULATING,
                                                                                         EquipmentVariable.TARGET_V);
    private static final Set<EquipmentVariable> LCC_CONVERTER_VARIABLES = EnumSet.of(EquipmentVariable.POWER_FACTOR);
    private static final Set<EquipmentVariable> VSC_CONVERTER_VARIABLES = EnumSet.of(EquipmentVariable.VOLTAGE_REGULATOR_ON,
                                                                                     EquipmentVariable.VOLTAGE_SETPOINT,
                                                                                     EquipmentVariable.REACTIVE_POWER_SETPOINT);

    private static final Set<EquipmentVariable> LINE_VARIABLES = EnumSet.of(EquipmentVariable.DISCONNECTED);

    private final String variable;

    EquipmentVariable(String variable) {
        this.variable = variable;
    }

    @Override
    public String getVariableName() {
        return variable;
    }

    public static EquipmentVariable fromString(String variable) {
        return NAME_TO_VARIABLE.get(variable);
    }

    public static EquipmentVariable getByDefaultVariable(MappableEquipmentType equipmentType) {
        return switch (equipmentType) {
            case GENERATOR -> EquipmentVariable.TARGET_P;
            case HVDC_LINE -> EquipmentVariable.ACTIVE_POWER_SETPOINT;
            case LOAD, BOUNDARY_LINE -> EquipmentVariable.P0;
            case SWITCH -> EquipmentVariable.OPEN;
            case PHASE_TAP_CHANGER -> EquipmentVariable.PHASE_TAP_POSITION;
            case RATIO_TAP_CHANGER -> EquipmentVariable.RATIO_TAP_POSITION;
            case LCC_CONVERTER_STATION -> EquipmentVariable.POWER_FACTOR;
            case VSC_CONVERTER_STATION -> EquipmentVariable.VOLTAGE_SETPOINT;
            case TRANSFORMER, LINE -> EquipmentVariable.DISCONNECTED;
        };
    }

    public static Set<EquipmentVariable> getByDefaultVariables(MappableEquipmentType equipmentType) {
        Set<EquipmentVariable> equipmentVariables = new HashSet<>();
        equipmentVariables.add(getByDefaultVariable(equipmentType));
        if (equipmentType == MappableEquipmentType.LOAD) {
            equipmentVariables.add(EquipmentVariable.FIXED_ACTIVE_POWER);
            equipmentVariables.add(EquipmentVariable.VARIABLE_ACTIVE_POWER);
        }
        return equipmentVariables;
    }

    public static boolean isVariableCompatible(MappableEquipmentType equipmentType, EquipmentVariable equipmentVariable) {
        return switch (equipmentType) {
            case GENERATOR -> GENERATOR_VARIABLES.contains(equipmentVariable);
            case HVDC_LINE -> HVDC_LINE_VARIABLES.contains(equipmentVariable);
            case LOAD -> LOAD_VARIABLES.contains(equipmentVariable);
            case BOUNDARY_LINE -> equipmentVariable == EquipmentVariable.P0;
            case SWITCH -> equipmentVariable == EquipmentVariable.OPEN;
            case PHASE_TAP_CHANGER -> PHASE_TAP_CHANGER_VARIABLES.contains(equipmentVariable);
            case TRANSFORMER -> TWO_WINDINGS_TRANSFORMER_VARIABLES.contains(equipmentVariable);
            case RATIO_TAP_CHANGER -> RATIO_TAP_CHANGER_VARIABLES.contains(equipmentVariable);
            case LCC_CONVERTER_STATION -> LCC_CONVERTER_VARIABLES.contains(equipmentVariable);
            case VSC_CONVERTER_STATION -> VSC_CONVERTER_VARIABLES.contains(equipmentVariable);
            case LINE -> LINE_VARIABLES.contains(equipmentVariable);
        };
    }

    public static void checkVariableCompatibility(MappableEquipmentType equipmentType, EquipmentVariable equipmentVariable) {
        Objects.requireNonNull(equipmentType);
        Objects.requireNonNull(equipmentVariable);
        if (!isVariableCompatible(equipmentType, equipmentVariable)) {
            throw new AssertionError("Variable type " + equipmentVariable + " not compatible with equipment type " + equipmentType);
        }
    }

    public static EquipmentVariable check(MappableEquipmentType equipmentType, EquipmentVariable equipmentVariable) {
        Objects.requireNonNull(equipmentType);
        if (equipmentVariable == null) {
            return getByDefaultVariable(equipmentType);
        } else {
            Set<EquipmentVariable> equipmentVariables = check(equipmentType, Set.of(equipmentVariable));
            return equipmentVariables.iterator().next();
        }
    }

    public static Set<EquipmentVariable> check(MappableEquipmentType equipmentType, Set<EquipmentVariable> equipmentVariablesToCheck) {
        Objects.requireNonNull(equipmentType);
        if (equipmentVariablesToCheck == null || equipmentVariablesToCheck.isEmpty()) {
            return getByDefaultVariables(equipmentType);
        } else {
            Set<EquipmentVariable> equipmentVariables = new HashSet<>();
            for (EquipmentVariable equipmentVariable : equipmentVariablesToCheck) {
                checkVariableCompatibility(equipmentType, equipmentVariable);
                equipmentVariables.add(equipmentVariable);
            }
            return equipmentVariables;
        }
    }
}
