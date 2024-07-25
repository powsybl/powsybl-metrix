/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.timeseries.TimeSeriesIndex;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class DefaultTimeSeriesMapperObserver implements TimeSeriesMapperObserver {

    @Override
    public void start() {
        // default empty implementation
    }

    @Override
    public void end() {
        // default empty implementation
    }

    @Override
    public void versionStart(int version) {
        // default empty implementation
    }

    @Override
    public void versionEnd(int version) {
        // default empty implementation
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        // default empty implementation
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        // default empty implementation
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        // default empty implementation
    }
}
