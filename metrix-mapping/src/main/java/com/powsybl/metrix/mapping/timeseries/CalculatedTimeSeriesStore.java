/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.*;

import java.util.*;
import java.util.stream.Collectors;

public class CalculatedTimeSeriesStore implements ReadOnlyTimeSeriesStore {

    private final Map<String, NodeCalc> nodes;
    private final Map<String, Map<String, String>> tags;
    private final Set<TimeSeriesIndex> indexSet = new HashSet<>();
    private final Set<String> dependentTsNamesVerified = new HashSet<>();

    private final ReadOnlyTimeSeriesStore store;

    public CalculatedTimeSeriesStore(Map<String, NodeCalc> nodes, Map<String, Map<String, String>> tags, ReadOnlyTimeSeriesStore store) {
        this.nodes = Objects.requireNonNull(nodes);
        this.tags = Objects.requireNonNull(tags);
        this.store = Objects.requireNonNull(store);
    }

    public CalculatedTimeSeriesStore(Map<String, NodeCalc> nodes, ReadOnlyTimeSeriesStore store) {
        this(nodes, Map.of(), store);
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
            .map(this::computeIndex)
            .orElse(InfiniteTimeSeriesIndex.INSTANCE);
        Map<String, String> timeSeriesTags = tags.containsKey(timeSeriesName) ? tags.get(timeSeriesName) : Map.of();
        return Optional.of(new TimeSeriesMetadata(timeSeriesName, TimeSeriesDataType.DOUBLE, timeSeriesTags, index));
    }

    private Optional<NodeCalc> findFirstOrderNodeCalc(NodeCalc node, Set<String> storedTimeSeriesNames) {
        Set<String> dependentTsNames = TimeSeriesNames.list(node);
        if (dependentTsNames.stream().anyMatch(storedTimeSeriesNames::contains)) {
            return Optional.of(node);
        }
        if (dependentTsNames.isEmpty()) {
            return Optional.empty();
        }
        return nodes.entrySet()
            .stream()
            .filter(tsNode -> dependentTsNames.contains(tsNode.getKey()))
            .map(tsNode -> findFirstOrderNodeCalc(tsNode.getValue(), storedTimeSeriesNames))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private TimeSeriesIndex computeIndex(NodeCalc node) {
        Set<String> dependentTsNames = TimeSeriesNames.list(node);
        Set<String> dependentTsNamesToVerify = new HashSet<>(dependentTsNames);
        dependentTsNamesToVerify.removeAll(dependentTsNamesVerified);
        if (!dependentTsNamesToVerify.isEmpty()) {
            indexSet.add(CalculatedTimeSeries.computeIndex(node, new FromStoreTimeSeriesNameResolver(store, -1)));
            dependentTsNamesVerified.addAll(dependentTsNames);
        }
        return indexSet.iterator().next();
    }

    @Override
    public List<TimeSeriesMetadata> getTimeSeriesMetadata(Set<String> timeSeriesNames) {
        Objects.requireNonNull(timeSeriesNames);
        return timeSeriesNames.stream().filter(this::timeSeriesExists).map(this::getTimeSeriesMetadata)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
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
            .toList();
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
