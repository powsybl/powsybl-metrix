/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration.chunk;

import com.powsybl.computation.*;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.MetrixConfig;
import com.powsybl.metrix.integration.MetrixDslData;
import com.powsybl.metrix.integration.MetrixParameters;
import com.powsybl.metrix.integration.network.MetrixVariantProvider;
import com.powsybl.metrix.integration.dataGenerator.MetrixInputDataGenerator;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.integration.exceptions.MetrixException;
import com.powsybl.timeseries.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.powsybl.metrix.integration.dataGenerator.MetrixInputDataGenerator.LODF_MATRIX_FILE_NAME;
import static com.powsybl.metrix.integration.dataGenerator.MetrixInputDataGenerator.LOGS_FILE_NAME;
import static com.powsybl.metrix.integration.dataGenerator.MetrixInputDataGenerator.PTDF_MATRIX_FILE_NAME;
import static com.powsybl.metrix.integration.timeseries.InitOptimizedTimeSeriesWriter.INPUT_OPTIMIZED_FILE_NAME;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixChunk {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixChunk.class);

    private static final String WORKING_DIR_PREFIX = "metrix_chunk_";
    private static final String LOGS_FILE_DETAIL_PREFIX = "metrix";
    private static final String LOGS_FILE_DETAIL_SUFFIX = ".log";

    private final Network network;

    private final ComputationManager computationManager;

    private final MetrixConfig config;

    private final MetrixChunkLogger metrixChunkLogger;

    private final MetrixChunkParam metrixChunkParam;

    public MetrixChunk(Network network, ComputationManager computationManager, MetrixChunkParam metrixChunkParam, MetrixConfig config, MetrixChunkLogger metrixChunkLogger) {
        this.network = Objects.requireNonNull(network);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.config = Objects.requireNonNull(config);
        this.metrixChunkLogger = metrixChunkLogger;
        this.metrixChunkParam = metrixChunkParam;
    }

    public CompletableFuture<List<TimeSeries>> run(MetrixParameters parameters, MetrixDslData metrixDslData, MetrixVariantProvider variantProvider) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(metrixChunkParam.contingenciesProvider);

        Optional<MetrixChunkLogger> optionalLogger = metrixChunkLogger != null ? Optional.of(metrixChunkLogger) : Optional.empty();
        Map<String, String> variables = Map.of("PATH", config.getHomeDir().resolve("bin").toString());

        return computationManager.execute(new ExecutionEnvironment(variables, WORKING_DIR_PREFIX, config.isDebug()),
            new AbstractExecutionHandler<>() {

                @Override
                public List<CommandExecution> before(Path workingDir) throws IOException {
                    List<CommandExecution> commands = new MetrixInputDataGenerator(config, workingDir, metrixChunkLogger).generateMetrixInputData(
                        variantProvider, network, parameters, metrixDslData, metrixChunkParam);
                    optionalLogger.ifPresent(MetrixChunkLogger::beforeMetrixExecution);
                    return commands;

                }

                @Override
                public List<TimeSeries> after(Path workingDir, ExecutionReport report) throws IOException {
                    List<TimeSeries> results = new ArrayList<>();

                    optionalLogger.ifPresent(MetrixChunkLogger::afterMetrixExecution);

                    if (report.getErrors().isEmpty()) {
                        optionalLogger.ifPresentOrElse(logger -> parseResults(workingDir, results, variantProvider, logger),
                            () -> parseResults(workingDir, results, variantProvider));
                    } else {
                        report.log();
                    }

                    // Retrieve log file
                    retrieveLogFile(workingDir);

                    // Retrieve PTDF and LODF matrix files
                    retrieveFile(workingDir, PTDF_MATRIX_FILE_NAME);
                    retrieveFile(workingDir, LODF_MATRIX_FILE_NAME);

                    // Retrieve network point file
                    copyNetworkPointFile(workingDir);

                    if (metrixChunkParam.logFileDetail != null) {
                        int i = 0;
                        Path sourcePath;
                        while (Files.exists(sourcePath = workingDir.resolve(LOGS_FILE_DETAIL_PREFIX + String.format("%03d", i) + LOGS_FILE_DETAIL_SUFFIX))) {
                            Files.copy(sourcePath, Paths.get(String.format(metrixChunkParam.logFileDetail.toString(), i)));
                            i++;
                        }
                    }

                    return results;
                }
            });
    }

    private void copyNetworkPointFile(Path workingDir) throws IOException {
        if (metrixChunkParam.networkPointFile != null) {
            try (Stream<Path> paths = Files.list(workingDir)) {
                List<Path> files = paths.filter(path -> path.getFileName().toString().matches(Pattern.quote(network.getId()) + "(.*)\\.xiidm")).toList();
                if (files.size() != 1) {
                    LOGGER.error("More than one network point files '{}'", files.size());
                    throw new MetrixException("More than one network point files " + files.size());
                } else {
                    Files.copy(workingDir.resolve(files.getFirst()), metrixChunkParam.networkPointFile);
                }
            }
        }
    }

    private void retrieveLogFile(Path workingDir) throws IOException {
        if (metrixChunkParam.logFile != null) {
            if (Files.exists(workingDir.resolve(LOGS_FILE_NAME))) {
                Files.copy(workingDir.resolve(LOGS_FILE_NAME), metrixChunkParam.logFile);
            } else {
                LOGGER.warn("Failed to retrieve metrix main log file !");
            }
        }
    }

    private void retrieveFile(Path workingDir, String fileName) throws IOException {
        if (Files.exists(workingDir.resolve(fileName + ".gz"))) {
            Files.copy(workingDir.resolve(fileName), Path.of(fileName));
        } else {
            LOGGER.warn("Failed to retrieve metrix '{}' file !", fileName);
        }
    }

    private int parseResults(Path workingDir, List<TimeSeries> results, MetrixVariantProvider variantProvider) {
        int variantCount;
        if (variantProvider != null) {
            int firstVariant = variantProvider.getVariantRange().lowerEndpoint();
            int lastVariant = variantProvider.getVariantRange().upperEndpoint();
            variantCount = lastVariant - firstVariant + 1;

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
        } else {
            variantCount = 0;
        }
        return variantCount;
    }

    private void parseResults(Path workingDir, List<TimeSeries> results, MetrixVariantProvider variantProvider,
                              MetrixChunkLogger logger) {
        logger.beforeResultParsing();

        int variantCount = parseResults(workingDir, results, variantProvider);
        logger.afterResultParsing(variantCount);
    }
}
