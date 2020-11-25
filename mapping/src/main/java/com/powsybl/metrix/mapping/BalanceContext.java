/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

/**
 * @author Thiyagarasa Pratheep <pratheep.thiyagarasa@rte-france.com>
 */

public class BalanceContext {

    private final int version;
    private double balanceMin = Double.MAX_VALUE;
    private double balanceMax = -Double.MAX_VALUE;
    private double balanceSum = 0;
    private int pointCount = 0;

    public BalanceContext(int version) {
        this.version = version;
    }

    public double getAverage() {
        return this.pointCount == 0 ? Double.NaN : this.balanceSum / this.pointCount;
    }

    public void updateValue(double value) {
        this.balanceMin = Math.min(this.balanceMin, value);
        this.balanceMax = Math.max(this.balanceMax, value);
        this.balanceSum += value;
        this.pointCount++;
    }

    public int getVersion() {
        return version;
    }

    public double getBalanceMin() {
        return balanceMin;
    }

    public double getBalanceMax() {
        return balanceMax;
    }

    public double getBalanceSum() {
        return balanceSum;
    }
}
