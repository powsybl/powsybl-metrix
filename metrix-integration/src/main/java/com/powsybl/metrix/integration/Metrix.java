/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.google.common.io.CharStreams;
import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.metrix.integration.metrix.MetrixAnalysisResult;
import com.powsybl.metrix.integration.metrix.MetrixChunkParam;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.tools.ToolRunningContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipOutputStream;

public class Metrix extends AbstractMetrix {

    private final PrintStream out;

    public Metrix(Reader remedialActionsReader, ReadOnlyTimeSeriesStore store, ReadOnlyTimeSeriesStore resultStore,
                  ZipOutputStream logArchive, ToolRunningContext context,
                  MetrixAppLogger logger, MetrixAnalysisResult analysisResult) {
        super(
            remedialActionsReader,
            store,
            resultStore,
            logArchive,
            context.getLongTimeExecutionComputationManager(),
            logger,
            analysisResult
        );
        this.out = context.getErrorStream();
    }

    @Override
    protected void executeMetrixChunks(
            MetrixRunParameters runParameters,
            ResultListener listener,
            MetrixConfig metrixConfig,
            WorkingDirectory commonWorkingDir,
            ChunkCutter chunkCutter,
            String schemaName) {

        if (remedialActionsReader != null) {
            try (BufferedReader bufferedReader = new BufferedReader(remedialActionsReader);
                 Writer writer = new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(commonWorkingDir.toPath().resolve(REMEDIAL_ACTIONS_CSV))), StandardCharsets.UTF_8)) {
                CharStreams.copy(bufferedReader, writer);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        List<CompletableFuture<?>> futures = executeVersions(analysisResult, runParameters, listener, metrixConfig, commonWorkingDir, chunkCutter);
        if (futures.isEmpty()) {
            return;
        }

        for (CompletableFuture<?> future : futures) {
            future.join();
        }
    }

    private List<CompletableFuture<?>> executeVersions(
            MetrixAnalysisResult analysisResult,
            MetrixRunParameters runParameters,
            ResultListener listener,
            MetrixConfig metrixConfig,
            WorkingDirectory commonWorkingDir,
            ChunkCutter chunkCutter) {

        List<CompletableFuture<?>> allFutures = new ArrayList<>();
        for (int version : runParameters.getVersions()) {
            List<CompletableFuture<?>> versionFutures = executeVersion(analysisResult, runParameters, listener, metrixConfig, commonWorkingDir, chunkCutter, version);
            allFutures.addAll(versionFutures);
        }
        return allFutures;
    }

    private List<CompletableFuture<?>> executeVersion(
        MetrixAnalysisResult analysisResult,
        MetrixRunParameters runParameters,
        ResultListener listener,
        MetrixConfig metrixConfig,
        WorkingDirectory commonWorkingDir,
        ChunkCutter chunkCutter,
        int version) {

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int chunk = chunkCutter.getChunkOffset(); chunk < chunkCutter.getChunkCount(); chunk++) {
            final int chunkNum = chunk;
            ContingenciesProvider contingenciesProvider = network -> analysisResult.contingencies;
            MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder().simpleInit(version, runParameters.isIgnoreLimits(),
                    runParameters.isIgnoreEmptyFilter(), contingenciesProvider, null,
                    commonWorkingDir.toPath().resolve(getLogFileName(version, chunk)),
                    commonWorkingDir.toPath().resolve(getLogDetailFileNameFormat(version, chunk)),
                    remedialActionsReader != null ? commonWorkingDir.toPath().resolve(REMEDIAL_ACTIONS_CSV) : null).build();
            MetrixChunk metrixChunk = new MetrixChunk(NetworkSerDe.copy(analysisResult.network), computationManager, metrixChunkParam, metrixConfig, null);
            Range<Integer> range = chunkCutter.getChunkRange(chunk);
            MetrixVariantProvider variantProvider = new MetrixTimeSeriesVariantProvider(analysisResult.network, store, analysisResult.mappingParameters,
                    analysisResult.mappingConfig, analysisResult.metrixDslData, metrixChunkParam, range, out);
            CompletableFuture<List<TimeSeries>> currentFuture = metrixChunk.run(analysisResult.metrixParameters, analysisResult.metrixDslData, variantProvider);
            CompletableFuture<Void> info = currentFuture.thenAccept(timeSeriesList ->
                    listener.onChunkResult(version, chunkNum, timeSeriesList, null)
            );
            futures.add(info);
        }
        return futures;
    }
}
