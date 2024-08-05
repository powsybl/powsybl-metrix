/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.commons.PowsyblException;
import com.powsybl.timeseries.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.metrix.mapping.timeseries.TimeSeriesStoreUtil.isNotVersioned;
import static com.powsybl.timeseries.TimeSeries.DEFAULT_VERSION_NUMBER_FOR_UNVERSIONED_TIMESERIES;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class InMemoryTimeSeriesStore implements ReadOnlyTimeSeriesStore {

    private final Map<String, Map<Integer, TimeSeries>> stringTimeSeries = new HashMap<>();
    private final Map<String, Map<Integer, TimeSeries>> doubleTimeSeries = new HashMap<>();

    @Override
    public Set<String> getTimeSeriesNames(TimeSeriesFilter filter) {
        return getTimeSeriesNames().collect(Collectors.toSet());
    }

    @Override
    public boolean timeSeriesExists(String timeSeriesName) {
        return getTimeSeriesNames().anyMatch(tsName -> tsName.equals(timeSeriesName));
    }

    private Stream<String> getTimeSeriesNames() {
        return Stream.concat(
            stringTimeSeries.keySet().stream(),
            doubleTimeSeries.keySet().stream()
        );
    }

    @Override
    public Optional<TimeSeriesMetadata> getTimeSeriesMetadata(String timeSeriesName) {
        List<TimeSeriesMetadata> timeSeriesMetadata = getTimeSeriesMetadata(Collections.singleton(timeSeriesName));
        if (!timeSeriesMetadata.isEmpty()) {
            return Optional.of(timeSeriesMetadata.get(0));
        }
        return Optional.empty();
    }

    @Override
    public List<TimeSeriesMetadata> getTimeSeriesMetadata(Set<String> timeSeriesNames) {
        Map<String, TimeSeriesMetadata> metadataList = Stream
            .concat(stringTimeSeries.entrySet().stream(), doubleTimeSeries.entrySet().stream())
            .filter(timeSeries -> timeSeriesNames.contains(timeSeries.getKey()))
            .map(Map.Entry::getValue)
            .filter(timeSeriesVersions -> !timeSeriesVersions.isEmpty())
            .map(timeSeriesVersions -> timeSeriesVersions.values().stream().findFirst().get())
            .map(TimeSeries::getMetadata)
            .collect(Collectors.toMap(TimeSeriesMetadata::getName, Function.identity()));

        return metadataList.values().stream().toList();
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions() {
        return Stream
            .concat(stringTimeSeries.values().stream(), doubleTimeSeries.values().stream())
            .flatMap(values -> values.keySet().stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions(String timeSeriesName) {
        return Stream
            .concat(stringTimeSeries.entrySet().stream(), doubleTimeSeries.entrySet().stream())
            .filter(timeSeries -> timeSeriesName.equals(timeSeries.getKey()))
            .flatMap(timeSeries -> timeSeries.getValue().keySet().stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Optional<DoubleTimeSeries> getDoubleTimeSeries(String timeSeriesName, int version) {
        List<DoubleTimeSeries> localDoubleTimeSeries = getDoubleTimeSeries(Collections.singleton(timeSeriesName), version);
        if (!localDoubleTimeSeries.isEmpty()) {
            return Optional.of(localDoubleTimeSeries.get(0));
        }
        return Optional.empty();
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(Set<String> timeSeriesNames, int version) {
        return getTimeSeries(doubleTimeSeries, DoubleTimeSeries.class, timeSeriesNames, version);
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(int version) {
        return getDoubleTimeSeries(doubleTimeSeries.keySet(), version);
    }

    @Override
    public Optional<StringTimeSeries> getStringTimeSeries(String timeSeriesName, int version) {
        List<StringTimeSeries> localStringTimeSeries = getStringTimeSeries(Collections.singleton(timeSeriesName), version);
        if (!localStringTimeSeries.isEmpty()) {
            return Optional.of(localStringTimeSeries.get(0));
        }
        return Optional.empty();
    }

    @Override
    public List<StringTimeSeries> getStringTimeSeries(Set<String> timeSeriesNames, int version) {
        return getTimeSeries(stringTimeSeries, StringTimeSeries.class, timeSeriesNames, version);
    }

    /**
     * Returns the stored version number of the timeSeriesName depending on if the timeSeriesName is versioned or not
     */
    private int getTimeSeriesStoredVersion(String timeSeriesName, int version) {
        return isNotVersioned(getTimeSeriesDataVersions(timeSeriesName)) ? DEFAULT_VERSION_NUMBER_FOR_UNVERSIONED_TIMESERIES : version;
    }

    /**
     * Returns TimeSeries of the timeSeriesName depending on if the timeSeriesName is versioned or not
     */
    private TimeSeries getTimeSeries(String timeSeriesName, Map<Integer, TimeSeries> timeSeriesPerVersion, int version) {
        int storedVersion = getTimeSeriesStoredVersion(timeSeriesName, version);
        return timeSeriesPerVersion.get(storedVersion);
    }

    private <T extends TimeSeries> List<T> getTimeSeries(Map<String, Map<Integer, TimeSeries>> timeSeriesList, Class<T> timeSeriesTypeClass, Set<String> timeSeriesNames, int version) {
        return timeSeriesList.entrySet().stream()
            .filter(timeSeries -> timeSeriesNames.contains(timeSeries.getKey()))
            .map(timeSeries -> getTimeSeries(timeSeries.getKey(), timeSeries.getValue(), version))
            .filter(Objects::nonNull)
            .map(timeSeriesTypeClass::cast)
            .collect(Collectors.toList());
    }

    @Override
    public void addListener(TimeSeriesStoreListener listener) {
        throw new NotImplementedException("Not impletemented");
    }

    @Override
    public void removeListener(TimeSeriesStoreListener listener) {
        throw new NotImplementedException("Not impletemented");
    }

    public void importTimeSeries(BufferedReader reader) {
        TimeSeriesCsvConfig config = new TimeSeriesCsvConfig(ZoneId.systemDefault(), ';', true, TimeSeries.TimeFormat.DATE_TIME, 20000, false);
        Map<Integer, List<TimeSeries>> timeSeries = TimeSeries.parseCsv(reader, config);
        HashMap<TimeSeriesDataType, HashMap<String, Map<Integer, TimeSeries>>> tsByType = timeSeries.entrySet().stream()
            .flatMap(tsVersionEntry ->
                tsVersionEntry.getValue().stream().map(tsVersion -> Pair.of(tsVersion, tsVersionEntry.getKey()))
            )
            .collect(Collectors.groupingBy(
                tsVersion -> tsVersion.getKey().getMetadata().getDataType(),
                HashMap::new,
                Collectors.groupingBy(
                    tsVersion -> tsVersion.getKey().getMetadata().getName(),
                    HashMap::new,
                    Collectors.toMap(Pair::getValue, Pair::getKey)
                )
            ));

        HashMap<String, Map<Integer, TimeSeries>> importedDoubleTimeseries = tsByType.get(TimeSeriesDataType.DOUBLE);
        if (importedDoubleTimeseries != null) {
            doubleTimeSeries.putAll(importedDoubleTimeseries);
        }

        HashMap<String, Map<Integer, TimeSeries>> importedStringTimeseries = tsByType.get(TimeSeriesDataType.STRING);
        if (importedStringTimeseries != null) {
            stringTimeSeries.putAll(importedStringTimeseries);
        }
    }

    public void importTimeSeries(List<Path> csvTimeseries) {
        for (Path timeseriesCsv : csvTimeseries) {
            try (BufferedReader reader = Files.newBufferedReader(timeseriesCsv)) {
                importTimeSeries(reader);
            } catch (IOException e) {
                throw new PowsyblException("Failed to import time series", e);
            }
        }
    }
}
