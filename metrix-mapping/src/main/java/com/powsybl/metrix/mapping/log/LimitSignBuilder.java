package com.powsybl.metrix.mapping.log;

import java.util.Objects;

public class LimitSignBuilder extends AbstractLogBuilder implements LogDescriptionBuilder {

    private String timeSeriesName;

    private String variable;

    private double timeSeriesValue;

    private String sign;

    private static final String MAPPING_SIGN_PROBLEM = "mapping sign problem" + LABEL_SEPARATOR;

    public LimitSignBuilder timeSeriesName(String timeSeriesName) {
        this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
        return this;
    }

    public LimitSignBuilder variable(String variable) {
        this.variable = Objects.requireNonNull(variable);
        return this;
    }

    public LimitSignBuilder timeSeriesValue(double timeSeriesValue) {
        this.timeSeriesValue = timeSeriesValue;
        return this;
    }

    public LimitSignBuilder min() {
        this.sign = " positive ";
        return this;
    }

    public LimitSignBuilder max() {
        this.sign = " negative ";
        return this;
    }

    public LimitSignBuilder build() {
        this.label = MAPPING_SIGN_PROBLEM;
        this.message = "Impossible to map " + variable + " " + formatDouble(timeSeriesValue) + " of ts " + timeSeriesName + sign + variable + " value";
        return this;
    }
}
