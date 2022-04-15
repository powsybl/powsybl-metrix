/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.metrix.mapping.log;

import static com.powsybl.metrix.mapping.log.RangeLogWithVariableChanged.BC_RANGE_PROBLEM;
import static com.powsybl.metrix.mapping.log.RangeLogWithVariableChanged.MAPPING_RANGE_PROBLEM;

public class RangeWithMinPViolatedByTargetP implements LogDescriptionBuilder {

    private String notIncludedVariable;
    private String id;
    private String minValue;
    private String maxValue;
    private String value;
    private String type;
    private String problem;

    public RangeWithMinPViolatedByTargetP mapped() {
        this.type = "mapped";
        this.problem = MAPPING_RANGE_PROBLEM;
        return this;
    }

    public RangeWithMinPViolatedByTargetP baseCase() {
        this.type = "base case";
        this.problem = BC_RANGE_PROBLEM;
        return this;
    }

    public RangeWithMinPViolatedByTargetP notIncludedVariable(String notIncludedVariable) {
        this.notIncludedVariable = notIncludedVariable;
        return this;
    }

    public RangeWithMinPViolatedByTargetP id(String id) {
        this.id = id;
        return this;
    }

    public RangeWithMinPViolatedByTargetP minValue(double minValue) {
        this.minValue = formatDouble(minValue);
        return this;
    }

    public RangeWithMinPViolatedByTargetP maxValue(double maxValue) {
        this.maxValue = formatDouble(maxValue);
        return this;
    }

    public RangeWithMinPViolatedByTargetP value(double value) {
        this.value = formatDouble(value);
        return this;
    }

    public LogContent build() {
        LogContent log = new LogContent();
        log.message = String.format("%s %s of %s not included in %s to %s, but %s has not been changed",
                notIncludedVariable, value, id, minValue, maxValue, notIncludedVariable);
        log.label = String.format("%s%s minP violated by %s targetP", problem, type, type);
        return log;
    }
}
