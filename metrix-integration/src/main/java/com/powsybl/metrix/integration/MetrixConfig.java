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

public class MetrixConfig {

    protected  MetrixConfig() { }

    private static final boolean DEFAULT_DEBUG = false;
    private static final boolean DEFAULT_CONSTANT_LOAD_FACTOR = false;
    private static final int DEFAULT_CHUNK_SIZE = 10;
    private static final int RESULT_NUMBER_LIMIT = 10000;
    private static final int DEFAULT_DEBUG_LOG_LEVEL = 0;
    private static final int DEFAULT_LOG_LEVEL = 2;

    public static MetrixConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static MetrixConfig load(PlatformConfig platformConfig) {
        ModuleConfig moduleConfig = platformConfig.getOptionalModuleConfig("metrix")
                .orElseThrow(() -> new IllegalStateException("Metrix module configuration could not be found"));
        Path homeDir = moduleConfig.getPathProperty("home-dir");
        boolean debug = moduleConfig.getBooleanProperty("debug", DEFAULT_DEBUG);
        boolean constantLossFactor = moduleConfig.getOptionalBooleanProperty("constant-loss-factor").orElse(DEFAULT_CONSTANT_LOAD_FACTOR);

        int chunkSize = moduleConfig.getOptionalIntProperty("chunk-size")
                .orElseGet(() -> moduleConfig.getOptionalIntProperty("chunkSize")
                        .orElse(DEFAULT_CHUNK_SIZE));

        int resultNumberLimit = moduleConfig.getOptionalIntProperty("result-limit")
                .orElseGet(() -> moduleConfig.getOptionalIntProperty("resultLimit")
                        .orElse(RESULT_NUMBER_LIMIT));

        int debugLogLevel = moduleConfig.getOptionalIntProperty("debug-log-level")
                .orElseGet(() -> moduleConfig.getOptionalIntProperty("debugLogLevel")
                        .orElse(DEFAULT_DEBUG_LOG_LEVEL));

        int noDebugLogLevel = moduleConfig.getOptionalIntProperty("log-level")
                .orElseGet(() -> moduleConfig.getOptionalIntProperty("logLevel")
                        .orElse(DEFAULT_LOG_LEVEL));

        return new MetrixConfig(homeDir, debug, constantLossFactor, chunkSize, resultNumberLimit, debugLogLevel, noDebugLogLevel);
    }

    private Path homeDir;

    private boolean debug;

    private boolean constantLossFactor;

    private int chunkSize;

    private int resultNumberLimit;

    private int debugLogLevel;

    private int noDebugLogLevel;

    private static int validateChunkSize(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Invalid chunk size " + chunkSize);
        }
        return chunkSize;
    }

    private static int validateLogLevel(int logLevel) {
        if (logLevel < 0 || logLevel > 5) {
            throw new IllegalArgumentException("Invalid loglevel " + logLevel);
        }
        return logLevel;
    }

    public MetrixConfig(Path homeDir, boolean debug, boolean constantLossFactor, int chunkSize, int resultNumberLimit, int debugLogLevel, int noDebugLogLevel) {
        this.homeDir = Objects.requireNonNull(homeDir);
        this.debug = debug;
        this.constantLossFactor = constantLossFactor;
        this.chunkSize = validateChunkSize(chunkSize);
        this.resultNumberLimit = resultNumberLimit;
        this.debugLogLevel = validateLogLevel(debugLogLevel);
        this.noDebugLogLevel = validateLogLevel(noDebugLogLevel);
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

    public boolean isConstantLossFactor() {
        return constantLossFactor;
    }

    public MetrixConfig setConstantLossFactor(boolean constantLossFactor) {
        this.constantLossFactor = constantLossFactor;
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

    public int getDebugLogLevel() {
        return debugLogLevel;
    }

    public MetrixConfig setDebugLogLevel(int debugLogLevel) {
        this.debugLogLevel = validateLogLevel(debugLogLevel);
        return this;
    }

    public int getNoDebugLogLevel() {
        return noDebugLogLevel;
    }

    public MetrixConfig setNoDebugLogLevel(int noDebugLogLevel) {
        this.noDebugLogLevel = validateLogLevel(noDebugLogLevel);
        return this;
    }

    public String logLevel() {
        int logLevel = isDebug() ? getDebugLogLevel() : getNoDebugLogLevel();
        String[] logLevels = new String[]{"trace", "debug", "info", "warning", "error", "critical"};
        if (logLevel >= logLevels.length) {
            return "";
        }
        return logLevels[logLevel];
    }
}
