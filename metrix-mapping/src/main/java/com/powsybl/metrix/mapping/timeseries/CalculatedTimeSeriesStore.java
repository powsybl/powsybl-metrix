/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNames;

import java.util.*;
import java.util.stream.Collectors;

public class CalculatedTimeSeriesStore implements ReadOnlyTimeSeriesStore {

    private final Map<String, NodeCalc> nodes;

    private final ReadOnlyTimeSeriesStore store;

    public CalculatedTimeSeriesStore(Map<String, NodeCalc> nodes, ReadOnlyTimeSeriesStore store) {
        this.nodes = Objects.requireNonNull(nodes);
        this.store = Objects.requireNonNull(store);
    }

    public NodeCalc getTimeSeriesNodeCalc(String timeSeriesName) {
        Objects.requireNonNull(timeSeriesName);
        return nodes.get(timeSeriesName);
    }

    @Override
    public Set<String> getTimeSeriesNames(TimeSeriesFilter filter) {
        return nodes.keySet();
    }

    @Override
    public boolean timeSeriesExists(String timeSeriesName) {
        Objects.requireNonNull(timeSeriesName);
        return nodes.containsKey(timeSeriesName);
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions(String timeSeriesName) {
        NodeCalc node = nodes.get(timeSeriesName);
        if (node == null) {
            return Collections.emptySet();
        }
        return CalculatedTimeSeries.computeVersions(node, new FromStoreTimeSeriesNameResolver(store, -1));
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions() {
        return store.getTimeSeriesDataVersions();
    }

    @Override
    public Optional<TimeSeriesMetadata> getTimeSeriesMetadata(String timeSeriesName) {
        Objects.requireNonNull(timeSeriesName);
        NodeCalc node = nodes.get(timeSeriesName);
        if (node == null) {
            return Optional.empty();
        }
        Optional<NodeCalc> optFstOrderNodeCalc = findFirstOrderNodeCalc(node, store.getTimeSeriesNames(null));
        TimeSeriesIndex index = optFstOrderNodeCalc
                .map(fstOrderNodeCalc -> CalculatedTimeSeries.computeIndex(fstOrderNodeCalc, new FromStoreTimeSeriesNameResolver(store, -1)))
                .orElse(InfiniteTimeSeriesIndex.INSTANCE);
        return Optional.of(new TimeSeriesMetadata(timeSeriesName, TimeSeriesDataType.DOUBLE, index));
    }

    private Optional<NodeCalc> findFirstOrderNodeCalc(NodeCalc node, Set<String> storedTimeseriesNames) {
        Set<String> dependentTsNames = TimeSeriesNames.list(node);
        if (dependentTsNames.stream().anyMatch(storedTimeseriesNames::contains)) {
            return Optional.of(node);
        }
        if (dependentTsNames.isEmpty()) {
            return Optional.empty();
        }
        return nodes.entrySet()
                .stream()
                .filter(tsNode -> dependentTsNames.contains(tsNode.getKey()))
                .map(tsNode -> findFirstOrderNodeCalc(tsNode.getValue(), storedTimeseriesNames))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public List<TimeSeriesMetadata> getTimeSeriesMetadata(Set<String> timeSeriesNames) {
        Objects.requireNonNull(timeSeriesNames);
        return timeSeriesNames.stream().map(this::getTimeSeriesMetadata)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private CalculatedTimeSeries createCalculatedTimeSeries(String timeSeriesName, NodeCalc nodeCalc, int version) {
        CalculatedTimeSeries calculatedTimeSeries = new CalculatedTimeSeries(timeSeriesName, nodeCalc);
        calculatedTimeSeries.setTimeSeriesNameResolver(new FromStoreTimeSeriesNameResolver(store, version));
        return calculatedTimeSeries;
    }

    @Override
    public Optional<DoubleTimeSeries> getDoubleTimeSeries(String timeSeriesName, int version) {
        Objects.requireNonNull(timeSeriesName);
        NodeCalc node = nodes.get(timeSeriesName);
        if (node == null) {
            return Optional.empty();
        }
        return Optional.of(createCalculatedTimeSeries(timeSeriesName, node, version));
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(Set<String> timeSeriesNames, int version) {
        return timeSeriesNames.stream().map(timeSeriesName -> getDoubleTimeSeries(timeSeriesName, version))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(int version) {
        return nodes.entrySet().stream()
                .map(e -> createCalculatedTimeSeries(e.getKey(), e.getValue(), version))
                .collect(Collectors.toList());
    }

    @Override
    public List<StringTimeSeries> getStringTimeSeries(Set<String> timeSeriesNames, int version) {
        return Collections.emptyList();
    }

    @Override
    public Optional<StringTimeSeries> getStringTimeSeries(String timeSeriesName, int version) {
        return Optional.empty();
    }

    @Override
    public void addListener(TimeSeriesStoreListener listener) {
        // nothing to do
    }

    @Override
    public void removeListener(TimeSeriesStoreListener listener) {
        // nothing to do
    }
}
