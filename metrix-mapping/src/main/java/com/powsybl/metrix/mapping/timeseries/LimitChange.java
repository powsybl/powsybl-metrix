package com.powsybl.metrix.mapping.timeseries;

public class LimitChange {
    private final double baseCaseLimit;

    private double limit;

    private int baseCaseLimitNbOfViolation = 0;

    public LimitChange(double baseCaseLimit, double limit) {
        this.baseCaseLimit = baseCaseLimit;
        this.limit = limit;
    }

    public double getBaseCaseLimit() {
        return baseCaseLimit;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public int getBaseCaseLimitNbOfViolation() {
        return baseCaseLimitNbOfViolation;
    }

    public void setBaseCaseLimitNbOfViolation(int baseCaseLimitNbOfViolation) {
        this.baseCaseLimitNbOfViolation = baseCaseLimitNbOfViolation;
    }
}
