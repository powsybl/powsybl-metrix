/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.io.MetrixConfigResult;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.metrix.integration.metrix.MetrixChunkParam;
import com.powsybl.metrix.integration.metrix.MetrixAnalysisResult;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.metrix.mapping.timeseries.InMemoryTimeSeriesStore;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MetrixTest {
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
    void testMetrix() throws IOException {
        Path workspace = Files.createDirectory(fileSystem.getPath("/tmp"));
        Files.createDirectories(workspace);
        ComputationManager computationManager = new LocalComputationManager(workspace);

        ResultListener resultListener = new ResultListener() {
            @Override
            public void onChunkResult(int version, int chunk, List<TimeSeries> timeSeriesList, Network networkPoint) {
                // default empty implementation
            }

            @Override
            public void onEnd() {
                // default empty implementation
            }
        };

        MetrixAppLogger appLogger = new MetrixAppLogger() {
            @Override
            public void log(String message, Object... args) {

            }

            @Override
            public MetrixAppLogger tagged(String tag) {
                return this;
            }
        };

        Reader remedialActionReader = new StringReader("");

        InMemoryTimeSeriesStore timeSeriesStore = new InMemoryTimeSeriesStore();
        InMemoryTimeSeriesStore resultStore = new InMemoryTimeSeriesStore();

        MetrixDslData dslData = new MetrixDslData();
        TimeSeriesMappingConfig timeSeriesMappingConfig = new TimeSeriesMappingConfig();
        MetrixParameters metrixParameters = new MetrixParameters();
        MappingParameters mappingParameters = new MappingParameters();
        MetrixConfigResult metrixConfigResult = new MetrixConfigResult(new HashMap<>(), new HashMap<>());

        // create test network
        Network network = NetworkSerDe.read(Objects.requireNonNull(getClass().getResourceAsStream("/simpleNetwork.xml")));

        MetrixAnalysisResult analysisResult = new MetrixAnalysisResult(dslData, timeSeriesMappingConfig, network, metrixParameters, mappingParameters, metrixConfigResult, Collections.emptyList(), Collections.emptyList());

        try (ZipOutputStream log = new ZipOutputStream(Files.newOutputStream(fileSystem.getPath("logs.gz")))) {
            MetrixMock metrix = new MetrixMock(
                remedialActionReader,
                timeSeriesStore,
                resultStore,
                log,
                computationManager,
                appLogger,
                analysisResult
            );

            MetrixRunParameters runParameters = new MetrixRunParameters(0, 3, new TreeSet<>(Collections.singleton(1)), 2, true, true, true);

            MetrixRunResult run = metrix.run(runParameters, resultListener, null);
            assertThat(run).isNotNull();
        }
    }

    @Test
    void testMetrixChunk() throws IOException, ExecutionException, InterruptedException {
        Files.createDirectories(fileSystem.getPath("/tmp", "etc"));
        LocalComputationConfig localComputationConfig = LocalComputationConfig.load(PlatformConfig.defaultConfig(), fileSystem);
        LocalCommandExecutor commandExecutor = new LocalCommandExecutor() {
            @Override
            public int execute(String s, List<String> list, Path path, Path path1, Path path2, Map<String, String> map) {
                return 0;
            }

            @Override
            public void stop(Path path) {

            }

            @Override
            public void stopForcibly(Path path) {

            }
        };
        ComputationManager computationManager = new LocalComputationManager(localComputationConfig, commandExecutor, Executors.newSingleThreadExecutor());

        Network network = NetworkSerDe.read(Objects.requireNonNull(MetrixTest.class.getResourceAsStream("/simpleNetwork.xml")));

        Path remedialActionFile = fileSystem.getPath("remedialActionFile.txt");
        Files.createFile(remedialActionFile);
        Path logFile = fileSystem.getPath("log");
        Path logDetailFile = fileSystem.getPath("logDebug");

        MetrixParameters parameters = new MetrixParameters();
        ContingenciesProvider contingenciesProvider = network1 -> Collections.emptyList();
        MetrixDslData dslData = new MetrixDslData();
        MetrixVariantProvider variantProvider = new MetrixVariantProvider() {
            @Override
            public Range<Integer> getVariantRange() {
                return Range.closed(0, 5);
            }

            @Override
            public TimeSeriesIndex getIndex() {
                return new InfiniteTimeSeriesIndex();
            }

            @Override
            public Set<String> getMappedBreakers() {
                return Collections.emptySet();
            }

            @Override
            public void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader, Path workingDir) {

            }
        };

        MetrixConfig metrixConfig = MetrixConfig.load();
        metrixConfig.setHomeDir(fileSystem.getPath("/tmp"));
        metrixConfig.setCommand("metrix-simulator");
        MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder().simpleInit(1, false, false,
                contingenciesProvider, null, logFile, logDetailFile, remedialActionFile).build();
        MetrixChunk metrixChunk = new MetrixChunk(network, computationManager, metrixChunkParam, metrixConfig, null);
        CompletableFuture<List<TimeSeries>> run1 = metrixChunk.run(parameters, dslData, variantProvider);
        CompletableFuture<List<TimeSeries>> run2 = metrixChunk.run(parameters, dslData, null);
        run1.join();
        run2.join();
        assertThat(run1.get()).isNotNull();
        assertThat(run2.get()).isNotNull();
    }
}
