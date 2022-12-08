/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesFilter;

import java.util.Collections;
import java.util.Set;

import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigTableLoader.checkIndexUnicity;

public class ComputationRange {

    private Set<Integer> versions;
    private int firstVariant;
    private int variantCount;

    public ComputationRange() {
    }

    public ComputationRange(Set<Integer> versions, int firstVariant, int variantCount) {
        this.versions = versions;
        this.firstVariant = firstVariant;
        this.variantCount = variantCount;
    }

    public Set<Integer> getVersions() {
        return versions;
    }

    public void setVersions(Set<Integer> versions) {
        this.versions = versions;
    }

    public int getFirstVariant() {
        return firstVariant;
    }

    public void setFirstVariant(int firstVariant) {
        this.firstVariant = firstVariant;
    }

    public int getVariantCount() {
        return variantCount;
    }

    public void setVariantCount(int variantCount) {
        this.variantCount = variantCount;
    }

    public static ComputationRange check(ReadOnlyTimeSeriesStore store) {
        return check(null, store);
    }

    public static ComputationRange check(ComputationRange computationRange, ReadOnlyTimeSeriesStore store) {
        ComputationRange fixed = computationRange;
        if (computationRange == null) {
            fixed = new ComputationRange(store.getTimeSeriesDataVersions(), 0, checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).getPointCount());
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
            fixed.setVariantCount(checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).getPointCount());
        }
        return fixed;
    }
}
