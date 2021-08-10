package com.powsybl.metrix.mapping.log;

import java.util.List;
import java.util.Objects;

public class ZeroDistributionKeyInfo extends AbstractLogBuilder implements LogDescriptionBuilder {

    private final String timeSeriesName;

    private final double timeSeriesValue;

    private final List<String> equipmentIds;

    public ZeroDistributionKeyInfo(String timeSeriesName, double timeSeriesValue,
                                   List<String> equipmentIds) {
        this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
        this.timeSeriesValue = timeSeriesValue;
        this.equipmentIds = Objects.requireNonNull(equipmentIds);
    }

    public ZeroDistributionKeyInfo build() {
        this.label = "zero distribution key warning";
        this.message = "Distribution key are all equal to zero in scaling down " + formatDouble(timeSeriesValue) + " of ts '" + timeSeriesName +
                " on equipments " + equipmentIds + " -> uniform distribution";
        return this;
    }
}
