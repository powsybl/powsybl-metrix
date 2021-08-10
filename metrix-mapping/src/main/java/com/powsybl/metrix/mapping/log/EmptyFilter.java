package com.powsybl.metrix.mapping.log;

public class EmptyFilter extends AbstractLogBuilder implements LogDescriptionBuilder {

    private String timeSeriesName;

    private double timeSeriesValue;

    public EmptyFilter timeSeriesName(String timeSeriesName) {
        this.timeSeriesName = timeSeriesName;
        return this;
    }

    public EmptyFilter timeSeriesValue(double timeSeriesValue) {
        this.timeSeriesValue = timeSeriesValue;
        return this;
    }

    public EmptyFilter build() {
        this.message = "Impossible to scale down " + formatDouble(timeSeriesValue) + " of ts '" + timeSeriesName +
                " to empty equipment list";
        this.label = "empty filter error";
        return this;
    }
}
