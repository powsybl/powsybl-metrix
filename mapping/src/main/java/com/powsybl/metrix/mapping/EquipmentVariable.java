/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public enum EquipmentVariable implements MappingVariable {
    targetP("targetP"),
    targetQ("targetQ"),
    minP("minP"),
    maxP("maxP"),
    p0("p0"),
    q0("q0"),
    fixedActivePower("fixedActivePower"),
    variableActivePower("variableActivePower"),
    fixedReactivePower("fixedReactivePower"),
    variableReactivePower("variableReactivePower"),
    activePowerSetpoint("activePowerSetpoint"),
    open("open"),
    currentTap("currentTap");

    private static final String NAME = "equipment";

    static String getName() {
        return NAME;
    }

    @Override
    public String getFieldName() {
        return getName();
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

    static MappingVariable parseJson(JsonParser parser) throws IOException {
        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            if (token == JsonToken.VALUE_STRING) {
                return EquipmentVariable.valueOf(parser.getValueAsString());
            } else {
                throw new IllegalStateException("Unexpected JSON token: " + token);
            }
        }
        throw new IllegalStateException("Invalid EquipmentVariable JSON");
    }

    private static final Set<EquipmentVariable> GENERATOR_VARIABLES = EnumSet.of(EquipmentVariable.targetP,
                                                                                 EquipmentVariable.targetQ,
                                                                                 EquipmentVariable.minP,
                                                                                 EquipmentVariable.maxP);
    private static final Set<EquipmentVariable> HVDC_LINE_VARIABLES = EnumSet.of(EquipmentVariable.activePowerSetpoint,
                                                                                 EquipmentVariable.minP,
                                                                                 EquipmentVariable.maxP);
    private static final Set<EquipmentVariable> LOAD_VARIABLES = EnumSet.of(EquipmentVariable.p0,
                                                                            EquipmentVariable.q0,
                                                                            EquipmentVariable.fixedActivePower,
                                                                            EquipmentVariable.variableActivePower,
                                                                            EquipmentVariable.fixedReactivePower,
                                                                            EquipmentVariable.variableReactivePower);
    private static final Set<EquipmentVariable> PST_VARIABLES = EnumSet.of(EquipmentVariable.currentTap);

    private final String variable;

    EquipmentVariable(String variable) {
        this.variable = variable;
    }

    @Override
    public String getVariableName() {
        return variable;
    }

    public static EquipmentVariable getByDefaultVariable(MappableEquipmentType equipmentType) {
        switch (equipmentType) {
            case GENERATOR:
                return EquipmentVariable.targetP;
            case HVDC_LINE:
                return EquipmentVariable.activePowerSetpoint;
            case LOAD:
            case BOUNDARY_LINE:
                return EquipmentVariable.p0;
            case SWITCH:
                return EquipmentVariable.open;
            case PST:
                return EquipmentVariable.currentTap;
            default:
                throw new AssertionError("Unsupported equipment type " + equipmentType);
        }
    }

    public static Set<EquipmentVariable> getByDefaultVariables(MappableEquipmentType equipmentType) {
        Set<EquipmentVariable> equipmentVariables = new HashSet<>();
        equipmentVariables.add(getByDefaultVariable(equipmentType));
        if (equipmentType == MappableEquipmentType.LOAD) {
            equipmentVariables.add(EquipmentVariable.fixedActivePower);
            equipmentVariables.add(EquipmentVariable.variableActivePower);
        }
        return equipmentVariables;
    }

    public static void checkVariableCompatibility(MappableEquipmentType equipmentType, EquipmentVariable equipmentVariable) {
        Objects.requireNonNull(equipmentType);
        boolean compatible;
        switch (equipmentType) {
            case GENERATOR:
                compatible = GENERATOR_VARIABLES.contains(equipmentVariable);
                break;
            case HVDC_LINE:
                compatible = HVDC_LINE_VARIABLES.contains(equipmentVariable);
                break;
            case LOAD:
                compatible = LOAD_VARIABLES.contains(equipmentVariable);
                break;
            case BOUNDARY_LINE:
                compatible = equipmentVariable == EquipmentVariable.p0;
                break;
            case SWITCH:
                compatible = equipmentVariable == EquipmentVariable.open;
                break;
            case PST:
                compatible = equipmentVariable == EquipmentVariable.currentTap;
                break;
            default:
                throw new AssertionError("Unsupported equipment type " + equipmentType);
        }
        if (!compatible) {
            throw new AssertionError("Variable type " + equipmentVariable + " not compatible with equipment type " + equipmentType);
        }
    }

    public static EquipmentVariable check(MappableEquipmentType equipmentType, EquipmentVariable equipmentVariable) {
        Objects.requireNonNull(equipmentType);
        if (equipmentVariable == null) {
            return getByDefaultVariable(equipmentType);
        } else {
            Set<EquipmentVariable> equipmentVariables = check(equipmentType, ImmutableSet.of(equipmentVariable));
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
