package com.powsybl.metrix.mapping;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.metrix.mapping.exception.TimeSeriesMappingException;
import com.powsybl.metrix.data.timeseries.FileSystemTimeSeriesStore;
import com.powsybl.metrix.mapping.keys.TimeSeriesDistributionKey;
import com.powsybl.timeseries.DoubleTimeSeries;
import com.powsybl.timeseries.InfiniteTimeSeriesIndex;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.StoredDoubleTimeSeries;
import com.powsybl.timeseries.StringTimeSeries;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesException;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.TimeSeriesTable;
import com.powsybl.timeseries.ast.TimeSeriesNameNodeCalc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.metrix.data.timeseries.TimeSeriesStoreUtil.checkIndexUnicity;
import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigTableLoader.buildPlannedOutagesStore;
import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigTableLoader.buildStoreWithPlannedOutages;
import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigTableLoader.checkValues;
import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigTableLoader.computeDisconnectedEquipmentTimeSeries;
import static com.powsybl.metrix.data.timeseries.FileSystemTimeSeriesStore.ExistingFilePolicy.APPEND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
class TimeSeriesMappingConfigTableLoaderTest {
    private FileSystem fileSystem;
    private RegularTimeSeriesIndex index;
    private FileSystemTimeSeriesStore tsStore;
    private TimeSeriesMappingConfigTableLoader tableLoader;

    @BeforeEach
    void setUp() throws IOException {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());

        // TimeSeries index
        Instant now = Instant.ofEpochMilli(978303600000L);
        index = RegularTimeSeriesIndex.create(now, now.plus(3, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d, 4d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts2", index, 1d, 3d, 5d, 7d);
        StoredDoubleTimeSeries ts3 = TimeSeries.createDouble("ts3", index, 1d, 3d, 5d, 7d);
        StoredDoubleTimeSeries ts4 = TimeSeries.createDouble("distributionKeyTs", index, 1d, 2d, 3d, 4d);
        StoredDoubleTimeSeries ts5 = TimeSeries.createDouble("mappedTs", index, 1d, 2d, 3d, 4d);
        StoredDoubleTimeSeries ts6 = TimeSeries.createDouble("equipmentTs", index, 1d, 2d, 3d, 4d);
        StringTimeSeries ts7 = TimeSeries.createString("disconnected_ids", index, "id1", "id2", "id1,id2", "");

        // TimeSeriesStore
        Path resDir = Files.createDirectory(fileSystem.getPath("/tmp"));
        tsStore = new FileSystemTimeSeriesStore(resDir);
        tsStore.importTimeSeries(List.of(ts1, ts2), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts1), 2, APPEND);
        tsStore.importTimeSeries(List.of(ts3), -1, APPEND);
        tsStore.importTimeSeries(List.of(ts4), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts5), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts6), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts7), 1, APPEND);

        // Mapping config
        TimeSeriesMappingConfig mappingConfig = getTimeSeriesMappingConfig();

        // Table loader
        tableLoader = new TimeSeriesMappingConfigTableLoader(mappingConfig, tsStore);
    }

    private static TimeSeriesMappingConfig getTimeSeriesMappingConfig() {
        TimeSeriesMappingConfig mappingConfig = new TimeSeriesMappingConfig();
        mappingConfig.setMappedTimeSeriesNames(Set.of("mappedTs"));
        mappingConfig.setTimeSeriesToEquipment(Map.of("equipmentTs", Set.of(new MappingKey(EquipmentVariable.P0, "id"))));
        mappingConfig.setTimeSeriesToPlannedOutagesMapping(Map.of("disconnected_ids", Set.of("id1", "id2")));
        mappingConfig.setDistributionKeys(Map.of(new MappingKey(EquipmentVariable.TARGET_P, "id"), new TimeSeriesDistributionKey("distributionKeyTs")));
        mappingConfig.setTimeSeriesNodes(Map.of("calculatedTs", new TimeSeriesNameNodeCalc("ts1")));
        return mappingConfig;
    }

    @AfterEach
    void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void usedTimeSeriesNamesTest() {
        Iterable<String> usedTimeSeriesNames = tableLoader.findUsedTimeSeriesNames();
        assertThat(usedTimeSeriesNames).hasSize(4).containsExactlyInAnyOrder("mappedTs", "equipmentTs", "distributionKeyTs", "disconnected_ids");

        Set<String> timeSeriesNamesToLoad = tableLoader.findTimeSeriesNamesToLoad();
        assertThat(timeSeriesNamesToLoad).hasSize(4).containsExactlyInAnyOrder("mappedTs", "equipmentTs", "distributionKeyTs", "disconnected_ids");

        Set<String> usedTimeSeriesNamesToLoad = tableLoader.findTimeSeriesNamesToLoad(Set.of("mappedTs", "calculatedTs"));
        assertThat(usedTimeSeriesNamesToLoad).hasSize(2).containsExactlyInAnyOrder("mappedTs", "ts1");
    }

    @Test
    void loadTest() {
        TimeSeriesTable loadToTable = tableLoader.loadToTable(1, tsStore, Range.closed(0, 1), Set.of("ts2", "calculatedTs"));
        assertThat(loadToTable.getTimeSeriesNames()).hasSize(3).containsExactlyInAnyOrder("ts1", "ts2", "calculatedTs");

        TimeSeriesTable loadTable = tableLoader.load(1, Set.of("ts2"), Range.closed(0, 1));
        assertThat(loadTable.getTimeSeriesNames()).hasSize(4).containsExactlyInAnyOrder("distributionKeyTs", "equipmentTs", "mappedTs", "ts2");
    }

    @Test
    void plannedOutagesTest() {
        final String disconnectedIdsTsName = "disconnected_ids";

        // Expected results
        // step index -> disconnected id list
        // 0 : id1
        // 1 : id2
        // 2 : id1, id2
        // 3 : none
        Map<String, double[]> expectedResults = Map.of(
                disconnectedIdsTsName + "_id1", new double[]{0, 1, 0, 1},
                disconnectedIdsTsName + "_id2", new double[]{1, 0, 0, 1});

        // Compute disconnected equipment time series
        StringTimeSeries plannedOutagesTimeSeries = tsStore.getStringTimeSeries(disconnectedIdsTsName, 1).orElseThrow(() -> new TimeSeriesException("Invalid planned outages time series name"));
        List<DoubleTimeSeries> actualDoubleTimeSeries = computeDisconnectedEquipmentTimeSeries(disconnectedIdsTsName, plannedOutagesTimeSeries.toArray(), Set.of("id1", "id2"), index);
        assertThat(actualDoubleTimeSeries).hasSize(2);
        actualDoubleTimeSeries.forEach(ts -> assertTrue(expectedResults.containsKey(ts.getMetadata().getName())));
        actualDoubleTimeSeries.forEach(ts -> assertArrayEquals(expectedResults.get(ts.getMetadata().getName()), ts.toArray()));

        // Build time series store containing disconnected time series
        Map<String, Set<String>> timeSeriesToPlannedOutagesMapping = Map.of(disconnectedIdsTsName, Set.of("id1", "id2"));
        ReadOnlyTimeSeriesStore actualStore = buildPlannedOutagesStore(tsStore, 1, timeSeriesToPlannedOutagesMapping);
        assertTrue(actualStore.timeSeriesExists(disconnectedIdsTsName + "_id1"));
        assertTrue(actualStore.timeSeriesExists(disconnectedIdsTsName + "_id2"));

        // Build store containing initial time series and disconnected time series
        ReadOnlyTimeSeriesStore actualStoreWithPlannedOutages = buildStoreWithPlannedOutages(tsStore, 1, timeSeriesToPlannedOutagesMapping);
        assertTrue(actualStoreWithPlannedOutages.timeSeriesExists(disconnectedIdsTsName + "_id1"));
        assertTrue(actualStoreWithPlannedOutages.timeSeriesExists(disconnectedIdsTsName + "_id2"));
        tsStore.getTimeSeriesNames(null).forEach(ts -> assertTrue(actualStoreWithPlannedOutages.timeSeriesExists(ts)));

        // Same with initial store containing
        ReadOnlyTimeSeriesStore secondActualStoreWithPlannedOutages = buildStoreWithPlannedOutages(actualStoreWithPlannedOutages, 1, timeSeriesToPlannedOutagesMapping);
        assertEquals(actualStoreWithPlannedOutages, secondActualStoreWithPlannedOutages);
    }

    @Test
    void testCheckIndexUnicity() {
        StoredDoubleTimeSeries ts = TimeSeries.createDouble("ts", index, 1d, 2d, 3d, 4d);
        TimeSeriesIndex actualIndex = checkIndexUnicity(new ReadOnlyTimeSeriesStoreCache(List.of(ts)), Set.of("ts"));
        assertEquals(actualIndex, index);

        TimeSeriesIndex actualTableIndex = tableLoader.checkIndexUnicity();
        assertEquals(actualTableIndex, index);

        TimeSeriesIndex actualEmptyIndex = checkIndexUnicity(new ReadOnlyTimeSeriesStoreCache(List.of(ts)), Collections.emptySet());
        assertEquals(InfiniteTimeSeriesIndex.INSTANCE, actualEmptyIndex);

        // It should fail in case of store containing time series with different index
        TimeSeriesIndex otherIndex = RegularTimeSeriesIndex.create(Interval.parse("2015-01-01T01:00:00Z/2015-01-01T02:00:00Z"), Duration.ofHours(1));
        StoredDoubleTimeSeries otherTs = TimeSeries.createDouble("otherTs", otherIndex, 1d, 2d);
        ReadOnlyTimeSeriesStoreCache otherTsStore = new ReadOnlyTimeSeriesStoreCache(List.of(ts, otherTs));
        Set<String> tsSet = Set.of("ts", "otherTs");
        TimeSeriesException exception = assertThrows(TimeSeriesException.class, () -> checkIndexUnicity(otherTsStore, tsSet));
        assertTrue(exception.getMessage().contains("Time series involved in the mapping must have the same index"));
    }

    @Test
    void testCheckValues() {
        // Assertion when it works
        try {
            tableLoader.checkValues(Set.of(1));

            // existingVersions.containsAll(versions)
            checkValues(tsStore, Set.of(1), Set.of("ts1"));

            // isNotVersioned(existingVersions)
            checkValues(tsStore, Set.of(-1), Set.of("ts3"));

            // !existingVersions.isEmpty()
            checkValues(tsStore, Set.of(1), Set.of("ts4"));
        } catch (Exception e) {
            fail();
        }

        // It should fail when a version is missing
        Set<Integer> versions = Set.of(1, 2);
        Set<String> timeSeriesNames = Set.of("ts2");
        TimeSeriesMappingException exception = assertThrows(TimeSeriesMappingException.class, () -> checkValues(tsStore, versions, timeSeriesNames));
        assertEquals("The time series store does not contain values for ts ts2 and version(s) [2]", exception.getMessage());
    }
}
