/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.ImmutableMap;
import com.powsybl.computation.*;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputDataGenerator;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.integration.exceptions.MetrixException;
import com.powsybl.metrix.integration.metrix.MetrixChunkParam;
import com.powsybl.timeseries.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.metrix.integration.timeseries.InitOptimizedTimeSeriesWriter.INPUT_OPTIMIZED_FILE_NAME;

public class MetrixChunk {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixChunk.class);

    private static final String WORKING_DIR_PREFIX = "metrix_chunk_";
    private static final String LOGS_FILE_NAME = "logs.txt";
    private static final String LOGS_FILE_DETAIL_PREFIX = "metrix";
    private static final String LOGS_FILE_DETAIL_SUFFIX = ".log";

    private final Network network;

    private final ComputationManager computationManager;

    private final MetrixConfig config;

    private final Path remedialActionFile;

    private final Path logFile;

    private final Path logFileDetail;

    private final Path networkPointFile;

    private final MetrixChunkLogger metrixChunkLogger;

    private final ContingenciesProvider contingenciesProvider;

    public MetrixChunk(Network network, ComputationManager computationManager, MetrixChunkParam metrixChunkParam, MetrixConfig config, MetrixChunkLogger metrixChunkLogger) {
        this.network = Objects.requireNonNull(network);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.config = Objects.requireNonNull(config);
        this.logFile = metrixChunkParam.logFile;
        this.logFileDetail = metrixChunkParam.logFileDetail;
        this.networkPointFile = metrixChunkParam.networkPointFile;
        this.remedialActionFile = metrixChunkParam.remedialActionsFile;
        this.contingenciesProvider = metrixChunkParam.contingenciesProvider;
        this.metrixChunkLogger = metrixChunkLogger;
    }

    public CompletableFuture<List<TimeSeries>> run(MetrixParameters parameters, MetrixDslData metrixDslData, MetrixVariantProvider variantProvider) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(contingenciesProvider);

        Optional<MetrixChunkLogger> optionalLogger = metrixChunkLogger != null ? Optional.of(metrixChunkLogger) : Optional.empty();
        Map<String, String> variables = ImmutableMap.of("PATH", config.getHomeDir().resolve("bin").toString());

        return computationManager.execute(new ExecutionEnvironment(variables, WORKING_DIR_PREFIX, config.isDebug()),
                new AbstractExecutionHandler<List<TimeSeries>>() {

                    @Override
                    public List<CommandExecution> before(Path workingDir) throws IOException {
                        List<CommandExecution> commandes = new MetrixInputDataGenerator(config, workingDir, metrixChunkLogger).generateMetrixInputData(
                                remedialActionFile, variantProvider, network, contingenciesProvider, parameters, metrixDslData);
                        optionalLogger.ifPresent(MetrixChunkLogger::beforeMetrixExecution);
                        return commandes;

                    }

                    @Override
                    public List<TimeSeries> after(Path workingDir, ExecutionReport report) throws IOException {
                        List<TimeSeries> results = new ArrayList<>();

                        optionalLogger.ifPresent(MetrixChunkLogger::afterMetrixExecution);

                        if (report.getErrors().isEmpty()) {
                            optionalLogger.ifPresent(MetrixChunkLogger::beforeResultParsing);

                            if (variantProvider != null) {
                                int firstVariant = variantProvider.getVariantRange().lowerEndpoint();
                                int lastVariant = variantProvider.getVariantRange().upperEndpoint();
                                int variantCount = lastVariant - firstVariant + 1;

                                MetrixOutputData result = new MetrixOutputData(firstVariant, variantCount);
                                for (int variantNum = firstVariant; variantNum <= lastVariant; variantNum++) {
                                    result.readFile(workingDir, variantNum);
                                }

                                List<TimeSeries> initOptimizedTimeSeriesList = new ArrayList<>();
                                Path initOptimizedFilePath = workingDir.resolve(INPUT_OPTIMIZED_FILE_NAME);
                                if (Files.exists(initOptimizedFilePath)) {
                                    initOptimizedTimeSeriesList = TimeSeries.parseJson(initOptimizedFilePath);
                                }

                                result.createTimeSeries(variantProvider.getIndex(), initOptimizedTimeSeriesList, results);

                                optionalLogger.ifPresent(logger -> logger.afterResultParsing(variantCount));
                            } else {
                                optionalLogger.ifPresent(logger -> logger.afterResultParsing(0));
                            }
                        } else {
                            report.log();
                        }

                        if (logFile != null) {
                            if (Files.exists(workingDir.resolve(LOGS_FILE_NAME))) {
                                Files.copy(workingDir.resolve(LOGS_FILE_NAME), logFile);
                            } else {
                                LOGGER.warn("Failed to retrieve metrix main log file !");
                            }
                        }

                        if (networkPointFile != null) {
                            try (Stream<Path> paths = Files.list(workingDir)) {
                                List<Path> files = paths.filter(path -> path.getFileName().toString().matches(network.getId() + "(.*)\\.xiidm")).collect(Collectors.toList());
                                if (files.size() != 1) {
                                    LOGGER.error("More than one network point files '{}'", files.size());
                                    throw new MetrixException("More than one network point files " + files.size());
                                } else {
                                    Files.copy(workingDir.resolve(files.get(0)), networkPointFile);
                                }
                            }
                        }

                        if (logFileDetail != null) {
                            int i = 0;
                            Path sourcePath;
                            while (Files.exists(sourcePath = workingDir.resolve(LOGS_FILE_DETAIL_PREFIX + String.format("%03d", i) + LOGS_FILE_DETAIL_SUFFIX))) {
                                Files.copy(sourcePath, Paths.get(String.format(logFileDetail.toString(), i)));
                                i++;
                            }
                        }

                        return results;
                    }
                });
    }
}
