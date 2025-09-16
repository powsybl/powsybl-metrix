/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.observer;

import com.google.common.collect.Range;
import com.powsybl.metrix.commons.observer.DefaultTimeSeriesMapperObserver;
import com.powsybl.timeseries.TimeSeriesIndex;

import java.util.Map;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class DefaultEquipmentTimeSeriesMapperObserver extends DefaultTimeSeriesMapperObserver {

    public void addTimeSeries(String timeSeriesName, int version, Range<Integer> pointRange, double[] values, Map<String, String> tags, TimeSeriesIndex index) {
        // default empty implementation
    }
}
