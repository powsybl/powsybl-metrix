/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.integration;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;

import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MetrixConfig {

    private static final boolean DEFAULT_DEBUG = false;
    private static final boolean DEFAULT_ANGLE_DE_PERTE_FIXE = false;
    private static final int DEFAULT_CHUNK_SIZE = 10;
    private static final int RESULT_NUMBER_LIMIT = 10000;
    private static final long COMPUTATION_RETRY_DELAY = 600000L;

    public static MetrixConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static MetrixConfig load(PlatformConfig platformConfig) {
        ModuleConfig moduleConfig = platformConfig.getModuleConfig("metrix");
        Path homeDir = moduleConfig.getPathProperty("home-dir");
        boolean debug = moduleConfig.getBooleanProperty("debug", DEFAULT_DEBUG);
        boolean angleDePerteFixe = moduleConfig.getOptionalBooleanProperty("angleDePerteFixe")
                                               .orElseGet(() -> moduleConfig.getOptionalBooleanProperty("constant-loss-factor")
                                                                            .orElse(DEFAULT_ANGLE_DE_PERTE_FIXE));

        int chunkSize = moduleConfig.getOptionalIntProperty("chunk-size")
            .orElseGet(() -> moduleConfig.getOptionalIntProperty("chunkSize")
                .orElse(DEFAULT_CHUNK_SIZE));

        int resultNumberLimit = moduleConfig.getOptionalIntProperty("result-limit")
            .orElseGet(() -> moduleConfig.getOptionalIntProperty("resultLimit")
                .orElse(RESULT_NUMBER_LIMIT));

        long computationRetryDelay = moduleConfig.getOptionalLongProperty("computation-retry-delay")
                .orElseGet(() -> moduleConfig.getOptionalLongProperty("computation-retry-delay")
                        .orElse(COMPUTATION_RETRY_DELAY));

        return new MetrixConfig(homeDir, debug, angleDePerteFixe, chunkSize, resultNumberLimit, computationRetryDelay);
    }

    private Path homeDir;

    private boolean debug;

    private boolean angleDePerteFixe;

    private int chunkSize;

    private int resultNumberLimit;

    private long computationRetryDelay;

    private static int validateChunkSize(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Invalid chunk size " + chunkSize);
        }
        return chunkSize;
    }

    public MetrixConfig(Path homeDir, boolean debug, boolean angleDePerteFixe, int chunkSize, int resultNumberLimit, long computationRetryDelay) {
        this.homeDir = Objects.requireNonNull(homeDir);
        this.debug = debug;
        this.angleDePerteFixe = angleDePerteFixe;
        this.chunkSize = validateChunkSize(chunkSize);
        this.resultNumberLimit = resultNumberLimit;
        this.computationRetryDelay = computationRetryDelay;
    }

    public Path getHomeDir() {
        return homeDir;
    }

    public MetrixConfig setHomeDir(Path homeDir) {
        this.homeDir = homeDir;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public MetrixConfig setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isAngleDePerteFixe() {
        return angleDePerteFixe;
    }

    public MetrixConfig setAngleDePerteFixe(boolean angleDePerteFixe) {
        this.angleDePerteFixe = angleDePerteFixe;
        return this;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public MetrixConfig setChunkSize(int chunkSize) {
        this.chunkSize = validateChunkSize(chunkSize);
        return this;
    }

    public int getResultNumberLimit() {
        return resultNumberLimit;
    }

    public MetrixConfig setResultNumberLimit(int resultNumberLimit) {
        this.resultNumberLimit = resultNumberLimit;
        return this;
    }

    public long getComputationRetryDelay() {
        return computationRetryDelay;
    }
}
