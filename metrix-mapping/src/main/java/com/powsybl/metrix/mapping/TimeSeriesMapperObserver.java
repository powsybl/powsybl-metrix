/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesTable;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public interface TimeSeriesMapperObserver {

    void start();

    void end();

    void versionStart(int version);

    void versionEnd(int version);

    void timeSeriesMappingStart(int point, TimeSeriesIndex index);

    default void map(int version, int point, TimeSeriesTable table) {
        // no op
    }

    void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable identifiable, MappingVariable variable, double equipmentValue);

    void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance);
}
