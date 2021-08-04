/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.google.common.collect.Range;
import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.xml.NetworkXml;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.metrix.integration.metrix.MetrixChunkParam;
import com.powsybl.metrix.integration.metrix.MetrixAnalysisResult;
import com.powsybl.metrix.mapping.MappingParameters;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;
import com.powsybl.timeseries.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipOutputStream;

public class Metrix extends AbstractMetrix {

    private static final Logger LOGGER = LoggerFactory.getLogger(Metrix.class);

    public Metrix(ContingenciesProvider contingenciesProvider, Reader remedialActionsReader,
                  ReadOnlyTimeSeriesStore store, ReadOnlyTimeSeriesStore resultStore,
                  ZipOutputStream logArchive, ComputationManager computationManager,
                  MetrixAppLogger logger, MetrixAnalysisResult analysisResult) {
        super(
            contingenciesProvider,
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
            WorkingDirectory commonWorkingDir,
            ChunkCutter chunkCutter,
            int chunkCount,
            int chunkSize) {

        List<CompletableFuture> futures = new ArrayList<>();
        for (int version : runParameters.getVersions()) {

            for (int chunk = 0; chunk < chunkCount; chunk++) {
                final int chunkNum = chunk;
                Network networkPoint = null;
                Path networkPointFile = null;
                if (runParameters.isNetworkComputation() && chunk == chunkCount - 1) {
                    networkPointFile = commonWorkingDir.toPath().resolve(NETWORK_POINT_XIIDM);
                    networkPoint = NetworkXml.read(networkPointFile);
                }
                MetrixChunkParam metrixChunkParam = new MetrixChunkParam.MetrixChunkParamBuilder().simpleInit(version, runParameters.isIgnoreLimits(),
                        runParameters.isIgnoreEmptyFilter(), contingenciesProvider, networkPointFile,
                        commonWorkingDir.toPath().resolve(getLogFileName(version, chunk)),
                        commonWorkingDir.toPath().resolve(getLogDetailFileNameFormat(version, chunk)),
                        remedialActionsReader != null ? commonWorkingDir.toPath().resolve(REMEDIAL_ACTIONS_CSV_GZ) : null).build();
                MetrixChunk metrixChunk = new MetrixChunk(network, computationManager, metrixChunkParam, metrixConfig, null);
                Range<Integer> range = chunkCutter.getChunkRange(chunk);
                MetrixVariantProvider variantProvider = new MetrixTimeSeriesVariantProvider(network, store, MappingParameters.load(),
                        mappingConfig, metrixDslData, metrixChunkParam, range, System.err);
                CompletableFuture<List<TimeSeries>> currentFuture = metrixChunk.run(metrixParameters, metrixDslData, variantProvider);
                Network finalNetworkPoint = networkPoint;
                CompletableFuture<Void> info = currentFuture.thenAccept(out -> {
                    // Add log to archive
                    if (logArchive != null) {
                        try {
                            addLogToArchive(commonWorkingDir.toPath().resolve(getLogFileName(version, chunkNum)), logArchive);
                            addLogDetailToArchive(commonWorkingDir.toPath(), getLogDetailFileNameFormat(version, chunkNum), logArchive);
                        } catch (IOException e) {
                            LOGGER.error(e.toString(), e);
                            appLogger.tagged("info")
                                    .log("Log file not found for chunk %d of version %d", chunkCount, version);
                        }
                    }

                    listener.onChunkResult(version, chunkNum, out, finalNetworkPoint);
                });
                futures.add(info);
            }
        }

        if (!futures.isEmpty()) {
            for (CompletableFuture future : futures) {
                future.join();
            }
        }
    }
}
