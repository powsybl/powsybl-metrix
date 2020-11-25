/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.metrix.mapping.MappingVariable;
import com.powsybl.timeseries.TimeSeriesTable;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public interface MetrixVariantReader {

    void onVariantStart(int variantNum);

    void onEquipmentVariant(Identifiable identifiable, MappingVariable variable, double value);

    void onVariant(int version, int point, TimeSeriesTable table);

    void onVariantEnd(int variantNum);
}
