/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.commons;

import com.google.common.collect.Range;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.powsybl.metrix.commons.data.timeseries.TimeSeriesStoreUtil.checkIndexUnicity;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class ComputationRange {

    private Set<Integer> versions;
    private List<Range<Integer>> ranges;

    public ComputationRange() {
    }

    public ComputationRange(Set<Integer> versions, int firstVariant, int variantCount) {
        this(versions, List.of(Range.closed(firstVariant == -1 ? 0 : firstVariant, variantCount == -1 ? Integer.MAX_VALUE : firstVariant + variantCount - 1)));
    }

    public ComputationRange(Set<Integer> versions, Range<Integer> range) {
        this(versions, List.of(range));
    }

    public ComputationRange(Set<Integer> versions, List<Range<Integer>> ranges) {
        this.versions = versions;
        checkRanges(ranges);
        this.ranges = new ArrayList<>(ranges);
    }

    public Set<Integer> getVersions() {
        return versions;
    }

    public void setVersions(Set<Integer> versions) {
        this.versions = versions;
    }

    public List<Range<Integer>> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range<Integer>> ranges) {
        this.ranges = ranges;
    }

    public static ComputationRange check(ReadOnlyTimeSeriesStore store) {
        return check(null, store);
    }

    public static ComputationRange check(ComputationRange computationRange, ReadOnlyTimeSeriesStore store) {
        ComputationRange fixed = computationRange;
        if (computationRange == null) {
            fixed = new ComputationRange(store.getTimeSeriesDataVersions(), List.of(Range.closed(0, checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).getPointCount())));
        }
        if (fixed.getVersions() == null || fixed.getVersions().isEmpty()) {
            fixed.setVersions(store.getTimeSeriesDataVersions());
        }
        if (fixed.getVersions().isEmpty()) {
            fixed.setVersions(Collections.singleton(1));
        }
        if (fixed.getRanges().isEmpty()) {
            fixed.setRanges(List.of(Range.closed(0, checkIndexUnicity(store, store.getTimeSeriesNames(new TimeSeriesFilter().setIncludeDependencies(true))).getPointCount() - 1)));
        }
        return fixed;
    }

    private static void checkRange(int lowerEndpoint, int upperEndpoint) {
        if (lowerEndpoint < 0) {
            throw new IllegalArgumentException("First variant (" + lowerEndpoint + ") has to be positive");
        }
        if (upperEndpoint < lowerEndpoint) {
            throw new IllegalArgumentException("Last variant (" + upperEndpoint +
                    ") has to be greater or equals to first variant (" + lowerEndpoint + ")");
        }
    }

    private static void checkRanges(List<Range<Integer>> ranges) {
        // Sort the ranges
        List<Range<Integer>> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparing(Range::lowerEndpoint));

        // Check the first range
        Range<Integer> prev = sorted.getFirst();
        checkRange(prev.lowerEndpoint(), prev.upperEndpoint());

        // Check the other ranges and the overlaps
        for (int i = 1; i < sorted.size(); i++) {
            Range<Integer> curr = sorted.get(i);
            checkRange(curr.lowerEndpoint(), curr.upperEndpoint());
            if (prev.isConnected(curr)) {
                throw new IllegalArgumentException(prev + " overlaps with range " + curr);
            }
            prev = curr;
        }
    }
}
