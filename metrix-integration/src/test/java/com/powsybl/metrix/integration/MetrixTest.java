/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalCommandExecutor;
import com.powsybl.computation.local.LocalComputationConfig;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.io.MetrixConfigResult;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.metrix.mapping.timeseries.InMemoryTimeSeriesStore;
import com.powsybl.timeseries.InfiniteTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class MetrixTest extends AbstractConverterTest {

    @Test
    public void testMetrix() throws IOException {
        Path workspace = tmpDir.resolve("workspace");
        Files.createDirectories(workspace);
        ComputationManager computationManager = new LocalComputationManager(workspace);

        NetworkSource source = new NetworkSource() {

            @Override
            public Network copy() {
                return NetworkXml.read(MetrixTest.class.getResourceAsStream("/simpleNetwork.xml"));
            }

            @Override
            public void write(OutputStream os) {
                /* noop */
            }
        };

        ResultListener resultListener = new ResultListener() {
            @Override
            public void onChunkResult(int version, int chunk, List<TimeSeries> timeSeriesList) {
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
        Consumer<MetrixConfigResult> metrixDslDataConsumer = MetrixConfigResult::getMetrixTimeSeriesNodes;
        StringWriter logWriter = new StringWriter();

        Supplier<Reader> mappingReader = () -> new StringReader("");
        Supplier<Reader> metrixdslReader = () -> new StringReader("");
        Supplier<Reader> remedialActionReader = () -> new StringReader("");

        InMemoryTimeSeriesStore timeSeriesStore = new InMemoryTimeSeriesStore();
        InMemoryTimeSeriesStore resultStore = new InMemoryTimeSeriesStore();

        try (ZipOutputStream log = new ZipOutputStream(Files.newOutputStream(tmpDir.resolve("logs.gz")))) {
            MetrixMock metrix = new MetrixMock(
                source,
                null,
                mappingReader,
                metrixdslReader,
                remedialActionReader,
                timeSeriesStore,
                resultStore,
                log,
                computationManager,
                appLogger,
                future -> { /* noop */ },
                logWriter,
                metrixDslDataConsumer
            );

            MetrixRunParameters runParameters = new MetrixRunParameters(0, 3, new TreeSet<>(Collections.singleton(1)), 2, true, true);

            MetrixRunResult run = metrix.run(runParameters, resultListener);
            assertThat(run).isNotNull();
            assertThat(logWriter.toString()).isEqualTo("Message\n");
        }
    }

    @Test
    public void testMetrixChunk() throws IOException, ExecutionException, InterruptedException {
        Files.createDirectories(fileSystem.getPath("/tmp", "etc"));
        LocalComputationConfig localComputationConfig = LocalComputationConfig.load(PlatformConfig.defaultConfig(), fileSystem);
        LocalCommandExecutor commandExecutor = new LocalCommandExecutor() {
            @Override
            public int execute(String s, List<String> list, Path path, Path path1, Path path2, Map<String, String> map) throws IOException, InterruptedException {
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

        Network network = NetworkXml.read(MetrixTest.class.getResourceAsStream("/simpleNetwork.xml"));

        Path remedialActionFile = tmpDir.resolve("remedialActionFile.txt");
        Files.createFile(remedialActionFile);
        Path logFile = tmpDir.resolve("log");
        Path logDetailFile = tmpDir.resolve("logDebug");

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
            public void readVariants(Range<Integer> variantReadRange, MetrixVariantReader reader) {

            }
        };

        MetrixConfig metrixConfig = MetrixConfig.load();
        metrixConfig.setHomeDir(fileSystem.getPath("/tmp"));
        MetrixChunk metrixChunk = new MetrixChunk(network, computationManager, metrixConfig, remedialActionFile, logFile, logDetailFile);
        CompletableFuture<List<TimeSeries>> run1 = metrixChunk.run(parameters, contingenciesProvider, dslData, variantProvider);
        CompletableFuture<List<TimeSeries>> run2 = metrixChunk.run(parameters, contingenciesProvider, dslData, null);
        run1.join();
        run2.join();
        assertThat(run1.get()).isNotNull();
        assertThat(run2.get()).isNotNull();
    }
}
