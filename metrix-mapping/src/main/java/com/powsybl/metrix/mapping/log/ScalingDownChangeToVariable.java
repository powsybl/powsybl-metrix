package com.powsybl.metrix.mapping.log;

public class ScalingDownChangeToVariable implements LogDescriptionBuilder {

    private String toVariable;
    private String type = "";
    private boolean disabled = false;
    private boolean synthesis = false;
    private String changedVariable;
    private String timeSeriesName;
    private String timeSeriesValue = "at least one value";
    private String sum = "";

    public ScalingDownChangeToVariable changedVariable(String changedVariable) {
        this.changedVariable = changedVariable;
        return this;
    }

    public ScalingDownChangeToVariable timeSeriesName(String timeSeriesName) {
        this.timeSeriesName = timeSeriesName;
        return this;
    }

    public ScalingDownChangeToVariable timeSeriesValue(double timeSeriesValue) {
        this.timeSeriesValue = formatDouble(timeSeriesValue);
        return this;
    }

    public ScalingDownChangeToVariable sum(double sum) {
        this.sum = " " + formatDouble(sum);
        return this;
    }

    public ScalingDownChangeToVariable toVariable(String toVariable) {
        this.toVariable = toVariable;
        return this;
    }

    public ScalingDownChangeToVariable mapped() {
        this.type = "mapped ";
        return this;
    }

    public ScalingDownChangeToVariable basecase() {
        this.type = "base case ";
        return this;
    }

    public ScalingDownChangeToVariable disabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public ScalingDownChangeToVariable synthesis(boolean synthesis) {
        this.synthesis = synthesis;
        return this;
    }

    public LogContent build() {
        LogContent log = new LogContent();
        log.label = String.format("%sat least one %s changed to %s%s%s%s", SCALING_DOWN_PROBLEM, changedVariable,
                type, toVariable, disabled ? IGNORE_LIMITS_DISABLED : "", synthesis ? TS_SYNTHESIS : "");
        log.message = String.format("Impossible to scale down %s of ts %s%s%s%s has been applied", timeSeriesValue, timeSeriesName,
                synthesis ? ", modified " : ", ", changedVariable, sum);
        return log;
    }
}
