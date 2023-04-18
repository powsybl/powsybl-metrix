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
    phaseTapPosition("phaseTapPosition"),
    ratioTapPosition("ratioTapPosition"),
    voltageRegulatorOn("voltageRegulatorOn"),
    targetV("targetV"),
    nominalV("nominalV"),
    regulationMode("regulationMode"),
    ratedU1("ratedU1"),
    ratedU2("ratedU2"),
    loadTapChangingCapabilities("loadTapChangingCapabilities"),
    phaseRegulating("phaseRegulating"),
    ratioRegulating("ratioRegulating"),
    voltageSetpoint("voltageSetpoint"),
    reactivePowerSetpoint("reactivePowerSetpoint"),
    powerFactor("powerFactor"),
    disconnected("disconnected"),
    targetDeadband("targetDeadband");

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
            generator.writeString(variable.name());
            generator.writeEndObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static MappingVariable parseJson(JsonParser parser) throws IOException {
        JsonToken token = parser.nextToken();
        if (token != null) {
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
                                                                                 EquipmentVariable.maxP,
                                                                                 EquipmentVariable.voltageRegulatorOn,
                                                                                 EquipmentVariable.targetV,
                                                                                 EquipmentVariable.disconnected);
    private static final Set<EquipmentVariable> HVDC_LINE_VARIABLES = EnumSet.of(EquipmentVariable.activePowerSetpoint,
                                                                                 EquipmentVariable.minP,
                                                                                 EquipmentVariable.maxP,
                                                                                 EquipmentVariable.nominalV);
    private static final Set<EquipmentVariable> LOAD_VARIABLES = EnumSet.of(EquipmentVariable.p0,
                                                                            EquipmentVariable.q0,
                                                                            EquipmentVariable.fixedActivePower,
                                                                            EquipmentVariable.variableActivePower,
                                                                            EquipmentVariable.fixedReactivePower,
                                                                            EquipmentVariable.variableReactivePower);
    private static final Set<EquipmentVariable> PHASE_TAP_CHANGER_VARIABLES = EnumSet.of(EquipmentVariable.phaseTapPosition,
                                                                                         EquipmentVariable.phaseRegulating,
                                                                                         EquipmentVariable.regulationMode,
                                                                                         EquipmentVariable.targetDeadband);
    private static final Set<EquipmentVariable> TWO_WINDINGS_TRANSFORMER_VARIABLES = EnumSet.of(EquipmentVariable.ratedU1,
                                                                                                EquipmentVariable.ratedU2,
                                                                                                EquipmentVariable.disconnected);
    private static final Set<EquipmentVariable> RATIO_TAP_CHANGER_VARIABLES = EnumSet.of(EquipmentVariable.ratioTapPosition,
                                                                                         EquipmentVariable.loadTapChangingCapabilities,
                                                                                         EquipmentVariable.ratioRegulating,
                                                                                         EquipmentVariable.targetV);
    private static final Set<EquipmentVariable> LCC_CONVERTER_VARIABLES = EnumSet.of(EquipmentVariable.powerFactor);
    private static final Set<EquipmentVariable> VSC_CONVERTER_VARIABLES = EnumSet.of(EquipmentVariable.voltageRegulatorOn,
                                                                                     EquipmentVariable.voltageSetpoint,
                                                                                     EquipmentVariable.reactivePowerSetpoint);

    private static final Set<EquipmentVariable> LINE_VARIABLES = EnumSet.of(EquipmentVariable.disconnected);

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
            case PHASE_TAP_CHANGER:
                return EquipmentVariable.phaseTapPosition;
            case RATIO_TAP_CHANGER:
                return EquipmentVariable.ratioTapPosition;
            case LCC_CONVERTER_STATION:
                return EquipmentVariable.powerFactor;
            case VSC_CONVERTER_STATION:
                return EquipmentVariable.voltageSetpoint;
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

    public static boolean isVariableCompatible(MappableEquipmentType equipmentType, EquipmentVariable equipmentVariable) {
        switch (equipmentType) {
            case GENERATOR:
                return GENERATOR_VARIABLES.contains(equipmentVariable);
            case HVDC_LINE:
                return HVDC_LINE_VARIABLES.contains(equipmentVariable);
            case LOAD:
                return LOAD_VARIABLES.contains(equipmentVariable);
            case BOUNDARY_LINE:
                return equipmentVariable == EquipmentVariable.p0;
            case SWITCH:
                return equipmentVariable == EquipmentVariable.open;
            case PHASE_TAP_CHANGER:
            case PST:
                return PHASE_TAP_CHANGER_VARIABLES.contains(equipmentVariable);
            case TRANSFORMER:
                return TWO_WINDINGS_TRANSFORMER_VARIABLES.contains(equipmentVariable);
            case RATIO_TAP_CHANGER:
                return RATIO_TAP_CHANGER_VARIABLES.contains(equipmentVariable);
            case LCC_CONVERTER_STATION:
                return LCC_CONVERTER_VARIABLES.contains(equipmentVariable);
            case VSC_CONVERTER_STATION:
                return VSC_CONVERTER_VARIABLES.contains(equipmentVariable);
            case LINE:
                return LINE_VARIABLES.contains(equipmentVariable);
            default:
                throw new AssertionError("Unsupported equipment type " + equipmentType);
        }
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
