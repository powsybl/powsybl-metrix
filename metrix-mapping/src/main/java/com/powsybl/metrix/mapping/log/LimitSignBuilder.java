/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.metrix.mapping.log;

import java.util.Objects;

public class LimitSignBuilder implements LogDescriptionBuilder {

    private String timeSeriesName;

    private String variable;

    private double timeSeriesValue;

    private String sign;

    private static final String MAPPING_SIGN_PROBLEM = "mapping sign problem" + LABEL_SEPARATOR;

    public LimitSignBuilder timeSeriesName(String timeSeriesName) {
        this.timeSeriesName = Objects.requireNonNull(timeSeriesName);
        return this;
    }

    public LimitSignBuilder variable(String variable) {
        this.variable = Objects.requireNonNull(variable);
        return this;
    }

    public LimitSignBuilder timeSeriesValue(double timeSeriesValue) {
        this.timeSeriesValue = timeSeriesValue;
        return this;
    }

    public LimitSignBuilder min() {
        this.sign = " positive ";
        return this;
    }

    public LimitSignBuilder max() {
        this.sign = " negative ";
        return this;
    }

    public LogContent build() {
        LogContent log = new LogContent();
        log.label = MAPPING_SIGN_PROBLEM;
        log.message = String.format("Impossible to map %s %s of ts %s%s%s value", variable, formatDouble(timeSeriesValue),
                timeSeriesName, sign, variable);
        return log;
    }
}
