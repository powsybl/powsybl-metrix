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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
public class FileSystemTimeSeriesStore implements ReadOnlyTimeSeriesStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemTimeSeriesStore.class);
    private final Path fileSystemStorePath;

    private final Map<String, TimeSeriesMetadata> existingTimeSeriesMetadataCache;
    private final Map<String, Object> fileLocks = new ConcurrentHashMap<>();

    public FileSystemTimeSeriesStore(Path path) throws IOException {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalArgumentException(String.format("Path %s is not a directory", path));
        }
        this.fileSystemStorePath = Objects.requireNonNull(path);
        this.existingTimeSeriesMetadataCache = initExistingTimeSeriesCache();
    }

    public enum ExistingFilePolicy {
        THROW_EXCEPTION,
        OVERWRITE,
        APPEND
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> childlist = Files.list(path)) {
                for (Path child : childlist.toList()) {
                    deleteRecursive(child);
                }
            }
        }
        Files.delete(path);
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
        return getTimeSeriesMetadata(timeSeriesMetadata -> set.contains(timeSeriesMetadata.getName())).toList();
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions() {
        return getTimeSeriesDataVersions("");
    }

    @Override
    public Set<Integer> getTimeSeriesDataVersions(String s) {
        return getTimeSeriesMetadata(timeSeriesMetadata -> s.isEmpty() || timeSeriesMetadata.getName().equals(s)).findFirst().map(tsMeta -> {
            Path tsPath = fileSystemStorePath.resolve(tsMeta.getName());
            try (Stream<Path> paths = Files.list(tsPath)) {
                return paths.map(path -> Integer.parseInt(path.getFileName().toString())).collect(Collectors.toSet());
            } catch (IOException e) {
                throw new PowsyblException(String.format("Failed to list versions for timeserie %s", tsMeta.getName()));
            }
        }).orElse(Collections.emptySet());
    }

    private Stream<TimeSeriesMetadata> getTimeSeriesMetadata(Predicate<TimeSeriesMetadata> filter) {
        return existingTimeSeriesMetadataCache.values().stream().filter(filter);
    }

    @Override
    public Optional<DoubleTimeSeries> getDoubleTimeSeries(String s, int version) {
        return getTimeSeries(DoubleTimeSeries.class, s, version);
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(Set<String> set, int version) {
        return set.stream().map(tsName -> getDoubleTimeSeries(tsName, version))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    @Override
    public List<DoubleTimeSeries> getDoubleTimeSeries(int version) {
        return getDoubleTimeSeries(existingTimeSeriesMetadataCache.keySet(), version);
    }

    @Override
    public Optional<StringTimeSeries> getStringTimeSeries(String s, int version) {
        return getTimeSeries(StringTimeSeries.class, s, version);
    }

    @Override
    public List<StringTimeSeries> getStringTimeSeries(Set<String> set, int version) {
        return set.stream().map(tsName -> getStringTimeSeries(tsName, version))
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
                Optional<T> optionalTimeSeries = Optional.empty();
                if (timeSerieTypeClass.isAssignableFrom(DoubleTimeSeries.class) && TimeSeriesDataType.DOUBLE.equals(ts.getMetadata().getDataType())
                    || timeSerieTypeClass.isAssignableFrom(StringTimeSeries.class) && TimeSeriesDataType.STRING.equals(ts.getMetadata().getDataType())) {
                    optionalTimeSeries = Optional.of(timeSerieTypeClass.cast(ts));
                }
                return optionalTimeSeries;
            })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
            return switch (timeSeries.size()) {
                case 0 -> Optional.empty();
                case 1 -> Optional.of(timeSeries.get(0));
                default -> throw new PowsyblException("Found more than one timeseries");
            };
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

    /**
     * Import a list of TimeSeries in the current FileSystemTimeSeriesStore
     * @deprecated use {@link #importTimeSeries(List, int, ExistingFilePolicy)} instead
     */
    @Deprecated(since = "2.3.0")
    public void importTimeSeries(List<TimeSeries> timeSeriesList, int version, boolean overwriteExisting, boolean append) {
        ExistingFilePolicy existingFilePolicy;
        if (append) {
            existingFilePolicy = ExistingFilePolicy.APPEND;
        } else if (overwriteExisting) {
            existingFilePolicy = ExistingFilePolicy.OVERWRITE;
        } else {
            existingFilePolicy = ExistingFilePolicy.THROW_EXCEPTION;
        }
        importTimeSeries(timeSeriesList, version, existingFilePolicy);
    }

    /**
     * <p>Import a list of TimeSeries in the current FileSystemTimeSeriesStore.</p>
     * <p>If a file already exists for such TimeSeries, the new TimeSeries will be appended to it</p>
     */
    public void importTimeSeries(List<TimeSeries> timeSeriesList, int version) {
        importTimeSeries(timeSeriesList, version, ExistingFilePolicy.APPEND);
    }

    /**
     * <p>Import a list of TimeSeries in the current FileSystemTimeSeriesStore.</p>
     * <p>If a file already exists for such TimeSeries, depending on {@code existingFiles}, the existing file will either
     * be kept as it is, overwritten or the new TimeSeries will be appended to it</p>
     */
    public void importTimeSeries(List<TimeSeries> timeSeriesList, int version, ExistingFilePolicy existingFilePolicy) {
        timeSeriesList.forEach(ts -> {
            String tsName = ts.getMetadata().getName();
            try {
                Path tsFolder = Files.createDirectories(fileSystemStorePath.resolve(tsName));
                Path versionFile = tsFolder.resolve(String.valueOf(version));
                if (existingFilePolicy == ExistingFilePolicy.THROW_EXCEPTION && Files.exists(versionFile)) {
                    throw new PowsyblException(String.format("Timeserie %s already exist", tsName));
                }
                synchronized (getFileLock(versionFile.toString())) {
                    manageVersionFile(ts, versionFile, existingFilePolicy);
                }
            } catch (IOException e) {
                throw new PowsyblException("Failed to write timeseries", e);
            }
        });
    }

    private void manageVersionFile(TimeSeries ts, Path versionFile, ExistingFilePolicy existingFilePolicy) throws IOException {
        TimeSeries updatedTs = ts;
        if (Files.exists(versionFile)) {
            // A file already exists

            // The case THROW_EXCEPTION cannot happen here as it was already tested before calling this method
            if (existingFilePolicy == ExistingFilePolicy.APPEND) {
                // Get the existing TimeSeries
                List<TimeSeries> existingTsList = TimeSeries.parseJson(versionFile);
                if (existingTsList.size() != 1) {
                    throw new PowsyblException("Existing ts file should contain one and only one ts");
                }
                TimeSeries existingTs = existingTsList.get(0);

                // You cannot append to an infinite TimeSeries
                if (InfiniteTimeSeriesIndex.INSTANCE.getType().equals(existingTs.getMetadata().getIndex().getType())
                    || InfiniteTimeSeriesIndex.INSTANCE.getType().equals(ts.getMetadata().getIndex().getType())) {
                    throw new PowsyblException("Cannot append a TimeSeries with infinite index");
                }

                // Type of the new TimeSeries
                TimeSeriesDataType dataType = ts.getMetadata().getDataType();

                // You cannot append to a TimeSeries of a different type
                if (!dataType.equals(existingTs.getMetadata().getDataType())) {
                    throw new PowsyblException("Cannot append to a TimeSeries with different data type");
                }

                // Append the data
                updatedTs = appendTimeSeries(existingTs, ts, dataType);
            }
        } else {
            // Initialize a new empty file
            Files.createFile(versionFile);
        }
        try (BufferedWriter bf = Files.newBufferedWriter(versionFile)) {
            bf.write(updatedTs.toJson());
        }

        // Update the Metadata cache
        existingTimeSeriesMetadataCache.put(updatedTs.getMetadata().getName(), updatedTs.getMetadata());
    }

    private boolean compareIndexes(TimeSeriesIndex existingIndex, TimeSeriesIndex newIndex) {
        return existingIndex.getClass().equals(newIndex.getClass())
            && existingIndex.equals(newIndex);
    }

    private TimeSeries appendTimeSeries(TimeSeries<?, ?> existingTimeSeries, TimeSeries<?, ?> newTimeSeries, TimeSeriesDataType dataType) {
        // Indexes to concatenate
        TimeSeriesIndex existingIndex = existingTimeSeries.getMetadata().getIndex();
        TimeSeriesIndex newIndex = newTimeSeries.getMetadata().getIndex();

        // Compare the indexes
        if (compareIndexes(existingIndex, newIndex)) {
            // Same indexes -> compare the chunks offsets and add the chunks
            return appendTimeSeriesWithSameIndex(existingTimeSeries, newTimeSeries);
        }

        // Sort the indexes
        boolean existingComesFirst;
        if (existingIndex.getInstantAt(existingIndex.getPointCount() - 1).isBefore(newIndex.getInstantAt(0))) {
            existingComesFirst = true;
        } else if (newIndex.getInstantAt(newIndex.getPointCount() - 1).isBefore(existingIndex.getInstantAt(0))) {
            existingComesFirst = false;
        } else {
            throw new PowsyblException("Indexes to concatenate cannot overlap");
        }

        // Append the indexes
        TimeSeriesIndex updatedIndex = appendTimeSeriesIndex(existingIndex, newIndex, existingComesFirst);

        // Append the tags
        Map<String, String> updatedTags = new HashMap<>(existingTimeSeries.getMetadata().getTags());
        updatedTags.putAll(newTimeSeries.getMetadata().getTags());

        // New metadata
        TimeSeriesMetadata updatedMetadata = new TimeSeriesMetadata(existingTimeSeries.getMetadata().getName(), dataType, updatedTags, updatedIndex);

        // Append the chunks
        if (dataType.equals(TimeSeriesDataType.DOUBLE)) {
            // Chunks
            List<DoubleDataChunk> chunks = appendChunks(
                ((StoredDoubleTimeSeries) existingTimeSeries).getChunks(),
                ((StoredDoubleTimeSeries) newTimeSeries).getChunks(),
                existingIndex, newIndex,
                existingComesFirst);
            return new StoredDoubleTimeSeries(updatedMetadata, chunks);
        } else {
            // Chunks
            List<StringDataChunk> chunks = appendChunks(
                ((StringTimeSeries) existingTimeSeries).getChunks(),
                ((StringTimeSeries) newTimeSeries).getChunks(),
                existingIndex, newIndex,
                existingComesFirst);
            return new StringTimeSeries(updatedMetadata, chunks);
        }
    }

    private Set<Integer> getChunkPoints(TimeSeries<?, ?> timeSeries) {
        if (timeSeries instanceof AbstractTimeSeries<?, ?, ?> storedDoubleTimeSeries) {
            return storedDoubleTimeSeries.getChunks().stream()
                .flatMap(chunk -> IntStream.range(chunk.getOffset(), chunk.getOffset() + chunk.getLength()).boxed())
                .collect(Collectors.toSet());
        } else {
            throw new PowsyblException("Unsupported TimeSeries type for TimeSeries " + timeSeries);
        }
    }

    private TimeSeries appendTimeSeriesWithSameIndex(TimeSeries<?, ?> existingTimeSeries, TimeSeries<?, ?> newTimeSeries) {
        // Check that the timeseries don't have chunks with the same offset
        Set<Integer> existingPoints = getChunkPoints(existingTimeSeries);
        Set<Integer> newPoints = getChunkPoints(newTimeSeries);
        existingPoints.retainAll(newPoints);
        if (!existingPoints.isEmpty()) {
            // At least one offset is present in the two timeseries
            throw new PowsyblException(String.format("The two TimeSeries with the same index contain chunks with the same offset: %s", existingPoints));
        }

        // Add the chunks
        if (existingTimeSeries instanceof StoredDoubleTimeSeries existingStoredDoubleTimeSeries
            && newTimeSeries instanceof StoredDoubleTimeSeries newStoredDoubleTimeSeries) {
            List<DoubleDataChunk> chunks = existingStoredDoubleTimeSeries.getChunks();
            chunks.addAll(newStoredDoubleTimeSeries.getChunks());
            return new StoredDoubleTimeSeries(existingTimeSeries.getMetadata(), chunks);
        } else if (existingTimeSeries instanceof StringTimeSeries existingStringTimeSeries
            && newTimeSeries instanceof StringTimeSeries newStringTimeSeries) {
            List<StringDataChunk> chunks = existingStringTimeSeries.getChunks();
            chunks.addAll(newStringTimeSeries.getChunks());
            return new StringTimeSeries(existingTimeSeries.getMetadata(), chunks);
        } else {
            throw new PowsyblException("Expected both TimeSeries to be instances of the same class");
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends DataChunk> List<T> appendChunks(List<T> existingChunks, List<T> newChunks,
                                                       TimeSeriesIndex existingIndex, TimeSeriesIndex newIndex,
                                                       boolean existingComesFirst) {
        // Sort the chunks
        List<T> firstChunks;
        List<T> lastChunks;
        TimeSeriesIndex lastIndex;
        if (existingComesFirst) {
            firstChunks = existingChunks;
            lastChunks = newChunks;
            lastIndex = newIndex;
        } else {
            firstChunks = newChunks;
            lastChunks = existingChunks;
            lastIndex = existingIndex;
        }

        // Add the first chunks
        List<T> chunks = new ArrayList<>(firstChunks);
        final AtomicInteger offset = new AtomicInteger(existingComesFirst ? existingIndex.getPointCount() : newIndex.getPointCount());

        // Add the other chunks
        lastChunks.forEach(chunk -> {
            T chunkWithOffset;
            int chunkOffset = chunk.getOffset();
            if (chunk instanceof DoubleDataChunk doubleChunk) {
                chunkWithOffset = (T) new UncompressedDoubleDataChunk(
                    offset.get() + chunkOffset,
                    doubleChunk
                        .stream(lastIndex)
                        .map(DoublePoint::getValue)
                        .mapToDouble(Double::doubleValue)
                        .toArray()).tryToCompress();
            } else if (chunk instanceof StringDataChunk stringChunk) {
                chunkWithOffset = (T) new UncompressedStringDataChunk(
                    offset.get() + chunkOffset,
                    stringChunk
                        .stream(lastIndex)
                        .map(StringPoint::getValue)
                        .toArray(String[]::new)).tryToCompress();
            } else {
                // This case should not happen
                throw new PowsyblException("Unsupported chunk type: " + chunk.getClass().getName());
            }
            chunks.add(chunkWithOffset);
        });
        return chunks;
    }

    private boolean compareSpacingWithDurationBetweenIndexes(RegularTimeSeriesIndex firstIndex, RegularTimeSeriesIndex lastIndex) {
        return Duration.between(
                firstIndex.getInstantAt(firstIndex.getPointCount() - 1),
                lastIndex.getInstantAt(0))
            .toMillis() == firstIndex.getSpacing();
    }

    /**
     * Generate a LongStream with the times of a TimeSeriesIndex
     */
    private LongStream extractTimesFromIndex(TimeSeriesIndex timeSeriesIndex) {
        return timeSeriesIndex.stream().map(Instant::toEpochMilli).mapToLong(Long::longValue);
    }

    private TimeSeriesIndex appendTimeSeriesIndex(TimeSeriesIndex existingIndex, TimeSeriesIndex newIndex, boolean existingComesFirst) {
        if (existingIndex instanceof RegularTimeSeriesIndex regularExistingTimeSeriesIndex && newIndex instanceof RegularTimeSeriesIndex regularNewTimeSeriesIndex
            && regularExistingTimeSeriesIndex.getSpacing() == regularNewTimeSeriesIndex.getSpacing()
            && (existingComesFirst && compareSpacingWithDurationBetweenIndexes(regularExistingTimeSeriesIndex, regularNewTimeSeriesIndex)
            || !existingComesFirst && compareSpacingWithDurationBetweenIndexes(regularNewTimeSeriesIndex, regularExistingTimeSeriesIndex))) {
            // If both indexes are regular, both spacing are equals and the space between the first and the second index is equal to the spacing, the updated index is also regular
            return existingComesFirst ?
                new RegularTimeSeriesIndex(regularExistingTimeSeriesIndex.getStartTime(), regularNewTimeSeriesIndex.getEndTime(), regularExistingTimeSeriesIndex.getSpacing()) :
                new RegularTimeSeriesIndex(regularNewTimeSeriesIndex.getStartTime(), regularExistingTimeSeriesIndex.getEndTime(), regularExistingTimeSeriesIndex.getSpacing());
        } else {
            // Else the index is irregular
            return new IrregularTimeSeriesIndex(existingComesFirst ?
                LongStream.concat(extractTimesFromIndex(existingIndex), extractTimesFromIndex(newIndex)).toArray() :
                LongStream.concat(extractTimesFromIndex(newIndex), extractTimesFromIndex(existingIndex)).toArray()
            );
        }
    }

    /**
     * Import a list of TimeSeries in the current FileSystemTimeSeriesStore
     * @deprecated use {@link #importTimeSeries(BufferedReader, ExistingFilePolicy)}  instead
     */
    @Deprecated(since = "2.3.0")
    public void importTimeSeries(BufferedReader reader, boolean overwriteExisting, boolean append) {
        Map<Integer, List<TimeSeries>> integerListMap = TimeSeries.parseCsv(reader, new TimeSeriesCsvConfig());
        integerListMap.forEach((key, value) -> importTimeSeries(value, key, overwriteExisting, append));
    }

    /**
     * <p>Import a list of TimeSeries in the current FileSystemTimeSeriesStore.</p>
     * <p>If a file already exists for such TimeSeries, depending on {@code existingFiles}, the existing file will either
     * be kept as it is, overwritten or the new TimeSeries will be appended to it</p>
     */
    public void importTimeSeries(BufferedReader reader, ExistingFilePolicy existingFilePolicy) {
        Map<Integer, List<TimeSeries>> integerListMap = TimeSeries.parseCsv(reader, new TimeSeriesCsvConfig());
        integerListMap.forEach((key, value) -> importTimeSeries(value, key, existingFilePolicy));
    }

    private Map<String, TimeSeriesMetadata> initExistingTimeSeriesCache() throws IOException {
        if (!Files.exists(fileSystemStorePath)) {
            Files.createDirectories(fileSystemStorePath);
        }

        assert Files.isDirectory(fileSystemStorePath);

        try (Stream<Path> children = Files.list(fileSystemStorePath)) {
            return children.filter(Files::isDirectory)
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

}
