/**
 * Copyright (c) 2021-2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.limits;

import com.powsybl.metrix.mapping.EquipmentVariable;
import com.powsybl.timeseries.TimeSeriesIndex;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public abstract class AbstractBoundLimitBuilder {
    double minP;
    double maxP;
    double targetP;
    boolean ignoreLimits;
    boolean isOkMinP;
    boolean isOkMaxP;
    boolean isUnmappedMinP;
    boolean isUnmappedMaxP;
    int version;
    TimeSeriesIndex index;
    final String targetPVariableName = EquipmentVariable.TARGET_P.getVariableName();
    final String maxPVariableName = EquipmentVariable.MAX_P.getVariableName();
    final String minPVariableName = EquipmentVariable.MIN_P.getVariableName();
    public static final int CONSTANT_VARIANT_ID = -1;
    String id;

}
