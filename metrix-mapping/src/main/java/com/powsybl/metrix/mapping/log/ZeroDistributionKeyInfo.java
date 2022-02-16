package com.powsybl.metrix.mapping.log;

import java.util.List;
import java.util.Objects;

public class ZeroDistributionKeyInfo implements LogDescriptionBuilder {

    private final String timeSeriesName;

    private final double timeSeriesValue;

    private final List<String> equipmentIds;

    public ZeroDistributionKeyInfo(String timeSeriesName, double timeSeriesValue,
                                   List<String> equipmentIds) {
        this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
        this.timeSeriesValue = timeSeriesValue;
        this.equipmentIds = Objects.requireNonNull(equipmentIds);
    }

    public LogContent build() {
        LogContent log = new LogContent();
        log.label = "zero distribution key warning";
        log.message = String.format("Distribution key are all equal to zero in scaling down %s of ts %s on equipments %s -> uniform distribution",
                formatDouble(timeSeriesValue), timeSeriesName, equipmentIds);
        return log;
    }
}
