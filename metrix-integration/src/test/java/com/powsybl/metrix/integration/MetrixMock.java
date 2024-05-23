/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.commons.io.WorkingDirectory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.metrix.integration.io.ResultListener;
import com.powsybl.metrix.integration.metrix.MetrixAnalysisResult;
import com.powsybl.timeseries.ReadOnlyTimeSeriesStore;

import java.io.Reader;
import java.util.zip.ZipOutputStream;

public class MetrixMock extends AbstractMetrix {
    public MetrixMock(Reader remedialActionsReader, ReadOnlyTimeSeriesStore store,
                      ReadOnlyTimeSeriesStore resultStore, ZipOutputStream logArchive, ComputationManager computationManager,
                      MetrixAppLogger appLogger, MetrixAnalysisResult analysisResult) {
        super(remedialActionsReader, store, resultStore, logArchive, computationManager, appLogger, analysisResult);
    }

    @Override
    protected void executeMetrixChunks(MetrixRunParameters runParameters, ResultListener listener, MetrixConfig metrixConfig, WorkingDirectory commonWorkingDir, ChunkCutter chunkCutter, String schemaName) {
    }
}
