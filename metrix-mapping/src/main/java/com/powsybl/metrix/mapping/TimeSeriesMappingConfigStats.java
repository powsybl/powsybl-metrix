/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.DoubleStream;

import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigTableLoader.checkIndexUnicity;

public final class TimeSeriesMappingConfigStats {

    private final ReadOnlyTimeSeriesStore store;
    private final ComputationRange computationRange;
    private TimeSeriesIndex index = null;

    public TimeSeriesMappingConfigStats(ReadOnlyTimeSeriesStore store, ComputationRange computationRange) {
        this.store = Objects.requireNonNull(store);
        this.computationRange = Objects.requireNonNull(computationRange);
    }

    private DoubleStream getTimeSeriesStream(NodeCalc nodeCalc, int version, ComputationRange computationRange) {
        CalculatedTimeSeries calculatedTimeSeries = createCalculatedTimeSeries(nodeCalc, version, store);
        return Arrays.stream(calculatedTimeSeries.toArray()).skip(computationRange.getFirstVariant()).limit(computationRange.getVariantCount());
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
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).min().orElse(Double.NaN)).min().orElse(Double.NaN);
    }

    public double getTimeSeriesMax(NodeCalc nodeCalc, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).max().orElse(Double.NaN)).max().orElse(Double.NaN);
    }

    public double getTimeSeriesMax(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).max().orElse(Double.NaN)).max().orElse(Double.NaN);
    }

    public double getTimeSeriesAvg(NodeCalc nodeCalc, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).average().orElse(Double.NaN)).average().orElse(Double.NaN);
    }

    public double getTimeSeriesAvg(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).average().orElse(Double.NaN)).average().orElse(Double.NaN);
    }

    public double getTimeSeriesSum(NodeCalc nodeCalc, ComputationRange computationRange) {
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).sum()).sum();
    }

    public double getTimeSeriesSum(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).sum()).sum();
    }

    public double getTimeSeriesMedian(NodeCalc nodeCalc, ComputationRange computationRange) {
        double[] values = computationRange.getVersions().stream().flatMapToDouble(version -> {
            CalculatedTimeSeries calculatedTimeSeries = createCalculatedTimeSeries(nodeCalc, version, store);
            return Arrays.stream(calculatedTimeSeries.toArray()).skip(computationRange.getFirstVariant()).limit(computationRange.getVariantCount());
        }).toArray();
        return Arrays.stream(values).sorted().skip(new BigDecimal(values.length / 2).longValue()).limit(1).findFirst().orElse(Double.NaN);
    }

    public double getTimeSeriesMedian(String timeSeriesName) {
        NodeCalc nodeCalc = new TimeSeriesNameNodeCalc(timeSeriesName);
        return computationRange.getVersions().stream().mapToDouble(version -> getTimeSeriesStream(nodeCalc, version, computationRange).sum()).sum();
    }
}
