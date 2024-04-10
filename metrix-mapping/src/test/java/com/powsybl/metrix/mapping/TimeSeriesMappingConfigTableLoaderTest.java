package com.powsybl.metrix.mapping;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.metrix.mapping.timeseries.FileSystemTimeseriesStore;
import com.powsybl.timeseries.RegularTimeSeriesIndex;
import com.powsybl.timeseries.StoredDoubleTimeSeries;
import com.powsybl.timeseries.TimeSeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static com.powsybl.metrix.mapping.TimeSeriesMappingConfigTableLoader.checkValues;
import static com.powsybl.metrix.mapping.timeseries.FileSystemTimeseriesStore.ExistingFiles.APPEND;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
class TimeSeriesMappingConfigTableLoaderTest {
    private FileSystem fileSystem;

    @BeforeEach
    public void setUp() {
        this.fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.fileSystem.close();
    }

    @Test
    void testCheckValues() throws IOException {
        // TimeSeries index
        Instant now = Instant.ofEpochMilli(978303600000L);
        RegularTimeSeriesIndex index = RegularTimeSeriesIndex.create(now, now.plus(2, ChronoUnit.HOURS), Duration.ofHours(1));

        // TimeSeries
        StoredDoubleTimeSeries ts1 = TimeSeries.createDouble("ts1", index, 1d, 2d, 3d);
        StoredDoubleTimeSeries ts2 = TimeSeries.createDouble("ts2", index, 1d, 3d, 5d);
        StoredDoubleTimeSeries ts3 = TimeSeries.createDouble("ts3", index, 1d, 3d, 5d);

        // TimeSeriesStore
        Path resDir = Files.createDirectory(fileSystem.getPath("/tmp"));
        FileSystemTimeseriesStore tsStore = new FileSystemTimeseriesStore(resDir);
        tsStore.importTimeSeries(List.of(ts1, ts2), 1, APPEND);
        tsStore.importTimeSeries(List.of(ts1), 2, APPEND);
        tsStore.importTimeSeries(List.of(ts3), -1, APPEND);

        // Assertion when it works
        try {
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
