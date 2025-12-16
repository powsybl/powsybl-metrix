/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.config;

import com.google.common.collect.Range;
import com.powsybl.metrix.commons.ComputationRange;
import com.powsybl.timeseries.CalculatedTimeSeries;
import com.powsybl.timeseries.FromStoreTimeSeriesNameResolver;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.DoubleStream;

import static com.powsybl.metrix.commons.data.timeseries.TimeSeriesStoreUtil.checkIndexUnicity;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public final class TimeSeriesMappingConfigStats {

    private final ReadOnlyTimeSeriesStore store;
    private final ComputationRange computationRange;
    private TimeSeriesIndex index = null;

    public static double[] filterByRanges(double[] array, List<Range<Integer>> ranges) {
        if (array.length == 0 || ranges.isEmpty()) {
            return new double[0];
        }
        // Normalize intervals by checking the coverage of the array
        List<int[]> intervals = new ArrayList<>(ranges.size());
        for (Range<Integer> r : ranges) {
            int start = Math.max(0, r.lowerEndpoint());
            int end = Math.min(array.length, r.upperEndpoint() + 1);
            if (start < end) {
                intervals.add(new int[]{start, end});
            }
        }
        // If no range covers any part of the input array, return an empty array
        if (intervals.isEmpty()) {
            return new double[0];
        }
        // Sort the intervals by start index
        intervals.sort(Comparator.comparingInt(a -> a[0]));
        // Merge overlapping intervals and compute the resulting sie
        List<int[]> merged = new ArrayList<>();
        int[] cur = intervals.getFirst();
        int size = 0;
        for (int i = 1; i < intervals.size(); i++) {
            int[] next = intervals.get(i);
            if (next[0] <= cur[1]) {
                cur[1] = Math.max(cur[1], next[1]);
            } else {
                merged.add(cur);
                size += cur[1] - cur[0];
                cur = next;
            }
        }
        merged.add(cur);
        size += cur[1] - cur[0];
        // Copy only needed slices
        double[] result = new double[size];
        int pos = 0;
        for (int[] m : merged) {
            System.arraycopy(array, m[0], result, pos, m[1] - m[0]);
            pos += m[1] - m[0];
        }
        return result;
    }

    public TimeSeriesMappingConfigStats(ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        this.store = Objects.requireNonNull(store);
        this.computationRange = Objects.requireNonNull(computationRange);
    }

    private DoubleStream getTimeSeriesStream(NodeCalc nodeCalc, int version, ComputationRange computationRange) {
        CalculatedTimeSeries calculatedTimeSeries = createCalculatedTimeSeries(nodeCalc, version, store);
        return Arrays.stream(filterByRanges(calculatedTimeSeries.toArray(), computationRange.getRanges()));
    }

    private CalculatedTimeSeries createCalculatedTimeSeries(NodeCalc nodeCalc, int version, ReadOnlyTimeSeriesStore store) {
        CalculatedTimeSeries calculatedTimeSeries = new CalculatedTimeSeries("", nodeCalc, new FromStoreTimeSeriesNameResolver(store, version));
        calculatedTimeSeries.synchronize(getIndex());
        return calculatedTimeSeries;
    }

    private TimeSeriesIndex getIndex() {
        if (index == null) {
            this.index = checkIndexUnicity(store, store.getTimeSeriesNames(null));
        }
        return index;
    }

    public double getTimeSeriesMin(NodeCalc nodeCalc, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).min().orElse(Double.NaN)).min().orElse(Double.NaN);
    }

    public double getTimeSeriesMin(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return getTimeSeriesMin(nodeCalc, computationRange);
    }

    public double getTimeSeriesMax(NodeCalc nodeCalc, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).max().orElse(Double.NaN)).max().orElse(Double.NaN);
    }

    public double getTimeSeriesMax(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return getTimeSeriesMax(nodeCalc, computationRange);
    }

    public double getTimeSeriesAvg(NodeCalc nodeCalc, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).average().orElse(Double.NaN)).average().orElse(Double.NaN);
    }

    public double getTimeSeriesAvg(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return getTimeSeriesAvg(nodeCalc, computationRange);
    }

    public double getTimeSeriesSum(NodeCalc nodeCalc, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).sum()).sum();
    }

    public double getTimeSeriesSum(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return getTimeSeriesSum(nodeCalc, computationRange);
    }

    public double getTimeSeriesMedian(NodeCalc nodeCalc, ComputationRange computationRange) {
        double[] values = computationRange.getVersions().stream().flatMapToDouble(version -> {
            CalculatedTimeSeries calculatedTimeSeries = createCalculatedTimeSeries(nodeCalc, version, store);
            return Arrays.stream(filterByRanges(calculatedTimeSeries.toArray(), computationRange.getRanges()));
        }).toArray();
        return Arrays.stream(values).sorted().skip(new BigDecimal(values.length / 2).longValue()).limit(1).findFirst().orElse(Double.NaN);
    }

    public double getTimeSeriesMedian(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return getTimeSeriesMedian(nodeCalc, computationRange);
    }
}
