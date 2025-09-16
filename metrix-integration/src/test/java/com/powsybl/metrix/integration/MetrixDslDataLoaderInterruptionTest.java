package com.powsybl.metrix.integration;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.computation.AbstractTaskInterruptionTest;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.data.datatable.DataTableStore;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStoreCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Nicolas Rol {@literal <nicolas.rol at rte-france.com>}
 */
class MetrixDslDataLoaderInterruptionTest extends AbstractTaskInterruptionTest {

    private FileSystem fileSystem;
    private Path dslFile;
    private Path mappingFile;
    private Network network;
    private final MetrixParameters parameters = new MetrixParameters();
    private final MappingParameters mappingParameters = MappingParameters.load();

    @BeforeEach
    void setUp() throws Exception {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        dslFile = fileSystem.getPath("/test.dsl");
        mappingFile = fileSystem.getPath("/mapping.dsl");
        network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        // Create mapping file for use in all tests
        try (Writer writer = Files.newBufferedWriter(mappingFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "timeSeries['tsN'] = 1000",
                "timeSeries['tsN_1'] = 2000",
                "timeSeries['tsITAM'] = 3000",
                "timeSeries['ts1'] = 100",
                "timeSeries['ts2'] = 200",
                "timeSeries['ts3'] = 300",
                "timeSeries['ts4'] = 400",
                "timeSeries['ts5'] = 500"
            ));
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        fileSystem.close();
    }

    @ParameterizedTest
    @Timeout(10)
    @ValueSource(booleans = {false, true})
    void testCancelMetrixDslDataLoaderShort(boolean isDelayed) throws Exception {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write(String.join(System.lineSeparator(),
                "load('FVALDI11_L') {",
                "   preventiveSheddingPercentage 20",
                "}",
                "load('FVALDI11_L2') {",
                "   preventiveSheddingPercentage 30",
                "   preventiveSheddingCost 12000",
                "}",
                "load('FVERGE11_L') {",
                "   preventiveSheddingPercentage 0",
                "   preventiveSheddingCost 10000",
                "}",
                "load('FSSV.O11_L') {",
                "   curativeSheddingPercentage 40",
                "}"));
        }
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        testCancelShortTask(isDelayed, () -> MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig));
    }

    @ParameterizedTest
    @Timeout(10)
    @ValueSource(booleans = {false, true})
    void testCancelMetrixDslDataLoaderLong(boolean isDelayed) throws IOException, InterruptedException {

        try (Writer writer = Files.newBufferedWriter(dslFile, StandardCharsets.UTF_8)) {
            writer.write("""
                for (int i = 0; i < 10; i++) {
                    sleep(500)
                    print(i)
                }
                """ + String.join(System.lineSeparator(),
                "load('FVALDI11_L') {",
                "   preventiveSheddingPercentage 20",
                "}",
                "load('FVALDI11_L2') {",
                "   preventiveSheddingPercentage 30",
                "   preventiveSheddingCost 12000",
                "}",
                "load('FVERGE11_L') {",
                "   preventiveSheddingPercentage 0",
                "   preventiveSheddingCost 10000",
                "}",
                "load('FSSV.O11_L') {",
                "   curativeSheddingPercentage 40",
                "}"));
        }
        ReadOnlyTimeSeriesStore store = new ReadOnlyTimeSeriesStoreCache();
        TimeSeriesMappingConfig tsConfig = new TimeSeriesDslLoader(mappingFile).load(network, mappingParameters, store, new DataTableStore(), null);
        testCancelLongTask(isDelayed, () -> MetrixDslDataLoader.load(dslFile, network, parameters, store, new DataTableStore(), tsConfig));
    }
}
