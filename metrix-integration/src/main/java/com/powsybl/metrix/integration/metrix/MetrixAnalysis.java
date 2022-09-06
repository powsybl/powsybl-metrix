/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.metrix.integration.metrix;

import com.google.common.base.Stopwatch;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.metrix.integration.*;
import com.powsybl.metrix.integration.exceptions.MappingScriptLoadingException;
import com.powsybl.metrix.integration.exceptions.MetrixScriptLoadingException;
import com.powsybl.metrix.integration.io.MetrixConfigResult;
import com.powsybl.metrix.integration.utils.LocalThreadExecutor;
import com.powsybl.metrix.mapping.ComputationRange;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.metrix.mapping.TimeSeriesDslLoader;
import com.powsybl.metrix.mapping.TimeSeriesMappingConfig;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.ast.NodeCalc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MetrixAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetrixAnalysis.class);
    private static final String DEFAULT_SCHEMA_NAME = "default";
    public static final String TIME_SERIES_MAPPING_LOADING = "[%s] Time series mapping loaded in %d ms";
    public static final String TIME_SERIES_MAPPING_LOADING_ERROR = "[%s] Error loading Time series mapping after %d ms";
    public static final String METRIX_DSL_DATA_LOADING = "[%s] Metrix dsl data loaded in %d ms";
    public static final String METRIX_DSL_DATA_LOADING_ERROR = "[%s] Error loading Metrix dsl data after %d ms";

    private final NetworkSource networkSource;
    private final Reader mappingReader;
    private final Reader metrixDslReader;
    private final Reader remedialActionsReader;
    private final ContingenciesProvider contingenciesProvider;
    private final ReadOnlyTimeSeriesStore store;
    private final MetrixAppLogger appLogger;
    private final Consumer<Future<?>> updateTask;
    private final Writer scriptLogWriter;
    private final Writer inputLogWriter;
    private final String schemaName;
    private final ComputationRange computationRange;

    public MetrixAnalysis(NetworkSource networkSource, Reader mappingReader,
                          Reader metrixDslReader, Reader remedialActionsReader, ContingenciesProvider contingenciesProvider,
                          ReadOnlyTimeSeriesStore store, MetrixAppLogger logger, ComputationRange computationRange) {
        this(networkSource, mappingReader, metrixDslReader, remedialActionsReader, contingenciesProvider, store, logger, ignore -> {
        }, null, null, null, computationRange);
    }

    public MetrixAnalysis(NetworkSource networkSource, Reader mappingReader, Reader metrixDslReader,
                          Reader remedialActionsReader, ContingenciesProvider contingenciesProvider, ReadOnlyTimeSeriesStore store, MetrixAppLogger appLogger,
                          Consumer<Future<?>> updateTask, Writer scriptLogWriter, Writer inputLogWriter, String schemaName, ComputationRange computationRange) {
        this.networkSource = Objects.requireNonNull(networkSource);
        this.mappingReader = Objects.requireNonNull(mappingReader);
        this.metrixDslReader = metrixDslReader;
        this.remedialActionsReader = remedialActionsReader;
        this.contingenciesProvider = contingenciesProvider;
        this.store = store;
        this.appLogger = appLogger;
        this.updateTask = updateTask;
        this.scriptLogWriter = scriptLogWriter;
        this.inputLogWriter = inputLogWriter;
        this.schemaName = schemaName != null ? schemaName : DEFAULT_SCHEMA_NAME;
        this.computationRange = computationRange;
    }

    public MetrixAnalysisResult runAnalysis(String id) {
        MetrixParameters metrixParameters = MetrixParameters.load();
        MappingParameters mappingParameters = MappingParameters.load();

        Network network = networkSource.copy();

        try (BufferedWriter scriptLogBufferedWriter = scriptLogWriter != null ? new BufferedWriter(scriptLogWriter) : null;
             BufferedWriter inputLogBufferedWriter = inputLogWriter != null ? new BufferedWriter(inputLogWriter) : null) {
            TimeSeriesMappingConfig mappingConfig = loadMappingConfig(mappingReader, network, mappingParameters, scriptLogBufferedWriter, id);
            Map<String, NodeCalc> timeSeriesNodesAfterMapping = new HashMap<>(mappingConfig.getTimeSeriesNodes());
            MetrixDslData metrixDslData = null;
            Map<String, NodeCalc> timeSeriesNodesAfterMetrix = null;
            if (metrixDslReader != null) {
                metrixDslData = loadMetrixDslData(metrixDslReader, network, metrixParameters, mappingConfig, scriptLogBufferedWriter, id);
                timeSeriesNodesAfterMetrix = new HashMap<>(mappingConfig.getTimeSeriesNodes());
            }
            MetrixInputAnalysisResult inputs = new MetrixInputAnalysis(remedialActionsReader, contingenciesProvider, network, metrixDslData, inputLogBufferedWriter).runAnalysis();
            MetrixConfigResult metrixConfigResult = new MetrixConfigResult(timeSeriesNodesAfterMapping, timeSeriesNodesAfterMetrix);
            return new MetrixAnalysisResult(metrixDslData, mappingConfig, network, metrixParameters, mappingParameters, metrixConfigResult, inputs.getContingencies(), inputs.getRemedials());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TimeSeriesMappingConfig loadMappingConfig(Reader mappingReader, Network network, MappingParameters mappingParameters, Writer writer, String id) {
        appLogger.tagged("info")
                .log("[%s] Loading time series mapping...", schemaName);
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean inError = false;
        try {
            CompletableFuture<TimeSeriesMappingConfig> mappingFuture = new LocalThreadExecutor<TimeSeriesMappingConfig>("Script_TS_" + id)
                    .supplyAsync(() -> TimeSeriesDslLoader.load(mappingReader, network, mappingParameters, store, writer, computationRange));
            updateTask.accept(mappingFuture);
            return mappingFuture.get();
        } catch (ExecutionException e) {
            inError = true;
            throw new MappingScriptLoadingException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Mapping has been interrupted!", e);
            inError = true;
            Thread.currentThread().interrupt();
            throw new RuntimeException(e.getMessage());
        } finally {
            appLogger.tagged("performance")
                    .log(inError ? TIME_SERIES_MAPPING_LOADING_ERROR : TIME_SERIES_MAPPING_LOADING, schemaName, stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
            updateTask.accept(null);
        }
    }

    private MetrixDslData loadMetrixDslData(Reader metrixDslReader, Network network, MetrixParameters metrixParameters, TimeSeriesMappingConfig mappingConfig, Writer writer, String id) {
        appLogger.tagged("info")
                .log("[%s] Loading metrix dsl...", schemaName);
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean inError = false;
        try {
            CompletableFuture<MetrixDslData> metrixFuture = new LocalThreadExecutor<MetrixDslData>("Script_M_" + id)
                    .supplyAsync(() -> MetrixDslDataLoader.load(metrixDslReader, network, metrixParameters, store, mappingConfig, writer));
            updateTask.accept(metrixFuture);
            MetrixDslData metrixDslData = metrixFuture.get();
            metrixDslData.setComputationType(metrixParameters.getComputationType());
            return metrixDslData;
        } catch (ExecutionException e) {
            inError = true;
            throw new MetrixScriptLoadingException(e);
        } catch (InterruptedException e) {
            LOGGER.warn("Metrix dsl loading has been interrupted!", e);
            inError = true;
            Thread.currentThread().interrupt();
            return null;
        } finally {
            appLogger.tagged("performance")
                    .log(inError ? METRIX_DSL_DATA_LOADING_ERROR : METRIX_DSL_DATA_LOADING, schemaName, stopwatch.stop().elapsed(TimeUnit.MILLISECONDS));
            updateTask.accept(null);
        }
    }
}
