/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.google.common.io.CharStreams;
import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.metrix.integration.metrix.MetrixChunkParam;
import com.powsybl.metrix.integration.metrix.MetrixAnalysisResult;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeries;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipOutputStream;

public class Metrix extends AbstractMetrix {

    public Metrix(Reader remedialActionsReader, ReadOnlyTimeSeriesStore store, ReadOnlyTimeSeriesStore resultStore,
                  ZipOutputStream logArchive, ComputationManager computationManager,
                  MetrixAppLogger logger, MetrixAnalysisResult analysisResult) {
        super(
            remedialActionsReader,
            store,
            resultStore,
            logArchive,
            computationManager,
            logger,
            analysisResult
        );
    }

    @Override
    protected void executeMetrixChunks(
            Network network,
            MetrixRunParameters runParameters,
            ResultListener listener,
            MetrixConfig metrixConfig,
            MetrixParameters metrixParameters,
            MappingParameters mappingParameters,
            WorkingDirectory commonWorkingDir,
            ChunkCutter chunkCutter,
            String schemaName,
            int chunkCount,
            int chunkSize,
            int chunkOffset) {

        if (remedialActionsReader != null) {
            try (BufferedReader bufferedReader = new BufferedReader(remedialActionsReader);
                 Writer writer = new OutputStreamWriter(new BufferedOutputStream(Files.newOutputStream(commonWorkingDir.toPath().resolve(REMEDIAL_ACTIONS_CSV))), StandardCharsets.UTF_8)) {
                CharStreams.copy(bufferedReader, writer);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int version : runParameters.getVersions()) {

            for (int chunk = chunkOffset; chunk < chunkCount; chunk++) {
                final int chunkNum = chunk;
                MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder().simpleInit(version, runParameters.isIgnoreLimits(),
                        runParameters.isIgnoreEmptyFilter(), contingenciesProvider, null,
                        commonWorkingDir.toPath().resolve(getLogFileName(version, chunk)),
                        commonWorkingDir.toPath().resolve(getLogDetailFileNameFormat(version, chunk)),
                        remedialActionsReader != null ? commonWorkingDir.toPath().resolve(REMEDIAL_ACTIONS_CSV) : null).build();
                MetrixChunk metrixChunk = new MetrixChunk(NetworkXml.copy(network), computationManager, metrixChunkParam, metrixConfig, null);
                Range<Integer> range = chunkCutter.getChunkRange(chunk);
                MetrixVariantProvider variantProvider = new MetrixTimeSeriesVariantProvider(network, store, mappingParameters,
                        mappingConfig, metrixDslData, metrixChunkParam, range, System.err);
                CompletableFuture<List<TimeSeries>> currentFuture = metrixChunk.run(metrixParameters, metrixDslData, variantProvider);
                CompletableFuture<Void> info = currentFuture.thenAccept(timeSeriesList ->
                        listener.onChunkResult(version, chunkNum, timeSeriesList, null)
                );
                futures.add(info);
            }
        }

        if (!futures.isEmpty()) {
            for (CompletableFuture<?> future : futures) {
                future.join();
            }
        }
    }
}
