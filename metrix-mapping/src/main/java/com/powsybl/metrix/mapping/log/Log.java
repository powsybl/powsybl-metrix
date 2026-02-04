/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log;

import com.powsybl.timeseries.TimeSeriesIndex;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public record Log(System.Logger.Level level, TimeSeriesIndex index, int version, int point, String label,
                  String message) {
}
