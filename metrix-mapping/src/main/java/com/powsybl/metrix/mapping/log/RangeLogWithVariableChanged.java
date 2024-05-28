/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public class RangeLogWithVariableChanged implements LogDescriptionBuilder {

    private String oldValue;
    private String toVariable;
    private String newValue;
    private boolean disabled = false;
    private String problemDescription;
    private String actionDescription;
    private String notIncludedVariable;
    private String value;
    private String id;
    private String minValue;
    private String maxValue;

    protected static final String MAPPING_RANGE_PROBLEM = "mapping range problem" + LABEL_SEPARATOR;

    protected static final String BC_RANGE_PROBLEM = "base case range problem" + LABEL_SEPARATOR;

    public RangeLogWithVariableChanged oldValue(String oldValue) {
        this.oldValue = oldValue;
        return this;
    }

    public RangeLogWithVariableChanged newValue(double newValue) {
        this.newValue = formatDouble(newValue);
        return this;
    }

    public RangeLogWithVariableChanged toVariable(String toVariable) {
        this.toVariable = toVariable;
        return this;
    }

    public RangeLogWithVariableChanged disabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public String getLabel(String problemDescription, String actionDescription) {
        return String.format("%s%s changed to %s %s", problemDescription, oldValue,
                toVariable.isEmpty() ? "0" : actionDescription, toVariable);
    }

    public RangeLogWithVariableChanged isMapping() {
        this.problemDescription = MAPPING_RANGE_PROBLEM;
        this.actionDescription = "mapped ";
        return this;
    }

    public RangeLogWithVariableChanged isBaseCase() {
        this.problemDescription = BC_RANGE_PROBLEM;
        this.actionDescription = "base case ";
        return this;
    }

    public RangeLogWithVariableChanged notIncludedVariable(String notIncludedVariable) {
        this.notIncludedVariable = notIncludedVariable;
        return this;
    }

    public RangeLogWithVariableChanged id(String id) {
        this.id = id;
        return this;
    }

    public RangeLogWithVariableChanged minValue(double minValue) {
        this.minValue = formatDouble(minValue);
        return this;
    }

    public RangeLogWithVariableChanged maxValue(double maxValue) {
        this.maxValue = formatDouble(maxValue);
        return this;
    }

    public RangeLogWithVariableChanged value(double value) {
        this.value = formatDouble(value);
        return this;
    }

    public LogContent build() {
        LogContent log = new LogContent();
        log.label = String.format("%s%s changed to %s%s%s", problemDescription, oldValue, toVariable.isEmpty() ? "0" : actionDescription,
                toVariable, disabled ? IGNORE_LIMITS_DISABLED : "");
        log.message = String.format("%s %s of %s not included in %s to %s, %s changed to %s", notIncludedVariable,
                value, id, minValue, maxValue, oldValue, newValue);
        return log;
    }
}
