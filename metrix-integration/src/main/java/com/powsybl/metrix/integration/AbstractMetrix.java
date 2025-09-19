/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.io.ByteStreams;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.metrix.integration.chunk.ChunkCutter;
import com.powsybl.metrix.integration.dataGenerator.MetrixOutputData;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.metrix.integration.analysis.MetrixAnalysisResult;
import com.powsybl.metrix.mapping.config.TimeSeriesMappingConfigTableLoader;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.powsybl.metrix.integration.postprocessing.MetrixPostProcessingTimeSeries.getPostProcessingTimeSeries;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public abstract class AbstractMetrix {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetrix.class);

    protected static final String DEFAULT_SCHEMA_NAME = "default";

    protected static final String REMEDIAL_ACTIONS_CSV = "remedialActions.csv";

    protected static final String LOG_FILE_PREFIX = "log";
    protected static final String LOG_FILE_DETAIL_PREFIX = "metrix";

    public static final String MAX_THREAT_PREFIX = MetrixOutputData.MAX_THREAT_NAME + "1_FLOW_";

    protected final Reader remedialActionsReader;

    protected final ReadOnlyTimeSeriesStore store;

    protected final ReadOnlyTimeSeriesStore resultStore;

    protected final ZipOutputStream logArchive;

    protected final ComputationManager computationManager;

    protected final MetrixAppLogger appLogger;

    protected final MetrixAnalysisResult analysisResult;

    protected AbstractMetrix(Reader remedialActionsReader, ReadOnlyTimeSeriesStore store, ReadOnlyTimeSeriesStore resultStore,
                             ZipOutputStream logArchive, ComputationManager computationManager,
                             MetrixAppLogger appLogger, MetrixAnalysisResult analysisResult) {
        this.remedialActionsReader = remedialActionsReader;
        this.store = Objects.requireNonNull(store);
        this.resultStore = Objects.requireNonNull(resultStore);
        this.logArchive = logArchive;
        this.computationManager = Objects.requireNonNull(computationManager);
        this.appLogger = Objects.requireNonNull(appLogger);
        this.analysisResult = Objects.requireNonNull(analysisResult);
    }

    protected static int computeChunkSize(MetrixRunParameters runParameters, int chunkSizeFromConfig, TimeSeriesIndex index) {
        if (runParameters.getChunkSize() != -1 && runParameters.getChunkSize() < index.getPointCount()) {
            return runParameters.getChunkSize();
        }
        return chunkSizeFromConfig != -1 && chunkSizeFromConfig < index.getPointCount() ? chunkSizeFromConfig : index.getPointCount();
    }

    public MetrixRunResult run(MetrixRunParameters runParameters, ResultListener listener, String nullableSchemaName) {
        Objects.requireNonNull(runParameters);
        Objects.requireNonNull(listener);
        String schemaName = nullableSchemaName != null ? nullableSchemaName : DEFAULT_SCHEMA_NAME;

        MetrixConfig metrixConfig = MetrixConfig.load();

        if (analysisResult.metrixDslData() != null) {
            int estimatedResultNumber = analysisResult.metrixDslData().minResultNumberEstimate(analysisResult.metrixParameters());
            if (estimatedResultNumber > metrixConfig.getResultNumberLimit()) {
                throw new PowsyblException(String.format("Metrix configuration will produce more result time-series (%d) than the maximum allowed (%d).\n" +
                        "Reduce the number of monitored branches and/or number of contingencies.", estimatedResultNumber, metrixConfig.getResultNumberLimit()));
            }
        }

        if (runParameters.isNetworkComputation()) {
            analysisResult.metrixParameters().setWithAdequacyResults(true);
            analysisResult.metrixParameters().setWithRedispatchingResults(true);
        }

        TimeSeriesMappingConfigTableLoader loader = new TimeSeriesMappingConfigTableLoader(analysisResult.mappingConfig(), store);
        TimeSeriesIndex index = loader.checkIndexUnicity();
        loader.checkValues(runParameters.getVersions());
        ChunkCutter chunkCutter = initChunkCutter(runParameters, metrixConfig.getChunkSize(), index);

        LOGGER.info("Running metrix {} on network {}", analysisResult.metrixParameters().getComputationType(), analysisResult.network().getNameOrId());
        appLogger.log("[%s] Running metrix", schemaName);

        try (WorkingDirectory commonWorkingDir = new WorkingDirectory(computationManager.getLocalDir(), "metrix-commons-", metrixConfig.isDebug())) {

            executeMetrixChunks(
                    runParameters,
                    listener,
                    metrixConfig,
                    commonWorkingDir,
                    chunkCutter,
                    schemaName);

            addLogsToArchive(runParameters, commonWorkingDir, chunkCutter.getChunkCount(), chunkCutter.getChunkOffset());
            listener.onEnd();

            MetrixRunResult runResult = new MetrixRunResult();
            appLogger.log("[%s] Computing postprocessing timeseries", schemaName);
            runResult.setPostProcessingTimeSeries(getPostProcessingTimeSeries(analysisResult.metrixDslData(), analysisResult.mappingConfig(), analysisResult.contingencies(), resultStore, nullableSchemaName));
            return runResult;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ChunkCutter initChunkCutter(MetrixRunParameters runParameters, int chunkSizeFromConfig, TimeSeriesIndex index) {
        int firstVariant = computeFirstVariant(runParameters, index);
        int lastVariant = computeLastVariant(runParameters, index, firstVariant);
        int chunkSize = computeChunkSize(runParameters, chunkSizeFromConfig, index);
        return new ChunkCutter(firstVariant, lastVariant, chunkSize);
    }

    private int computeFirstVariant(MetrixRunParameters runParameters, TimeSeriesIndex index) {
        if (runParameters.getFirstVariant() == -1) {
            return 0;
        }
        if (runParameters.getFirstVariant() < 0 || runParameters.getFirstVariant() > index.getPointCount() - 1) {
            throw new IllegalArgumentException("First variant is out of range [0, "
                    + (index.getPointCount() - 1) + "]");
        }
        return runParameters.getFirstVariant();
    }

    private int computeLastVariant(MetrixRunParameters runParameters, TimeSeriesIndex index, int firstVariant) {
        if (runParameters.getVariantCount() == -1) {
            return index.getPointCount() - 1;
        }
        return Math.min(firstVariant + runParameters.getVariantCount() - 1, index.getPointCount() - 1);
    }

    private void addLogsToArchive(
            MetrixRunParameters runParameters,
            WorkingDirectory commonWorkingDir,
            int chunkCount,
            int chunkOffset
    ) {
        if (logArchive == null) {
            return;
        }
        for (int version : runParameters.getVersions()) {
            addVersionLogsToArchive(commonWorkingDir, chunkCount, chunkOffset, version);
        }
    }

    private void addVersionLogsToArchive(
            WorkingDirectory commonWorkingDir,
            int chunkCount,
            int chunkOffset,
            int version
    ) {
        for (int chunk = chunkOffset; chunk < chunkCount; chunk++) {
            try {
                addLogToArchive(commonWorkingDir.toPath().resolve(getLogFileName(version, chunk)), logArchive);
                addLogDetailToArchive(commonWorkingDir.toPath(), getLogDetailFileNameFormat(version, chunk), logArchive);
            } catch (IOException e) {
                LOGGER.error(e.toString(), e);
                appLogger.tagged("info")
                        .log("Log file not found for chunk %d of version %d", chunk, version);
            }
        }
    }

    protected abstract void executeMetrixChunks(
            MetrixRunParameters runParameters,
            ResultListener listener,
            MetrixConfig metrixConfig,
            WorkingDirectory commonWorkingDir,
            ChunkCutter chunkCutter,
            String schemaName
    ) throws IOException;

    protected static String getLogFileName(int version, int chunk) {
        return LOG_FILE_PREFIX + "_" + version + "_" + chunk + ".txt";
    }

    protected static String getLogDetailFileNameFormat(int version, int chunk) {
        return LOG_FILE_DETAIL_PREFIX + "%03d" + "_" + version + "_" + chunk + ".log";
    }

    protected static void addLogToArchive(Path logFile, ZipOutputStream logArchive) throws IOException {
        if (Files.exists(logFile)) {
            boolean gzipped = logFile.getFileName().toString().endsWith(".gz");
            String fileName = gzipped ? logFile.getFileName().toString().substring(0, logFile.getFileName().toString().length() - 3)
                    : logFile.getFileName().toString();
            ZipEntry zipEntry = new ZipEntry(fileName);
            logArchive.putNextEntry(zipEntry);
            try (InputStream is = gzipped ? new GZIPInputStream(Files.newInputStream(logFile))
                    : Files.newInputStream(logFile)) {
                ByteStreams.copy(is, logArchive);
            } finally {
                logArchive.closeEntry();
            }
        }
    }

    protected static void addLogDetailToArchive(Path workingDir, String logDetailFileNameFormat, ZipOutputStream logArchive) throws IOException {
        Path path;
        int i = 0;
        while (Files.exists(path = workingDir.resolve(String.format(logDetailFileNameFormat, i)))) {
            addLogToArchive(path, logArchive);
            i++;
        }
    }
}
