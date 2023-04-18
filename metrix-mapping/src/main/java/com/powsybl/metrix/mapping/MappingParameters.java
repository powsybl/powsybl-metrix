/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.google.common.collect.ImmutableMap;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

public class MappingParameters {

    private static final float DEFAULT_TOLERANCE_THRESHOLD = 0.0001f;

    public static MappingParameters load() {
        MappingParameters parameters = new MappingParameters();
        PlatformConfig.defaultConfig().getOptionalModuleConfig("mapping-default-parameters")
                .ifPresent(config -> parameters.setToleranceThreshold(config.getOptionalFloatProperty("tolerance-threshold")
                        .orElseGet(() -> config.getFloatProperty("toleranceThreshold", DEFAULT_TOLERANCE_THRESHOLD))));
        return parameters;
    }

    private float toleranceThreshold;

    private boolean withTimeSeriesStats;

    public MappingParameters() {
        this(DEFAULT_TOLERANCE_THRESHOLD, false);
    }

    public MappingParameters(float toleranceThreshold, boolean withTimeSeriesStats) {
        this.toleranceThreshold = toleranceThreshold;
        this.withTimeSeriesStats = withTimeSeriesStats;
    }

    public float getToleranceThreshold() {
        return toleranceThreshold;
    }

    public MappingParameters setToleranceThreshold(float toleranceThreshold) {
        this.toleranceThreshold = toleranceThreshold;
        return this;
    }

    public boolean getWithTimeSeriesStats() {
        return withTimeSeriesStats;
    }

    public MappingParameters setWithTimeSeriesStats(boolean withTimeSeriesStats) {
        this.withTimeSeriesStats = withTimeSeriesStats;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toleranceThreshold, withTimeSeriesStats);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MappingParameters) {
            MappingParameters other = (MappingParameters) obj;
            return toleranceThreshold == other.toleranceThreshold && withTimeSeriesStats == other.withTimeSeriesStats;
        }
        return false;
    }

    @Override
    public String toString() {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                .put("toleranceThreshold", toleranceThreshold)
                .put("withTimeSeriesStats", withTimeSeriesStats);

        return builder.build().toString();
    }
}
