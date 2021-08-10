package com.powsybl.metrix.mapping.log;

import java.util.Objects;

public class LimitLogBuilder extends AbstractLogBuilder implements LogDescriptionBuilder {

    private String id;
    private String variableToChange;
    private String variable;
    private int nbViolation;
    private double oldValue;
    private double newValue;
    private String comparision;
    private String evolution;

    private static final String LIMIT_CHANGE = "limit change" + LABEL_SEPARATOR;

    public LimitLogBuilder id(String id) {
        this.id = Objects.requireNonNull(id);
        return this;
    }

    public LimitLogBuilder variable(String variable) {
        this.variable = Objects.requireNonNull(variable);
        return this;
    }

    public LimitLogBuilder nbViolation(int nbViolation) {
        this.nbViolation = nbViolation;
        return this;
    }

    public LimitLogBuilder variableToChange(String variableToChange) {
        this.variableToChange = Objects.requireNonNull(variableToChange);
        return this;
    }

    public LimitLogBuilder oldValue(double oldValue) {
        this.oldValue = oldValue;
        return this;
    }

    public LimitLogBuilder newValue(double newValue) {
        this.newValue = newValue;
        return this;
    }

    public LimitLogBuilder build() {
        this.label = LIMIT_CHANGE + variableToChange;
        this.message = variableToChange + " of " + id + comparision + variable + " for " + nbViolation +
                " variants, " + variableToChange + evolution + formatDouble(oldValue) + " to " + formatDouble(newValue);
        return this;
    }

    public LimitLogBuilder isMin() {
        this.comparision = " higher than ";
        this.evolution = " decreased from ";
        return this;
    }

    public LimitLogBuilder isMax() {
        this.comparision = " lower than ";
        this.evolution = " increased from ";
        return this;
    }
}
