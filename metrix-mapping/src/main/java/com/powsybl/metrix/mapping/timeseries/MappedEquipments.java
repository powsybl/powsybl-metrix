/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.iidm.network.Identifiable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MappedEquipments {
    private final double timeSeriesValue;

    private final Set<Identifiable<?>> identifiables;

    private final Set<ScalingDownPowerChange> scalingDownPowerChange = new HashSet<>();

    private final Set<ScalingDownLimitViolation> scalingDownLimitViolation = new HashSet<>();

    public MappedEquipments(double timeSeriesValue, Set<Identifiable<?>> identifiables) {
        this.timeSeriesValue = timeSeriesValue;
        this.identifiables = Objects.requireNonNull(identifiables);
    }

    public double getTimeSeriesValue() {
        return timeSeriesValue;
    }

    public Set<Identifiable<?>> getIdentifiables() {
        return identifiables;
    }

    public Set<ScalingDownPowerChange> getScalingDownPowerChange() {
        return scalingDownPowerChange;
    }

    public Set<ScalingDownLimitViolation> getScalingDownLimitViolation() {
        return scalingDownLimitViolation;
    }
}
