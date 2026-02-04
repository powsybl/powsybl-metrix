/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class TimeSeriesMapperParameters {

    private final TreeSet<Integer> versions;

    private final Range<Integer> pointRange;

    private final boolean ignoreLimits;

    private final boolean ignoreEmptyFilter;

    private final boolean identifyConstantTimeSeries;

    private final float toleranceThreshold;

    private final Set<String> requiredTimeseries;

    public TimeSeriesMapperParameters(NavigableSet<Integer> versions, Range<Integer> pointRange, boolean ignoreLimits, boolean ignoreEmptyFilter, boolean identifyConstantTimeSeries, float toleranceThreshold) {
        this(versions, pointRange, ignoreLimits, ignoreEmptyFilter, identifyConstantTimeSeries, Collections.emptySet(), toleranceThreshold);
    }

    public TimeSeriesMapperParameters(NavigableSet<Integer> versions, Range<Integer> pointRange, boolean ignoreLimits, boolean ignoreEmptyFilter,
                                      boolean identifyConstantTimeSeries, Set<String> requiredTimeseries, float toleranceThreshold) {
        this.versions = new TreeSet<>(Objects.requireNonNull(versions));
        this.pointRange = Objects.requireNonNull(pointRange);
        this.ignoreLimits = ignoreLimits;
        this.ignoreEmptyFilter = ignoreEmptyFilter;
        this.identifyConstantTimeSeries = identifyConstantTimeSeries;
        this.toleranceThreshold = toleranceThreshold;
        this.requiredTimeseries = Objects.requireNonNull(requiredTimeseries);
    }

    public NavigableSet<Integer> getVersions() {
        return Collections.unmodifiableNavigableSet(versions);
    }

    public Range<Integer> getPointRange() {
        return pointRange;
    }

    public boolean isIgnoreLimits() {
        return ignoreLimits;
    }

    public boolean isIgnoreEmptyFilter() {
        return ignoreEmptyFilter;
    }

    public boolean isIdentifyConstantTimeSeries() {
        return identifyConstantTimeSeries;
    }

    public float getToleranceThreshold() {
        return toleranceThreshold;
    }

    public Set<String> getRequiredTimeseries() {
        return requiredTimeseries;
    }
}
