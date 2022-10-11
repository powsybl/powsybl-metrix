/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesFilter;

import java.util.Collections;

public final class ComputationRangeChecker {

    private ComputationRangeChecker() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    public static ComputationRange check(ComputationRange computationRange, ReadOnlyTimeSeriesStore store) {
        ComputationRange fixed = computationRange;
        if (computationRange == null) {
            fixed = new ComputationRange(store.getTimeSeriesDataVersions(), 0, TimeSeriesMappingConfig.checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).getPointCount());
        }
        if (fixed.getVersions() == null || fixed.getVersions().isEmpty()) {
            fixed.setVersions(store.getTimeSeriesDataVersions());
        }
        if (fixed.getVersions().isEmpty()) {
            fixed.setVersions(Collections.singleton(1));
        }
        if (fixed.getFirstVariant() == -1) {
            fixed.setFirstVariant(0);
        }
        if (fixed.getVariantCount() == -1) {
            fixed.setVariantCount(TimeSeriesMappingConfig.checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).getPointCount());
        }
        return fixed;
    }
}
