/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.metrix.mapping;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;


/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian@rte-france.com>
 */
public class MappingConfig {

    private static final int DEFAULT_CHUNK_SIZE = 10;
    private static final int RESULT_NUMBER_LIMIT = 10000;

    public static MappingConfig load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static MappingConfig load(PlatformConfig platformConfig) {
        ModuleConfig moduleConfig = platformConfig.getModuleConfig("com/powsybl/metrix/mapping");

        int chunkSize = moduleConfig.getOptionalIntProperty("chunk-size")
            .orElseGet(() -> moduleConfig.getOptionalIntProperty("chunkSize")
                .orElse(DEFAULT_CHUNK_SIZE));

        int resultNumberLimit = moduleConfig.getOptionalIntProperty("result-limit")
            .orElseGet(() -> moduleConfig.getOptionalIntProperty("resultLimit")
                .orElse(RESULT_NUMBER_LIMIT));

        return new MappingConfig(chunkSize, resultNumberLimit);
    }

    private int chunkSize;

    private int resultNumberLimit;

    private static int validateChunkSize(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Invalid chunk size " + chunkSize);
        }
        return chunkSize;
    }

    public MappingConfig(int chunkSize, int resultNumberLimit) {
        this.chunkSize = validateChunkSize(chunkSize);
        this.resultNumberLimit = resultNumberLimit;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public MappingConfig setChunkSize(int chunkSize) {
        this.chunkSize = validateChunkSize(chunkSize);
        return this;
    }

    public int getResultNumberLimit() {
        return resultNumberLimit;
    }

    public MappingConfig setResultNumberLimit(int resultNumberLimit) {
        this.resultNumberLimit = resultNumberLimit;
        return this;
    }
}
