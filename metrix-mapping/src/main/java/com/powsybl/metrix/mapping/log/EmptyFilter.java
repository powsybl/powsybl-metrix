/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.metrix.mapping.log;

public class EmptyFilter implements LogDescriptionBuilder {

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

    public LogContent build() {
        LogContent log = new LogContent();
        log.message = String.format("Impossible to scale down %s of ts %s to empty equipment list",
                formatDouble(timeSeriesValue), timeSeriesName);
        log.label = "empty filter error";
        return log;
    }
}
