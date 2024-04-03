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
import com.powsybl.timeseries.TimeSeriesTable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MultipleTimeSeriesMapperObserver implements TimeSeriesMapperObserver {

    protected final List<TimeSeriesMapperObserver> observers;

    public MultipleTimeSeriesMapperObserver(List<TimeSeriesMapperObserver> observers) {
        this.observers = Objects.requireNonNull(observers);
    }

    public MultipleTimeSeriesMapperObserver(TimeSeriesMapperObserver... observers) {
        this(Arrays.asList(observers));
    }

    @Override
    public void start() {
        observers.forEach(TimeSeriesMapperObserver::start);
    }

    @Override
    public void end() {
        observers.forEach(TimeSeriesMapperObserver::end);
    }

    @Override
    public void versionStart(int version) {
        observers.forEach(o -> o.versionStart(version));
    }

    @Override
    public void versionEnd(int version) {
        observers.forEach(o -> o.versionEnd(version));
    }

    @Override
    public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
        observers.forEach(o -> o.timeSeriesMappingStart(point, index));
    }

    @Override
    public void map(int version, int point, TimeSeriesTable table) {
        observers.forEach(o -> o.map(version, point, table));
    }

    @Override
    public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
        observers.forEach(o -> o.timeSeriesMappedToEquipment(point, timeSeriesName, identifiable, variable, equipmentValue));
    }

    @Override
    public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
        observers.forEach(o -> o.timeSeriesMappingEnd(point, index, balance));
    }
}
