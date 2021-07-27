/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.zip.ZipOutputStream;

public class MetrixMock extends AbstractMetrix {
    public MetrixMock(NetworkSource networkSource, ContingenciesProvider contingenciesProvider,
                      Supplier<Reader> mappingReaderSupplier, Supplier<Reader> metrixDslReaderSupplier,
                      Supplier<Reader> remedialActionsReaderSupplier, ReadOnlyTimeSeriesStore store,
                      ReadOnlyTimeSeriesStore resultStore, ZipOutputStream logArchive, ComputationManager computationManager,
                      MetrixAppLogger appLogger, Consumer<Future<?>> updateTask, Writer logWriter, Consumer<MetrixDslData> onResult) {
        super(networkSource, contingenciesProvider, mappingReaderSupplier, metrixDslReaderSupplier, remedialActionsReaderSupplier,
                store, resultStore, logArchive, computationManager, appLogger, updateTask, logWriter, onResult);
    }

    @Override
    protected void executeMetrixChunks(NetworkSource network, MetrixRunParameters runParameters, ResultListener listener, MetrixConfig metrixConfig, MetrixParameters metrixParameters, WorkingDirectory commonWorkingDir, ChunkCutter chunkCutter, int chunkCount, int chunkSize) throws IOException {

    }
}
