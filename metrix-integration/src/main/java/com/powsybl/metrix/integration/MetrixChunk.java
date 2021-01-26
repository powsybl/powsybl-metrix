/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.powsybl.computation.*;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.timeseries.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixChunk {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixChunk.class);

    private static final String WORKING_DIR_PREFIX = "metrix_chunk_";
    private static final String VARIANTES_FILE_NAME = "variantes.csv";
    private static final String LOGS_FILE_NAME = "logs.txt";
    private static final String LOGS_FILE_DETAIL_PREFIX = "metrix";
    private static final String LOGS_FILE_DETAIL_SUFFIX = ".log";
    private static final String REMEDIAL_ACTION_FILE_NAME = "parades.csv";
    private static final List<InputFile> FORT_FILE_NAMES = ImmutableList.of(new InputFile("fort.2"),
            new InputFile("fort.44_BIN"),
            new InputFile("fort.45_BIN"),
            new InputFile("fort.46_BIN"),
            new InputFile("fort.47_BIN"),
            new InputFile("fort.48_BIN"));
    private static final String METRIX_COMMAND_ID = "metrix";
    private static final String METRIX_PROGRAM = "metrix";
    private static final String METRIX_LOG_LEVEL_OPT_NAME = "log-level";

    private final Network network;

    private final ComputationManager computationManager;

    private final MetrixConfig config;

    private final Path remedialActionFile;

    private final Path logFile;

    private final Path logFileDetail;

    private final MetrixChunkLogger logger;

    public MetrixChunk(Network network, ComputationManager computationManager, MetrixConfig config, Path remedialActionFile, Path logFile, Path logFileDetail) {
        this(network, computationManager, config, remedialActionFile, logFile, logFileDetail, null);
    }

    public MetrixChunk(Network network, ComputationManager computationManager, MetrixConfig config,
                       Path remedialActionFile, Path logFile, Path logFileDetail, MetrixChunkLogger logger) {
        this.network = Objects.requireNonNull(network);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.config = Objects.requireNonNull(config);
        this.logFile = logFile;
        this.logFileDetail = logFileDetail;
        this.remedialActionFile = remedialActionFile;
        this.logger = logger;
    }

    private static String getLogLevelValue(int level) {
        String value = "info";
        switch (level) {
            case 0:
                value = "trace";
                break;
            case 1:
                value = "debug";
                break;
            case 2:
                value = "info";
                break;
            case 3:
                value = "warning";
                break;
            case 4:
                value = "error";
                break;
            case 5:
                value = "critical";
                break;
            default:
                LOGGER.warn("Unknown Metrix log level value '{}', using default 'info'. Available level range : 0-5", level);
                break;
        }
        return value;
    }

    private void copyDic(Path workingDir, List<InputFile> inputFiles) throws IOException {
        try (Stream<Path> stream = Files.list(config.getHomeDir().resolve("etc"))
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().matches("METRIX(.*)\\.dic"))) {
            stream.forEach(dicFile -> {
                String dicFileName = dicFile.getFileName().toString();
                try {
                    Files.copy(dicFile, workingDir.resolve(dicFileName));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                inputFiles.add(new InputFile(dicFileName));
            });
        }
    }

    private void copyInputFile(Path originPath, String destinationFileName, Path workingDir, List<InputFile> inputFiles) {
        if (originPath != null) {
            try {
                Files.copy(originPath, workingDir.resolve(destinationFileName));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            inputFiles.add(new InputFile(destinationFileName));
        }
    }

    public CompletableFuture<List<TimeSeries>> run(MetrixParameters parameters, ContingenciesProvider contingenciesProvider,
                                                   MetrixDslData metrixDslData, MetrixVariantProvider variantProvider) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(contingenciesProvider);

        Optional<MetrixChunkLogger> optionalLogger = logger != null ? Optional.of(logger) : Optional.empty();
        Map<String, String> variables = ImmutableMap.of("PATH", config.getHomeDir().resolve("bin").toString());

        return computationManager.execute(new ExecutionEnvironment(variables, WORKING_DIR_PREFIX, config.isDebug()),
                new AbstractExecutionHandler<List<TimeSeries>>() {

                    @Override
                    public List<CommandExecution> before(Path workingDir) throws IOException {

                        LOGGER.info("Generating Metrix chunk input data in '{}'", workingDir.toAbsolutePath());

                        List<InputFile> inputFiles = new ArrayList<>(2);
                        inputFiles.addAll(FORT_FILE_NAMES);
                        copyDic(workingDir, inputFiles); // copy METRIX*.dic
                        copyInputFile(remedialActionFile, REMEDIAL_ACTION_FILE_NAME, workingDir, inputFiles); // copy parades.csv

                        List<OutputFile> outputFiles;

                        Command command;

                        if (variantProvider != null) {

                            // create Metrix network

                            MetrixNetwork metrixNetwork = MetrixNetwork.create(network, contingenciesProvider, variantProvider.getMappedBreakers(), parameters, remedialActionFile);

                            int firstVariant = variantProvider.getVariantRange().lowerEndpoint();
                            int lastVariant = variantProvider.getVariantRange().upperEndpoint();
                            int variantCount = lastVariant - firstVariant + 1;

                            optionalLogger.ifPresent(MetrixChunkLogger::beforeVariantsWriting);

                            // write variants
                            MetrixVariantsWriter variantsWriter = new MetrixVariantsWriter(variantProvider, metrixNetwork);
                            variantsWriter.write(Range.closed(firstVariant, lastVariant), workingDir.resolve(VARIANTES_FILE_NAME));

                            optionalLogger.ifPresent(logger -> logger.afterVariantsWriting(variantCount));
                            optionalLogger.ifPresent(MetrixChunkLogger::beforeNetworkWriting);

                            // write DIE
                            new MetrixInputData(metrixNetwork, metrixDslData, parameters)
                                    .write(workingDir, true, config.isConstantLossFactor());

                            optionalLogger.ifPresent(MetrixChunkLogger::afterNetworkWriting);

                            outputFiles = new ArrayList<>(1 + variantCount);
                            outputFiles.add(new OutputFile(LOGS_FILE_NAME));
                            for (int variantNum = firstVariant; variantNum <= lastVariant; variantNum++) {
                                outputFiles.add(new OutputFile(MetrixOutputData.getFileName(variantNum)));
                            }

                            // create command to execute
                            command = new SimpleCommandBuilder()
                                    .id(METRIX_COMMAND_ID)
                                    .program(METRIX_PROGRAM)
                                    .args(LOGS_FILE_NAME,
                                            VARIANTES_FILE_NAME,
                                            MetrixOutputData.FILE_NAME_PREFIX,
                                            Integer.toString(firstVariant),
                                            Integer.toString(variantCount))
                                    .option(METRIX_LOG_LEVEL_OPT_NAME, getLogLevelValue(config.isDebug() ? config.getDebugLogLevel() : config.getNoDebugLogLevel()))
                                    .inputFiles(inputFiles)
                                    .outputFiles(outputFiles)
                                    .build();
                        } else {
                            // create Metrix network
                            MetrixNetwork metrixNetwork = MetrixNetwork.create(network, contingenciesProvider, null, parameters, remedialActionFile);

                            optionalLogger.ifPresent(MetrixChunkLogger::beforeVariantsWriting);

                            // write variants
                            new MetrixVariantsWriter(null, null)
                                    .write(null, workingDir.resolve(VARIANTES_FILE_NAME));

                            optionalLogger.ifPresent(logger -> logger.afterVariantsWriting(0));
                            optionalLogger.ifPresent(MetrixChunkLogger::beforeNetworkWriting);

                            // write DIE
                            new MetrixInputData(metrixNetwork, metrixDslData, parameters)
                                    .write(workingDir, true, config.isConstantLossFactor());

                            optionalLogger.ifPresent(MetrixChunkLogger::afterNetworkWriting);

                            outputFiles = new ArrayList<>(2);
                            outputFiles.add(new OutputFile(MetrixOutputData.getFileName(-1)));
                            outputFiles.add(new OutputFile(LOGS_FILE_NAME));

                            command = new SimpleCommandBuilder()
                                    .id(METRIX_COMMAND_ID)
                                    .program(METRIX_PROGRAM)
                                    .args(LOGS_FILE_NAME,
                                            VARIANTES_FILE_NAME,
                                            MetrixOutputData.FILE_NAME_PREFIX,
                                            Integer.toString(-1),
                                            Integer.toString(1))
                                    .option(METRIX_LOG_LEVEL_OPT_NAME, getLogLevelValue(config.isDebug() ? config.getDebugLogLevel() : config.getNoDebugLogLevel()))
                                    .inputFiles(inputFiles)
                                    .outputFiles(outputFiles)
                                    .build();
                        }

                        // overload HADES_DIR variable with working dir
                        ImmutableMap<String, String> overloadedVariables = ImmutableMap.of("HADES_DIR", ".");

                        ImmutableList<CommandExecution> commandExecutions = ImmutableList.of(new CommandExecution(command, 1, 0, null, overloadedVariables));

                        optionalLogger.ifPresent(MetrixChunkLogger::beforeMetrixExecution);

                        return commandExecutions;
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

                                result.createTimeSeries(variantProvider.getIndex(), results);

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
