/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.metrix.integration;

import com.powsybl.commons.config.PlatformConfig;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Paul Bui-Quang {@literal <paul.buiquang at rte-france.com>}
 */
public class MetrixConfig {

    protected MetrixConfig() { }

    private static final Path DEFAULT_HOME_DIR = Paths.get(System.getProperty("user.home")).resolve(".metrix");
    private static final String DEFAULT_COMMAND = "metrix-simulator";
    private static final boolean DEFAULT_DEBUG = false;
    private static final boolean DEFAULT_CONSTANT_LOAD_FACTOR = false;
    private static final int DEFAULT_CHUNK_SIZE = 10;
    private static final int DEFAULT_RESULT_NUMBER_LIMIT = 10000;
    private static final int DEFAULT_DEBUG_LOG_LEVEL = 0;
    private static final int DEFAULT_LOG_LEVEL = 2;

    public static MetrixConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static MetrixConfig load(PlatformConfig platformConfig) {
        MetrixConfig metrixConfig = new MetrixConfig();
        platformConfig.getOptionalModuleConfig("metrix")
            .ifPresent(moduleConfig -> metrixConfig
                .setHomeDir(moduleConfig.getPathProperty("home-dir", DEFAULT_HOME_DIR))
                .setCommand(moduleConfig.getStringProperty("command", DEFAULT_COMMAND))
                .setDebug(moduleConfig.getBooleanProperty("debug", DEFAULT_DEBUG))
                .setConstantLossFactor(moduleConfig.getBooleanProperty("constant-loss-factor", DEFAULT_CONSTANT_LOAD_FACTOR))
                .setChunkSize(moduleConfig.getIntProperty("chunk-size", moduleConfig.getIntProperty("chunkSize", DEFAULT_CHUNK_SIZE)))
                .setResultNumberLimit(moduleConfig.getIntProperty("result-limit", moduleConfig.getIntProperty("resultLimit", DEFAULT_RESULT_NUMBER_LIMIT)))
                .setDebugLogLevel(moduleConfig.getIntProperty("debug-log-level", moduleConfig.getIntProperty("debugLogLevel", DEFAULT_DEBUG_LOG_LEVEL)))
                .setLogLevel(moduleConfig.getIntProperty("log-level", moduleConfig.getIntProperty("logLevel", DEFAULT_LOG_LEVEL)))
            );
        return metrixConfig;
    }

    private Path homeDir = DEFAULT_HOME_DIR;

    private String command = DEFAULT_COMMAND;

    private boolean debug = DEFAULT_DEBUG;

    private boolean constantLossFactor = DEFAULT_CONSTANT_LOAD_FACTOR;

    private int chunkSize = DEFAULT_CHUNK_SIZE;

    private int resultNumberLimit = DEFAULT_RESULT_NUMBER_LIMIT;

    private int debugLogLevel = DEFAULT_DEBUG_LOG_LEVEL;

    private int logLevel = DEFAULT_LOG_LEVEL;

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

    public Path getHomeDir() {
        return homeDir;
    }

    public MetrixConfig setHomeDir(Path homeDir) {
        this.homeDir = homeDir;
        return this;
    }

    public String getCommand() {
        return command;
    }

    public MetrixConfig setCommand(String command) {
        this.command = command;
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

    public int getLogLevel() {
        return logLevel;
    }

    public MetrixConfig setLogLevel(int logLevel) {
        this.logLevel = validateLogLevel(logLevel);
        return this;
    }

    public String logLevel() {
        int level = isDebug() ? getDebugLogLevel() : getLogLevel();
        String[] logLevels = new String[]{"trace", "debug", "info", "warning", "error", "critical"};
        if (level >= logLevels.length) {
            return "";
        }
        return logLevels[level];
    }
}
