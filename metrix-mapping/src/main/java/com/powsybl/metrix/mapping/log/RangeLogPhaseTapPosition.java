package com.powsybl.metrix.mapping.log;

import com.powsybl.metrix.mapping.EquipmentVariable;

public class RangeLogPhaseTapPosition implements LogDescriptionBuilder {
    private String id;
    private String value;
    private String minValue;
    private String maxValue;
    private String newValue;
    private String toVariable;

    private static final String PHASE_TAP_POSTION_VARIABLE_NAME = EquipmentVariable.phaseTapPosition.getVariableName();
    private static final String MAPPING_RANGE_PROBLEM = "mapping range problem" + LABEL_SEPARATOR;

    public String getLabel() {
        return String.format("%s%s changed to %s", MAPPING_RANGE_PROBLEM, PHASE_TAP_POSTION_VARIABLE_NAME,
                toVariable);
    }

    public RangeLogPhaseTapPosition id(String id) {
        this.id = id;
        return this;
    }

    public RangeLogPhaseTapPosition newValue(double newValue) {
        this.newValue = formatDouble(newValue);
        return this;
    }

    public RangeLogPhaseTapPosition value(double value) {
        this.value = formatDouble(value);
        return this;
    }

    public RangeLogPhaseTapPosition minValue(double minValue) {
        this.minValue = formatDouble(minValue);
        return this;
    }

    public RangeLogPhaseTapPosition maxValue(double maxValue) {
        this.maxValue = formatDouble(maxValue);
        return this;
    }

    public RangeLogPhaseTapPosition toVariable(String toVariable) {
        this.toVariable = toVariable;
        return this;
    }

    public LogContent build() {
        LogContent log = new LogContent();
        log.label = getLabel();
        log.message = String.format("%s %s of %s not included in %s to %s, %s changed to %s", PHASE_TAP_POSTION_VARIABLE_NAME,
                value, id, minValue, maxValue, PHASE_TAP_POSTION_VARIABLE_NAME, newValue);
        return log;
    }
}
