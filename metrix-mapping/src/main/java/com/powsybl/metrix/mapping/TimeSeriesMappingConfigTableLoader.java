/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException;
import com.powsybl.timeseries.*;
import com.powsybl.timeseries.ast.NodeCalc;
import com.powsybl.timeseries.ast.TimeSeriesNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.powsybl.metrix.mapping.TimeSeriesMapper.CONNECTED_VALUE;
import static com.powsybl.metrix.mapping.TimeSeriesMapper.DISCONNECTED_VALUE;
import static com.powsybl.metrix.mapping.timeseries.TimeSeriesStoreUtil.isNotVersioned;

/**
 * @author Marianne Funfrock {@literal <marianne.funfrock at rte-france.com>}
 */
public class TimeSeriesMappingConfigTableLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeSeriesMappingConfigTableLoader.class);
    private static final int MIN_NUMBER_OF_POINTS = 50;

    protected final TimeSeriesMappingConfig config;
    protected final ReadOnlyTimeSeriesStore store;

    public TimeSeriesMappingConfigTableLoader(TimeSeriesMappingConfig config, ReadOnlyTimeSeriesStore store) {
        this.config = Objects.requireNonNull(config);
        this.store = Objects.requireNonNull(store);
    }

    public TimeSeriesTable load(int version, Set<String> requiredTimeSeries, Range<Integer> pointRange) {
        Set<String> usedTimeSeriesNames = StreamSupport.stream(findUsedTimeSeriesNames().spliterator(), false).collect(Collectors.toSet());
        usedTimeSeriesNames.addAll(requiredTimeSeries);
        ReadOnlyTimeSeriesStore storeWithPlannedOutages = buildStoreWithPlannedOutages(store, version, config.getTimeSeriesToPlannedOutagesMapping());
        return loadToTable(version, storeWithPlannedOutages, pointRange, usedTimeSeriesNames);
    }

    public TimeSeriesTable loadToTable(int version, ReadOnlyTimeSeriesStore store, Range<Integer> pointRange, Iterable<String> usedTimeSeriesNames) {
        Set<String> timeSeriesNamesToLoad = findTimeSeriesNamesToLoad(usedTimeSeriesNames);

        TimeSeriesIndex index = checkIndexUnicity(store, timeSeriesNamesToLoad);
        checkValues(store, new TreeSet<>(Set.of(version)), timeSeriesNamesToLoad);

        TimeSeriesTable table = new TimeSeriesTable(version, version, index);

        // load time series series
        List<DoubleTimeSeries> loadedTimeSeries = Collections.emptyList();
        if (!timeSeriesNamesToLoad.isEmpty()) {
            List<DoubleTimeSeries> timeSeriesList = store.getDoubleTimeSeries(timeSeriesNamesToLoad, version);
            int nbPointsToCompute = pointRange.upperEndpoint() - pointRange.lowerEndpoint() + 1;
            if (index.getPointCount() != nbPointsToCompute) {
                // to avoid loading all values
                int nbPointsToLoad = Math.max(pointRange.upperEndpoint() + 1, Math.min(index.getPointCount(), MIN_NUMBER_OF_POINTS));
                try {
                    List<List<DoubleTimeSeries>> split = TimeSeries.split(timeSeriesList, nbPointsToLoad);
                    loadedTimeSeries = split.getFirst();
                } catch (RuntimeException e) {
                    LOGGER.warn("Failed to split timeSeries with {} pointsToLoad and {} pointsToCompute (reason : {}). Will take the whole time series", nbPointsToLoad, nbPointsToCompute, e.getMessage());
                    loadedTimeSeries = store.getDoubleTimeSeries(timeSeriesNamesToLoad, version);
                }
            } else {
                loadedTimeSeries = store.getDoubleTimeSeries(timeSeriesNamesToLoad, version);
            }
        }
        List<DoubleTimeSeries> timeSeriesToAddToTable = new ArrayList<>(loadedTimeSeries);
        ReadOnlyTimeSeriesStore storeCache = new ReadOnlyTimeSeriesStoreCache(loadedTimeSeries);
        TimeSeriesNameResolver resolver = new FromStoreTimeSeriesNameResolver(storeCache, version);

        // add calculated time series
        for (String mappedTimeSeriesName : usedTimeSeriesNames) {
            NodeCalc nodeCalc = config.timeSeriesNodes.get(mappedTimeSeriesName);
            if (nodeCalc != null) {
                CalculatedTimeSeries timeSeries = new CalculatedTimeSeries(mappedTimeSeriesName, nodeCalc);
                timeSeries.setTimeSeriesNameResolver(resolver);
                timeSeriesToAddToTable.add(timeSeries);
            }
        }

        table.load(version, timeSeriesToAddToTable);

        return table;
    }

    /**
     * <p>Deduce from planned outages string time series giving a list of comma separated disconnected equipments
     * corresponding double time series for each disconnected equipment</p>
     * <p>Example for 1 planned outages time series 'planned_outages_ts' with 4 steps:
     *     <ul>
     *         <li>disconnected ids:
     *         <ul><li>step1 : id1</li>
     *         <li>step2 : id2</li>
     *         <li>step3 : id1, id2</li>
     *         <li>step4 : none</li></ul></li>
     *     <li>returned store contains 2 double time series:
     *         <ul><li>'planned_outages'_id1 with values: 'DISCONNECTED_VALUE', 'CONNECTED_VALUE', 'DISCONNECTED_VALUE', 'CONNECTED_VALUE'</li>
     *         <li>'planned_outages'_id2 with values: 'CONNECTED_VALUE', 'DISCONNECTED_VALUE', 'DISCONNECTED_VALUE', 'CONNECTED_VALUE'</li></ul></li>
     *         </ul>
     *</p>
     * @param  timeSeriesName                       planned outages time series name
     * @param  timeSeriesValues                     planned outages time series values
     * @param  disconnectedIds                      planned outages disconnected ids set
     * @param  index                                index of double time series to create
     * @return double time series for each disconnected equipment
     */
    public static List<DoubleTimeSeries> computeDisconnectedEquipmentTimeSeries(String timeSeriesName, String[] timeSeriesValues, Set<String> disconnectedIds, TimeSeriesIndex index) {
        List<DoubleTimeSeries> doubleTimeSeries = new ArrayList<>();
        int nbPoints = index.getPointCount();
        for (String id : disconnectedIds) {
            double[] values = new double[nbPoints];
            Arrays.fill(values, CONNECTED_VALUE);
            for (int i = 0; i < nbPoints; i++) {
                if (timeSeriesValues[i] == null) {
                    continue;
                }
                String[] ids = timeSeriesValues[i].split(",");
                if (Arrays.asList(ids).contains(id)) {
                    values[i] = DISCONNECTED_VALUE;
                }
            }
            DoubleTimeSeries doubleTs = new StoredDoubleTimeSeries(
                    new TimeSeriesMetadata(plannedOutagesEquipmentTsName(timeSeriesName, id), TimeSeriesDataType.DOUBLE, index),
                    new UncompressedDoubleDataChunk(0, values).tryToCompress());
            doubleTimeSeries.add(doubleTs);
        }
        return doubleTimeSeries;
    }

    public static String plannedOutagesEquipmentTsName(String tsName, String id) {
        return String.format("%s_%s", tsName, id);
    }

    /**
     * Check if store contains already disconnected equipment time series deduced from planned outages string time series
     * <ul>
     *     <li>if so, returns store</li>
     *     <li>if not, build the store containing disconnected equipment time series</li>
     *</ul>
     * @param  store                                store containing the planned outages time series
     * @param  version                              version to compute
     * @param  timeSeriesToPlannedOutagesMapping    map of planned outages time series giving the set of disconnected equipment ids
     * @return depending on the check, store or aggregator of store and disconnected equipment time series store
     */

    public static ReadOnlyTimeSeriesStore buildStoreWithPlannedOutages(ReadOnlyTimeSeriesStore store, int version, Map<String, Set<String>> timeSeriesToPlannedOutagesMapping) {
        if (timeSeriesToPlannedOutagesMapping.isEmpty()) {
            return store;
        }

        // Check if store already contains equipment outages time series
        Set<Boolean> timeSeriesExist = timeSeriesToPlannedOutagesMapping.keySet().stream()
            .flatMap(key -> timeSeriesToPlannedOutagesMapping.get(key).stream()
                .map(value -> new AbstractMap.SimpleEntry<>(key, value)))
            .map(e -> store.timeSeriesExists(plannedOutagesEquipmentTsName(e.getKey(), e.getValue())))
            .collect(Collectors.toSet());
        if (timeSeriesExist.size() == 1 && timeSeriesExist.contains(true)) {
            return store;
        }

        ReadOnlyTimeSeriesStore plannedOutagesStore = buildPlannedOutagesStore(store, version, timeSeriesToPlannedOutagesMapping);
        return new ReadOnlyTimeSeriesStoreAggregator(store, plannedOutagesStore);
    }

    /**
     * <p>Deduce from all planned outages time series corresponding time series of the given version for each disconnected equipment</p>
     * @param  store                                store containing the planned outages time series
     * @param  version                              version to compute
     * @param  timeSeriesToPlannedOutagesMapping    map of planned outages time series giving the set of disconnected equipment ids
     * @return store containing double time series of each disconnected equipment
     */

    public static ReadOnlyTimeSeriesStore buildPlannedOutagesStore(ReadOnlyTimeSeriesStore store, int version, Map<String, Set<String>> timeSeriesToPlannedOutagesMapping) {
        List<DoubleTimeSeries> doubleTimeSeries = new ArrayList<>();

        // Build equipment planned outages time series
        TimeSeriesIndex index = checkIndexUnicity(store, timeSeriesToPlannedOutagesMapping.keySet());
        for (Map.Entry<String, Set<String>> entry : timeSeriesToPlannedOutagesMapping.entrySet()) {
            String timeSeriesName = entry.getKey();
            Set<String> disconnectedIds = entry.getValue();
            StringTimeSeries plannedOutagesTimeSeries = store.getStringTimeSeries(timeSeriesName, version).orElseThrow(() -> new TimeSeriesException("Invalid planned outages time series name " + timeSeriesName));
            List<DoubleTimeSeries> disconnectedEquipmentTimeSeries = computeDisconnectedEquipmentTimeSeries(timeSeriesName, plannedOutagesTimeSeries.toArray(), disconnectedIds, index);
            doubleTimeSeries.addAll(disconnectedEquipmentTimeSeries);
        }
        return new ReadOnlyTimeSeriesStoreCache(doubleTimeSeries);
    }

    public Set<String> findTimeSeriesNamesToLoad() {
        return findTimeSeriesNamesToLoad(findUsedTimeSeriesNames());
    }

    public Iterable<String> findUsedTimeSeriesNames() {
        return Iterables.concat(config.mappedTimeSeriesNames,
                config.timeSeriesToEquipmentMap.keySet(),
                config.timeSeriesToPlannedOutagesMapping.keySet(),
                config.distributionKeys.values().stream()
                        .filter(distributionKey -> distributionKey instanceof TimeSeriesDistributionKey)
                        .map(distributionKey -> ((TimeSeriesDistributionKey) distributionKey).getTimeSeriesName())
                        .collect(Collectors.toSet()));
    }

    public Set<String> findTimeSeriesNamesToLoad(Iterable<String> usedTimeSeriesNames) {
        Set<String> timeSeriesNamesToLoad = new HashSet<>();

        // load data of each mapped time series and for each of the equipment time series
        for (String timeSeriesName : usedTimeSeriesNames) {
            NodeCalc nodeCalc = config.timeSeriesNodes.get(timeSeriesName);
            if (nodeCalc != null) {
                // it is a calculated time series
                // find stored time series used in this calculated time series
                timeSeriesNamesToLoad.addAll(TimeSeriesNames.list(nodeCalc));
            } else {
                // it is a stored time series
                timeSeriesNamesToLoad.add(timeSeriesName);
            }
        }

        return timeSeriesNamesToLoad;
    }

    public TimeSeriesIndex checkIndexUnicity() {
        return checkIndexUnicity(store, findTimeSeriesNamesToLoad());
    }

    public static TimeSeriesIndex checkIndexUnicity(ReadOnlyTimeSeriesStore store, Set<String> timeSeriesNamesToLoad) {
        Set<TimeSeriesIndex> indexes = timeSeriesNamesToLoad.isEmpty() ? Collections.emptySet()
                : store.getTimeSeriesMetadata(timeSeriesNamesToLoad)
                .stream()
                .map(TimeSeriesMetadata::getIndex)
                .filter(index -> !(index instanceof InfiniteTimeSeriesIndex))
                .collect(Collectors.toSet());

        if (indexes.isEmpty()) {
            return InfiniteTimeSeriesIndex.INSTANCE;
        } else if (indexes.size() > 1) {
            throw new TimeSeriesMappingException("Time series involved in the mapping must have the same index: "
                    + indexes);
        }
        return indexes.iterator().next();
    }

    public void checkValues(Set<Integer> versions) {
        checkValues(store, versions, findTimeSeriesNamesToLoad());
    }

    public static void checkValues(ReadOnlyTimeSeriesStore store, Set<Integer> versions, Set<String> timeSeriesNamesToLoad) {
        timeSeriesNamesToLoad.forEach(timeSeriesName -> {
            Set<Integer> existingVersions = store.getTimeSeriesDataVersions(timeSeriesName);
            if (!isNotVersioned(existingVersions) && !existingVersions.isEmpty() && !existingVersions.containsAll(versions)) {
                Set<Integer> undefinedVersions = new HashSet<>(versions);
                undefinedVersions.removeAll(existingVersions);
                throw new TimeSeriesMappingException("The time series store does not contain values for ts " + timeSeriesName + " and version(s) " + undefinedVersions);
            }
        });
    }
}
