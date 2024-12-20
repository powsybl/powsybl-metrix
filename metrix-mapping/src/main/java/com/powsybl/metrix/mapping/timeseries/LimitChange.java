/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

/**
 * @author Valentin Berthault {@literal <valentin.berthault at rte-france.com>}
 */
public class LimitChange {
    private final double baseCaseLimit;

    private double limit;

    private int baseCaseLimitNbOfViolation = 0;

    public LimitChange(double baseCaseLimit, double limit) {
        this.baseCaseLimit = baseCaseLimit;
        this.limit = limit;
    }

    public double getBaseCaseLimit() {
        return baseCaseLimit;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public int getBaseCaseLimitNbOfViolation() {
        return baseCaseLimitNbOfViolation;
    }

    public void setBaseCaseLimitNbOfViolation(int baseCaseLimitNbOfViolation) {
        this.baseCaseLimitNbOfViolation = baseCaseLimitNbOfViolation;
    }
}
