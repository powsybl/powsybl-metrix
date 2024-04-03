/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.mapping.log;

import com.powsybl.timeseries.TimeSeriesIndex;

public class LogBuilder {
    private System.Logger.Level level;

    private TimeSeriesIndex index;

    private int point;

    private int version;

    private String message;

    private String label;

    public LogBuilder logDescription(LogContent log) {
        this.message = log.message;
        this.label = log.label;
        return this;
    }

    public LogBuilder level(System.Logger.Level level) {
        this.level = level;
        return this;
    }

    public LogBuilder index(TimeSeriesIndex index) {
        this.index = index;
        return this;
    }

    public LogBuilder point(int point) {
        this.point = point;
        return this;
    }

    public LogBuilder version(int version) {
        this.version = version;
        return this;
    }

    public Log build() {
        return new Log(level, index, version, point, label, message);
    }
}
