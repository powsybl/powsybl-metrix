/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class DefaultNetworkSourceImpl implements NetworkSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNetworkSourceImpl.class);

    private final Path caseFile;
    private final ComputationManager computationManager;

    public DefaultNetworkSourceImpl(Path caseFile, ComputationManager computationManager) {
        this.caseFile = Objects.requireNonNull(caseFile);
        this.computationManager = Objects.requireNonNull(computationManager);
    }

    @Override
    public Network copy() {
        return Importers.loadNetwork(caseFile, computationManager, ImportConfig.load(), null);
    }

    @Override
    public void write(OutputStream os) {
        try {
            Files.copy(caseFile, os);
        } catch (IOException e) {
            LOGGER.error("Failed to copy network {}", caseFile);
            throw new UncheckedIOException(e);
        }
    }
}
