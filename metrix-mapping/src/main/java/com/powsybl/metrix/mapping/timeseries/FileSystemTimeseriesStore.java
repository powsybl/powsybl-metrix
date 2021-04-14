/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping.timeseries;

import com.powsybl.commons.PowsyblException;
import com.powsybl.timeseries.*;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Paul Bui-Quang <paul.buiquang at rte-france.com>
 */
public class FileSystemTimeseriesStore implements ReadOnlyTimeSeriesStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemTimeseriesStore.class);
    private final Path fileSystemStorePath;

    private final Map<String, TimeSeriesMetadata> existingTimeSeriesMetadataCache;
    private final Map<String, Object> fileLocks = new HashMap<>();

    public FileSystemTimeseriesStore(Path path) throws IOException {
        this.fileSystemStorePath = Objects.requireNonNull(path);
        this.existingTimeSeriesMetadataCache = initExistingTimeSeriesCache();

        assert Files.isDirectory(path);
    }

    @Override
    public Set<String> getTimeSeriesNames(TimeSeriesFilter timeSeriesFilter) {
        return existingTimeSeriesMetadataCache.keySet();
    }

    @Override
    public boolean timeSeriesExists(String s) {
        return existingTimeSeriesMetadataCache.containsKey(s);
    }

    @Override
    public Optional<TimeSeriesMetadata> getTimeSeriesMetadata(String s) {
        if (existingTimeSeriesMetadataCache.containsKey(s)) {
            return Optional.of(existingTimeSeriesMetadataCache.get(s));
        }
        return Optional.empty();
    }

    @Override
    public List<TimeSeriesMetadata> getTimeSeriesMetadata(Set<String> set) {
        return getTimeSeriesMetadata(timeSeriesMetadata -> set.contains(timeSeriesMetadata.getName())).collect(Collectors.toList());
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions() {
        return getTimeSeriesMetadata(ts -> true).findFirst().map(tsMeta -> {
            Path tsPath = fileSystemStorePath.resolve(tsMeta.getName());
            try {
                return Files.list(tsPath).map(path -> Integer.parseInt(path.getFileName().toString())).collect(Collectors.toSet());
            } catch (IOException e) {
                throw new PowsyblException(String.format("Failed to list versions for timeserie %s", tsMeta.getName()));
            }
        }).orElse(Collections.emptySet());
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions(String s) {
        return getTimeSeriesMetadata(timeSeriesMetadata -> timeSeriesMetadata.getName().equals(s)).findFirst().map(tsMeta -> {
            Path tsPath = fileSystemStorePath.resolve(tsMeta.getName());
            try {
                return Files.list(tsPath).map(path -> Integer.parseInt(path.getFileName().toString())).collect(Collectors.toSet());
            } catch (IOException e) {
                throw new PowsyblException(String.format("Failed to list versions for timeserie %s", tsMeta.getName()));
            }
        }).orElse(Collections.emptySet());
    }

    private Stream<TimeSeriesMetadata> getTimeSeriesMetadata(Predicate<TimeSeriesMetadata> filter) {
        return existingTimeSeriesMetadataCache.values().stream().filter(filter);
    }

    @Override
    public Optional<DoubleTimeSeries> getDoubleTimeSeries(String s, int i) {
        return getTimeSeries(DoubleTimeSeries.class, s, i);
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(Set<String> set, int i) {
        return set.stream().map(tsName -> getDoubleTimeSeries(tsName, i))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(int i) {
        return getDoubleTimeSeries(existingTimeSeriesMetadataCache.keySet(), i);
    }

    @Override
    public Optional<StringTimeSeries> getStringTimeSeries(String s, int i) {
        return getTimeSeries(StringTimeSeries.class, s, i);
    }

    @Override
    public List<StringTimeSeries> getStringTimeSeries(Set<String> set, int i) {
        return set.stream().map(tsName -> getStringTimeSeries(tsName, i))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private Object getFileLock(String tsFilePath) {
        return fileLocks.computeIfAbsent(tsFilePath, key -> new Object());
    }

    private <T extends TimeSeries> Optional<T> getTimeSeries(Class<T> timeSerieTypeClass, String name, int version) {
        Path tsPath = fileSystemStorePath.resolve(String.format("%s/%d", name, version));
        synchronized (getFileLock(tsPath.toString())) {
            if (!Files.exists(tsPath)) {
                throw new PowsyblException(String.format("Timeserie %s (version : %d) does not exist", name, version));
            }

            List<T> timeSeries = TimeSeries.parseJson(tsPath).stream().map(ts -> {
                if (timeSerieTypeClass.isAssignableFrom(DoubleTimeSeries.class) && TimeSeriesDataType.DOUBLE.equals(ts.getMetadata().getDataType())) {
                    return timeSerieTypeClass.cast(ts);
                }
                if (timeSerieTypeClass.isAssignableFrom(StringTimeSeries.class) && TimeSeriesDataType.STRING.equals(ts.getMetadata().getDataType())) {
                    return timeSerieTypeClass.cast(ts);
                }
                throw new PowsyblException(String.format("Wrong data type for timeseries %s", name));
            }).collect(Collectors.toList());
            if (timeSeries.size() != 1) {
                throw new PowsyblException("Found more than one timeseries");
            }
            return Optional.of(timeSeries.get(0));
        }
    }

    @Override
    public void addListener(TimeSeriesStoreListener timeSeriesStoreListener) {
        throw new NotImplementedException("Not impletemented");
    }

    @Override
    public void removeListener(TimeSeriesStoreListener timeSeriesStoreListener) {
        throw new NotImplementedException("Not impletemented");
    }

    public void importTimeSeries(List<TimeSeries> timeSeriesList, int version, boolean overwriteExisting, boolean append) {
        timeSeriesList.stream().forEach(ts -> {
            String tsName = ts.getMetadata().getName();
            existingTimeSeriesMetadataCache.put(tsName, ts.getMetadata());
            try {
                Path tsFolder = Files.createDirectories(fileSystemStorePath.resolve(tsName));
                Path versionFile = tsFolder.resolve(String.valueOf(version));
                if (!append && !overwriteExisting && Files.exists(versionFile)) {
                    throw new RuntimeException(String.format("Timeserie %s already exist", tsName));
                }
                synchronized (getFileLock(versionFile.toString())) {
                    TimeSeries updatedTs = ts;
                    if (Files.exists(versionFile)) {
                        List<TimeSeries> existingTsList = TimeSeries.parseJson(versionFile);
                        if (existingTsList.size() != 1) {
                            throw new RuntimeException("Existing ts file should contain one and only one ts");
                        }
                        TimeSeries existingTs = existingTsList.get(0);
                        if (append && InfiniteTimeSeriesIndex.INSTANCE.getType().equals(existingTs.getMetadata().getIndex().getType())) {
                            throw new PowsyblException("Cannot append to a calculated timeserie");
                        } else {
                            TimeSeriesDataType dataType = ts.getMetadata().getDataType();

                            if (dataType.equals(TimeSeriesDataType.DOUBLE)) {
                                UncompressedDoubleDataChunk tsSingleChunk = new UncompressedDoubleDataChunk(0, Stream
                                        .concat(((DoubleTimeSeries) existingTs).stream(), ((DoubleTimeSeries) ts).stream())
                                        .map(DoublePoint::getValue)
                                        .mapToDouble(d -> d)
                                        .toArray());
                                updatedTs = new StoredDoubleTimeSeries(existingTs.getMetadata(), tsSingleChunk);
                            } else {
                                UncompressedStringDataChunk tsSingleChunk = new UncompressedStringDataChunk(0, Stream
                                        .concat(((StringTimeSeries) existingTs).stream(), ((StringTimeSeries) ts).stream())
                                        .map(StringPoint::getValue)
                                        .collect(Collectors.toList())
                                        .toArray(new String[]{}));
                                updatedTs = new StringTimeSeries(existingTs.getMetadata(), tsSingleChunk);
                            }
                        }
                    } else {
                        Files.createFile(versionFile);
                    }
                    try (BufferedWriter bf = Files.newBufferedWriter(versionFile)) {
                        bf.write(updatedTs.toJson());
                    }
                }
            } catch (IOException e) {
                throw new PowsyblException("Failed to write timeseries", e);
            }
        });
    }

    public void importTimeSeries(BufferedReader reader, boolean overwriteExisting, boolean append) {
        Map<Integer, List<TimeSeries>> integerListMap = TimeSeries.parseCsv(reader, new TimeSeriesCsvConfig());
        integerListMap.forEach((key, value) -> importTimeSeries(value, key, overwriteExisting, append));
    }

    private Map<String, TimeSeriesMetadata> initExistingTimeSeriesCache() throws IOException {
        if (!Files.exists(fileSystemStorePath)) {
            Files.createDirectories(fileSystemStorePath);
        }

        assert Files.isDirectory(fileSystemStorePath);

        try (Stream<Path> children = Files.list(fileSystemStorePath)) {
            return children.filter(path -> Files.isDirectory(path))
                    .map(path -> {
                        try {
                            return Files.list(path);
                        } catch (IOException e) {
                            throw new PowsyblException(String.format("Failed to list timeserie version file resources for %s", path));
                        }
                    })
                    .map(versionPaths ->
                            versionPaths.findFirst().orElseThrow(() -> new PowsyblException("Failed to find a timeserie version resource"))
                    )
                    .map(TimeSeries::parseJson)
                    .map(tsList -> {
                        if (tsList.size() == 1) {
                            return tsList.get(0).getMetadata();
                        }
                        throw new PowsyblException("Invalid timeseries resource count");
                    })
                    .collect(Collectors.toMap(TimeSeriesMetadata::getName, tsMeta -> tsMeta));
        }
    }

    public void delete() {
        try {
            deleteRecursive(fileSystemStorePath);
        } catch (IOException e) {
            LOGGER.error("Failed to delete filesystem timeserie store", e);
        }
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> childlist = Files.list(path)) {
                for (Path child : childlist.collect(Collectors.toList())) {
                    deleteRecursive(child);
                }
            }
        }
        Files.delete(path);
    }

}
