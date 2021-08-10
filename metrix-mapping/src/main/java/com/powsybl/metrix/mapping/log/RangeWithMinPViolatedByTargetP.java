package com.powsybl.metrix.mapping.log;

import static com.powsybl.metrix.mapping.log.RangeLogWithVariableChanged.BC_RANGE_PROBLEM;
import static com.powsybl.metrix.mapping.log.RangeLogWithVariableChanged.MAPPING_RANGE_PROBLEM;

public class RangeWithMinPViolatedByTargetP extends AbstractLogBuilder implements LogDescriptionBuilder {

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

    public RangeWithMinPViolatedByTargetP build() {
        this.message = notIncludedVariable + " " + value + " of " + id + " not included in " + minValue
                + " to " + maxValue + ", but " + notIncludedVariable + " has not been changed";
        this.label = problem + type + " minP violated by " + type + " targetP";
        return this;
    }
}
