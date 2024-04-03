/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.integration.contingency;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.contingency.Contingency;

import java.util.Objects;

public class Probability extends AbstractExtension<Contingency> {
    static final String EXTENSION_NAME = "ContingencyProbability";
    private final Double probabilityBase;
    private final String probabilityTimeSeriesRef;

    public Probability(Double probabilityBase, String probabilityTimeSeriesRef) {
        this.probabilityBase = probabilityBase;
        this.probabilityTimeSeriesRef = probabilityTimeSeriesRef;
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    public Double getProbabilityBase() {
        return probabilityBase;
    }

    public String getProbabilityTimeSeriesRef() {
        return probabilityTimeSeriesRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Probability that = (Probability) o;
        return Objects.equals(probabilityBase, that.probabilityBase) &&
                Objects.equals(probabilityTimeSeriesRef, that.probabilityTimeSeriesRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(probabilityBase, probabilityTimeSeriesRef);
    }
}

