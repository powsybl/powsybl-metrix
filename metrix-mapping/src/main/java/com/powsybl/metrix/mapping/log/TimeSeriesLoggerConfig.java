/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.log;

import java.time.format.DateTimeFormatter;

/**
 * @author berthault {@literal <valentinberthault at outlook.fr>}
 */
public class TimeSeriesLoggerConfig {
    public final char separator;

    public final DateTimeFormatter dateTimeFormatter;

    public TimeSeriesLoggerConfig(char separator, DateTimeFormatter dateTimeFormatter) {
        this.separator = separator;
        this.dateTimeFormatter = dateTimeFormatter;
    }
}
