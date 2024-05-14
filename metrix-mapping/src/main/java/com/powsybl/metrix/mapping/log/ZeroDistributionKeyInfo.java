/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log;

import java.util.List;
import java.util.Objects;

/**
 * @author berthault {@literal <valentinberthault at outlook.fr>}
 */
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
