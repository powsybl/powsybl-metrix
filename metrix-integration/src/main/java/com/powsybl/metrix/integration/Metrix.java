/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Range;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.computation.*;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.json.ContingencyJsonModule;
import com.powsybl.iidm.export.ExportOptions;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.commons.BatchCompletableFuture;
import com.powsybl.metrix.commons.Configuration;
import com.powsybl.metrix.commons.MetrixAppLogger;
import com.powsybl.metrix.commons.scripts.MappingScriptLoadingException;
import com.powsybl.metrix.commons.scripts.MetrixScriptLoadingException;
import com.powsybl.metrix.mapping.*;
import com.powsybl.metrix.mapping.json.TimeSeriesMappingConfigJsonModule;
import com.powsybl.timeseries.DoubleTimeSeries;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import com.powsybl.timeseries.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.powsybl.metrix.integration.MetrixComputationType.LF;

public class Metrix {

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrix.class);

    private static final String DEFAULT_SCHEMA_NAME = "default";
    private static final String INPUT_TIME_SERIES_FILE_NAME_PREFIX = "input_time_series_";
    private static final String OUTPUT_TIME_SERIES_FILE_NAME_PREFIX = "output_time_series_";
    private static final String NETWORK_XIIDM = "network.xiidm";
    private static final String NETWORK_XIIDM_GZ = NETWORK_XIIDM + ".gz";
    private static final String MAPPING_CONFIG_JSON = "mappingConfig.json";
    private static final String MAPPING_CONFIG_JSON_GZ = MAPPING_CONFIG_JSON + ".gz";
    private static final String MAPPING_PARAMETERS_JSON = "mappingParameters.json";
    private static final String MAPPING_PARAMETERS_JSON_GZ = MAPPING_PARAMETERS_JSON + ".gz";
    private static final String METRIX_DSL_DATA_JSON = "metrixDslData.json";
    private static final String METRIX_DSL_DATA_JSON_GZ = METRIX_DSL_DATA_JSON + ".gz";
    private static final String METRIX_PARAMETERS_JSON = "metrixParameters.json";
    private static final String METRIX_PARAMETERS_JSON_GZ = METRIX_PARAMETERS_JSON + ".gz";
    private static final String REMEDIAL_ACTIONS_CSV = "remedialActions.csv";
    private static final String REMEDIAL_ACTIONS_CSV_GZ = REMEDIAL_ACTIONS_CSV + ".gz";
    private static final String CONTINGENCIES_JSON = "contingencies.json";
    private static final String CONTINGENCIES_JSON_GZ = CONTINGENCIES_JSON + ".gz";

    private static final String LOG_FILE_PREFIX = "log";
    private static final String METRIX_CHUNK_ID = "metrix-chunk-";

    private static final long LF_COMPUTATION_TIMEOUT = 2700L;
    private static final int MAX_PARALLEL_LONG_COMPUTATIONS = 3;

    public static final String MAX_THREAT_PREFIX = MetrixOutputData.MAX_THREAT_NAME + "1_FLOW_";
    public static final String BASECASE_LOAD_PREFIX = "basecaseLoad_";
    public static final String BASECASE_OVERLOAD_PREFIX = "basecaseOverload_";
    public static final String OUTAGE_LOAD_PREFIX = "outageLoad_";
    public static final String OUTAGE_OVERLOAD_PREFIX = "outageOverload_";
    public static final String OVERALL_OVERLOAD_PREFIX = "overallOverload_";

    public interface ResultListener {

        void onBegin();

        void onVersionResultBegin(int version);

        void onChunkResult(int version, int chunk, List<TimeSeries> timeSeriesList);

        void onVersionResultEnd(int version);

        void onEnd();
    }

    public static class DefaultResultListener implements ResultListener {

        @Override
        public void onBegin() {
            // default empty implementation
        }

        @Override
        public void onVersionResultBegin(int version) {
            // default empty implementation
        }

        @Override
        public void onChunkResult(int version, int chunk, List<TimeSeries> timeSeriesList) {
            // default empty implementation
        }

        @Override
        public void onVersionResultEnd(int version) {
            // default empty implementation
        }

        @Override
        public void onEnd() {
            // default empty implementation
        }
    }

    private final NetworkSource networkSource;

    private final Supplier<Reader> mappingReaderSupplier;

    private final ContingenciesProvider contingenciesProvider;

    private final Supplier<Reader> metrixDslReaderSupplier;

    private final Supplier<Reader> remedialActionsReaderSupplier;

    private final ReadOnlyTimeSeriesStore store;

    private final ReadOnlyTimeSeriesStore resultStore;

    private final ZipOutputStream logArchive;

    private final ComputationManager computationManager;

    private final MetrixAppLogger appLogger;

    private TimeSeriesMappingConfig mappingConfig = null;

    private MetrixDslData metrixDslData = null;

    private Consumer<Future> updateTask;

    private Consumer<MetrixDslData> onResult;

    private Writer logWriter;

    private BiFunction<MetrixParameters, List<String>, ComputationParameters> computationParamSupplier;

    public Metrix(NetworkSource networkSource, ContingenciesProvider contingenciesProvider, Supplier<Reader> mappingReaderSupplier,
                  Supplier<Reader> metrixDslReaderSupplier, Supplier<Reader> remedialActionsReaderSupplier,
                  ReadOnlyTimeSeriesStore store, ReadOnlyTimeSeriesStore resultStore, ZipOutputStream logArchive, ComputationManager computationManager,
                  MetrixAppLogger appLogger, Consumer<Future> updateTask) {
        this(networkSource, contingenciesProvider, mappingReaderSupplier, metrixDslReaderSupplier, remedialActionsReaderSupplier, store, resultStore, logArchive, computationManager, appLogger, updateTask, null, null, null);
    }

    public Metrix(NetworkSource networkSource, ContingenciesProvider contingenciesProvider, Supplier<Reader> mappingReaderSupplier,
                  Supplier<Reader> metrixDslReaderSupplier, Supplier<Reader> remedialActionsReaderSupplier,
                  ReadOnlyTimeSeriesStore store, ReadOnlyTimeSeriesStore resultStore, ZipOutputStream logArchive, ComputationManager computationManager,
                  MetrixAppLogger appLogger, Consumer<Future> updateTask, Writer logWriter, Consumer<MetrixDslData> onResult, BiFunction<MetrixParameters, List<String>, ComputationParameters> computationParamSupplier) {
        this.networkSource = Objects.requireNonNull(networkSource);
        this.mappingReaderSupplier = Objects.requireNonNull(mappingReaderSupplier);
        this.contingenciesProvider = contingenciesProvider;
        this.metrixDslReaderSupplier = metrixDslReaderSupplier;
        this.remedialActionsReaderSupplier = remedialActionsReaderSupplier;
        this.store = Objects.requireNonNull(store);
        this.resultStore = Objects.requireNonNull(resultStore);
        this.logArchive = logArchive;
        this.computationManager = Objects.requireNonNull(computationManager);
        this.appLogger = Objects.requireNonNull(appLogger);
        this.updateTask = Objects.requireNonNull(updateTask);
        this.logWriter = logWriter;
        this.onResult = onResult;
        this.computationParamSupplier = computationParamSupplier;
    }

    private static String getInputTimeSeriesFileName(int version, int chunk) {
        return INPUT_TIME_SERIES_FILE_NAME_PREFIX + version + "_" + chunk + ".json";
    }

    private static String getInputTimeSeriesFileNameGz(int version, int chunk) {
        return getInputTimeSeriesFileName(version, chunk) + ".gz";
    }

    private static String getOutputTimeSeriesFileName(int version, int chunk) {
        return OUTPUT_TIME_SERIES_FILE_NAME_PREFIX + version + "_" + chunk + ".json";
    }

    private static String getOutputTimeSeriesFileNameGz(int version, int chunk) {
        return getOutputTimeSeriesFileName(version, chunk) + ".gz";
    }

    private void splitTimeSeries(Path workingDir, int version, int chunkSize, String schemaName) {
        appLogger.tagged("info")
                .log("Starting time series (version %d) split", version);

        Stopwatch stopwatch = Stopwatch.createStarted();

        List<DoubleTimeSeries> timeSeriesList = store.getDoubleTimeSeries(version);
        List<List<DoubleTimeSeries>> split = TimeSeries.split(timeSeriesList, chunkSize);
        for (int chunk = 0; chunk < split.size(); chunk++) {
            Path inputTimeSeriesFileGz = workingDir.resolve(getInputTimeSeriesFileNameGz(version, chunk));
            try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(inputTimeSeriesFileGz))), StandardCharsets.UTF_8)) {
                TimeSeries.writeJson(writer, split.get(chunk));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        appLogger.tagged("performance")
                .log("[%s] Time series (version %d) split in %d chunks in %d ms",
                        schemaName, version, split.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    private static void compress(Supplier<Reader> readerSupplier, WorkingDirectory directory, String fileNameGz) {
        try (BufferedReader reader = new BufferedReader(readerSupplier.get());
             Writer writer = new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(directory.toPath().resolve(fileNameGz)))), StandardCharsets.UTF_8)) {
            CharStreams.copy(reader, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int computeChunkSize(MetrixRunParameters runParameters, MetrixConfig metrixConfig, TimeSeriesIndex index) {
        if (runParameters.getChunkSize() != -1 && runParameters.getChunkSize() < index.getPointCount()) {
            return runParameters.getChunkSize();
        } else if (metrixConfig.getChunkSize() != -1 && metrixConfig.getChunkSize() < index.getPointCount()) {
            return metrixConfig.getChunkSize();
        } else {
            return index.getPointCount();
        }
    }

    public static TimeSeriesMappingConfig loadMappingConfig(
            Supplier<Reader> mappingReaderSupplier,
            String schemaName,
            Network network,
            MappingParameters mappingParameters,
            ReadOnlyTimeSeriesStore store,
            Writer writer,
            MetrixAppLogger appLogger,
            Consumer<Future> updateTask,
            ComputationRange computationRange) {
        appLogger.tagged("info")
                .log("[%s] Loading time series mapping...", schemaName);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Reader mappingReader = mappingReaderSupplier.get()) {
            CompletableFuture<TimeSeriesMappingConfig> mappingFuture = CompletableFuture.supplyAsync(() -> TimeSeriesDslLoader.load(mappingReader, network, mappingParameters, store, writer, computationRange));
            updateTask.accept(mappingFuture);
            stopwatch.stop();
            TimeSeriesMappingConfig mappingConfig = mappingFuture.get();
            appLogger.tagged("performance")
                    .log("[%s] Time series mapping loaded in %d ms", schemaName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return mappingConfig;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new MappingScriptLoadingException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Mapping has been interrupted!", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            updateTask.accept(null);
        }
    }

    public static MetrixDslData loadMetrixDslData(Supplier<Reader> metrixDslReaderSupplier, String schemaName, Network network, MetrixParameters metrixParameters, ReadOnlyTimeSeriesStore store, TimeSeriesMappingConfig mappingConfig, Writer writer, MetrixAppLogger appLogger, Consumer<Future> updateTask) {
        appLogger.tagged("info")
                .log("[%s] Loading metrix dsl...", schemaName);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try (Reader metrixDslReader = metrixDslReaderSupplier.get()) {
            CompletableFuture<MetrixDslData> metrixFuture = CompletableFuture.supplyAsync(() -> MetrixDslDataLoader.load(metrixDslReader, network, metrixParameters, store, mappingConfig, writer));
            updateTask.accept(metrixFuture);
            stopwatch.stop();
            MetrixDslData metrixDslData = metrixFuture.get();
            metrixDslData.setComputationType(metrixParameters.getComputationType());
            appLogger.tagged("performance")
                    .log("[%s] Metrix dsl data loaded in %d ms", schemaName, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return  metrixDslData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new MetrixScriptLoadingException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Metrix dsl loading has been interrupted!", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            updateTask.accept(null);
        }
    }

    public MetrixRunResult run(MetrixRunParameters runParameters, ResultListener listener) {
        return run(runParameters, listener, DEFAULT_SCHEMA_NAME);
    }

    public MetrixRunResult run(MetrixRunParameters runParameters, ResultListener listener, String nullableSchemaName) {
        Objects.requireNonNull(runParameters);
        Objects.requireNonNull(listener);
        String schemaName = nullableSchemaName != null ? nullableSchemaName : DEFAULT_SCHEMA_NAME;

        MetrixConfig metrixConfig = MetrixConfig.load();
        MetrixParameters metrixParameters = MetrixParameters.load();
        MappingParameters mappingParameters = MappingParameters.load();

        Network network = networkSource.copy();

        if (remedialActionsReaderSupplier != null) {
            MetrixNetwork.checkCSVRemedialActionFile(remedialActionsReaderSupplier);
        }

        try (BufferedWriter writer = logWriter != null ? new BufferedWriter(logWriter) : null) {
            if (writer != null) {
                writer.write("Message");
                writer.newLine();
            }
            ComputationRange computationRange = new ComputationRange(runParameters.getVersions(), runParameters.getFirstVariant(), runParameters.getVariantCount());
            mappingConfig = loadMappingConfig(mappingReaderSupplier, schemaName, network, mappingParameters, store, writer, appLogger, updateTask, computationRange);
            if (metrixDslReaderSupplier != null) {
                metrixDslData = loadMetrixDslData(metrixDslReaderSupplier, schemaName, network, metrixParameters, store, mappingConfig, writer, appLogger, updateTask);
                if (onResult != null) {
                    onResult.accept(metrixDslData);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int estimatedResultNumber = metrixDslData.minResultNumberEstimate(metrixParameters);
        if (estimatedResultNumber > metrixConfig.getResultNumberLimit()) {
            throw new PowsyblException(String.format("Metrix configuration will produce more result time-series (%d) than the maximum allowed (%d).\n" +
                    "Reduce the number of monitored branches and/or number of contingencies.", estimatedResultNumber, metrixConfig.getResultNumberLimit()));
        }

        TimeSeriesIndex timeSeriesMappingIndex = mappingConfig.checkIndexUnicity(store);
        mappingConfig.checkValues(store, runParameters.getVersions());

        int firstVariant;
        if (runParameters.getFirstVariant() != -1) {
            if (runParameters.getFirstVariant() < 0 || runParameters.getFirstVariant() > timeSeriesMappingIndex.getPointCount() - 1) {
                throw new IllegalArgumentException("First variant is out of range [0, "
                        + (timeSeriesMappingIndex.getPointCount() - 1) + "]");
            }
            firstVariant = runParameters.getFirstVariant();
        } else {
            firstVariant = 0;
        }

        int lastVariant;
        if (runParameters.getVariantCount() != -1) {
            lastVariant = firstVariant + runParameters.getVariantCount() - 1;
            if (lastVariant > timeSeriesMappingIndex.getPointCount() - 1) {
                lastVariant = timeSeriesMappingIndex.getPointCount() - 1;
            }
        } else {
            lastVariant = timeSeriesMappingIndex.getPointCount() - 1;
        }

        int chunkSize = computeChunkSize(runParameters, metrixConfig, timeSeriesMappingIndex);

        ChunkCutter chunkCutter = new ChunkCutter(firstVariant, lastVariant, chunkSize);
        int chunkCount = chunkCutter.getChunkCount();

        LOGGER.info("Running metrix {} on network {}", metrixParameters.getComputationType(), network.getName());
        appLogger.log("[%s] Running metrix", schemaName);

        try (WorkingDirectory commonWorkingDir = new WorkingDirectory(computationManager.getLocalDir(), "metrix-commons-", metrixConfig.isDebug())) {

            // compress data
            try (OutputStream os = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(commonWorkingDir.toPath().resolve(NETWORK_XIIDM_GZ))))) {
                ExportOptions exportOptions = new ExportOptions();
                exportOptions.setVersion(Configuration.load().getNetworkExportVersion());
                NetworkXml.write(network, exportOptions, os);
            }

            if (contingenciesProvider != null) {
                ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                        .registerModule(new ContingencyJsonModule());
                List<Contingency> contingencies = contingenciesProvider.getContingencies(network);
                try (OutputStream os = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(commonWorkingDir.toPath().resolve(CONTINGENCIES_JSON_GZ))))) {
                    objectMapper.writeValue(os, contingencies);
                }
            }

            ComputationParameters params = computationParamSupplier != null ? computationParamSupplier.apply(metrixParameters,
                    runParameters
                            .getVersions()
                            .stream()
                            .map(version -> METRIX_CHUNK_ID + version)
                            .collect(Collectors.toList())
            ) : ComputationParameters.empty();

            try (OutputStream os = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(commonWorkingDir.toPath().resolve(MAPPING_CONFIG_JSON_GZ))))) {
                final ObjectMapper objectMapper = JsonUtil.createObjectMapper()
                        .registerModule(new TimeSeriesMappingConfigJsonModule());
                objectMapper.writeValue(os, mappingConfig);
            }

            try (OutputStream os = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(commonWorkingDir.toPath().resolve(METRIX_DSL_DATA_JSON_GZ))))) {
                final ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(os, metrixDslData);
            }
            try (OutputStream os = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(commonWorkingDir.toPath().resolve(METRIX_PARAMETERS_JSON_GZ))))) {
                final ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(os, metrixParameters);
            }
            try (OutputStream os = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(commonWorkingDir.toPath().resolve(MAPPING_PARAMETERS_JSON_GZ))))) {
                final ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(os, mappingParameters);
            }
            if (remedialActionsReaderSupplier != null) {
                compress(remedialActionsReaderSupplier, commonWorkingDir, REMEDIAL_ACTIONS_CSV_GZ);
            }

            List<CompletableFuture> futures = new ArrayList<>();
            List<Command> commands = new ArrayList<>();
            Set<Integer> retry = Collections.synchronizedSet(new HashSet<>());
            for (int version : runParameters.getVersions()) {

                CompletableFuture currentFuture = execute(
                        runParameters,
                        listener,
                        params,
                        metrixConfig,
                        commonWorkingDir,
                        chunkCutter,
                        schemaName,
                        chunkCount,
                        chunkSize,
                        version,
                        commands,
                        retry);

                futures.add(currentFuture);
                if (!LF.equals(metrixParameters.getComputationType()) && futures.size() >= MAX_PARALLEL_LONG_COMPUTATIONS) {
                    waitForBatch(futures);
                }
            }

            if (!futures.isEmpty()) {
                waitForBatch(futures);
            }

            listener.onEnd();

            MetrixRunResult runResult = new MetrixRunResult();
            appLogger.log("[%s] Computing postprocessing timeseries", schemaName);
            runResult.setPostProcessingTimeSeries(getPostProcessingTimeSeries(metrixDslData, mappingConfig, resultStore));
            return runResult;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CompletableFuture execute(
            MetrixRunParameters runParameters,
            ResultListener listener,
            ComputationParameters params,
            MetrixConfig metrixConfig,
            WorkingDirectory commonWorkingDir,
            ChunkCutter chunkCutter,
            String schemaName,
            int chunkCount,
            int chunkSize,
            int version,
            List<Command> commands,
            Set<Integer> retry) {
        ExecutionEnvironment execEnv = ExecutionEnvironment.createDefault()
                .setDebug(metrixConfig.isDebug())
                .setWorkingDirPrefix("metrix-" + version + "-");

        return computationManager.execute(execEnv,
                new AbstractExecutionHandler<Void>() {
                    @Override
                    public List<CommandExecution> before(Path workingDir) throws IOException {

                        // split time series into chunks
                        splitTimeSeries(workingDir, version, chunkSize, schemaName);

                        List<InputFile> inputFiles = new ArrayList<>();
                        String[] filenamesGz = new String[]{
                            NETWORK_XIIDM_GZ,
                            MAPPING_CONFIG_JSON_GZ,
                            MAPPING_PARAMETERS_JSON_GZ,
                            METRIX_DSL_DATA_JSON_GZ,
                            METRIX_PARAMETERS_JSON_GZ,
                            CONTINGENCIES_JSON_GZ,
                            REMEDIAL_ACTIONS_CSV_GZ
                        };
                        for (String fileNameGz : filenamesGz) {
                            Path fileGz = commonWorkingDir.toPath().resolve(fileNameGz);
                            if (Files.exists(fileGz)) {
                                Files.copy(fileGz, workingDir.resolve(fileNameGz));
                                inputFiles.add(new InputFile(fileNameGz, FilePreProcessor.FILE_GUNZIP));
                            }
                        }
                        inputFiles.add(new InputFile(chunk -> getInputTimeSeriesFileNameGz(version, chunk), FilePreProcessor.FILE_GUNZIP));

                        List<OutputFile> outputFiles = new ArrayList<>(2);
                        outputFiles.add(new OutputFile(chunk -> getOutputTimeSeriesFileName(version, chunk), FilePostProcessor.FILE_GZIP));
                        if (logArchive != null) {
                            outputFiles.add(new OutputFile(chunk -> getLogFileName(version, chunk), FilePostProcessor.FILE_GZIP));
                        }

                        com.powsybl.computation.Command command = new SimpleCommandBuilder()
                                .id(METRIX_CHUNK_ID + version)
                                .program("itools")
                                .args(chunk -> {
                                    Range<Integer> range = chunkCutter.getChunkRange(chunk);

                                    List<String> args = new ArrayList<>();
                                    args.add("metrix-chunk");

                                    args.add("--case-file");
                                    args.add(NETWORK_XIIDM);

                                    args.add("--mapping-config-file");
                                    args.add(MAPPING_CONFIG_JSON);

                                    args.add("--mapping-parameters-file");
                                    args.add(MAPPING_PARAMETERS_JSON);

                                    args.add("--metrix-parameters-file");
                                    args.add(METRIX_PARAMETERS_JSON);

                                    if (metrixDslReaderSupplier != null) {
                                        args.add("--metrix-dsl-data-file");
                                        args.add(METRIX_DSL_DATA_JSON);
                                    }

                                    if (contingenciesProvider != null) {
                                        args.add("--contingencies-file");
                                        args.add(CONTINGENCIES_JSON);
                                    }

                                    if (remedialActionsReaderSupplier != null) {
                                        args.add("--remedial-actions-file");
                                        args.add(REMEDIAL_ACTIONS_CSV);
                                    }

                                    args.add("--input-time-series-json-file");
                                    args.add(getInputTimeSeriesFileName(version, chunk));

                                    args.add("--version");
                                    args.add(Integer.toString(version));

                                    if (runParameters.isIgnoreLimits()) {
                                        args.add("--ignore-limits");
                                    }

                                    if (runParameters.isIgnoreEmptyFilter()) {
                                        args.add("--ignore-empty-filter");
                                    }

                                    args.add("--first-variant");
                                    args.add(Integer.toString(range.lowerEndpoint()));

                                    args.add("--variant-count");
                                    args.add(Integer.toString(range.upperEndpoint() - range.lowerEndpoint() + 1));

                                    args.add("--output-time-series-json-file");
                                    args.add(getOutputTimeSeriesFileName(version, chunk));

                                    if (logArchive != null) {
                                        args.add("--log-file");
                                        args.add(getLogFileName(version, chunk));
                                    }

                                    return args;
                                })
                                .inputFiles(inputFiles)
                                .outputFiles(outputFiles)
                                .build();

                        commands.add(command);
                        appLogger.log("Metrix computation scheduled for version %d", version);
                        return Collections.singletonList(new CommandExecution(command, chunkCount, 0));
                    }

                    @Override
                    public void onExecutionStart(CommandExecution execution, int chunk) {
                        appLogger.tagged("info")
                                .log("[%s] Metrix started on chunk %d of version %d", schemaName, chunk, version);
                    }

                    @Override
                    public void onExecutionCompletion(CommandExecution execution, int chunk) {
                        appLogger.tagged("info")
                                .log("[%s] Metrix complete on chunk %d of version %d", schemaName, chunk, version);
                    }

                    @Override
                    public Void after(Path workingDir, ExecutionReport report) {
                        if (!report.getErrors().isEmpty()) {
                            List<Integer> chunks = report.getErrors().stream().map(ExecutionError::getIndex).collect(Collectors.toList());
                            LOGGER.error("Metrix failed on chunk(s) {} of version {}", chunks, version);
                            appLogger.tagged("error").log("Metrix failed on chunk(s) %s of version %s", chunks, version);
                        }

                        appLogger.tagged("info")
                                .log("[%s] Merging %d chunks results of version %d", schemaName, chunkCount, version);

                        Stopwatch stopwatch = Stopwatch.createStarted();

                        listener.onVersionResultBegin(version);

                        for (int chunk = 0; chunk < chunkCount; chunk++) {
                            Path outputTimeSeriesFileGz = workingDir.resolve(getOutputTimeSeriesFileNameGz(version, chunk));

                            // Add log to archive
                            if (logArchive != null) {
                                try {
                                    addLogToArchive(workingDir.resolve(getErrFileName(version, chunk)), logArchive);
                                    addLogToArchive(workingDir.resolve(getOutFileName(version, chunk)), logArchive);
                                    addLogToArchive(workingDir.resolve(getLogFileNameGz(version, chunk)), logArchive);
                                } catch (IOException e) {
                                    LOGGER.error(e.toString(), e);
                                    appLogger.tagged("info")
                                            .log("[%s] Log file not found for chunk %d of version %d", schemaName, chunkCount, version);
                                }
                            }

                            if (!Files.exists(outputTimeSeriesFileGz)) {
                                LOGGER.warn("Output time series chunk {} of version {} not found", chunk, version);
                                appLogger.tagged("warn").log("Output time series chunk %s of version %s not found", chunk, version);
                                retry.add(version);
                                continue;
                            }

                            try (Reader reader = new InputStreamReader(new GZIPInputStream(new BufferedInputStream(Files.newInputStream(outputTimeSeriesFileGz))), StandardCharsets.UTF_8)) {
                                List<TimeSeries> timeSeriesList = TimeSeries.parseJson(reader);
                                listener.onChunkResult(version, chunk, timeSeriesList);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }

                        listener.onVersionResultEnd(version);

                        appLogger.tagged("performance")
                                .log("[%s] Output time series of version %d merged in %d ms", schemaName, version, stopwatch.elapsed(TimeUnit.MILLISECONDS));

                        return null;
                    }
                }, params);
    }

    private void waitForBatch(List<CompletableFuture> futures) {
        BatchCompletableFuture combined = new BatchCompletableFuture(futures.toArray(new CompletableFuture[]{}));
        updateTask.accept(combined);
        combined.join();
        updateTask.accept(null);
        futures.clear();
    }

    private static String getErrFileName(int version, int chunk) {
        return METRIX_CHUNK_ID + version + "_" + chunk + ".err";
    }

    private static String getOutFileName(int version, int chunk) {
        return METRIX_CHUNK_ID + version + "_" + chunk + ".out";
    }

    private static String getLogFileName(int version, int chunk) {
        return LOG_FILE_PREFIX + "_" + version + "_" + chunk + ".txt";
    }

    private static String getLogFileNameGz(int version, int chunk) {
        return getLogFileName(version, chunk) + ".gz";
    }

    private static void addLogToArchive(Path logFile, ZipOutputStream logArchive) throws IOException {
        if (Files.exists(logFile)) {
            boolean gzipped = logFile.getFileName().toString().endsWith(".gz");
            String fileName = gzipped ? logFile.getFileName().toString().substring(0, logFile.getFileName().toString().length() - 3)
                    : logFile.getFileName().toString();
            ZipEntry zipEntry = new ZipEntry(fileName);
            logArchive.putNextEntry(zipEntry);
            try (InputStream is = gzipped ? new GZIPInputStream(Files.newInputStream(logFile))
                    : Files.newInputStream(logFile)) {
                ByteStreams.copy(is, logArchive);
            }
            logArchive.closeEntry();
        }
    }

    // Post-processing methods
    static Map<String, NodeCalc> getPostProcessingTimeSeries(MetrixDslData dslData,
                                                             TimeSeriesMappingConfig mappingConfig,
                                                             ReadOnlyTimeSeriesStore store) {
        Map<String, NodeCalc> postProcessingTimeSeries = new HashMap<>();
        if (dslData != null && mappingConfig != null) {
            Map<String, NodeCalc> calculatedTimeSeries = new HashMap<>(mappingConfig.getTimeSeriesNodes());
            createBasecasePostprocessingTimeSeries(dslData, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store);
            createOutagePostprocessingTimeSeries(dslData, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store);
        }
        return postProcessingTimeSeries;
    }

    private static NodeCalc createLoadTimeSeries(NodeCalc flowTimeSeries, NodeCalc ratingTimeSeries) {
        return BinaryOperation.multiply(BinaryOperation.div(flowTimeSeries, ratingTimeSeries), new FloatNodeCalc(100));
    }

    private static NodeCalc createLoadTimeSeries(NodeCalc flowTimeSeries, NodeCalc ratingTimeSeriesOrEx, NodeCalc ratingTimeSeriesExOr) {
        if (ratingTimeSeriesOrEx == ratingTimeSeriesExOr) {
            return createLoadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx);
        } else {
            NodeCalc negativeRatingTimeSeries = UnaryOperation.negative(ratingTimeSeriesExOr);
            NodeCalc zero = new IntegerNodeCalc(0);
            NodeCalc ratingTimeSeries = BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(flowTimeSeries, zero), ratingTimeSeriesOrEx),
                    BinaryOperation.multiply(BinaryOperation.lessThan(flowTimeSeries, zero), negativeRatingTimeSeries));
            return createLoadTimeSeries(flowTimeSeries, ratingTimeSeries);
        }
    }

    private static NodeCalc createOverloadTimeSeries(NodeCalc flowTimeSeries, NodeCalc ratingTimeSeriesOrEx, NodeCalc ratingTimeSeriesExOr) {
        NodeCalc positiveOverloadTimeSeries = BinaryOperation.minus(flowTimeSeries, ratingTimeSeriesOrEx);
        NodeCalc negativeRatingTimeSeries = UnaryOperation.negative(ratingTimeSeriesExOr);
        NodeCalc negativeOverloadTimeSeries = BinaryOperation.minus(flowTimeSeries, negativeRatingTimeSeries);
        return BinaryOperation.plus(BinaryOperation.multiply(BinaryOperation.greaterThan(flowTimeSeries, ratingTimeSeriesOrEx), positiveOverloadTimeSeries),
                BinaryOperation.multiply(BinaryOperation.lessThan(flowTimeSeries, negativeRatingTimeSeries), negativeOverloadTimeSeries));
    }

    private static NodeCalc createOverallOverloadTimeSeries(NodeCalc basecaseOverloadTimeSeries, NodeCalc outageOverloadTimeSeries) {
        return BinaryOperation.plus(UnaryOperation.abs(basecaseOverloadTimeSeries), UnaryOperation.abs(outageOverloadTimeSeries));
    }

    private static void createBasecasePostprocessingTimeSeries(MetrixDslData metrixDslData,
                                                               TimeSeriesMappingConfig mappingConfig,
                                                               Map<String, NodeCalc> postProcessingTimeSeries,
                                                               Map<String, NodeCalc> calculatedTimeSeries,
                                                               ReadOnlyTimeSeriesStore store) {

        for (String branch : metrixDslData.getBranchMonitoringNList()) {
            MetrixInputData.MonitoringType branchMonitoringN = metrixDslData.getBranchMonitoringN(branch);
            if (branchMonitoringN == MetrixInputData.MonitoringType.MONITORING) {
                createBasecasePostprocessingTimeSeries(branch, MetrixVariable.thresholdN, MetrixVariable.thresholdNEndOr, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store);
            } else if (branchMonitoringN == MetrixInputData.MonitoringType.RESULT && mappingConfig.getTimeSeriesName(new MappingKey(MetrixVariable.analysisThresholdN, branch)) != null) {
                createBasecasePostprocessingTimeSeries(branch, MetrixVariable.analysisThresholdN, MetrixVariable.analysisThresholdNEndOr, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store);
            }
        }
    }

    private static void createBasecasePostprocessingTimeSeries(String branch,
                                                               MetrixVariable thresholdN,
                                                               MetrixVariable thresholdNEndOr,
                                                               TimeSeriesMappingConfig mappingConfig,
                                                               Map<String, NodeCalc> postProcessingTimeSeries,
                                                               Map<String, NodeCalc> calculatedTimeSeries,
                                                               ReadOnlyTimeSeriesStore store) {
        try {
            if (!store.timeSeriesExists(MetrixOutputData.FLOW_NAME + branch)) {
                LOGGER.debug("FLOW time-series not found for {}", branch);
                return;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating basecase postprocessing time-series for {}", branch);
            }
            NodeCalc flowTimeSeries = new TimeSeriesNameNodeCalc(MetrixOutputData.FLOW_NAME + branch);
            String ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(thresholdN, branch));
            NodeCalc ratingTimeSeriesOrEx = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);

            NodeCalc ratingTimeSeriesExOr = ratingTimeSeriesOrEx;
            if (thresholdNEndOr != null) {
                ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(thresholdNEndOr, branch));
                if (ratingTimeSeriesName != null) {
                    ratingTimeSeriesExOr = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);
                }
            }

            // Basecase load
            postProcessingTimeSeries.put(BASECASE_LOAD_PREFIX + branch, createLoadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx, ratingTimeSeriesExOr));
            // Basecase overload
            postProcessingTimeSeries.put(BASECASE_OVERLOAD_PREFIX + branch, createOverloadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx, ratingTimeSeriesExOr));
        } catch (IllegalStateException ise) {
            LOGGER.debug("Monitored branch {} not found in network", branch);
        }

    }

    private static void createOutagePostprocessingTimeSeries(MetrixDslData metrixDslData,
                                                             TimeSeriesMappingConfig mappingConfig,
                                                             Map<String, NodeCalc> postProcessingTimeSeries,
                                                             Map<String, NodeCalc> calculatedTimeSeries,
                                                             ReadOnlyTimeSeriesStore store) {

        for (String branch : metrixDslData.getBranchMonitoringNkList()) {
            MetrixInputData.MonitoringType branchMonitoringNk = metrixDslData.getBranchMonitoringNk(branch);
            if (branchMonitoringNk == MetrixInputData.MonitoringType.MONITORING) {
                createOutagePostprocessingTimeSeries(branch, MetrixVariable.thresholdN1, MetrixVariable.thresholdN1EndOr, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store);
            } else if (branchMonitoringNk == MetrixInputData.MonitoringType.RESULT && mappingConfig.getTimeSeriesName(new MappingKey(MetrixVariable.analysisThresholdNk, branch)) != null) {
                createOutagePostprocessingTimeSeries(branch, MetrixVariable.analysisThresholdNk, MetrixVariable.analysisThresholdNkEndOr, mappingConfig, postProcessingTimeSeries, calculatedTimeSeries, store);
            }
        }
    }

    private static void createOutagePostprocessingTimeSeries(String branch,
                                                             MetrixVariable thresholdN1,
                                                             MetrixVariable thresholdN1EndOr,
                                                             TimeSeriesMappingConfig mappingConfig,
                                                             Map<String, NodeCalc> postProcessingTimeSeries,
                                                             Map<String, NodeCalc> calculatedTimeSeries,
                                                             ReadOnlyTimeSeriesStore store) {
        try {

            if (!store.timeSeriesExists(MAX_THREAT_PREFIX + branch)) {
                LOGGER.debug("MAX_THREAT time-series not found for {}", branch);
                return;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating outage postprocessing time-series for {}", branch);
            }

            NodeCalc flowTimeSeries = new TimeSeriesNameNodeCalc(MAX_THREAT_PREFIX + branch);
            String ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(thresholdN1, branch));
            NodeCalc ratingTimeSeriesOrEx = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);

            NodeCalc ratingTimeSeriesExOr = ratingTimeSeriesOrEx;
            if (thresholdN1EndOr != null) {
                ratingTimeSeriesName = mappingConfig.getTimeSeriesName(new MappingKey(thresholdN1EndOr, branch));
                if (ratingTimeSeriesName != null) {
                    ratingTimeSeriesExOr = calculatedTimeSeries.computeIfAbsent(ratingTimeSeriesName, TimeSeriesNameNodeCalc::new);

                }
            }

            // Outage load
            postProcessingTimeSeries.put(OUTAGE_LOAD_PREFIX + branch, createLoadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx, ratingTimeSeriesExOr));
            // Outage overload
            NodeCalc outageOverLoadTimeSeries = createOverloadTimeSeries(flowTimeSeries, ratingTimeSeriesOrEx, ratingTimeSeriesExOr);
            postProcessingTimeSeries.put(OUTAGE_OVERLOAD_PREFIX + branch, outageOverLoadTimeSeries);
            NodeCalc basecaseOverLoadTimeSeries = postProcessingTimeSeries.get(BASECASE_OVERLOAD_PREFIX + branch);
            if (!Objects.isNull(basecaseOverLoadTimeSeries)) {
                postProcessingTimeSeries.put(OVERALL_OVERLOAD_PREFIX + branch, createOverallOverloadTimeSeries(basecaseOverLoadTimeSeries, outageOverLoadTimeSeries));
            }
        } catch (IllegalStateException ise) {
            LOGGER.debug("Monitored branch {} not found in network", branch);
        }
    }

}
