/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.timeseries.DoubleTimeSeries;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

/**
 * @author Quentin CAPY
 * @created 20/03/2020
 */
public final class TimeSeriesUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesUtils.class);

    private TimeSeriesUtils() {
    }

    public static List<double[]> getTimeSeriesVersions(String timeSeriesName, ReadOnlyTimeSeriesStore store, Set<Integer> versions) {
        List<double[]> result = new ArrayList<>();
        versions.forEach(v -> {
            Optional<DoubleTimeSeries> doubleTimeSeries = store.getDoubleTimeSeries(timeSeriesName, v);
            if (doubleTimeSeries.isPresent()) {
                result.add(store.getDoubleTimeSeries(timeSeriesName, v).get().toArray());
            }
        });
        return result;
    }

    public static double timeSeriesCountAverage(List<double[]> timeSeries, DoublePredicate condition) {
        try {
            return timeSeries.stream().mapToDouble(v -> timeSeriesStream(v).filter(condition).count()).average().orElse(Double.NaN);
        } catch (IllegalArgumentException iae) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error while processing time-series average count", iae);
            }
            return Double.NaN;
        }
    }

    public static double timeSeriesSumAndMean(List<double[]> timeSeries) {
        try {
            return timeSeries.stream().mapToDouble(v -> timeSeriesStream(v, x -> true).sum()).average().orElse(Double.NaN);
        } catch (IllegalArgumentException iae) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error while processing time-series mean", iae);
            }
            return Double.NaN;
        }
    }

    public static double timeSeriesMean(List<double[]> timeSeries) {
        return timeSeriesMean(timeSeries, test -> true);
    }

    public static double timeSeriesMean(List<double[]> timeSeries, DoublePredicate condition) {
        try {
            return timeSeries.stream().mapToDouble(v -> timeSeriesStream(v, condition).average().orElse(Double.NaN)).average().orElse(Double.NaN);
        } catch (IllegalArgumentException iae) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error while processing time-series mean", iae);
            }
            return Double.NaN;
        }
    }

    public static DoubleStream timeSeriesStream(double[] timeSeries, DoublePredicate condition) {
        return Arrays.stream(timeSeries).filter(d -> !Double.isNaN(d)).filter(condition);
    }

    public static DoubleStream timeSeriesStream(double[] timeSeries) {
        return timeSeriesStream(timeSeries, d -> true);
    }

    public static long timeSeriesCountAll(List<double[]> timeSeries, DoublePredicate condition) {
        try {
            return timeSeries.stream().mapToLong(v -> timeSeriesStream(v).filter(condition).count()).sum();
        } catch (IllegalArgumentException iae) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Error while processing time-series", iae);
            }
            return -1;
        }
    }

    public static Set<String> getTimeSeriesNamesSubset(Set<String> timeSeriesNames, String prefix) {
        return timeSeriesNames.stream()
            .filter(s -> s.startsWith(prefix))
            .collect(Collectors.toSet());
    }
}
