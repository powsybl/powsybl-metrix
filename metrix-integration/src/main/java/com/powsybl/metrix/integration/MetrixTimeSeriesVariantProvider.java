/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.contingency.Probability;
import com.powsybl.metrix.mapping.*;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesTable;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixTimeSeriesVariantProvider implements MetrixVariantProvider {

    private final Network network;

    private final ReadOnlyTimeSeriesStore store;

    private final MappingParameters mappingParameters;

    private final ContingenciesProvider contingenciesProvider;

    private final TimeSeriesMappingConfig config;

    private final int version;

    private final Range<Integer> variantRange;

    private final boolean ignoreLimits;

    private final boolean ignoreEmptyFilter;

    private final PrintStream err;

    private final TimeSeriesMapper mapper;

    public MetrixTimeSeriesVariantProvider(Network network, ReadOnlyTimeSeriesStore store, MappingParameters mappingParameters, TimeSeriesMappingConfig config, ContingenciesProvider contingenciesProvider,
                                           int version, Range<Integer> variantRange, boolean ignoreLimits, boolean ignoreEmptyFilter,
                                           PrintStream err) {
        this.network = Objects.requireNonNull(network);
        this.store = Objects.requireNonNull(store);
        this.mappingParameters = Objects.requireNonNull(mappingParameters);
        this.config = Objects.requireNonNull(config);
        this.version = version;
        this.variantRange = variantRange;
        this.ignoreLimits = ignoreLimits;
        this.ignoreEmptyFilter = ignoreEmptyFilter;
        this.contingenciesProvider = contingenciesProvider;
        this.err = Objects.requireNonNull(err);
        mapper = new TimeSeriesMapper(config, network, new TimeSeriesMappingLogger());
    }

    @Override
    public Range<Integer> getVariantRange() {
        return variantRange;
    }

    @Override
    public TimeSeriesIndex getIndex() {
        return config.checkIndexUnicity(store);
    }

    @Override
    public Set<String> getMappedBreakers() {
        return config.getBreakerToTimeSeriesMapping().keySet()
                .stream()
                .map(MappingKey::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader) {
        Objects.requireNonNull(variantReadRange);
        if (!variantRange.encloses(variantReadRange)) {
            throw new IllegalArgumentException("Variant range " + variantRange + " do not enclose read range " + variantReadRange);
        }
        Objects.requireNonNull(reader);

        DefaultTimeSeriesMapperObserver defaultTimeSeriesMapperObserver = new BalanceSummary(err) {
            @Override
            public void timeSeriesMappingStart(int point, TimeSeriesIndex index) {
                super.timeSeriesMappingStart(point, index);
                reader.onVariantStart(point);
            }

            @Override
            public void map(int version, int point, TimeSeriesTable table) {
                reader.onVariant(version, point, table);
            }

            @Override
            public void timeSeriesMappedToEquipment(int point, String timeSeriesName, Identifiable<?> identifiable, MappingVariable variable, double equipmentValue) {
                super.timeSeriesMappedToEquipment(point, timeSeriesName, identifiable, variable, equipmentValue);
                if (!Double.isNaN(equipmentValue)) {
                    reader.onEquipmentVariant(identifiable, variable, equipmentValue);
                }
            }

            @Override
            public void timeSeriesMappingEnd(int point, TimeSeriesIndex index, double balance) {
                super.timeSeriesMappingEnd(point, index, balance);
                reader.onVariantEnd(point);
            }
        };

        List<TimeSeriesMapperObserver> observers = Collections.singletonList(defaultTimeSeriesMapperObserver);

        TimeSeriesMapperParameters parameters = new TimeSeriesMapperParameters(new TreeSet<>(Collections.singleton(version)), variantReadRange, ignoreLimits, ignoreEmptyFilter, getContingenciesProbilitiesTs(), mappingParameters.getToleranceThreshold());
        mapper.mapToNetwork(store, parameters, observers);
    }

    private Set<String> getContingenciesProbilitiesTs() {
        return contingenciesProvider.getContingencies(network)
                .stream()
                .filter(contingency -> contingency.getExtension(Probability.class) != null && contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef() != null)
                .map(contingency -> contingency.getExtension(Probability.class).getProbabilityTimeSeriesRef())
                .collect(Collectors.toSet());
    }
}
