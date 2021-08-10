package com.powsybl.metrix.mapping.log;

public class ScalingDownLimitChangeSynthesis extends AbstractLogBuilder implements LogDescriptionBuilder {

    private String timeSeriesName;
    private String violatedVariable;
    private String variable;
    private String evolution;
    private String type = "";

    public ScalingDownLimitChangeSynthesis violatedVariable(String violatedVariable) {
        this.violatedVariable = violatedVariable;
        return this;
    }

    public ScalingDownLimitChangeSynthesis variable(String variable) {
        this.variable = variable;
        return this;
    }

    public ScalingDownLimitChangeSynthesis timeSeriesName(String timeSeriesName) {
        this.timeSeriesName = timeSeriesName;
        return this;
    }

    public ScalingDownLimitChangeSynthesis min() {
        this.evolution = " decreased";
        return this;
    }

    public ScalingDownLimitChangeSynthesis max() {
        this.evolution = " increased";
        return this;
    }

    public ScalingDownLimitChangeSynthesis mapped() {
        this.type = "mapped";
        return this;
    }

    public ScalingDownLimitChangeSynthesis baseCase() {
        this.type = "base case";
        return this;
    }

    public ScalingDownLimitChangeSynthesis buildLimitChange() {
        this.label = SCALING_DOWN_PROBLEM + "at least one " + violatedVariable + evolution + TS_SYNTHESIS;
        this.message = violatedVariable + " violated by " + variable + " in scaling down of at least one value of ts " + timeSeriesName +
                ", " + violatedVariable + " has been" + evolution + " for equipments";
        return this;
    }

    public ScalingDownLimitChangeSynthesis buildNotModified() {
        this.label = SCALING_DOWN_PROBLEM + type + " minP violated by mapped targetP" + TS_SYNTHESIS;
        this.message = "Impossible to scale down at least one value of ts " + timeSeriesName +
                ", but aimed targetP of equipments have been applied";
        return this;
    }
}
